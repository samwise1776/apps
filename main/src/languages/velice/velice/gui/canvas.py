"""2D canvas for Velice GUI."""
import os

from velice.gui.runtime import tk as _tk, app as _app


class Canvas:
    def __init__(self, width=600, height=400, bg="#ffffff", app=None):
        self.app = app or _app
        self.tk = None
        self.width, self.height = int(width), int(height)
        self.bg = bg
        self._items = {}

    def _ensure(self):
        if self.tk is None and not self.app.headless and self.app.root is not None:
            self.tk = _tk.Canvas(self.app.root, width=self.width, height=self.height,
                                 bg=self.bg, highlightthickness=0)

    def draw_rect(self, x, y, w, h, color="#000000", outline="", width=1):
        self._ensure()
        if self.tk is not None:
            return self.tk.create_rectangle(x, y, x + w, y + h, fill=color,
                                            outline=outline or color, width=width)
        return self.app.record("canvas.rect", x=x, y=y, w=w, h=h, color=color)

    def fill_rect(self, x, y, w, h, color="#000000"):
        return self.draw_rect(x, y, w, h, color, outline="", width=0)

    def draw_oval(self, x, y, w, h, color="#000000", outline="", width=1):
        self._ensure()
        if self.tk is not None:
            return self.tk.create_oval(x, y, x + w, y + h, fill=color,
                                       outline=outline or color, width=width)
        return self.app.record("canvas.oval", x=x, y=y, w=w, h=h, color=color)

    def draw_line(self, x1, y1, x2, y2, color="#000000", width=1):
        self._ensure()
        if self.tk is not None:
            return self.tk.create_line(x1, y1, x2, y2, fill=color, width=width)
        return self.app.record("canvas.line", x1=x1, y1=y1, x2=x2, y2=y2, color=color)

    def draw_polygon(self, points, color="#000000", outline="", width=1):
        self._ensure()
        if self.tk is not None:
            return self.tk.create_polygon(*points, fill=color,
                                          outline=outline or color, width=width)
        return self.app.record("canvas.polygon", points=points, color=color)

    def draw_text(self, x, y, text, color="#000000", size=12, bold=False, anchor="center"):
        self._ensure()
        if self.tk is not None:
            font = ("Helvetica", int(size), "bold" if bold else "normal")
            return self.tk.create_text(x, y, text=str(text), fill=color, font=font,
                                       anchor=self._anchor(anchor))
        return self.app.record("canvas.text", x=x, y=y, text=str(text), color=color)

    def draw_arc(self, x, y, w, h, start=0, extent=90, color="#000000", width=1):
        self._ensure()
        if self.tk is not None:
            return self.tk.create_arc(x, y, x + w, y + h, start=start, extent=extent,
                                      outline=color, width=width, style="arc")
        return self.app.record("canvas.arc", x=x, y=y, w=w, h=h, color=color)

    def _anchor(self, a):
        return {"center": "center", "nw": "nw", "ne": "ne", "sw": "sw",
                "se": "se", "n": "n", "s": "s", "e": "e", "w": "w"}.get(str(a), "center")

    def clear(self):
        self._ensure()
        if self.tk is not None:
            self.tk.delete("all")
        return self.app.record("canvas.clear")

    def update(self, x=0, y=0):
        self._ensure()
        if self.tk is not None:
            self.tk.pack(side="top", anchor="nw", padx=x, pady=y)

    def screenshot(self, path, app=None):
        app = app or self.app
        if app.headless or self.tk is None:
            return app.record("canvas.screenshot", path=str(path))
        try:
            self.tk.update()
            self.tk.postscript(file=str(path))
            return str(path)
        except Exception:
            return str(path)


def make_canvas(width=600, height=400, bg="#ffffff"):
    return Canvas(width, height, bg)
