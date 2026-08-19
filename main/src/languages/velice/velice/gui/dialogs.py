"""Dialogs, toasts, file pickers, and clipboard for Velice GUI."""
import os

from velice.gui.runtime import app as _app, tk, messagebox, filedialog, colorchooser


def toast(message, duration=2.0, app=None):
    app = app or _app
    if app.headless or app.root is None:
        return app.record("toast", message=message)
    try:
        top = tk.Toplevel(app.root)
        top.overrideredirect(True)
        top.attributes("-topmost", True)
        lab = tk.Label(top, text=str(message), bg="#333333", fg="#ffffff",
                       padx=18, pady=10, font=("Helvetica", 11))
        lab.pack()
        top.update_idletasks()
        top.geometry(f"+{app.root.winfo_screenwidth() - top.winfo_reqwidth() - 30}+30")
        top.after(int(duration * 1000), top.destroy)
    except Exception:
        pass


def alert(message, title="Velice", app=None):
    app = app or _app
    if app.headless:
        return app.record("alert", message=message, title=title)
    if messagebox is not None:
        return messagebox.showinfo(str(title), str(message))


def confirm(message, title="Velice", app=None):
    app = app or _app
    if app.headless:
        return app.record("confirm", message=message, title=title, **{"return": True})
    if messagebox is not None:
        return messagebox.askyesno(str(title), str(message))


def input_box(prompt="", title="Velice", default="", app=None):
    app = app or _app
    if app.headless:
        return app.record("input", prompt=prompt, title=title, **{"return": default})
    import tkinter.simpledialog as simpledialog
    return simpledialog.askstring(str(title), str(prompt), initialvalue=str(default))


def open_file(title="Open File", filters=(), app=None):
    app = app or _app
    if app.headless:
        return app.record("open_file", title=title, **{"return": None})
    if filedialog is not None:
        return filedialog.askopenfilename(title=str(title))


def save_file(title="Save File", default_name="", app=None):
    app = app or _app
    if app.headless:
        return app.record("save_file", title=title, **{"return": "/tmp/" + str(default_name)})
    if filedialog is not None:
        return filedialog.asksaveasfilename(title=str(title), initialfile=str(default_name))


def choose_folder(title="Choose Folder", app=None):
    app = app or _app
    if app.headless:
        return app.record("choose_folder", title=title, **{"return": None})
    if filedialog is not None:
        return filedialog.askdirectory(title=str(title))


def file_chooser(title="Choose File", mode="open", start_dir=None, filters=(), app=None):
    """Native file chooser dialog.

    mode: "open" | "save" | "folder". Returns the chosen path as a string
    (or nil when the dialog is cancelled).
    """
    app = app or _app
    if app.headless:
        return app.record("file_chooser", title=title, mode=mode,
                          start_dir=start_dir, **{"return": None})
    if filedialog is not None:
        kw = {"title": str(title)}
        if start_dir:
            kw["initialdir"] = str(start_dir)
        if mode == "save":
            return filedialog.asksaveasfilename(**kw)
        if mode == "folder":
            return filedialog.askdirectory(**kw)
        return filedialog.askopenfilename(**kw)


def choose_color(title="Choose Color", app=None):
    app = app or _app
    if app.headless:
        return app.record("choose_color", title=title, **{"return": "#7c6cf0"})
    if colorchooser is not None:
        rgb, hexs = colorchooser.askcolor(title=str(title))
        return hexs


def choose_font(title="Choose Font", app=None):
    app = app or _app
    if app.headless:
        return app.record("choose_font", title=title, **{"return": "Helvetica 12"})
    from tkinter import font as tkfont
    if tkfont is not None:
        result = tkfont.families()
        return result[0] if result else "Helvetica"


def message_box(message, kind="info", title="Velice", app=None):
    """kind: info | warning | error | question | confirm."""
    app = app or _app
    if app.headless or messagebox is None:
        return app.record("message_box", message=message, kind=kind, **{"return": True})
    fns = {"info": messagebox.showinfo, "warning": messagebox.showwarning,
           "error": messagebox.showerror, "question": messagebox.askyesno,
           "confirm": messagebox.askyesno}
    return fns.get(kind, messagebox.showinfo)(str(title), str(message))


class Clipboard:
    def copy(self, text, app=None):
        app = app or _app
        if app.headless:
            return app.record("clipboard.copy", text=text)
        if app.root is not None:
            app.root.clipboard_clear()
            app.root.clipboard_append(str(text))
        return True

    def paste(self, app=None):
        app = app or _app
        if app.headless:
            return app.record("clipboard.paste", **{"return": ""})
        if app.root is not None:
            try:
                return app.root.clipboard_get()
            except Exception:
                return ""
        return ""

    def cut(self, text, app=None):
        self.copy(text, app)
        return ""

    def has_text(self, app=None):
        return isinstance(self.paste(app), str)


clipboard = Clipboard()
