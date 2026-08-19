import contextlib
import io
import os
import sys
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, ROOT)

os.environ.setdefault("VELICE_GUI", "none")

from velice.lexer import Lexer  # noqa: E402
from velice.parser import Parser  # noqa: E402
from velice.interpreter import Interpreter  # noqa: E402
from velice.gui.runtime import app as gui_app  # noqa: E402
from velice.gui import widgets as gui_widgets  # noqa: E402


def reset_app():
    gui_app.headless = True
    gui_app.windows.clear()
    gui_app.named.clear()
    gui_app.log.clear()
    gui_app.root = None
    gui_app._timers.clear()


def run(code):
    tokens = Lexer(code).tokenize()
    ast = Parser(tokens, code).parse()
    interp = Interpreter()
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf):
        interp.run(ast, raise_errors=True)
    return buf.getvalue().strip()


def captured(fn):
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf):
        result = fn()
    return result if result not in (None, "") else buf.getvalue().strip()


class TestGeometry(unittest.TestCase):
    def setUp(self):
        reset_app()

    def test_set_bounds_via_props(self):
        out = run('''
import gui
var w = Window("T", 400, 300)
var f = Frame()
f.x = 5
f.y = 7
f.width = 120
f.height = 40
print(f.x, f.y, f.width, f.height)
''')
        self.assertEqual(out, "5 7 120 40")

    def test_methods_return_self(self):
        out = run('''
import gui
var w = Window("T", 400, 300)
var f = Frame()
f.move(3, 4).resize(50, 60).set_bounds(1, 2, 3, 4)
print(f.x, f.y, f.width, f.height)
''')
        self.assertEqual(out, "1 2 3 4")

    def test_layout_free_places_children(self):
        out = run('''
import gui
var w = Window("T", 400, 300)
w.layout = "free"
var f = Frame()
f.set_bounds(10, 20, 100, 50)
w.add(f)
print(f.x, f.y, f.width, f.height)
''')
        self.assertEqual(out, "10 20 100 50")

    def test_window_size_and_position(self):
        out = run('''
import gui
var w = Window("T", 400, 300)
w.width = 500
w.height = 200
print(w.width, w.height)
''')
        self.assertEqual(out, "500 200")


class TestLiveText(unittest.TestCase):
    def setUp(self):
        reset_app()

    def test_textfield_text(self):
        out = run('''
import gui
var w = Window("T", 300, 200)
var f = TextField("hello")
print(f.text)
f.text = "world"
print(f.text)
''')
        self.assertEqual(out, "hello\nworld")

    def test_textarea_text_and_helpers(self):
        out = run('''
import gui
var w = Window("T", 300, 200)
var a = TextArea("one")
a.append("two")
print(a.text.contains("two"))
a.clear()
print(a.text)
''')
        self.assertEqual(out, "true")

    def test_placeholder_prop(self):
        out = run('''
import gui
var w = Window("T", 300, 200)
var f = TextField()
f.placeholder = "Type here..."
print(f.placeholder)
''')
        self.assertEqual(out, "Type here...")


class TestWindowClose(unittest.TestCase):
    def setUp(self):
        reset_app()

    def test_close_cancellable(self):
        def body():
            out = run('''
import gui
var w = Window("T", 300, 200)
w.onClose { false }
print(w._request_close())
''')
            self.assertIsNotNone(gui_app.get_window("T"))
            return out
        self.assertEqual(captured(body), "false")

    def test_close_destroys(self):
        def body():
            out = run('''
import gui
var w = Window("T", 300, 200)
w.close()
print(gui.window("T") == nil)
''')
            return out
        self.assertEqual(captured(body), "true")

    def test_on_close_handlers_can_cleanup(self):
        seen = []

        def body():
            out = run('''
import gui
var w = Window("T", 300, 200)
w.onClose { print("bye") }
w._request_close()
''')
            seen.append(out)
        captured(body)
        self.assertEqual(seen, ["bye"])


