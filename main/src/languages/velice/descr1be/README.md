# Descr1be

A visual UI builder written in **Velice** and running on its native GUI library.
Design a window on a canvas, edit the generated declarative source, or describe
an app in plain English — the AI mode uses built-in heuristics with no external
service.

![mode](https://img.shields.io/badge/modes-Design%20%C2%B7%20Code%20%C2%B7%20AI-14b8a6.svg)
![dep](https://img.shields.io/badge/dependencies-zero-3776AB.svg)

## Run

```bash
unzip descr1be.zip
cd descr1be
python3 -m velice run src/main.velice
```

With the Velice CLI installed:

```bash
unzip descr1be.zip
cd descr1be
velice run src/main.velice
```

On a machine without a display, use `VELICE_GUI=none` (Descr1be is a GUI
application, so use this only for the headless smoke test).

## How it works

- **Design** — pick widgets from the palette and place them on the canvas; the
  inspector edits name, position, size, text, and other properties. Every
  change lands on the undo stack.
- **Code** — the canvas is generated as a declarative
  `window ... { ... }` source file. Edit it freely; switching back to Design
  re-parses the source into the project model.
- **AI** — type a short prompt such as "a login form" or "a counter with a
  button"; built-in heuristics produce a ready-made widget tree.
- **Run** launches the current project; **Export** writes the generated
  `.velice` file.

## Files

| File | What it does |
| --- | --- |
| `src/main.velice` | Entry point — imports the app and starts the GUI loop |
| `src/app.velice` | Main window, panels, and project handlers |
| `src/model.velice` | Widget/project data model |
| `src/theme.velice` | Color themes for the builder |
| `src/history.velice` | Undo/redo state history |
| `src/generator.velice` | Project model → editable Velice source |
| `src/sourceparse.velice` | Source → project model (round-trip) |
| `src/aibuilder.velice` | Prompt → widget tree heuristics |
| `src/projectio.velice` | Save/open projects |
| `src/render.velice` | Canvas rendering of widgets |
| `src/editor.velice` | Code panel editor |
| `src/inspector.velice` | Property inspector |
| `src/console.velice` | Output console |
| `src/templates.velice` | Widget templates |
| `src/t_smoke.velice` | Headless smoke test |

## Smoke test

```bash
VELICE_GUI=none python3 -m velice run src/t_smoke.velice
```

The `velice/` folder in this package is the Velice interpreter that Descr1be
runs on. See the [Velice guide](https://samwise1776.github.io/apps/velice.html)
for the full language reference.

## License

MIT.
