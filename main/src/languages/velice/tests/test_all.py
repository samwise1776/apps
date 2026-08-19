import contextlib
import io
import os
import re
import subprocess
import sys
import tempfile
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, ROOT)

from velice.lexer import Lexer, TT  # noqa: E402
from velice.parser import Parser  # noqa: E402
from velice.interpreter import Interpreter, VeliceError  # noqa: E402
from velice import ast_nodes as A  # noqa: E402


def run(code):
    tokens = Lexer(code).tokenize()
    ast = Parser(tokens, code).parse()
    interp = Interpreter()
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf):
        interp.run(ast)
    return buf.getvalue().strip()


def run_with(code, argv):
    tokens = Lexer(code).tokenize()
    ast = Parser(tokens, code).parse()
    interp = Interpreter(argv=argv)
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf):
        interp.run(ast)
    return buf.getvalue().strip()


def show(value):
    if isinstance(value, list):
        return '[' + ', '.join(show(v) for v in value) + ']'
    return str(value).lower() if isinstance(value, bool) else str(value)


class TestLexer(unittest.TestCase):
    def test_tokens(self):
        tokens = Lexer('let x = 42 + 3.14').tokenize()
        kinds = [t.type for t in tokens]
        self.assertIn(TT.LET, kinds)
        self.assertIn(TT.IDENT, kinds)
        self.assertIn(TT.INT, kinds)

    def test_keywords(self):
        tokens = Lexer('fn class struct enum match').tokenize()
        kinds = [t.type.value for t in tokens]
        for k in ['fn', 'class', 'struct', 'enum', 'match']:
            self.assertIn(k, kinds)

    def test_strings_and_numbers(self):
        tokens = Lexer('"hi" r"raw" 0x1F 1_000 2.5e3').tokenize()
        values = [t.value for t in tokens]
        for expected in ['hi', 'raw', 0x1F, 1000, 2500.0]:
            self.assertIn(expected, values)


class TestParser(unittest.TestCase):
    def test_program_shape(self):
        ast = Parser(Lexer('print("hi")').tokenize(), 'print("hi")').parse()
        self.assertEqual(len(ast.stmts), 1)

    def test_let_binding(self):
        ast = Parser(Lexer('let x = 5').tokenize(), 'let x = 5').parse()
        self.assertEqual(ast.stmts[0].name, 'x')

    def test_fn_decl(self):
        ast = Parser(Lexer('fn add(a, b) { return a + b }').tokenize(),
                     'fn add(a, b) { return a + b }').parse()
        decl = ast.stmts[0]
        self.assertEqual(decl.name, 'add')
        self.assertEqual([p.name for p in decl.params], ['a', 'b'])

    def test_var_decl(self):
        ast = Parser(Lexer('var a = "Hello"').tokenize(), 'var a = "Hello"').parse()
        decl = ast.stmts[0]
        self.assertEqual(decl.name, 'a')
        self.assertTrue(decl.mutable)

    def test_var_thunk(self):
        ast = Parser(Lexer('var a = (print("hi"))').tokenize(),
                     'var a = (print("hi"))').parse()
        self.assertIsInstance(ast.stmts[0].value, A.ThunkExpr)

    def test_var_params(self):
        ast = Parser(Lexer('fn calc(var a, var b) { return a + b }').tokenize(),
                     'fn calc(var a, var b) { return a + b }').parse()
        decl = ast.stmts[0]
        self.assertEqual([p.name for p in decl.params], ['a', 'b'])
        self.assertTrue(all(p.mutable for p in decl.params))