class TestEvents(unittest.TestCase):
    def setUp(self):
        reset_app()

    def test_mouse_events_supported(self):
        out = run('''
import gui
var w = Window("T", 300, 200)
var b = Button("x")
b.onMouseDown { }
b.onMouseUp { }
b.onDrag { }
b.onRightClick { }
b.onRightDown { }
''')
        self.assertEqual(out, "")

    def test_fire_passes_info(self):
        class FakeEvent:
            x = 3
            y = 4
            x_root = 0
            y_root = 0
            keysym = None
            state = 0
            num = None
            width = None
            height = None

        def body():
            run('''
import gui
var w = Window("T", 300, 200)
var b = Button("x", name = "b")
            b.onMouseDown { print(str(event["x"]) + str(event["y"]) + str(event["widget"] == "b")) }
''')
            b = gui_app.named["b"]
            b._fire("onMouseDown", FakeEvent())
        self.assertEqual(captured(body), "34true")

    def test_on_click_still_works(self):
        def body():
            run('''
import gui
var w = Window("T", 300, 200)
var b = Button("x", name = "b")
b.onClick { print("clicked") }
''')
            b = gui_app.named["b"]
            b._fire_now("onClick")
        self.assertEqual(captured(body), "clicked")


class TestTimers(unittest.TestCase):
    def setUp(self):
        reset_app()

    def test_after_runs_once(self):
        fired = []
        gui_app.after(0, lambda: fired.append(1))
        gui_app.run_timers()
        gui_app.run_timers()
        self.assertEqual(fired, [1])

    def test_every_repeats_and_cancel(self):
        fired = []
        tid = gui_app.every(0, lambda: fired.append(1))
        gui_app.run_timers()
        gui_app.run_timers()
        self.assertEqual(len(fired), 2)
        gui_app.cancel(tid)
        gui_app.run_timers()
        gui_app.run_timers()
        self.assertEqual(len(fired), 2)

    def test_timers_exposed_in_velice(self):
        def body():
            run('''
import gui
gui.after(0, fn() { print("tick") })
''')
            gui_app.run_timers()
        self.assertEqual(captured(body), "tick")


class TestCodeEditor(unittest.TestCase):
    def setUp(self):
        reset_app()

    def test_text_roundtrip(self):
        out = run('''
import gui
var w = Window("T", 500, 400)
var e = CodeEditor("let x = 1")
print(e.text)
e.text = "print(2)"
print(e.text)
e.clear()
print(len(e.text))
''')
        self.assertEqual(out, "let x = 1\nprint(2)\n0")

    def test_append_and_find(self):
        out = run('''
import gui
var w = Window("T", 500, 400)
var e = CodeEditor()
e.append("hello")
e.append("world")
print(e.text)
print(e.find("ll"))
''')
        self.assertEqual(out, "helloworld\n0")


class TestTableAndComboBox(unittest.TestCase):
    def setUp(self):
        reset_app()

    def test_table_rows(self):
        out = run('''
import gui
var w = Window("T", 500, 400)
var t = Table(["A", "B"], [[1, 2], [3, 4]])
print(len(t.rows))
t.rows = [[9, 9]]
print(len(t.rows))
t.clear()
print(len(t.rows))
''')
        self.assertEqual(out, "2\n1\n0")

    def test_combobox_selected(self):
        out = run('''
import gui
var w = Window("T", 300, 200)
var c = ComboBox(["a", "b", "c"])
c.selected = "b"
print(c.selected)
''')
        self.assertEqual(out, "b")

    def test_listbox_multiple_selection(self):
        out = run('''
import gui
var w = Window("T", 300, 200)
var l = ListBox(["a", "b", "c"], multiple = true)
l.selected = ["a", "c"]
print(len(l.selected))
''')
        self.assertEqual(out, "2")


class TestWidgetTreeOps(unittest.TestCase):
    def setUp(self):
        reset_app()

    def test_remove(self):
        out = run('''
import gui
var w = Window("T", 300, 200)
var a = Frame()
var b = Frame()
w.add(a)
w.add(b)
w.remove(a)
print(len(w.children))
''')
        self.assertEqual(out, "1")

    def test_reparent(self):
        out = run('''
import gui
var w = Window("T", 300, 200)
var p1 = Frame()
var p2 = Frame()
w.add(p1)
w.add(p2)
var c = Frame()
p1.add(c)
c.reparent(p2)
print(len(p1.children))
print(len(p2.children))
''')
        self.assertEqual(out, "0\n1")


