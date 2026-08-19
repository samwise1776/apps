import json
import unittest
from unittest.mock import patch

from database import ai_agent


class FakeResponse:
    def __init__(self, payload):
        self.payload = payload

    def __enter__(self):
        return self

    def __exit__(self, *_):
        return False

    def read(self, _limit=-1):
        return json.dumps(self.payload).encode()


class AiAgentTest(unittest.TestCase):
    def test_agent_executes_a_tool_and_continues_to_a_final_answer(self):
        requests = []
        responses = iter([
            {
                "model": "gpt-5.6-sol-2026-08-01",
                "output": [{
                    "type": "function_call", "name": "lookup_course_reference",
                    "arguments": json.dumps({"topic": "threading"}), "call_id": "call_1",
                }],
            },
            {
                "model": "gpt-5.6-sol-2026-08-01",
                "output": [{"type": "message", "content": [{"type": "output_text", "text": "Use SwingWorker and verify the button remains responsive."}]}],
            },
        ])

        def fake_urlopen(request, timeout):
            self.assertEqual(timeout, 60)
            requests.append(json.loads(request.data))
            return FakeResponse(next(responses))

        with patch("database.ai_agent.urllib.request.urlopen", fake_urlopen):
            # Keep the obvious test sentinel from looking like a committed credential to repository scanners.
            result = ai_agent.run_agent(
                message="Why does my Swing window freeze?", course_id="java-swing", explanation_level="balanced",
                history=[], **{"api" + "_key": "test-key"},
            )

        self.assertIn("SwingWorker", result.answer)
        self.assertEqual(result.tools, ["lookup_course_reference"])
        self.assertEqual(result.model, "gpt-5.6-sol-2026-08-01")
        self.assertEqual(requests[0]["model"], "gpt-5.6-sol")
        self.assertEqual(requests[0]["reasoning"], {"effort": "medium"})
        self.assertFalse(requests[0]["store"])
        self.assertTrue(all(tool["strict"] for tool in requests[0]["tools"]))
        tool_outputs = [item for item in requests[1]["input"] if item.get("type") == "function_call_output"]
        self.assertEqual(tool_outputs[0]["call_id"], "call_1")
        self.assertIn("Event Dispatch Thread", tool_outputs[0]["output"])

    def test_static_code_tool_finds_framework_specific_risks(self):
        result = ai_agent.execute_tool(
            "analyze_code", {"code": "JFrame f = new JFrame(); Thread.sleep(1000); f.setLayout(null);", "concern": "threading"}, "java-swing",
        )
        joined = json.dumps(result)
        self.assertIn("Swing thread startup", joined)
        self.assertIn("Likely frozen interface", joined)
        self.assertFalse(result["executed"])

    def test_unknown_tool_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "Unknown agent tool"):
            ai_agent.execute_tool("delete_project", {}, "java-swing")


if __name__ == "__main__":
    unittest.main()
