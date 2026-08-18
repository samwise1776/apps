"""Bridge between the Velice AST (declarative GUI DSL) and the widget runtime."""
from velice.gui.runtime import app as _app
from velice.gui import widgets as W
from velice.ast_nodes import Block
from velice.interpreter import VeliceError

_EVENT_ALIASES = {
    "onclick": "onClick", "onchange": "onChange", "oninput": "onChange",
    "onsubmit": "onSubmit", "onmouseenter": "onMouseEnter",
    "onmouseleave": "onMouseLeave", "onfocus": "onFocus", "onblur": "onBlur",
    "onkeydown": "onKeyDown", "onkeyup": "onKeyUp", "onclose": "onClose",
    "onload": "onLoad", "onselect": "onSelect",
}


class Builder:
    """Builds windows/widgets from WindowDecl / WidgetNode AST nodes."""

    def __init__(self, interp, env, app=None):
        self.interp = interp
        self.env = env
        self.app = app or _app

    def build_window(self, decl):
        props = self._props(decl.props)
        title = props.pop("title", decl.name)
        theme_name = props.pop("theme", None)
        win = W.Window(name=decl.name, title=title, app=self.app, **props)
        if theme_name:
            win.set_theme(str(theme_name))
        for child in (decl.children or []):
            if child.wtype == "event":
                body = child.children[0] if child.children else Block(0, 0, [])
                win.bind(child.wname, self._handler(body))
            else:
                win.add(self.build_widget(child, win))
        win.realize_tree()
        self._define_name(decl.name, win)
        return win

    def _define_name(self, name, widget):
        """Make a named DSL widget reachable as a Velice variable."""
        if not name:
            return
        try:
            self.interp.globals.define(str(name), widget, mutable=True)
        except Exception:
            pass

    def build_widget(self, node, parent):
        cls = self._class_for(node.wtype)
        if cls is None:
            raise VeliceError(f"GUI: unknown widget type '{node.wtype}' at line {node.line}")
        props = self._props(node.props)
        widget = cls(parent=parent, name=node.wname, app=self.app, **props)
        for ev, body in (node.events or []):
            widget.bind(ev, self._handler(body))
        for child in (node.children or []):
            if child.wtype == "event":
                widget.bind(child.wname, self._handler(child.children[0] if child.children else Block(0, 0, [])))
            else:
                widget.add(self.build_widget(child, widget))
        self._define_name(node.wname, widget)
        return widget

    def _handler(self, body):
        if not isinstance(body, Block):
            body = Block(0, 0, [body])

        def cb(info=None):
            env = self._child_env()
            env.define("event", info or {})
            return self.interp.exec(body, env)

        return cb

    def _child_env(self):
        from velice.interpreter import Env
        return Env(self.env)

    def _props(self, props):
        out = {}
        for k, v in (props or []):
            out[k] = self.interp.eval(v, self.env)
        return out

    @staticmethod
    def _class_for(wtype):
        return {
            "window": W.Window, "frame": W.Frame, "button": W.Button,
            "label": W.Label, "textfield": W.TextField,
            "passwordfield": W.PasswordField, "textarea": W.TextArea,
            "checkbox": W.Checkbox, "radiobutton": W.RadioButton,
            "toggle": W.ToggleSwitch, "slider": W.Slider, "spinner": W.Spinner,
            "progressbar": W.ProgressBar, "combobox": W.ComboBox,
            "listbox": W.ListBox, "table": W.Table, "image": W.ImageView,
            "hyperlink": W.Hyperlink, "menubar": W.MenuBar, "menu": W.Menu,
            "item": W.MenuItem, "separator": W.MenuSeparator,
            "tabs": W.TabView, "tabview": W.TabView, "tab": W.Tab,
            "page": W.Tab, "statusbar": W.StatusBar, "canvas": W.Canvas,
        }.get(str(wtype).lower())