class TestParserLexerRegression(unittest.TestCase):
    def setUp(self):
        reset_app()

    def test_empty_map_literal(self):
        out = run('''
var m = {}
m["a"] = 1
print(len(m))
''')
        self.assertEqual(out, "1")

    def test_single_quote_char_literal(self):
        out = run('''
var e = {"x": 42}
print(e['x'])
''')
        self.assertEqual(out, "42")

    def test_multiline_array_literal(self):
        out = run('''
var a = [1, 2,
        3, 4]
print(len(a))
''')
        self.assertEqual(out, "4")

    def test_multiline_map_literal(self):
        out = run('''
var m = {
    "a": 1,
    "b": [1, 2]
}
print(m["a"] + m["b"][1])
''')
        self.assertEqual(out, "3")

    def test_interp_with_char_index(self):
        out = run('''
var e = {"x": 9}
print("v=${e['x']}")
''')
        self.assertEqual(out, "v=9")


class TestDslEvents(unittest.TestCase):
    def setUp(self):
        reset_app()

    def test_dsl_event_defines_event_var(self):
        code = '''
import gui
window Main {
    width = 200
    Button btn {
        x = 5
        y = 5
        onClick {
            print("x=" + str(event["x"]))
        }
    }
}
run Main
'''
        tokens = Lexer(code).tokenize()
        ast = Parser(tokens, code).parse()
        interp = Interpreter()
        with contextlib.redirect_stdout(io.StringIO()):
            interp.run(ast, raise_errors=True)
        win = list(gui_app.windows.values())[0]
        btn = next(d for d in win.descendants() if getattr(d, "name", None) == "btn")
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf):
            btn.events["onClick"]({"x": 7, "y": 8})
        self.assertEqual(buf.getvalue().strip(), "x=7")

    def test_dsl_window_event_binds(self):
        code = '''
import gui
window Main {
    width = 200
    onClose {
        false
    }
}
run Main
'''
        tokens = Lexer(code).tokenize()
        ast = Parser(tokens, code).parse()
        interp = Interpreter()
        with contextlib.redirect_stdout(io.StringIO()):
            interp.run(ast, raise_errors=True)
        win = list(gui_app.windows.values())[0]
        self.assertIn("onClose", win.events)
        self.assertFalse(win.events["onClose"](None))

    def test_window_width_height_props_stored(self):
        code = '''
import gui
window Main {
    width = 320
    height = 240
}
run Main
'''
        tokens = Lexer(code).tokenize()
        ast = Parser(tokens, code).parse()
        interp = Interpreter()
        with contextlib.redirect_stdout(io.StringIO()):
            interp.run(ast, raise_errors=True)
        win = list(gui_app.windows.values())[0]
        self.assertEqual(win.props.get("width"), 320)
        self.assertEqual(win.props.get("height"), 240)


class TestScrollPane(unittest.TestCase):
    def setUp(self):
        reset_app()

    def test_scrollpane_creates_and_adds_child(self):
        code = '''
import gui
var pane = ScrollPane(640, 480)
pane.add(Label("line one"))
var win = Window("t", 300, 300)
win.add(pane)
'''
        tokens = Lexer(code).tokenize()
        ast = Parser(tokens, code).parse()
        interp = Interpreter()
        with contextlib.redirect_stdout(io.StringIO()):
            interp.run(ast, raise_errors=True)
        pane = list(gui_app.named.values())[0] if gui_app.named else None
        self.assertEqual(len(list(gui_app.windows.values())[0].children), 1)

    def test_scrollpane_members_exist_headless(self):
        code = '''
import gui
var pane = ScrollPane(200, 100)
var child = pane.add(Label("x"))
print(pane.widget_type)
print(child.widget_type)
'''
        out = run(code)
        self.assertIn("scrollpane", out)
        self.assertIn("label", out)

    def test_scroll_methods_noop_headless(self):
        out = run('''
import gui
var pane = ScrollPane(200, 100)
pane.scroll_to_top()
pane.scroll_to_bottom()
pane.scroll_to(0.5, 0.5)
print("ok")
''')
        self.assertEqual(out, "ok")


class TestFileChooser(unittest.TestCase):
    def setUp(self):
        reset_app()

    def test_file_chooser_headless_returns_nil(self):
        out = run('''
import gui
var picked = FileChooser("Pick a file", "open", "/home")
print(picked)
''')
        self.assertEqual(out, "nil")

    def test_file_chooser_recorded(self):
        code = 'import gui\nFileChooser("Pick", "folder")'
        tokens = Lexer(code).tokenize()
        ast = Parser(tokens, code).parse()
        interp = Interpreter()
        with contextlib.redirect_stdout(io.StringIO()):
            interp.run(ast, raise_errors=True)
        events = [e for e, kw in gui_app.log if e == "file_chooser"]
        self.assertEqual(len(events), 1)


if __name__ == "__main__":
    unittest.main()