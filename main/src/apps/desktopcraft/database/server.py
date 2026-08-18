#!/usr/bin/env python3
"""Desktopcraft static site and SQLite API server."""

from __future__ import annotations

import argparse
import base64
import hashlib
import hmac
import json
import os
import re
import secrets
import smtplib
import sqlite3
import ssl
import threading
import time
import urllib.error
import urllib.request
from contextlib import contextmanager
from datetime import datetime, timezone
from email.message import EmailMessage
from http import cookies
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any
from urllib.parse import unquote, urlparse

try:
    from . import ai_agent
except ImportError:
    import ai_agent

PROJECT_ROOT = Path(__file__).resolve().parents[1]


def load_local_environment(path: Path) -> None:
    """Load simple KEY=value settings without replacing real environment variables."""
    if not path.is_file():
        return
    for line_number, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[7:].lstrip()
        if "=" not in line:
            raise ValueError(f"Invalid .env line {line_number}: expected KEY=value.")
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip()
        if not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", key):
            raise ValueError(f"Invalid .env variable name on line {line_number}.")
        if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
            value = value[1:-1]
        os.environ.setdefault(key, value)


load_local_environment(PROJECT_ROOT / ".env")

SITE_ROOT = Path(os.environ.get("DESKTOPCRAFT_SITE_ROOT", PROJECT_ROOT / "dist")).resolve()
DB_PATH = Path(os.environ.get("DESKTOPCRAFT_DB_PATH", PROJECT_ROOT / "database" / "desktopcraft.db")).resolve()
FEEDBACK_FILE_DEFAULT = PROJECT_ROOT / "texta.txt"
SESSION_COOKIE = "desktopcraft_session"
# Keep remembered accounts signed in for a year unless the learner explicitly signs out.
SESSION_SECONDS = 60 * 60 * 24 * 365
MAX_BODY_BYTES = 6_000_000
COURSE_IDS = {"java-swing", "python-tkinter", "csharp-winforms", "cpp-qt", "javascript-electron"}
COMMUNITY_TOOLKITS = {
    "java": {"label": "Java Swing", "extension": "java"},
    "python": {"label": "Python Tkinter", "extension": "py"},
    "csharp": {"label": "C# WinForms", "extension": "cs"},
    "cpp": {"label": "C++ Qt Widgets", "extension": "cpp"},
    "electron": {"label": "JavaScript Electron", "extension": "js"},
}
USERNAME_PATTERN = re.compile(r"^[a-z0-9_.-]{3,24}$")
HELPER_LEVELS = {"concise", "balanced", "detailed"}
HELPER_COURSE_CONTEXT = {
    "java-swing": "Java Swing: JFrame, Swing controls, layout managers, event listeners, models, SwingWorker, and the Event Dispatch Thread.",
    "python-tkinter": "Python Tkinter: Tk, ttk widgets, pack, grid, command callbacks, variables, Treeview, after, and the Tk event loop.",
    "csharp-winforms": "C# WinForms: Form, controls, events, layout panels, ErrorProvider, data binding, timers, and async/await.",
    "cpp-qt": "C++ Qt Widgets: QApplication, widgets, layouts, signals and slots, model/view, QTimer, QThread, and the Qt event loop.",
    "javascript-electron": "JavaScript Electron: main and renderer processes, BrowserWindow, DOM events, contextBridge, IPC, native dialogs, and secure file access.",
}
HELPER_REQUESTS_PER_MINUTE = 12
helper_request_times: dict[int, list[int]] = {}
helper_request_lock = threading.Lock()
FEEDBACK_REQUESTS_PER_MINUTE = 5
FEEDBACK_EMAIL_PATTERN = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")
feedback_request_times: dict[str, list[int]] = {}
feedback_request_lock = threading.Lock()
feedback_file_lock = threading.Lock()


class FeedbackEmailNotConfigured(RuntimeError):
    """Raised when the server cannot deliver feedback through SMTP yet."""


def now() -> int:
    return int(time.time())


def connect(database_path: Path | None = None) -> sqlite3.Connection:
    connection = sqlite3.connect(database_path or DB_PATH, timeout=10)
    connection.row_factory = sqlite3.Row
    connection.execute("PRAGMA foreign_keys = ON")
    connection.execute("PRAGMA busy_timeout = 5000")
    return connection


@contextmanager
def database_connection(database_path: Path | None = None):
    database = connect(database_path)
    try:
        with database:
            yield database
    finally:
        database.close()


