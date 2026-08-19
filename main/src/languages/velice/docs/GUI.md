# Velice GUI

Velice ships with a native, Tk-backed GUI standard library. There is no
C/C++/Java — everything is built on the CPython standard library, so GUI
programs run on Windows, macOS, and Linux with zero extra dependencies.

Two styles are supported:

1. **Imperative** — build widgets, bind events, and call `gui.run()`.
2. **Declarative DSL** — `window Name { ... }` blocks with `run Name`.

Both styles create the same underlying widgets and can be mixed.

---

## Quick start

```velice
import gui

window = Window("My Application", 800, 600)
button = Button("Click Me")

button.on_click {
    print("Hello from Velice")
}

window.add(button)
gui.run()
```

Run it like any script:

```bash
python3 -m velice run examples/gui.velice
```

> **Headless environments.** Without a display (CI, servers) the runtime
> automatically falls back to a *recording* backend so programs still run and
> their calls are logged on the app instead of opening windows. Force this
> mode with `VELICE_GUI=none` (or `headless`, `0`).

---

## Importing the GUI

```velice
import gui            # module + all constructors/functions into scope
from gui import Window, Button
from gui import *     # everything
import gui as g       # use as g.run(), g.Window(...)
```

`import gui` is special: it also injects every export directly, so `Window`,
`Button`, `Label`, `Canvas`, `run`, ... are usable without a prefix.

---

## Widgets

| Widget | Purpose | Main constructor args |
| --- | --- | --- |
| `Window` | Top-level window | `title, width, height` |
| `Frame` | Layout container | — |
| `ScrollPane` | Scrollable viewport container | `width, height, vertical, horizontal` |
| `Button` | Clickable button | `text` |
| `Label` | Static text | `text` |
| `TextField` | Single-line input | `text, placeholder, max_length` |
| `PasswordField` | Masked input | `text` |
| `TextArea` | Multi-line text | `text, height` |
| `Checkbox` | Boolean toggle | `text, value` |
| `RadioButton` | Option in a group | `text, radio_value` |
| `ToggleSwitch` | Switch control | `text, value` |
| `Slider` | Numeric range | `value, min, max` |
| `Spinner` | Numeric stepper | `value, min, max` |
| `ProgressBar` | Progress indicator | `value, max` |
| `ComboBox` | Dropdown | `items, selected` |
| `ListBox` | List with selection | `items, multiple` |
| `Table` | Columns + rows | `columns, rows` |
| `Image` | Picture from file | `path` |
| `Hyperlink` | Clickable link | `text, url` |
| `MenuBar` / `Menu` / `MenuItem` / `MenuSeparator` | Menus | `text` |
| `TabView` / `Tab` | Tabbed panels | `text` |
| `StatusBar` | Bottom status text | `text` |
| `Canvas` | 2D drawing surface | `width, height, bg` |

### Properties

Set properties when constructing or later via assignment:

```velice
var w = Window("App", 800, 600)
w.title = "Renamed"           # property assignment
w.add(Label("hi"))

var b = Button("Start")
b.text = "Stop"               # updates in place
b.enabled = false
b.font_size = 16
b.visible = true
```

Common properties: `text`, `value`, `width`, `height`, `enabled`,
`visible`, `tooltip`, `font_size`, `bold`, `color`, `background`,
`placeholder`, `readonly`, `max_length`, `items`, `selected`, `multiple`.

Window methods and properties:

```velice
var w = Window("App", 800, 600)
w.closable(false)        # disable the window's close button (default: true)
w.resizable = false      # lock the window size
w.maximized = true
w.always_on_top = true
w.fullscreen = true
w.close()                # close programmatically
```

Named widgets are registered so they can be looked up later:

```velice
Button("x", name="btn1")      # registered as app.named["btn1"]
gui.window("btn1")            # lookup helper
```

---

## Events

Bind handlers with a trailing block after the event name. Inside the block,
`event` is a map with `x` / `y` / `key` / `state` for mouse and keyboard
events.

```velice
button.on_click      { print("clicked at", event.x, event.y) }
input.on_change      { print("text:", input.text) }
window.on_key_down   { print("key:", event.key) }
window.on_mouse_move { print("mouse:", event.x, event.y) }
window.on_close      { print("closing") }
list.on_select       { print("chose:", list.selected) }
slider.on_change     { print("value:", slider.value) }
```

Events are snake_case in the imperative API; the camelCase names
(`onClick`, `onKeyDown`, `onClose`, ...) also work.