class TestInterpreter(unittest.TestCase):
    def test_arithmetic(self):
        self.assertEqual(run('print(2 + 3 * 4)'), '14')

    def test_precedence(self):
        self.assertEqual(run('print((2 + 3) * 4)'), '20')

    def test_variables(self):
        self.assertEqual(run('let x = 5\nprint(x + 1)'), '6')

    def test_var_keyword(self):
        self.assertEqual(run('var a = "Hello"\nprint(a)'), 'Hello')

    def test_var_thunk_runs_on_call(self):
        self.assertEqual(run('var greet = (print("Hello from thunk"))\ngreet()\ngreet()'),
                         'Hello from thunk\nHello from thunk')

    def test_var_thunk_returns_value(self):
        self.assertEqual(run('var f = (5 * 4)\nprint(f())'), '20')

    def test_var_fn_params(self):
        self.assertEqual(run('fn calc(var a, var b) { return a + b }\nprint(calc(2, 3))'), '5')

    def test_immutability(self):
        tokens = Lexer('let x = 5\nx = 6').tokenize()
        ast = Parser(tokens, 'let x = 5\nx = 6').parse()
        with self.assertRaises(VeliceError):
            Interpreter().run(ast, raise_errors=True)

    def test_mutability(self):
        self.assertEqual(run('let mut x = 5\nx = 6\nprint(x)'), '6')

    def test_strings_interpolation(self):
        self.assertEqual(run('let n = "World"\nprint("Hello ${n}!")'),
                         'Hello World!')

    def test_functions(self):
        self.assertEqual(run('fn add(a, b) { return a + b }\nprint(add(2, 3))'),
                         '5')

    def test_default_args(self):
        code = 'fn f(a, b = 10) { return a + b }\nprint(f(1))'
        self.assertEqual(run(code), '11')

    def test_closures(self):
        code = 'fn outer() { let mut c = 0; return fn() { c = c + 1; return c } }\n' \
               'let f = outer()\nprint(f())\nprint(f())'
        self.assertEqual(run(code), '1\n2')

    def test_recursion(self):
        code = 'fn fib(n) { if n < 2 { return n } return fib(n-1) + fib(n-2) }\n' \
               'print(fib(10))'
        self.assertEqual(run(code), '55')

    def test_while(self):
        code = 'let mut i = 0\nlet mut s = 0\nwhile i < 5 { s = s + i; i = i + 1 }\nprint(s)'
        self.assertEqual(run(code), '10')

    def test_for(self):
        code = 'let mut s = 0\nfor x in 0..3 { s = s + x }\nprint(s)'
        self.assertEqual(run(code), '6')

    def test_range_operator(self):
        self.assertEqual(run('print(0..3)'), '[0, 1, 2, 3]')

    def test_arrays(self):
        self.assertEqual(run('let a = [1, 2, 3]\nprint(a[1])'), '2')
        self.assertEqual(run('let a = [1, 2]\nappend(a, 3)\nprint(len(a))'), '3')

    def test_maps(self):
        self.assertEqual(run('let m = {"k": 42}\nprint(m["k"])'), '42')

    def test_match(self):
        code = 'fn f(n) { return match n { 0 => "zero", _ => "other" } }\nprint(f(0))'
        self.assertEqual(run(code), 'zero')

    def test_classes(self):
        code = 'class C { fn init(v) { self.v = v } fn get() { return self.v } }\n' \
               'let c = C(42)\nprint(c.get())'
        self.assertEqual(run(code), '42')

    def test_inheritance(self):
        code = ('class A { fn init(x) { self.x = x } fn val() { return self.x } }\n'
                'class B extends A { fn val() { return self.x * 2 } }\n'
                'let b = B(21)\nprint(b.val())')
        self.assertEqual(run(code), '42')

    def test_try_catch(self):
        code = 'try { throw "boom" } catch e { print("caught:" + e) }'
        self.assertEqual(run(code), 'caught:boom')

    def test_pipes(self):
        self.assertEqual(run('fn sq(x) { return x * x }\nprint(3 |> sq)'), '9')

    def test_null_coalescing(self):
        self.assertEqual(run('print(nil ?? "default")'), 'default')

    def test_boolean_logic(self):
        self.assertEqual(run('print(true and false)'), 'false')
        self.assertEqual(run('print(true or false)'), 'true')
        self.assertEqual(run('print(not true)'), 'false')

    def test_string_methods(self):
        self.assertEqual(run('print(len("hello"))'), '5')

    def test_string_contains_method(self):
        self.assertEqual(run('print("hello".contains("ell"))'), 'true')
        self.assertEqual(run('print("hello".contains("xyz"))'), 'false')

    def test_string_ends_starts_methods(self):
        self.assertEqual(run('print("hello".ends("lo"))'), 'true')
        self.assertEqual(run('print("hello".ends("lo!"))'), 'false')
        self.assertEqual(run('print("hello".starts("he"))'), 'true')
        self.assertEqual(run('print("hello".starts("xyz"))'), 'false')

    def test_string_inbetween_method(self):
        self.assertEqual(run('print("foo(bar)baz".inbetween("()"))'), 'bar')
        self.assertEqual(run('print("print(\\"Hello\\")".inbetween("\\"\\""))'), 'Hello')
        self.assertEqual(run('print("no delimiters".inbetween("()"))'), '')

    def test_string_ends_quote(self):
        self.assertEqual(run('print("abc\\"".ends("\\""))'), 'true')

    def test_print_statement_extraction(self):
        code = (
            'a = "print(\\"Hello\\")"\n'
            'if a.contains("print(\\"") and a.ends(")") {\n'
            '    message = a.inbetween("\\"\\"")\n'
            '    print(message)\n'
            '}\n'
        )
        self.assertEqual(run(code), 'Hello')

    def test_argu_dot_access(self):
        self.assertEqual(run_with('print(argu.0)', ['one', 'two']), 'one')
        self.assertEqual(run_with('print(argu.1)', ['one', 'two']), 'two')

    def test_argu_len_and_print(self):
        self.assertEqual(run_with('print(argu.len)', ['one', 'two']), '2')
        self.assertEqual(run_with('print(argu)', ['a', 'b']), '[a, b]')

    def test_argu_index_and_iterate(self):
        self.assertEqual(run_with('print(argu[0])', ['a', 'b']), 'a')
        self.assertEqual(run_with('for x in argu { print(x) }', ['a', 'b']), 'a\nb')

    def test_argu_out_of_range_errors(self):
        with self.assertRaises(VeliceError):
            Interpreter(argv=['only']).run(
                Parser(Lexer('print(argu.5)').tokenize(), 'print(argu.5)').parse(),
                raise_errors=True,
            )

    def test_read_file_lines(self):
        path = os.path.join(tempfile.mkdtemp(), 'code.txt')
        with open(path, 'w') as f:
            f.write('line one\nline two\nline three\n')
        code = f'lines = readFileLines("{path}")\nprint(lines.len)\nprint(lines[1])'
        self.assertEqual(run(code), '3\nline two')

    def test_current_line_iteration(self):
        path = os.path.join(tempfile.mkdtemp(), 'code.txt')
        with open(path, 'w') as f:
            f.write('fn add(a, b) { return a + b }\nprint("hi")\n')
        code = (
            f'lines = readFileLines("{path}")\n'
            'while lines.hasMore() {\n'
            '    line = lines.currentLine()\n'
            '    if line.contains("print(") {\n'
            '        print(line)\n'
            '    }\n'
            '}\n'
        )
        self.assertEqual(run(code), 'print("hi")')

    def test_current_line_exhausts_to_nil(self):
        path = os.path.join(tempfile.mkdtemp(), 'code.txt')
        with open(path, 'w') as f:
            f.write('a\nb\n')
        code = (
            f'lines = readFileLines("{path}")\n'
            'print(lines.currentLine())\n'
            'print(lines.currentLine())\n'
            'print(lines.currentLine())\n'
        )
        self.assertEqual(run(code), 'a\nb\nnil')

    def test_filelines_contains(self):
        path = os.path.join(tempfile.mkdtemp(), 'code.txt')
        with open(path, 'w') as f:
            f.write('fn add(a, b) { return a + b }\nprint("hi")\n')
        code = (
            f'lines = readFileLines("{path}")\n'
            'print(lines.contains("print(\\""))\n'
            'print(lines.contains("nope"))\n'
        )
        self.assertEqual(run(code), 'true\nfalse')

    def test_read_file_lines_missing_file_errors(self):
        code = 'readFileLines("/no/such/file.velice")'
        with self.assertRaises(VeliceError):
            Interpreter().run(
                Parser(Lexer(code).tokenize(), code).parse(),
                raise_errors=True,
            )

    def test_typeof(self):
        self.assertEqual(run('print(typeof(42))'), 'int')

    def test_rand_int_range(self):
        for _ in range(30):
            v = run('var a = rand(1, 100)\nprint(a)')
            self.assertTrue(1 <= int(v) <= 100, f'out of range: {v}')

    def test_rand_boolean(self):
        for _ in range(30):
            v = run('var b = rand(true, false)\nprint(b)')
            self.assertIn(v, ('true', 'false'), f'not a boolean: {v}')

    def test_rand_float(self):
        v = float(run('print(rand(1.5, 3.5))'))
        self.assertTrue(1.5 <= v <= 3.5, f'out of range: {v}')

    def test_rand_single_value(self):
        self.assertIn(run('print(rand([10, 20, 30]))'), ('10', '20', '30'))
        v = run('print(rand(3))')
        self.assertIn(int(v), (0, 1, 2, 3))

    def test_print_multiple(self):
        self.assertEqual(run('print(1, 2, 3)'), '1 2 3')

    def test_documented_standard_library_aliases(self):
        self.assertEqual(run('print(reduce([1, 2, 3, 4], fn(total, value) { return total + value }))'), '10')
        self.assertEqual(run('print(reduce([], fn(total, value) { return total + value }, 5))'), '5')
        self.assertEqual(run('print(floor(3.9), ceil(3.1), round(3.14159, 2))'), '3 4 3.14')
        self.assertEqual(run('let data = json_parse("{\\"ready\\": true}")\nprint(data["ready"])'), 'true')
        self.assertEqual(run('print(json_stringify({"ready": true}))'), '{"ready": true}')


