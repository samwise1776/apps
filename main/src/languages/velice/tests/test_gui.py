import contextlib
import io
import os
import sys
import unittest
from unittest.mock import patch

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, ROOT)

os.environ.setdefault("VELICE_GUI", "none")

from velice.lexer import Lexer  # noqa: E402
from velice.parser import Parser  # noqa: E402
from velice.interpreter import Interpreter, VeliceError  # noqa: E402
from velice.gui.runtime import app as gui_app  # noqa: E402
from velice.gui.theme import THEMES, names as theme_names  # noqa: E402
from velice.gui import widgets as gui_widgets  # noqa: E402


def run(code, stdout=True):
    tokens = Lexer(code).tokenize()
    ast = Parser(tokens, code).parse()
    interp = Interpreter()
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf):
        interp.run(ast, raise_errors=True)
    return buf.getvalue().strip() if stdout else interp


def reset_app():
    gui_app.headless = True
    gui_app.windows.clear()
    gui_app.named.clear()
    gui_app.log.clear()
    gui_app.root = None


def captured(fn):
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf):
        fn()
    return buf.getvalue().strip()


class TestGuiImport(unittest.TestCase):
    def setUp(self):
        reset_app()

    def test_import_gui_exposes_constructors(self):
        out = run('''
import gui
var w = Window("T", 200, 100)
print(w.title)
var b = Button("hi")
print(b.text)
''')
        self.assertEqual(out, "T\nhi")

    def test_from_gui_import(self):
        out = run('''
from gui import Window, Button, Label
var w = Window("T", 200, 100)
w.add(Button("a"))
w.add(Label("b"))
print(len(w.children))
''')
        self.assertEqual(out, "2")

    def test_from_gui_wildcard(self):
        out = run('''
from gui import *
var w = Window("T", 200, 100)
var b = Button("x")
print(window("T") == w)
''')
        self.assertEqual(out, "true")

    def test_unknown_member_raises(self):
        with self.assertRaises(VeliceError):
            run('from gui import NoSuchWidget')

    def test_named_widget_registered(self):
        out = run('''
from gui import Button, window
var b = Button("x", name="btn1")
print(window("btn1") == b)
''')
        self.assertEqual(out, "true")


class TestGuiWidgets(unittest.TestCase):
    def setUp(self):
        reset_app()

    def test_bare_assignment_auto_declares(self):
        out = run('''
from gui import Window
window = Window("A", 300, 200)
print(window.title)
''')
        self.assertEqual(out, "A")

    def test_property_get_set(self):
        out = run('''
from gui import Window, Label
var w = Window("A", 300, 200)
w.title = "B"
w.add(Label("hi", name="l1"))
w.children[0].text = "changed"
print(w.title)
print(w.children[0].text)
''')
        self.assertEqual(out, "B\nchanged")

    def test_widget_type(self):
        out = run('''
from gui import Button, Label, TextField
print(Button("x").widget_type)
print(Label("x").widget_type)
print(TextField("x").widget_type)
''')
        self.assertEqual(out, "button\nlabel\ntextfield")

    def test_window_closable(self):
        interp = run('''
from gui import Window
var a = Window("A", 300, 200)
var b = Window("B", 300, 200)
var c = Window("C", 300, 200)
a.closable(false)
c.set("closable", false)
''', stdout=False)
        a = interp.globals.get("a")
        b = interp.globals.get("b")
        c = interp.globals.get("c")
        self.assertFalse(a._closable)
        self.assertTrue(b._closable)
        self.assertFalse(c._closable)

    def test_slider_spinner_progress(self):
        run('''
from gui import Slider, Spinner, ProgressBar
var s = Slider(30, 0, 100)
var sp = Spinner(5, 0, 10)
var p = ProgressBar(40, 100)
print(s.value, sp.min, p.max)
''')


class TestGuiEvents(unittest.TestCase):
    def setUp(self):
        reset_app()

    def test_imperative_on_click(self):
        out = run('''
from gui import Button
var b = Button("go")
b.on_click {
    print("clicked")
}
b._fire_now("onClick")
''')
        self.assertEqual(out, "clicked")

    def test_event_payload_available(self):
        out = run('''
from gui import Button
var b = Button("go")
var x = nil
b.on_click {
    x = event
}
b._fire_now("onClick")
print(x == nil)
''')
        self.assertEqual(out, "false")

    def test_bind_on_non_widget_raises(self):
        with self.assertRaises(VeliceError):
            run('''
var s = "hello"
s.on_click { print("no") }
''')


