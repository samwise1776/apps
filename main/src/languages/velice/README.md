# Velice

A general-purpose, dynamic, multi-paradigm programming language with a clean
C-family syntax — implemented as a dependency-free, tree-walking interpreter in
pure Python.

[![Quality](https://github.com/samwise1776/velice/actions/workflows/quality.yml/badge.svg)](https://github.com/samwise1776/velice/actions/workflows/quality.yml)
[![Python 3.10+](https://img.shields.io/badge/Python-3.10%2B-3776AB?logo=python&logoColor=white)](https://www.python.org/)
[![MIT](https://img.shields.io/badge/license-MIT-14b8a6.svg)](LICENSE)

**[Read the complete 25-chapter guide](https://samwise1776.github.io/apps/velice.html)** ·
**[Download Velice](https://github.com/samwise1776/apps/raw/refs/heads/main/apps/velice/velice.zip)** ·
**[Open Datacenter](https://samwise1776.github.io/apps/)**

```velice
fn fib(n) {
    return match n {
        n if n < 2 => n,
        _ => fib(n - 1) + fib(n - 2),
    }
}
print("fib(10) =", fib(10))   # → fib(10) = 55
```

## Features

- Immutable-by-default bindings (`let`, `let mut`, `const`)
- Functions as first-class values: closures, recursion, default args
- Pattern matching with guards (`match ... { _ => ... }`)
- Classes with single inheritance, traits, and structs
- Error handling: `try` / `catch` / `finally`, `throw`, `??`
- Functional helpers: pipes `|>`, `map`, `filter`, `reduce`
- Full standard library: collections, JSON, hashing, time, math, random
- Native GUI standard library (Tk-backed, zero extra dependencies):
  windows, buttons, inputs, tables, canvas drawing, dialogs, and events
- Descr1be: a visual UI builder written in Velice with design, code, and AI modes
- VS Code extension with syntax highlighting and file icons

## Quick start

```bash
# download the source archive, extract it, then open that folder

# run a script
python3 -m velice run examples/hello_world.velice

# REPL
python3 -m velice

# evaluate an expression
python3 -m velice eval "1 + 2 * 3"

# install the CLI
pip install -e .
velice run examples/hello_world.velice
```

Velice needs Python 3.10 or newer. The interpreter itself has no third-party
runtime dependencies. Linux users may need their distribution's Tk package for
visible GUI windows; use `VELICE_GUI=none` for headless servers and CI.

## Project layout

```
velice/
├── velice/            # the interpreter (lexer, parser, AST, evaluator, REPL)
├── examples/          # runnable .velice programs
├── docs/              # language specification and GUI reference
├── editor/vscode/     # VS Code extension (syntax, icons, snippets)
├── tests/             # unit tests
└── setup.py           # pip packaging
```

## Hello, World

```velice
print("Hello, World!")
```

## GUI

Velice ships with a native GUI library — no extra dependencies, no C/C++/Java:

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

A declarative DSL is also available (`window Main { ... }` / `run Main`).
See `docs/GUI.md` and `examples/gui*.velice`. On machines without a display,
set `VELICE_GUI=none` to run GUI programs headless.

## Descr1be

A visual UI builder bundled with Velice and written in Velice. Add widgets on a
design canvas, edit generated declarative source, or describe an app in plain
English — the AI mode uses built-in heuristics with no external service.

```bash
python3 -m velice run descr1be/src/main.velice
```

## Documentation

| Resource | What it covers |
| --- | --- |
| [Complete website guide](https://samwise1776.github.io/apps/velice.html) | 25 searchable chapters, copyable highlighted examples, GUI, tools, packages, and complete quick reference |
| [Language specification](docs/SPEC.md) | Grammar, semantics, operators, standard library, and command-line interface |
| [GUI reference](docs/GUI.md) | Widgets, layout, properties, events, dialogs, canvas, themes, and declarative UI |
| [Examples](examples/) | Small runnable programs for the language and GUI |
| [VS Code extension](editor/vscode/) | Local editor installation and commands |

Every code block in the website guide has a Copy button. Search narrows all 25
chapters instantly, and chapter progress is saved locally in the browser.

## VS Code

Install the extension from `editor/vscode/` (see its README) to get syntax
highlighting, a custom `.velice` file icon, snippets, and run commands.

## Verify a checkout

```bash
python3 -m compileall -q velice
VELICE_GUI=none python3 -m unittest discover -s tests -v
```

The same checks run automatically on supported Python versions for every push
and pull request.

## License

[MIT](LICENSE)
