"""Velice native GUI standard library.

This package glues the Tk-based widget runtime to the Velice interpreter:

    import gui

    var window = gui.Window("My Application", 800, 600)
    var button = gui.Button("Click Me")
    button.on_click { print("Hello from Velice") }
    window.add(button)
    gui.run()

Widgets are also registered under their ``name`` so ``gui.window("name")``
can look them up. All GUI calls run against the shared :data:`runtime.app`;
when no display is available (or ``VELICE_GUI=none``) the runtime records
calls instead, so programs still run headless.
"""
from velice.gui.runtime import app, App, GuiError
from velice.gui import widgets as W
from velice.gui import dialogs as D
from velice.interpreter import VLFunction, VeliceError


# ── widget constructors ─────────────────────────────────────────────────
def Window(title="Velice", width=800, height=600, **kw):
    return W.Window(title=title, width=width, height=height, **kw)


def Frame(**kw):
    return W.Frame(**kw)


def ScrollPane(width=320, height=200, vertical=True, horizontal=False, **kw):
    return W.ScrollPane(width=width, height=height, vertical=vertical,
                        horizontal=horizontal, **kw)


def Button(text="", **kw):
    return W.Button(text=text, **kw)


def Label(text="", **kw):
    return W.Label(text=text, **kw)


def TextField(text="", **kw):
    return W.TextField(text=text, **kw)


def PasswordField(text="", **kw):
    return W.PasswordField(text=text, **kw)


def TextArea(text="", **kw):
    return W.TextArea(text=text, **kw)


def Checkbox(text="", **kw):
    return W.Checkbox(text=text, **kw)


def RadioButton(text="", **kw):
    return W.RadioButton(text=text, **kw)


def ToggleSwitch(text="", **kw):
    return W.ToggleSwitch(text=text, **kw)


def Slider(value=50, min=0, max=100, **kw):
    return W.Slider(value=value, min=min, max=max, **kw)


def Spinner(value=0, min=0, max=100, **kw):
    return W.Spinner(value=value, min=min, max=max, **kw)


def ProgressBar(value=0, max=100, **kw):
    return W.ProgressBar(value=value, max=max, **kw)


def ComboBox(items=None, **kw):
    return W.ComboBox(items=items or [], **kw)


def ListBox(items=None, **kw):
    return W.ListBox(items=items or [], **kw)


def Table(columns=None, rows=None, **kw):
    return W.Table(columns=columns or [], rows=rows or [], **kw)


def Image(path="", **kw):
    return W.ImageView(path=path, **kw)


def Hyperlink(text="", url="", **kw):
    return W.Hyperlink(text=text, url=url, **kw)


def MenuBar(**kw):
    return W.MenuBar(**kw)


def Menu(text="", **kw):
    return W.Menu(text=text, **kw)


def MenuItem(text="", **kw):
    return W.MenuItem(text=text, **kw)


def MenuSeparator(**kw):
    return W.MenuSeparator(**kw)


def TabView(**kw):
    return W.TabView(**kw)


def Tab(text="", **kw):
    return W.Tab(text=text, **kw)


def StatusBar(text="", **kw):
    return W.StatusBar(text=text, **kw)


def Canvas(width=600, height=400, bg="#ffffff", **kw):
    return W.Canvas(width=width, height=height, bg=bg, **kw)


def CodeEditor(text="", **kw):
    return W.CodeEditor(text=text, **kw)


# ── runtime functions ───────────────────────────────────────────────────
def run():
    return app.run()


def update():
    return app.update()


def set_theme(name):
    return app.set_theme(name)


def get_window(name):
    return app.get_window(name)


def window(name):
    return app.get_window(name) or app.named.get(name)


def quit():
    if app.root is not None:
        try:
            app.root.destroy()
        except Exception:
            pass
    return None


def after(interp, ms, callback):
    app._interp = interp
    return app.after(ms, callback)


def every(interp, ms, callback):
    app._interp = interp
    return app.every(ms, callback)


def cancel(interp, timer_id):
    return app.cancel(timer_id)


for _fn in (after, every, cancel):
    _fn._wants_interp = True


# ── what is exposed to Velice programs ─────────────────────────────────
EXPORTS = {
    # widgets
    "Window": Window, "Frame": Frame, "ScrollPane": ScrollPane,
    "Button": Button, "Label": Label,
    "TextField": TextField, "PasswordField": PasswordField,
    "TextArea": TextArea, "Checkbox": Checkbox, "RadioButton": RadioButton,
    "ToggleSwitch": ToggleSwitch, "Slider": Slider, "Spinner": Spinner,
    "ProgressBar": ProgressBar, "ComboBox": ComboBox, "ListBox": ListBox,
    "Table": Table, "Image": Image, "Hyperlink": Hyperlink,
    "MenuBar": MenuBar, "Menu": Menu, "MenuItem": MenuItem,
    "MenuSeparator": MenuSeparator, "TabView": TabView, "Tab": Tab,
    "StatusBar": StatusBar, "Canvas": Canvas, "CodeEditor": CodeEditor,
    # runtime
    "run": run, "update": update, "set_theme": set_theme,
    "get_window": get_window, "window": window, "quit": quit,
    "after": after, "every": every, "cancel": cancel,
    # dialogs
    "toast": D.toast, "alert": D.alert, "confirm": D.confirm,
    "input_box": D.input_box, "open_file": D.open_file,
    "save_file": D.save_file, "choose_folder": D.choose_folder,
    "choose_color": D.choose_color, "choose_font": D.choose_font,
    "message_box": D.message_box, "FileChooser": D.file_chooser,
    "clipboard": D.clipboard,
}


def make_builtin(interp, name, obj):
    """Wrap a Python callable so Velice can call it as a native function."""
    if not callable(obj):
        return obj

    def run(i, args, kwargs):
        try:
            if getattr(obj, "_wants_interp", False):
                return obj(i, *args, **kwargs)
            return obj(*args, **kwargs)
        except GuiError as e:
            raise VeliceError(f"GUI error: {e}")
        except VeliceError:
            raise

    return VLFunction(name, [], None, None, is_native=True, native_fn=run)


__all__ = list(EXPORTS) + ["app", "App", "GuiError", "make_builtin", "EXPORTS"]