def initialize_database(database_path: Path | None = None) -> None:
    path = database_path or DB_PATH
    path.parent.mkdir(parents=True, exist_ok=True)
    schema = (Path(__file__).parent / "schema.sql").read_text(encoding="utf-8")
    with database_connection(path) as database:
        database.execute("PRAGMA journal_mode = WAL")
        database.executescript(schema)
        community_columns = {row["name"] for row in database.execute("PRAGMA table_info(community_apps)")}
        if "package_file_name" not in community_columns:
            database.execute("ALTER TABLE community_apps ADD COLUMN package_file_name TEXT")
        if "package_data" not in community_columns:
            database.execute("ALTER TABLE community_apps ADD COLUMN package_data BLOB")


def make_password(password: str) -> tuple[str, str]:
    salt = os.urandom(16)
    digest = hashlib.scrypt(password.encode("utf-8"), salt=salt, n=2**14, r=8, p=1, dklen=32)
    return base64.urlsafe_b64encode(digest).decode(), base64.urlsafe_b64encode(salt).decode()


def verify_password(password: str, encoded_digest: str, encoded_salt: str) -> bool:
    try:
        salt = base64.urlsafe_b64decode(encoded_salt.encode())
        expected = base64.urlsafe_b64decode(encoded_digest.encode())
        actual = hashlib.scrypt(password.encode("utf-8"), salt=salt, n=2**14, r=8, p=1, dklen=32)
        return hmac.compare_digest(actual, expected)
    except (ValueError, TypeError):
        return False


def token_hash(token: str) -> str:
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


def helper_available() -> bool:
    return bool(os.environ.get("OPENAI_API_KEY", "").strip())


def allow_helper_request(user_id: int) -> bool:
    timestamp = now()
    with helper_request_lock:
        recent = [value for value in helper_request_times.get(user_id, []) if value > timestamp - 60]
        if len(recent) >= HELPER_REQUESTS_PER_MINUTE:
            helper_request_times[user_id] = recent
            return False
        recent.append(timestamp)
        helper_request_times[user_id] = recent
        return True


def allow_feedback_request(address: str) -> bool:
    timestamp = now()
    with feedback_request_lock:
        for key in list(feedback_request_times):
            recent = [value for value in feedback_request_times[key] if value > timestamp - 60]
            if recent:
                feedback_request_times[key] = recent
            else:
                del feedback_request_times[key]
        recent = feedback_request_times.setdefault(address, [])
        if len(recent) >= FEEDBACK_REQUESTS_PER_MINUTE:
            return False
        recent.append(timestamp)
        return True


def feedback_file_path() -> Path:
    configured = os.environ.get("DESKTOPCRAFT_FEEDBACK_FILE", "").strip()
    if not configured:
        return FEEDBACK_FILE_DEFAULT
    path = Path(configured)
    return path if path.is_absolute() else PROJECT_ROOT / path


def feedback_email_settings() -> dict[str, Any]:
    recipients = [address.strip() for address in os.environ.get("DESKTOPCRAFT_FEEDBACK_TO", "").split(",") if address.strip()]
    host = os.environ.get("DESKTOPCRAFT_SMTP_HOST", "").strip()
    username = os.environ.get("DESKTOPCRAFT_SMTP_USER", "").strip()
    password = os.environ.get("DESKTOPCRAFT_SMTP_PASSWORD", "")
    sender = os.environ.get("DESKTOPCRAFT_SMTP_FROM", "").strip() or username or (recipients[0] if recipients else "")
    security = os.environ.get("DESKTOPCRAFT_SMTP_SECURITY", "starttls").strip().lower()

    if not recipients or not host or not sender:
        raise FeedbackEmailNotConfigured("Set DESKTOPCRAFT_FEEDBACK_TO, DESKTOPCRAFT_SMTP_HOST, and DESKTOPCRAFT_SMTP_FROM.")
    if any(not FEEDBACK_EMAIL_PATTERN.fullmatch(address) for address in [*recipients, sender]):
        raise FeedbackEmailNotConfigured("The feedback recipient and sender must be valid email addresses.")
    if bool(username) != bool(password):
        raise FeedbackEmailNotConfigured("Set both DESKTOPCRAFT_SMTP_USER and DESKTOPCRAFT_SMTP_PASSWORD, or leave both blank.")
    if security not in {"starttls", "ssl", "none"}:
        raise FeedbackEmailNotConfigured("DESKTOPCRAFT_SMTP_SECURITY must be starttls, ssl, or none.")
    try:
        port = int(os.environ.get("DESKTOPCRAFT_SMTP_PORT", "465" if security == "ssl" else "587"))
    except ValueError as exception:
        raise FeedbackEmailNotConfigured("DESKTOPCRAFT_SMTP_PORT must be a number.") from exception
    if not 1 <= port <= 65535:
        raise FeedbackEmailNotConfigured("DESKTOPCRAFT_SMTP_PORT must be between 1 and 65535.")
    return {
        "recipients": recipients,
        "host": host,
        "port": port,
        "username": username,
        "password": password,
        "sender": sender,
        "security": security,
    }