class TestGuiCanvas(unittest.TestCase):
    def setUp(self):
        reset_app()

    def test_canvas_records_draws(self):
        out = run('''
from gui import Canvas
var c = Canvas(200, 100)
c.draw_rect(0, 0, 10, 10, "#ff0000")
c.draw_line(1, 2, 3, 4, "#00ff00")
c.draw_text(5, 6, "hi")
c.clear()
''')
        events = [e for e, _ in gui_app.log if e.startswith("canvas.")]
        self.assertEqual(events,
                         ["canvas.rect", "canvas.line", "canvas.text", "canvas.clear"])

    def test_visible_canvas_attaches_to_window_before_replaying_draws(self):
        class FakeCanvas:
            def __init__(self, master, **props):
                self.master = master
                self.props = props
                self.operations = []
                self.packed = False

            def create_rectangle(self, *args, **kwargs):
                self.operations.append(("rect", args, kwargs))
                return 1

            def create_text(self, *args, **kwargs):
                self.operations.append(("text", args, kwargs))
                return 2

            def pack(self, **kwargs):
                self.packed = True

        class FakeApp:
            def __init__(self):
                self.headless = False
                self.root = object()
                self.theme = {}
                self.named = {}

        fake_app = FakeApp()
        window = gui_widgets.Widget(app=fake_app)
        window.tk = object()
        canvas = gui_widgets.Canvas(200, 100, "#fff", app=fake_app)

        canvas.draw_rect(1, 2, 30, 40, "#f00")
        canvas.draw_text(50, 60, "ready")
        self.assertIsNone(canvas.tk)

        with patch.object(gui_widgets.tk, "Canvas", FakeCanvas):
            window.add(canvas)

        self.assertIs(canvas.parent, window)
        self.assertIs(canvas.tk.master, window.tk)
        self.assertTrue(canvas.tk.packed)
        self.assertEqual([op[0] for op in canvas.tk.operations], ["rect", "text"])


class TestGuiDialogs(unittest.TestCase):
    def setUp(self):
        reset_app()

    def test_confirm_records(self):
        out = run('''
from gui import confirm
var res = confirm("Sure?")
print(res)
''')
        self.assertEqual(out, "true")

    def test_clipboard_copy_paste(self):
        run('''
from gui import clipboard
clipboard.copy("abc")
''')
        events = [e for e, _ in gui_app.log]
        self.assertIn("clipboard.copy", events)


class TestGuiThemes(unittest.TestCase):
    def test_all_built_in_themes_load(self):
        self.assertGreaterEqual(len(THEMES), 13)
        self.assertEqual(theme_names(), sorted(THEMES))
        for name, colors in THEMES.items():
            self.assertEqual(colors["name"], name)
            self.assertTrue(colors["background"].startswith("#"))
            self.assertTrue(colors["text"].startswith("#"))


class TestGuiDsl(unittest.TestCase):
    def setUp(self):
        reset_app()

    def test_declarative_window(self):
        out = run('''
import gui
window Main {
    title = "DSL"
    Button go {
        text = "Go"
    }
}
run Main
''')
        self.assertEqual(gui_app.get_window("Main").title, "DSL")

    def test_declarative_event_fires(self):
        def body():
            run('''
import gui
window Main {
    Button go {
        text = "Go"
        onClick { print("fired") }
    }
}
run Main
''')
            main = gui_app.get_window("Main")
            for w in main.descendants():
                if w.widget_type == "button":
                    w._fire_now("onClick")
        self.assertEqual(captured(body), "fired")

    def test_declarative_widget_names_are_variables(self):
        def body():
            run('''
import gui
window Main {
    Button go {
        text = "Go"
        onClick {
            print(go.text)
        }
    }
}
run Main
''')
            main = gui_app.get_window("Main")
            for w in main.descendants():
                if w.widget_type == "button":
                    w._fire_now("onClick")
        self.assertEqual(captured(body), "Go")

    def test_run_unknown_window_raises(self):
        with self.assertRaises(VeliceError):
            run('import gui\nrun Missing')

    def test_unknown_widget_type_raises(self):
        with self.assertRaises(VeliceError):
            run('''
import gui
window Main {
    Bogus w { }
}
''')


if __name__ == "__main__":
    unittest.main()
