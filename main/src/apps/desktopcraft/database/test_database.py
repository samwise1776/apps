import http.client
import base64
import json
import tempfile
import threading
import unittest
from unittest.mock import MagicMock, patch
from pathlib import Path

from database import server


class DatabaseApiTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        server.DB_PATH = Path(self.temp.name) / "desktopcraft-test.db"
        server.initialize_database()
        with server.feedback_request_lock:
            server.feedback_request_times.clear()
        with server.helper_request_lock:
            server.helper_request_times.clear()
        self.httpd = server.ThreadingHTTPServer(("127.0.0.1", 0), server.DesktopcraftHandler)
        self.thread = threading.Thread(target=self.httpd.serve_forever, daemon=True)
        self.thread.start()
        self.connection = http.client.HTTPConnection("127.0.0.1", self.httpd.server_port, timeout=5)

    def tearDown(self):
        self.connection.close()
        self.httpd.shutdown()
        self.httpd.server_close()
        self.thread.join(timeout=2)
        self.temp.cleanup()

    def request(self, method, path, payload=None, cookie=None):
        headers = {"Content-Type": "application/json"}
        if cookie: headers["Cookie"] = cookie
        body = json.dumps(payload).encode() if payload is not None else None
        self.connection.request(method, path, body=body, headers=headers)
        response = self.connection.getresponse()
        data = json.loads(response.read() or b"{}")
        return response, data

    def test_account_progress_and_leaderboard(self):
        response, payload = self.request("POST", "/api/auth/register", {"name": "Ada Builder", "username": "ada_builder", "password": "desktop-pass"})
        self.assertEqual(response.status, 201)
        self.assertEqual(payload["user"]["username"], "ada_builder")
        self.assertIn("Max-Age=31536000", response.getheader("Set-Cookie"))
        self.assertEqual(response.getheader("X-Content-Type-Options"), "nosniff")
        self.assertEqual(response.getheader("X-Frame-Options"), "DENY")
        self.assertIn("frame-ancestors 'none'", response.getheader("Content-Security-Policy"))
        cookie = response.getheader("Set-Cookie").split(";", 1)[0]

        response, payload = self.request("GET", "/api/auth/session", cookie=cookie)
        self.assertEqual(response.status, 200)
        self.assertEqual(payload["user"]["username"], "ada_builder")

        response, payload = self.request("POST", "/api/auth/register", {"name": "Another Ada", "username": "ada_builder", "password": "another-pass"})
        self.assertEqual(response.status, 409)
        self.assertIn("already taken", payload["error"])

        response, payload = self.request("PUT", "/api/progress", {"courseId": "java-swing", "activeLesson": 3, "completed": [0, 1, 3]}, cookie)
        self.assertEqual(response.status, 200)
        self.assertEqual(payload["xp"], 300)

        response, payload = self.request("GET", "/api/progress", cookie=cookie)
        self.assertEqual(response.status, 200)
        self.assertEqual(payload["courses"][0]["completed"], [0, 1, 3])

        response, payload = self.request("POST", "/api/quiz-attempts", {"courseId": "java-swing", "lessonIndex": 3, "score": 17}, cookie)
        self.assertEqual(response.status, 201)
        self.assertTrue(payload["ok"])

        response, payload = self.request("GET", "/api/leaderboard")
        self.assertEqual(response.status, 200)
        self.assertEqual(payload["entries"][0]["courses"]["java-swing"]["xp"], 300)

        response, payload = self.request("POST", "/api/auth/login", {"username": "ada_builder", "password": "desktop-pass"})
        self.assertEqual(response.status, 200)
        self.assertEqual(payload["user"]["name"], "Ada Builder")

        response, payload = self.request("POST", "/api/auth/logout", {}, cookie)
        self.assertEqual(response.status, 200)
        self.assertEqual(response.getheader("Set-Cookie").split(";", 1)[0], "desktopcraft_session=")

        response, payload = self.request("GET", "/api/progress", cookie=cookie)
        self.assertEqual(response.status, 401)
        self.assertIn("Sign in", payload["error"])

        with patch.dict("os.environ", {"OPENAI_API_KEY": ""}):
            response, payload = self.request("GET", "/api/helper/status")
            self.assertEqual(response.status, 200)
            self.assertFalse(payload["available"])

            response, payload = self.request("POST", "/api/helper", {"message": "Explain JFrame", "courseId": "java-swing"})
            self.assertEqual(response.status, 503)
            self.assertIn("not configured", payload["error"])

        self.assertEqual(
            server.extract_openai_text({"output": [{"type": "message", "content": [{"type": "output_text", "text": "Course answer"}]}]}),
            "Course answer",
        )

    def test_feedback_is_saved_and_email_delivery_is_requested(self):
        feedback_path = Path(self.temp.name) / "texta.txt"
        with (
            patch.dict("os.environ", {"DESKTOPCRAFT_FEEDBACK_FILE": str(feedback_path)}),
            patch.object(server, "send_feedback_email") as send_email,
        ):
            response, payload = self.request("POST", "/api/feedback", {
                "name": "Ada Builder",
                "email": "ada@example.com",
                "message": "Please add a lesson about accessible dialogs.",
                "website": "",
            })

        self.assertEqual(response.status, 201)
        self.assertTrue(payload["saved"])
        self.assertTrue(payload["emailed"])
        saved = feedback_path.read_text(encoding="utf-8")
        self.assertIn("Ada Builder", saved)
        self.assertIn("ada@example.com", saved)
        self.assertIn("Please add a lesson about accessible dialogs.", saved)
        send_email.assert_called_once()

    def test_authenticated_helper_returns_agent_identity_and_tool_trace(self):
        response, _ = self.request("POST", "/api/auth/register", {
            "name": "Agent Tester", "username": "agent_tester", "password": "desktop-pass",
        })
        cookie = response.getheader("Set-Cookie").split(";", 1)[0]
        agent_result = server.ai_agent.AgentResult(
            answer="Use SwingWorker, then verify the button stays responsive.",
            model="gpt-5.6-sol-test", tools=["lookup_course_reference", "analyze_code"],
        )
        with patch.dict("os.environ", {"OPENAI_API_KEY": "test-key"}), patch.object(server, "call_openai_helper", return_value=agent_result):
            response, payload = self.request("POST", "/api/helper", {
                "message": "Why does this window freeze?", "courseId": "java-swing",
                "explanationLevel": "balanced", "history": [],
            }, cookie)
        self.assertEqual(response.status, 200)
        self.assertTrue(payload["agent"])
        self.assertEqual(payload["model"], "gpt-5.6-sol-test")
        self.assertEqual(payload["toolsUsed"], ["lookup_course_reference", "analyze_code"])

    def test_feedback_email_uses_starttls_and_configured_recipient(self):
        submission = {
            "name": "Ada Builder",
            "email": "ada@example.com",
            "message": "A useful suggestion.",
            "source_address": "127.0.0.1",
            "received_at": "2026-07-21T12:00:00+00:00",
        }
        smtp_session = MagicMock()
        smtp_context = MagicMock()
        smtp_context.__enter__.return_value = smtp_session
        smtp_factory = MagicMock(return_value=smtp_context)
        settings = {
            "DESKTOPCRAFT_FEEDBACK_TO": "owner@example.com",
            "DESKTOPCRAFT_SMTP_HOST": "smtp.example.com",
            "DESKTOPCRAFT_SMTP_PORT": "587",
            "DESKTOPCRAFT_SMTP_USER": "owner@example.com",
            "DESKTOPCRAFT_SMTP_PASSWORD": "app-password",
            "DESKTOPCRAFT_SMTP_FROM": "owner@example.com",
            "DESKTOPCRAFT_SMTP_SECURITY": "starttls",
        }
        with patch.dict("os.environ", settings), patch.object(server.smtplib, "SMTP", smtp_factory):
            server.send_feedback_email(submission)

        smtp_factory.assert_called_once()
        smtp_session.starttls.assert_called_once()
        smtp_session.login.assert_called_once_with("owner@example.com", "app-password")
        sent_message = smtp_session.send_message.call_args.args[0]
        self.assertEqual(sent_message["To"], "owner@example.com")
        self.assertEqual(sent_message["Reply-To"], "ada@example.com")

    def test_people_can_publish_and_download_only_free_apps(self):
        response, payload = self.request("POST", "/api/apps", {
            "title": "Guest App",
            "description": "This must not publish anonymously.",
            "toolkit": "python",
            "sourceCode": "print('guest app cannot publish')",
        })
        self.assertEqual(response.status, 401)

        response, _ = self.request("POST", "/api/auth/register", {
            "name": "Ada Builder",
            "username": "ada_apps",
            "password": "desktop-pass",
        })
        cookie = response.getheader("Set-Cookie").split(";", 1)[0]
        source = "import tkinter as tk\nroot = tk.Tk()\nroot.title('Free Notes')\nroot.mainloop()"
        response, payload = self.request("POST", "/api/apps", {
            "title": "Free Notes",
            "description": "A small notes window shared with every builder for free.",
            "toolkit": "python",
            "sourceCode": source,
            "price": 999,
        }, cookie)
        self.assertEqual(response.status, 201)
        app_id = payload["app"]["id"]
        self.assertTrue(payload["app"]["isFree"])
        self.assertEqual(payload["app"]["price"], 0)
        self.assertEqual(payload["app"]["fileName"], "FreeNotes.py")

        response, payload = self.request("GET", "/api/apps", cookie=cookie)
        self.assertEqual(response.status, 200)
        self.assertTrue(payload["freeOnly"])
        self.assertEqual(len(payload["apps"]), 1)
        self.assertNotIn("sourceCode", payload["apps"][0])
        self.assertTrue(payload["apps"][0]["isOwner"])

        self.connection.request("GET", f"/api/apps/{app_id}/download")
        download = self.connection.getresponse()
        downloaded_source = download.read().decode("utf-8")
        self.assertEqual(download.status, 200)
        self.assertEqual(download.getheader("Content-Disposition"), 'attachment; filename="FreeNotes.py"')
        self.assertEqual(downloaded_source, source)

        response, payload = self.request("GET", "/api/apps")
        self.assertEqual(payload["apps"][0]["downloads"], 1)

        package_data = b"!<arch>\nDesktopcraft test package"
        response, payload = self.request("POST", "/api/apps", {
            "title": "Packaged Notes",
            "description": "An installable package created by Desktopcraft IDE.",
            "toolkit": "java",
            "sourceCode": "public class PackagedNotes { public static void main(String[] args) {} }",
            "packageFileName": "packaged-notes.deb",
            "packageBase64": base64.b64encode(package_data).decode(),
        }, cookie)
        self.assertEqual(response.status, 201)
        self.assertTrue(payload["app"]["package"])
        self.assertEqual(payload["app"]["fileName"], "packaged-notes.deb")
        package_id = payload["app"]["id"]
        self.connection.request("GET", f"/api/apps/{package_id}/download")
        package_download = self.connection.getresponse()
        self.assertEqual(package_download.status, 200)
        self.assertEqual(package_download.getheader("Content-Type"), "application/vnd.debian.binary-package")
        self.assertEqual(package_download.getheader("Content-Disposition"), 'attachment; filename="packaged-notes.deb"')
        self.assertEqual(package_download.read(), package_data)

        response, payload = self.request("DELETE", f"/api/apps/{app_id}", {}, cookie)
        self.assertEqual(response.status, 200)
        self.assertTrue(payload["ok"])


if __name__ == "__main__":
    unittest.main()
