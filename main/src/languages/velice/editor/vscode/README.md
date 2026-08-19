# Velice for VS Code

Language support for the **Velice** programming language:

- Syntax highlighting (TextMate grammar, `source.velice`)
- Custom **file icon** for `.velice` files in the Explorer
- Code snippets (functions, loops, match, classes, try/catch, pipes, and more)
- GUI snippets (`importgui`, `guiwindow`, `guibutton`, `guiton`, `guicanvas`, …)
- Run / REPL commands wired to the `velice` CLI
- Language configuration (comments, brackets, auto-indent)

## Features

| Feature | Detail |
| --- | --- |
| File icon | Rounded "V" logo for `.velice` files in the Explorer |
| Grammar | Full token coloring: strings, numbers, keywords (`argu`, …), types, builtins (`readFileLines`, `contains`, `ends`, `starts`, `inbetween`, …), operators, GUI events |
| Snippets | `let`, `fn`, `class`, `match`, `try`, `for`, `|>`, `argu`, `contains`, `ends`, `inbetween`, `readfilelines`, `importgui`, `guiwindow`, … |
| Commands | `Velice: Run File`, `Velice: Open REPL` |

### Language features

- `argu.0`, `argu[0]`, `argu.len` — access command-line arguments (`argu` = all args).
- String methods: `str.contains(x)`, `str.starts(x)`, `str.ends(x)`,
  `str.inbetween("()")`, `str.len`.
- `readFileLines(path)` returns a line reader: `lines.contains(x)`,
  `lines.currentLine()`, `lines.hasMore()`, `lines.current()`,
  `lines.reset()`, `lines.len`.

## GUI

Velice has a native (Tk-backed) GUI library — see `docs/GUI.md`. GUI programs
can be run directly from the extension. On machines without a display, run them
headless by setting `velice.guiHeadless` (or the `VELICE_GUI=none` env var),
which records the GUI calls instead of opening windows.

## Install (local)

1. Open this folder in VS Code.
2. Press `F5` to launch the Extension Development Host.
3. The `velice` language is now active for `.velice` files.

Or package and install manually:

```
npm install -g @vscode/vsce
vsce package
code --install-extension velice-0.1.0.vsix
```

## Enable the Velice file icon theme

The extension ships a file icon theme so `.velice` files get a custom icon while
everything else keeps your normal icons.

1. Open the Command Palette (`Ctrl+Shift+P`).
2. Run **Preferences: File Icon Theme**.
3. Select **Velice Icons**.

## Using the CLI integration

The extension calls the `velice` command (or `python3 -m velice`). Set the
path if it's not on your `PATH`:

```json
"velice.executablePath": "python3 -m velice"
```

- `Velice: Run File` — runs the open `.velice` file and prints output to the
  **Velice** output channel.
- `Velice: Open REPL` — opens an integrated terminal with the REPL.

## Project structure

```
vscode/
├── extension.js                 # activation + commands
├── package.json                 # manifest
├── language-configuration.json  # brackets, comments, indentation
├── snippets/
│   └── velice.code-snippets     # code snippets
├── syntaxes/
│   └── velice.tmLanguage.json   # syntax highlighting grammar
├── themes/
│   └── velice-icon-theme.json   # file icon theme
└── icons/
    └── velice-icon.svg          # the Velice logo / file icon
```