class TestCmdModule(unittest.TestCase):
    def test_write_runs_command_and_prints_stdout(self):
        out = run('import cmd\ncmd.write("echo Hello")')
        self.assertEqual(out, "Hello")

    def test_write_captured_in_variable(self):
        out = run('import cmd\na = cmd.write("echo Hello")\nprint(a)')
        self.assertEqual(out, "Hello")

    def test_write_returns_output_inside_expression(self):
        out = run('import cmd\nprint(cmd.write("echo Hello"))')
        self.assertEqual(out, "Hello")

    def test_write_streams_stderr_to_stderr(self):
        code = 'import cmd\ncmd.write("echo err 1>&2")'
        tokens = Lexer(code).tokenize()
        ast = Parser(tokens, code).parse()
        interp = Interpreter()
        out_buf, err_buf = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out_buf), contextlib.redirect_stderr(err_buf):
            interp.run(ast)
        self.assertEqual(out_buf.getvalue().strip(), "")
        self.assertEqual(err_buf.getvalue().strip(), "err")

    def test_capture_returns_output_silently(self):
        out = run('import cmd\na = cmd.capture("echo Hi")\nprint(a)')
        self.assertEqual(out, "Hi")

    def test_capture_alone_prints_nothing(self):
        out = run('import cmd\ncmd.capture("echo silent")')
        self.assertEqual(out, "")

    def test_alias_import(self):
        out = run('import cmd as c\nc.write("echo aliased")')
        self.assertEqual(out, "aliased")

    def test_from_import_member(self):
        out = run('from "cmd" import write\nwrite("echo direct")')
        self.assertEqual(out, "direct")


