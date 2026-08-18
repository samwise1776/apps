import contextlib
import io
import os
import sys
import tempfile
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, ROOT)

from velice.lexer import Lexer  # noqa: E402
from velice.parser import Parser  # noqa: E402
from velice.interpreter import Interpreter  # noqa: E402


def run(code, interp=None):
    tokens = Lexer(code).tokenize()
    ast = Parser(tokens, code).parse()
    if interp is None:
        interp = Interpreter()
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf):
        interp.run(ast, raise_errors=True)
    return buf.getvalue().strip()


class TestFileSystem(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()

    def test_read_write_file(self):
        code = f'''
var path = "{os.path.join(self.tmp, 'a', 'b', 'note.txt')}"
write_file(path, "hello world")
print(read_file(path))
print(file_exists(path))
'''
        self.assertEqual(run(code), "hello world\ntrue")

    def test_write_is_atomic_roundtrip(self):
        path = os.path.join(self.tmp, "data.txt")
        code = f'''
var path = "{path}"
write_file(path, "one")
write_file(path, "two")
print(read_file(path))
'''
        self.assertEqual(run(code), "two")
        self.assertEqual(sorted(os.listdir(self.tmp)), ["data.txt"])

    def test_append_file(self):
        path = os.path.join(self.tmp, "log.txt")
        code = f'''
var path = "{path}"
append_file(path, "a")
append_file(path, "b")
print(read_file(path))
'''
        self.assertEqual(run(code), "ab")

    def test_dir_helpers(self):
        d = os.path.join(self.tmp, "d1")
        code = f'''
make_dir("{d}")
write_file("{d}/x.txt", "x")
write_file("{d}/y.txt", "y")
print(is_dir("{d}"))
print(len(list_dir("{d}")))
remove_file("{d}/x.txt")
print(len(list_dir("{d}")))
'''
        self.assertEqual(run(code), "true\n2\n1")

    def test_path_helpers(self):
        code = '''
print(basename("/a/b/file.txt"))
print(dirname("/a/b/file.txt"))
print(extname("/a/b/file.txt"))
print(join_path("/a", "b"))
print(absolute_path(".") == current_dir())
'''
        out = run(code).splitlines()
        self.assertEqual(out[0], "file.txt")
        self.assertEqual(out[1], "/a/b")
        self.assertEqual(out[2], ".txt")
        self.assertEqual(out[3], "/a/b")
        self.assertEqual(out[4], "true")

    def test_copy_file(self):
        src = os.path.join(self.tmp, "s.txt")
        dst = os.path.join(self.tmp, "d.txt")
        code = f'''
write_file("{src}", "copy me")
copy_file("{src}", "{dst}")
print(read_file("{dst}"))
print(file_exists("{src}"))
'''
        self.assertEqual(run(code), "copy me\ntrue")


class TestJson(unittest.TestCase):
    def test_read_write_json(self):
        path = os.path.join(tempfile.mkdtemp(), "m.json")
        code = f'''
write_json("{path}", {{"name": "Velice", "version": 1}})
var m = read_json("{path}")
print(m["name"])
print(m["version"])
'''
        self.assertEqual(run(code), "Velice\n1")

    def test_json_pretty(self):
        code = '''
var s = json_pretty({"a": 1, "b": [1, 2, 3]})
print(s.contains("{") and s.contains("a"))
'''
        self.assertEqual(run(code), "true")


class TestProcess(unittest.TestCase):
    def test_run_shell(self):
        code = '''
var r = run_shell("echo hello")
print(r["code"])
print(r["stdout"].contains("hello"))
'''
        self.assertEqual(run(code), "0\ntrue")

    def test_run_shell_missing_command(self):
        code = '''
var r = run_shell("definitely_not_a_real_cmd_xyz")
print(r["code"] != 0)
'''
        self.assertEqual(run(code), "true")


class TestParseSource(unittest.TestCase):
    def test_parse_source_basic(self):
        code = '''
var tree = parse_source("let x = 42\\nprint(x)")
print(tree["kind"])
print(tree["statements"][0]["kind"])
print(tree["statements"][0]["name"])
print(tree["statements"][1]["expr"]["func"]["name"])
'''
        out = run(code).splitlines()
        self.assertEqual(out, ["program", "decl", "x", "print"])

    def test_parse_source_widget_tree(self):
        code = r'''
var tree = parse_source("""
window Demo {
    Label hi {
        text = "hi"
        y = 10
    }
}
""")
var win = tree["statements"][0]
print(win["kind"])
print(win["children"][0]["kind"])
print(win["children"][0]["wtype"])
'''
        out = run(code).splitlines()
        self.assertEqual(out, ["window", "widget", "Label"])

    def test_script_dir(self):
        interp = Interpreter()
        interp.source_path = "/tmp/demo/main.velice"
        code = '''
print(script_dir())
'''
        self.assertEqual(run(code, interp=interp), "/tmp/demo")


class TestMisc(unittest.TestCase):
    def test_env_and_random_id(self):
        code = '''
print(env("HOME") != nil)
var a = random_id("w")
var b = random_id("w")
print(a != b)
'''
        self.assertEqual(run(code), "true\ntrue")

    def test_python_path(self):
        code = '''
print(python_path() != "")
'''
        self.assertEqual(run(code), "true")


if __name__ == "__main__":
    unittest.main()