def feedback_email_available() -> bool:
    try:
        feedback_email_settings()
        return True
    except FeedbackEmailNotConfigured:
        return False


def feedback_text(submission: dict[str, str]) -> str:
    name = submission["name"] or "Not provided"
    email = submission["email"] or "Not provided"
    return (
        f"Received: {submission['received_at']}\n"
        f"Name: {name}\n"
        f"Reply email: {email}\n"
        f"Source address: {submission['source_address']}\n\n"
        f"Message:\n{submission['message']}\n"
    )


def append_feedback(submission: dict[str, str]) -> Path:
    path = feedback_file_path()
    path.parent.mkdir(parents=True, exist_ok=True)
    entry = f"\n{'=' * 72}\n{feedback_text(submission)}"
    with feedback_file_lock:
        descriptor = os.open(path, os.O_APPEND | os.O_CREAT | os.O_WRONLY, 0o600)
        try:
            try:
                os.chmod(path, 0o600)
            except OSError:
                pass
            with os.fdopen(descriptor, "a", encoding="utf-8") as output:
                descriptor = -1
                output.write(entry)
                output.flush()
                os.fsync(output.fileno())
        finally:
            if descriptor >= 0:
                os.close(descriptor)
    return path


def send_feedback_email(submission: dict[str, str]) -> None:
    settings = feedback_email_settings()
    email = EmailMessage()
    email["Subject"] = "New Desktopcraft feedback"
    email["From"] = settings["sender"]
    email["To"] = ", ".join(settings["recipients"])
    if submission["email"]:
        email["Reply-To"] = submission["email"]
    email.set_content(feedback_text(submission))

    context = ssl.create_default_context()
    if settings["security"] == "ssl":
        client_factory = smtplib.SMTP_SSL
        client_kwargs = {"context": context}
    else:
        client_factory = smtplib.SMTP
        client_kwargs = {}
    with client_factory(settings["host"], settings["port"], timeout=15, **client_kwargs) as client:
        if settings["security"] == "starttls":
            client.ehlo()
            client.starttls(context=context)
            client.ehlo()
        if settings["username"]:
            client.login(settings["username"], settings["password"])
        client.send_message(email, from_addr=settings["sender"], to_addrs=settings["recipients"])


def community_file_name(title: str, toolkit: str) -> str:
    words = re.findall(r"[A-Za-z0-9]+", title)
    stem = "".join(word[:1].upper() + word[1:] for word in words)[:60] or "DesktopcraftApp"
    if stem[0].isdigit():
        stem = f"App{stem}"
    return f"{stem}.{COMMUNITY_TOOLKITS[toolkit]['extension']}"


def public_community_app(row: sqlite3.Row, current_user_id: int | None = None) -> dict[str, Any]:
    package_file_name = row["package_file_name"]
    return {
        "id": row["id"],
        "title": row["title"],
        "description": row["description"],
        "toolkit": row["toolkit"],
        "toolkitLabel": COMMUNITY_TOOLKITS[row["toolkit"]]["label"],
        "fileName": package_file_name or row["file_name"],
        "sourceFileName": row["file_name"],
        "sourceBytes": row["source_bytes"],
        "package": bool(package_file_name),
        "downloads": row["download_count"],
        "createdAt": row["created_at"] * 1000,
        "updatedAt": row["updated_at"] * 1000,
        "creator": {"name": row["display_name"], "username": row["username"]},
        "isOwner": current_user_id == row["user_id"],
        "isFree": True,
        "price": 0,
        "downloadUrl": f"/api/apps/{row['id']}/download",
    }


def extract_openai_text(payload: dict[str, Any]) -> str:
    return ai_agent.extract_openai_text(payload)


def call_openai_helper(message: str, course_id: str, explanation_level: str, history: list[dict[str, str]]) -> ai_agent.AgentResult:
    return ai_agent.run_agent(
        message=message, course_id=course_id, explanation_level=explanation_level, history=history,
        api_key=os.environ["OPENAI_API_KEY"].strip(),
        model=os.environ.get("OPENAI_HELPER_MODEL", ai_agent.DEFAULT_MODEL).strip() or ai_agent.DEFAULT_MODEL,
        reasoning_effort=os.environ.get("OPENAI_HELPER_REASONING", "medium").strip().lower(),
    )


