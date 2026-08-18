"""GUI runtime: owns the Tk backend, windows, widget registry, and main loop.

The backend is Tk (part of the CPython standard library), which works on
Windows, macOS, and Linux. For headless environments (CI, servers) the
runtime falls back to a recording backend so programs still run.
"""
import os
import time

HEADLESS = os.environ.get("VELICE_GUI", "").lower() in ("none", "headless", "0")

try:
    import tkinter as tk
    from tkinter import ttk, messagebox, filedialog, colorchooser, font as tkfont
    _tk_import_error = None
except Exception as e:  # pragma: no cover
    tk = None
    _tk_import_error = e

_timer_counter = [0]


def _next_timer_id():
    _timer_counter[0] += 1
    return _timer_counter[0]


class GuiError(Exception):
    pass


def backend_available():
    return tk is not None and (HEADLESS or _has_display())


def _has_display():
    if os.name == "nt":
        return True
    try:
        import subprocess
        r = subprocess.run(["xdpyinfo"], capture_output=True)
        if r.returncode == 0:
            return True
    except Exception:
        pass
    return "DISPLAY" in os.environ


class App:
    def __init__(self, headless=None):
        self.headless = HEADLESS if headless is None else headless
        self.root = None
        self.windows = {}      # name -> Window
        self.named = {}        # name -> Widget
        self.theme_name = "Dark"
        self.theme = {}
        self._event_handlers = {}
        self._timers = {}
        self._interp = None
        self.log = []

    def ensure_root(self):
        if self.root is None and not self.headless:
            if tk is None:
                raise GuiError(f"tkinter unavailable: {_tk_import_error}")
            try:
                self.root = tk.Tk()
                self.root.withdraw()
            except Exception as e:
                # no usable display -> fall back to the recording backend
                self.headless = True
                self.record("fallback_headless", reason=str(e))
                self.root = None
        return self.root

    # ── registry ────────────────────────────────────────────────────────
    def add_window(self, window):
        self.windows[window.name or window.title or len(self.windows)] = window
        if window.name:
            self.named[window.name] = window
        return window

    def get_window(self, name):
        return self.windows.get(name) or self.named.get(name)

    # ── theme ──────────────────────────────────────────────────────────
    def set_theme(self, name):
        from velice.gui import theme as theme_mod
        if name not in theme_mod.THEMES:
            raise GuiError(f"unknown theme '{name}' (available: {', '.join(theme_mod.THEMES)})")
        self.theme_name = name
        self.theme = dict(theme_mod.THEMES[name])
        if self.root is not None:
            self.root.configure(bg=self.theme["background"])
            for w in self._all_widgets():
                w.apply_theme()
        return name

    def _all_widgets(self):
        out = []
        for win in self.windows.values():
            out.append(win)
            out.extend(win.descendants())
        return out

    # ── main loop ──────────────────────────────────────────────────────
    def run(self):
        if self.headless:
            return
        self.ensure_root()
        if not self.windows:
            self.root.mainloop()
        else:
            for win in self.windows.values():
                win.realize_tree()
                win.show()
            self.root.mainloop()

    def update(self):
        if self.root is not None:
            self.root.update_idletasks()
            self.root.update()

    # ── record / headless ──────────────────────────────────────────────
    def record(self, event, **kw):
        self.log.append((event, kw))
        return kw.get("return", None)

    # ── timers ─────────────────────────────────────────────────────────
    def after(self, ms, callback):
        """Run `callback` once after `ms` milliseconds."""
        return self._schedule(ms, callback, repeat=False)

    def every(self, ms, callback):
        """Run `callback` every `ms` milliseconds."""
        return self._schedule(ms, callback, repeat=True)

    def cancel(self, timer_id):
        if timer_id is None:
            return None
        info = self._timers.pop(timer_id, None)
        if info and self.root is not None:
            try:
                self.root.after_cancel(info.get("tk_id"))
            except Exception:
                pass
        return None

    def _schedule(self, ms, callback, repeat):
        timer_id = _next_timer_id()
        self._timers[timer_id] = {
            "ms": int(ms), "callback": callback, "repeat": bool(repeat),
            "interp": getattr(self, "_interp", None),
            "due": time.monotonic() + (int(ms) / 1000.0),
        }
        if self.root is not None:
            tk_id = self.root.after(int(ms), lambda: self._timer_tick(timer_id))
            self._timers[timer_id]["tk_id"] = tk_id
        else:
            self.record("timer", id=timer_id, ms=int(ms), repeat=bool(repeat))
        return timer_id

    def _timer_tick(self, timer_id):
        info = self._timers.get(timer_id)
        if info is None:
            return
        callback = info.get("callback")
        interp = info.get("interp") or getattr(self, "_interp", None)
        try:
            if callback is not None:
                if hasattr(callback, "call"):
                    callback.call(interp, [])
                else:
                    callback()
        except Exception:
            pass
        if not info.get("repeat"):
            self._timers.pop(timer_id, None)
            return
        if self.root is not None:
            tk_id = self.root.after(info.get("ms", 100), lambda: self._timer_tick(timer_id))
            self._timers[timer_id]["tk_id"] = tk_id
        else:
            self._timers[timer_id]["due"] = time.monotonic() + (info.get("ms", 100) / 1000.0)

    def run_timers(self):
        """Run any due timers; useful for headless tests."""
        due = [tid for tid, info in list(self._timers.items())
               if info.get("tk_id") is None and time.monotonic() >= info.get("due", 0)]
        for tid in due:
            self._timer_tick(tid)


app = App()