| Event | Fires when |
| --- | --- |
| `on_click` / `onClick` | Widget clicked |
| `on_double_click` | Double-clicked |
| `on_change` | Value changed (input, checkbox, slider) |
| `on_select` | List/combobox selection |
| `on_key_down` / `on_key_up` | Key pressed / released |
| `on_mouse_move` | Mouse moved over the widget |
| `on_mouse_enter` / `on_mouse_leave` | Mouse entered / left |
| `on_mouse_wheel` | Wheel scrolled |
| `on_focus` / `on_blur` | Widget gained / lost focus |
| `on_load` | Window shown |
| `on_close` | Window closing |

---

## Dialogs & clipboard

```velice
gui.alert("Hello!")                      # info box
gui.toast("Saved", 2.0)                  # transient toast
var ok = gui.confirm("Are you sure?")    # yes/no -> true/false
var name = gui.input_box("Name?", "Ask") # text input
var path = gui.open_file("Open")         # file picker
var path = gui.save_file("Save")         # save picker
var dir = gui.choose_folder("Pick")
var color = gui.choose_color()
var font = gui.choose_font()
var chosen = FileChooser("Pick a file")            # open dialog -> path (nil if cancelled)
var chosen = FileChooser("Pick", "save")           # mode: open | save | folder
var chosen = FileChooser("Pick", "folder", "/home")# optional start directory

gui.clipboard.copy("text")               # copy
var t = gui.clipboard.paste()            # paste
```

`FileChooser` is also exported directly (no `gui.` prefix needed):

```velice
var picked = FileChooser("Pick a file", "open", "/home/${User.name}")
if picked != nil {
    print("you chose:", picked)
}
```

## Scrolling containers

`ScrollPane` is a container whose children live in a scrollable viewport.
Put large content inside it instead of directly in the window:

```velice
import gui

var win = Window("Browser", 700, 500)

var pane = ScrollPane(640, 400)          # width, height of the viewport
var text = TextArea("line1\nline2\n...") # any widgets can go inside
text.set_readonly(true)
pane.add(text)

pane.scroll_to_top()                     # jump to the top
pane.scroll_to_bottom()                  # or bottom
pane.scroll_to(0.0, 0.5)                 # or a fraction (x, y) in 0..1

win.add(pane)
win.show()
gui.run()
```

Pass `horizontal=true` to `ScrollPane` to enable a horizontal scrollbar too.

---

## Canvas drawing

```velice
var c = Canvas(600, 400, "#ffffff")
c.draw_rect(20, 20, 80, 80, "#e74c3c")       # x, y, w, h, color
c.draw_oval(120, 20, 80, 80, "#2ecc71")
c.draw_line(20, 150, 580, 150, "#3498db", 3) # x1,y1,x2,y2,color,width
c.draw_polygon([300, 250, 350, 180, 400, 250], "#f1c40f")
c.draw_text(300, 320, "Hi", "#34495e", 20, true)  # ...size, bold
c.draw_arc(460, 20, 80, 80, 0, 120, "#9b59b6", 4)
c.clear()
c.screenshot("out.ps")
window.add(c)
```

---

## Runtime functions

| Function | Purpose |
| --- | --- |
| `gui.run()` | Start the event loop (no-op headless) |
| `gui.update()` | Flush pending UI updates |
| `gui.set_theme("Dark")` | Switch theme |
| `gui.window(name)` / `gui.get_window(name)` | Look up a named window/widget |
| `gui.quit()` | Close the app |

---

## Declarative DSL

```velice
import gui

window Main {
    title = "Declarative DSL"
    width = 640

    Label header {
        text = "Velice GUI"
        font_size = 18
        bold = true
    }

    Button greet {
        text = "Greet"
        onClick {
            print("Hello, world!")
        }
    }
}

run Main
```

- Widget names become Velice variables inside event handlers (`greet`, `header`).
- Events in the DSL use camelCase (`onClick`, `onChange`, `onClose`).
- `run Name` shows a declared window.

---

## Error handling

GUI problems raise normal Velice runtime errors:

- `from gui import NoSuchWidget` → `GUI: no such member 'NoSuchWidget'`
- `window Main { Bogus w { } }` → `GUI: unknown widget type 'Bogus'`
- `run Missing` → `GUI: no window named 'Missing'`
- Tk problems (e.g. no display) fall back to the headless recording backend
  instead of crashing.

## Tests & examples

```bash
# run the whole suite (GUI tests are headless)
PYTHONPATH=. python3 -m unittest discover -s tests

# run a GUI example in headless mode
VELICE_GUI=none python3 -m velice run examples/gui.velice
```

More examples: `examples/gui.velice`, `gui_events.velice`,
`gui_canvas.velice`, `gui_dialogs.velice`, `gui_dsl.velice`.