class DesktopcraftHandler(SimpleHTTPRequestHandler):
    server_version = "Desktopcraft/1.0"

    def __init__(self, *args: Any, **kwargs: Any) -> None:
        super().__init__(*args, directory=str(SITE_ROOT), **kwargs)

    def end_headers(self) -> None:
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Referrer-Policy", "strict-origin-when-cross-origin")
        self.send_header("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
        self.send_header("Content-Security-Policy", "default-src 'self'; connect-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'")
        self.send_header("Cross-Origin-Opener-Policy", "same-origin")
        self.send_header("X-Frame-Options", "DENY")
        super().end_headers()

    def log_message(self, message: str, *args: Any) -> None:
        print(f"[{self.log_date_time_string()}] {message % args}")

    def send_json(self, status: int, payload: dict[str, Any], extra_headers: dict[str, str] | None = None) -> None:
        data = json.dumps(payload, separators=(",", ":")).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        for key, value in (extra_headers or {}).items():
            self.send_header(key, value)
        self.end_headers()
        self.wfile.write(data)

    def read_json(self) -> dict[str, Any]:
        try:
            length = int(self.headers.get("Content-Length", "0"))
        except ValueError as exception:
            raise ValueError("Invalid request size.") from exception
        if length <= 0 or length > MAX_BODY_BYTES:
            raise ValueError("Request body is empty or too large.")
        try:
            payload = json.loads(self.rfile.read(length))
        except (json.JSONDecodeError, UnicodeDecodeError) as exception:
            raise ValueError("Request body must be valid JSON.") from exception
        if not isinstance(payload, dict):
            raise ValueError("Request body must be a JSON object.")
        return payload

    def session_token(self) -> str:
        jar = cookies.SimpleCookie(self.headers.get("Cookie", ""))
        return jar.get(SESSION_COOKIE).value if jar.get(SESSION_COOKIE) else ""

    def current_user(self) -> sqlite3.Row | None:
        token = self.session_token()
        if not token:
            return None
        timestamp = now()
        with database_connection() as database:
            database.execute("DELETE FROM sessions WHERE expires_at <= ?", (timestamp,))
            return database.execute(
                """SELECT users.id, users.username, users.display_name, users.created_at
                   FROM sessions JOIN users ON users.id = sessions.user_id
                   WHERE sessions.token_hash = ? AND sessions.expires_at > ?""",
                (token_hash(token), timestamp),
            ).fetchone()

    def require_user(self) -> sqlite3.Row | None:
        user = self.current_user()
        if not user:
            self.send_json(401, {"error": "Sign in to continue."})
        return user

    def create_session(self, user_id: int) -> tuple[str, int]:
        token = secrets.token_urlsafe(32)
        expires_at = now() + SESSION_SECONDS
        with database_connection() as database:
            database.execute(
                "INSERT INTO sessions(token_hash, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)",
                (token_hash(token), user_id, now(), expires_at),
            )
        return token, expires_at

    def session_cookie(self, token: str, max_age: int = SESSION_SECONDS) -> str:
        secure = "; Secure" if os.environ.get("DESKTOPCRAFT_SECURE_COOKIES", "").lower() in {"1", "true", "yes"} else ""
        return f"{SESSION_COOKIE}={token}; Path=/; HttpOnly; SameSite=Lax; Max-Age={max_age}{secure}"

    @staticmethod
    def public_user(user: sqlite3.Row) -> dict[str, Any]:
        return {"id": user["id"], "username": user["username"], "name": user["display_name"], "createdAt": user["created_at"] * 1000, "storageTier": "database"}

    def do_GET(self) -> None:
        path = urlparse(self.path).path
        if path == "/api/health":
            with database_connection() as database:
                user_count = database.execute("SELECT COUNT(*) FROM users").fetchone()[0]
            self.send_json(200, {"ok": True, "database": "sqlite", "users": user_count, "feedbackEmail": feedback_email_available()})
        elif path == "/api/auth/session":
            user = self.current_user()
            self.send_json(200, {"user": self.public_user(user) if user else None})
        elif path == "/api/helper/status":
            self.send_json(200, {"available": helper_available(), "signedIn": bool(self.current_user()), "agent": True,
                                 "model": os.environ.get("OPENAI_HELPER_MODEL", ai_agent.DEFAULT_MODEL),
                                 "tools": [tool["name"] for tool in ai_agent.TOOLS]})
        elif path == "/api/progress":
            user = self.require_user()
            if user:
                self.get_progress(user)
        elif path == "/api/leaderboard":
            self.get_leaderboard()
        elif path == "/api/apps":
            self.get_community_apps()
        elif match := re.fullmatch(r"/api/apps/(\d+)/download", path):
            self.download_community_app(int(match.group(1)))
        elif path.startswith("/api/"):
            self.send_json(404, {"error": "API route not found."})
        elif unquote(path) in {"/.env", "/.env.example", "/texta.txt"}:
            self.send_error(404)
        else:
            if path == "/": self.path = "/index.html"
            super().do_GET()

    def do_POST(self) -> None:
        path = urlparse(self.path).path
        try:
            if path == "/api/auth/register": self.register()
            elif path == "/api/auth/login": self.login()
            elif path == "/api/auth/logout": self.logout()
            elif path == "/api/feedback": self.feedback()
            elif path == "/api/helper": self.helper()
            elif path == "/api/quiz-attempts": self.save_quiz_attempt()
            elif path == "/api/apps": self.create_community_app()
            else: self.send_json(404, {"error": "API route not found."})
        except (ValueError, TypeError) as exception:
            self.send_json(400, {"error": str(exception)})

    def do_DELETE(self) -> None:
        path = urlparse(self.path).path
        try:
            if match := re.fullmatch(r"/api/apps/(\d+)", path):
                self.delete_community_app(int(match.group(1)))
            else:
                self.send_json(404, {"error": "API route not found."})
        except (ValueError, TypeError) as exception:
            self.send_json(400, {"error": str(exception)})

    def do_PUT(self) -> None:
        try:
            if urlparse(self.path).path == "/api/progress":
                user = self.require_user()
                if user: self.save_progress(user)
            else:
                self.send_json(404, {"error": "API route not found."})
        except (ValueError, TypeError) as exception:
            self.send_json(400, {"error": str(exception)})

    def register(self) -> None:
        payload = self.read_json()
        name = str(payload.get("name", "")).strip()
        username = str(payload.get("username", "")).strip().lower()
        password = str(payload.get("password", ""))
        if len(name) < 2 or len(name) > 80: raise ValueError("Enter a name with 2–80 characters.")
        if not USERNAME_PATTERN.fullmatch(username): raise ValueError("Use 3–24 letters, numbers, dots, dashes, or underscores for your username.")
        if len(password) < 8 or len(password) > 200: raise ValueError("Use 8–200 characters for your Desktopcraft password.")
        digest, salt = make_password(password)
        timestamp = now()
        try:
            with database_connection() as database:
                cursor = database.execute(
                    "INSERT INTO users(username, display_name, password_hash, password_salt, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                    (username, name, digest, salt, timestamp, timestamp),
                )
                user_id = cursor.lastrowid
                user = database.execute("SELECT id, username, display_name, created_at FROM users WHERE id = ?", (user_id,)).fetchone()
        except sqlite3.IntegrityError:
            self.send_json(409, {"error": "That Desktopcraft username is already taken."})
            return
        token, _ = self.create_session(user_id)
        self.send_json(201, {"user": self.public_user(user)}, {"Set-Cookie": self.session_cookie(token)})

    def login(self) -> None:
        payload = self.read_json()
        username = str(payload.get("username", "")).strip().lower()
        password = str(payload.get("password", ""))
        with database_connection() as database:
            user = database.execute(
                "SELECT id, username, display_name, password_hash, password_salt, created_at FROM users WHERE username = ?",
                (username,),
            ).fetchone()
        if not user or not verify_password(password, user["password_hash"], user["password_salt"]):
            self.send_json(401, {"error": "Desktopcraft username or password is incorrect."})
            return
        token, _ = self.create_session(user["id"])
        self.send_json(200, {"user": self.public_user(user)}, {"Set-Cookie": self.session_cookie(token)})

    def logout(self) -> None:
        token = self.session_token()
        if token:
            with database_connection() as database:
                database.execute("DELETE FROM sessions WHERE token_hash = ?", (token_hash(token),))
        self.send_json(200, {"ok": True}, {"Set-Cookie": self.session_cookie("", 0)})

    def feedback(self) -> None:
        payload = self.read_json()
        if str(payload.get("website", "")).strip():
            self.send_json(201, {"ok": True, "saved": True, "emailed": True, "message": "Thank you for the feedback."})
            return

        name = str(payload.get("name", "")).strip()
        email = str(payload.get("email", "")).strip()
        message = str(payload.get("message", "")).replace("\x00", "").strip()
        if len(name) > 80:
            raise ValueError("Name must be 80 characters or fewer.")
        if len(email) > 254 or (email and not FEEDBACK_EMAIL_PATTERN.fullmatch(email)):
            raise ValueError("Enter a valid reply email address or leave it blank.")
        if not 2 <= len(message) <= 5000:
            raise ValueError("Write a feedback message with 2–5,000 characters.")
        source_address = self.client_address[0]
        if not allow_feedback_request(source_address):
            self.send_json(429, {"error": "Too many feedback messages were sent from this address. Wait a minute and try again."})
            return

        submission = {
            "name": name.replace("\r", " ").replace("\n", " "),
            "email": email,
            "message": message,
            "source_address": source_address,
            "received_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        }
        try:
            append_feedback(submission)
        except OSError as exception:
            print(f"Feedback file error: {exception}")
            self.send_json(500, {"error": "Feedback could not be saved. Try again shortly."})
            return

        try:
            send_feedback_email(submission)
        except FeedbackEmailNotConfigured as exception:
            print(f"Feedback email is not configured: {exception}")
            self.send_json(202, {
                "ok": True,
                "saved": True,
                "emailed": False,
                "warning": "Your message was saved to texta.txt, but owner email delivery is not configured yet.",
            })
            return
        except (OSError, smtplib.SMTPException) as exception:
            print(f"Feedback email delivery failed: {exception}")
            self.send_json(502, {
                "error": "Feedback email delivery failed.",
                "saved": True,
                "emailed": False,
                "warning": "Your message was saved to texta.txt, but the email could not be delivered. The owner can still read the saved copy.",
            })
            return

        self.send_json(201, {
            "ok": True,
            "saved": True,
            "emailed": True,
            "message": "Thank you—your feedback was saved and emailed.",
        })

    def get_community_apps(self) -> None:
        user = self.current_user()
        with database_connection() as database:
            rows = database.execute(
                """SELECT community_apps.id, community_apps.user_id, community_apps.title,
                          community_apps.description, community_apps.toolkit, community_apps.file_name,
                          community_apps.package_file_name,
                          COALESCE(length(community_apps.package_data), length(CAST(community_apps.source_code AS BLOB))) AS source_bytes,
                          community_apps.download_count,
                          community_apps.created_at, community_apps.updated_at,
                          users.display_name, users.username
                   FROM community_apps JOIN users ON users.id = community_apps.user_id
                   ORDER BY community_apps.created_at DESC, community_apps.id DESC
                   LIMIT 500"""
            ).fetchall()
        current_user_id = int(user["id"]) if user else None
        self.send_json(200, {"apps": [public_community_app(row, current_user_id) for row in rows], "freeOnly": True})

    def create_community_app(self) -> None:
        user = self.require_user()
        if not user:
            return
        payload = self.read_json()
        title = str(payload.get("title", "")).strip()
        description = str(payload.get("description", "")).strip()
        toolkit = str(payload.get("toolkit", "")).strip().lower()
        source_code = str(payload.get("sourceCode", "")).replace("\x00", "").strip()
        package_file_name = str(payload.get("packageFileName", "")).strip().lower()
        package_base64 = str(payload.get("packageBase64", "")).strip()
        if not 3 <= len(title) <= 80:
            raise ValueError("Use 3–80 characters for the app title.")
        if not 10 <= len(description) <= 800:
            raise ValueError("Use 10–800 characters for the app description.")
        if toolkit not in COMMUNITY_TOOLKITS:
            raise ValueError("Choose a supported desktop toolkit.")
        source_bytes = len(source_code.encode("utf-8"))
        if not 20 <= source_bytes <= 100_000:
            raise ValueError("App source must be between 20 bytes and 100 KB.")
        package_data = None
        if package_base64 or package_file_name:
            if not re.fullmatch(r"[a-z0-9][a-z0-9+.-]{0,79}\.deb", package_file_name):
                raise ValueError("The package filename must be a lowercase Debian .deb filename.")
            try:
                package_data = base64.b64decode(package_base64, validate=True)
            except (ValueError, TypeError) as exception:
                raise ValueError("The Debian package is not valid base64 data.") from exception
            if not package_data.startswith(b"!<arch>\n"):
                raise ValueError("The uploaded file is not a Debian package.")
            if len(package_data) > 4_000_000:
                raise ValueError("The Debian package must be 4 MB or smaller.")

        timestamp = now()
        file_name = community_file_name(title, toolkit)
        with database_connection() as database:
            app_count = database.execute("SELECT COUNT(*) FROM community_apps WHERE user_id = ?", (user["id"],)).fetchone()[0]
            if app_count >= 100:
                self.send_json(409, {"error": "Each account can publish up to 100 community apps. Remove an older app before publishing another."})
                return
            cursor = database.execute(
                """INSERT INTO community_apps(user_id, title, description, toolkit, file_name, source_code,
                                               package_file_name, package_data, download_count, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)""",
                (user["id"], title, description, toolkit, file_name, source_code,
                 package_file_name or None, package_data, timestamp, timestamp),
            )
            row = database.execute(
                """SELECT community_apps.id, community_apps.user_id, community_apps.title,
                          community_apps.description, community_apps.toolkit, community_apps.file_name,
                          community_apps.package_file_name,
                          COALESCE(length(community_apps.package_data), length(CAST(community_apps.source_code AS BLOB))) AS source_bytes,
                          community_apps.download_count,
                          community_apps.created_at, community_apps.updated_at,
                          users.display_name, users.username
                   FROM community_apps JOIN users ON users.id = community_apps.user_id
                   WHERE community_apps.id = ?""",
                (cursor.lastrowid,),
            ).fetchone()
        self.send_json(201, {"app": public_community_app(row, int(user["id"])), "message": "Your app is published for free."})

    def download_community_app(self, app_id: int) -> None:
        with database_connection() as database:
            row = database.execute(
                "SELECT file_name, source_code, package_file_name, package_data FROM community_apps WHERE id = ?",
                (app_id,),
            ).fetchone()
            if not row:
                self.send_json(404, {"error": "Community app not found."})
                return
            database.execute("UPDATE community_apps SET download_count = download_count + 1 WHERE id = ?", (app_id,))

        data = bytes(row["package_data"]) if row["package_data"] is not None else row["source_code"].encode("utf-8")
        file_name = row["package_file_name"] or row["file_name"]
        content_type = "application/vnd.debian.binary-package" if row["package_data"] is not None else "text/plain; charset=utf-8"
        self.send_response(200)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Disposition", f"attachment; filename=\"{file_name}\"")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Security-Policy", "default-src 'none'; sandbox")
        self.end_headers()
        self.wfile.write(data)

    def delete_community_app(self, app_id: int) -> None:
        user = self.require_user()
        if not user:
            return
        with database_connection() as database:
            app = database.execute("SELECT user_id FROM community_apps WHERE id = ?", (app_id,)).fetchone()
            if not app:
                self.send_json(404, {"error": "Community app not found."})
                return
            if int(app["user_id"]) != int(user["id"]):
                self.send_json(403, {"error": "Only the creator can remove this app."})
                return
            database.execute("DELETE FROM community_apps WHERE id = ?", (app_id,))
        self.send_json(200, {"ok": True})

    def helper(self) -> None:
        if not helper_available():
            self.send_json(503, {"error": "Helper AI is not configured. The built-in course guide is still available."})
            return
        user = self.require_user()
        if not user:
            return
        if not allow_helper_request(int(user["id"])):
            self.send_json(429, {"error": "Helper AI is receiving too many questions. Wait a minute and try again."})
            return
        payload = self.read_json()
        message = str(payload.get("message", "")).strip()
        course_id = str(payload.get("courseId", ""))
        explanation_level = str(payload.get("explanationLevel", "balanced"))
        history_input = payload.get("history", [])
        if not 2 <= len(message) <= 2000:
            raise ValueError("Ask a question with 2–2,000 characters.")
        if course_id not in HELPER_COURSE_CONTEXT:
            raise ValueError("Choose a valid Desktopcraft course.")
        if explanation_level not in HELPER_LEVELS:
            explanation_level = "balanced"
        if not isinstance(history_input, list):
            raise ValueError("Helper history must be an array.")
        history: list[dict[str, str]] = []
        for item in history_input[-8:]:
            if not isinstance(item, dict) or item.get("role") not in {"user", "assistant"}:
                continue
            text = str(item.get("text", "")).strip()
            if text:
                history.append({"role": item["role"], "text": text[:2000]})
        try:
            result = call_openai_helper(message, course_id, explanation_level, history)
        except urllib.error.HTTPError as exception:
            status = 429 if exception.code == 429 else 502
            self.send_json(status, {"error": "Helper AI could not answer right now. Try again shortly."})
            return
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError, ValueError, KeyError):
            self.send_json(502, {"error": "Helper AI could not answer right now. Try again shortly."})
            return
        self.send_json(200, {"answer": result.answer, "agent": True, "model": result.model, "toolsUsed": result.tools})

    def get_progress(self, user: sqlite3.Row) -> None:
        with database_connection() as database:
            rows = database.execute(
                "SELECT course_id, active_lesson, completed_count, total_lessons, xp, updated_at FROM course_progress WHERE user_id = ?",
                (user["id"],),
            ).fetchall()
            completions = database.execute(
                "SELECT course_id, lesson_index FROM lesson_completions WHERE user_id = ? ORDER BY lesson_index",
                (user["id"],),
            ).fetchall()
        completed_by_course: dict[str, list[int]] = {course_id: [] for course_id in COURSE_IDS}
        for completion in completions: completed_by_course.setdefault(completion["course_id"], []).append(completion["lesson_index"])
        courses = [{
            "courseId": row["course_id"], "activeLesson": row["active_lesson"], "completed": completed_by_course.get(row["course_id"], []),
            "completedCount": row["completed_count"], "total": row["total_lessons"], "xp": row["xp"], "updatedAt": row["updated_at"] * 1000,
        } for row in rows]
        self.send_json(200, {"courses": courses})

    def save_progress(self, user: sqlite3.Row) -> None:
        payload = self.read_json()
        course_id = str(payload.get("courseId", ""))
        if course_id not in COURSE_IDS: raise ValueError("Unknown course.")
        completed_input = payload.get("completed", [])
        if not isinstance(completed_input, list): raise ValueError("Completed lessons must be an array.")
        completed = sorted({int(index) for index in completed_input if isinstance(index, int) and 0 <= index < 500})
        active_lesson = max(0, min(499, int(payload.get("activeLesson", 0))))
        timestamp = now()
        with database_connection() as database:
            database.execute("BEGIN IMMEDIATE")
            database.execute("DELETE FROM lesson_completions WHERE user_id = ? AND course_id = ?", (user["id"], course_id))
            database.executemany(
                "INSERT INTO lesson_completions(user_id, course_id, lesson_index, completed_at) VALUES (?, ?, ?, ?)",
                [(user["id"], course_id, index, timestamp) for index in completed],
            )
            database.execute(
                """INSERT INTO course_progress(user_id, course_id, active_lesson, completed_count, total_lessons, xp, updated_at)
                   VALUES (?, ?, ?, ?, 500, ?, ?)
                   ON CONFLICT(user_id, course_id) DO UPDATE SET active_lesson=excluded.active_lesson,
                   completed_count=excluded.completed_count, total_lessons=500, xp=excluded.xp, updated_at=excluded.updated_at""",
                (user["id"], course_id, active_lesson, len(completed), len(completed) * 100, timestamp),
            )
        self.send_json(200, {"ok": True, "completed": len(completed), "xp": len(completed) * 100})

    def save_quiz_attempt(self) -> None:
        user = self.require_user()
        if not user: return
        payload = self.read_json()
        course_id = str(payload.get("courseId", ""))
        lesson_index = int(payload.get("lessonIndex", -1))
        score = int(payload.get("score", -1))
        if course_id not in COURSE_IDS or not 0 <= lesson_index < 500 or not 0 <= score <= 20:
            raise ValueError("Invalid quiz attempt.")
        with database_connection() as database:
            database.execute(
                "INSERT INTO quiz_attempts(user_id, course_id, lesson_index, score, question_count, attempted_at) VALUES (?, ?, ?, ?, 20, ?)",
                (user["id"], course_id, lesson_index, score, now()),
            )
        self.send_json(201, {"ok": True})

    def get_leaderboard(self) -> None:
        with database_connection() as database:
            users = database.execute("SELECT id, username, display_name, created_at FROM users ORDER BY created_at").fetchall()
            progress = database.execute("SELECT user_id, course_id, completed_count, total_lessons, xp, updated_at FROM course_progress").fetchall()
        progress_by_user: dict[int, dict[str, Any]] = {}
        for row in progress:
            progress_by_user.setdefault(row["user_id"], {})[row["course_id"]] = {
                "completed": row["completed_count"], "total": row["total_lessons"], "xp": row["xp"], "updatedAt": row["updated_at"] * 1000,
            }
        entries = []
        for user in users:
            courses = progress_by_user.get(user["id"], {})
            entries.append({
                "name": user["display_name"], "username": user["username"], "createdAt": user["created_at"] * 1000,
                "updatedAt": max((course["updatedAt"] for course in courses.values()), default=0), "courses": courses,
            })
        entries.sort(key=lambda entry: (-sum(course["xp"] for course in entry["courses"].values()), -entry["updatedAt"], entry["username"]))
        self.send_json(200, {"entries": entries})


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the Desktopcraft website with its SQLite API.")
    parser.add_argument("--host", default=os.environ.get("HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.environ.get("PORT", "8000")))
    parser.add_argument("--init-only", action="store_true")
    args = parser.parse_args()
    initialize_database()
    if args.init_only:
        print(f"Desktopcraft database ready: {DB_PATH}")
        return
    server = ThreadingHTTPServer((args.host, args.port), DesktopcraftHandler)
    print(f"Desktopcraft server: http://{args.host}:{args.port}")
    print(f"SQLite database: {DB_PATH}")
    print(f"Feedback log: {feedback_file_path()}")
    print(f"Feedback email: {'configured' if feedback_email_available() else 'not configured (see .env.example)'}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