class TestUserProxy(unittest.TestCase):
    @staticmethod
    def username():
        import getpass
        return getpass.getuser()

    def test_interpolated_username(self):
        out = run('print("Hello ${User.name}!")')
        self.assertEqual(out, f"Hello {self.username()}!")

    def test_direct_dot_access(self):
        out = run('print(User.name)')
        self.assertEqual(out, self.username())

    def test_home_property(self):
        out = run('print(User.home)')
        self.assertEqual(out, os.path.expanduser("~"))

    def test_unknown_member_raises(self):
        tokens = Lexer('print(User.nope)').tokenize()
        ast = Parser(tokens, 'print(User.nope)').parse()
        interp = Interpreter()
        err_buf = io.StringIO()
        with contextlib.redirect_stderr(err_buf):
            interp.run(ast)
        self.assertIn("User: no such member 'nope'", err_buf.getvalue())


class TestExamples(unittest.TestCase):
    def test_default_repl_exits_cleanly(self):
        result = subprocess.run(
            [sys.executable, '-m', 'velice'], input='q\n',
            capture_output=True, text=True, cwd=ROOT,
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertNotIn('Traceback', result.stderr)

    def test_cli_eval_prints_expression_result(self):
        result = subprocess.run(
            [sys.executable, '-m', 'velice', 'eval', '1 + 2 * 3'],
            capture_output=True, text=True, cwd=ROOT,
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stdout.strip(), '7')

    def test_cli_eval_formats_boolean_result(self):
        result = subprocess.run(
            [sys.executable, '-m', 'velice', 'eval', '"velice".starts("vel")'],
            capture_output=True, text=True, cwd=ROOT,
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stdout.strip(), 'true')

    def test_all_examples_run(self):
        examples = os.path.join(ROOT, 'examples')
        for name in sorted(os.listdir(examples)):
            if not name.endswith('.velice'):
                continue
            path = os.path.join(examples, name)
            env = dict(os.environ, VELICE_GUI="none")
            result = subprocess.run(
                [sys.executable, '-m', 'velice', 'run', path],
                capture_output=True, text=True, cwd=ROOT, env=env,
            )
            self.assertEqual(
                result.returncode, 0,
                f'{name} failed:\n{result.stderr or result.stdout}',
            )


if __name__ == '__main__':
    unittest.main()
