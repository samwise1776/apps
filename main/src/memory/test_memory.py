import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import memory


class MemoryTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        root = Path(self.temp.name)
        self.store = root / "app_downloads.json"
        self.directory = root / "username.txt"
        self.log = root / "downloads.txt"
        self.store.write_text('{"schema_version":1,"accounts":{},"downloads":[]}\n')
        self.log.write_text("")
        self.registry = root / "apps.json"
        self.registry.write_text(json.dumps({"applications":[{"id":"DC-TEST-001","slug":"test","version":"1.0.0","status":"ACTIVE"}]}))
        self.paths = patch.multiple(memory, STORE=self.store, DIRECTORY=self.directory, LOG=self.log, REGISTRY=self.registry)
        self.paths.start()

    def tearDown(self):
        self.paths.stop(); self.temp.cleanup()

    def test_account_sign_in_and_download(self):
        memory.register("alice", "Alice@Example.com", "correct horse battery")
        self.assertTrue(memory.authenticate("alice", "correct horse battery"))
        self.assertFalse(memory.authenticate("alice", "incorrect password"))
        memory.record_download("alice", "correct horse battery", "test")
        data=json.loads(self.store.read_text())
        self.assertEqual(data["accounts"]["alice"]["email"], "alice@example.com")
        self.assertNotIn("correct horse battery", self.store.read_text())
        self.assertEqual(data["downloads"][0]["app_id"], "DC-TEST-001")
        self.assertIn("alice\talice@example.com", self.directory.read_text())

    def test_rejects_duplicate_email_and_short_password(self):
        with self.assertRaises(ValueError): memory.register("alice", "a@example.com", "short")
        memory.register("alice", "a@example.com", "long enough password")
        with self.assertRaises(ValueError): memory.register("other", "a@example.com", "another good password")


if __name__ == "__main__": unittest.main()
