# App 4 — DataDocs

**Status:** Runnable development snapshot  
**Download:** [app-4.zip](app-4.zip)  
**Stack:** Java Swing

[← All apps](README.md)

DataDocs is a focused desktop text editor with a dark interface and straightforward document creation controls.

## Features

- Welcome screen with a new-document action
- Multi-line editor with word wrapping
- System font selector
- Font-size selector
- Styled dark-theme controls
- Save workflow using the native file chooser

## Requirements

- A current Java Development Kit; tested with OpenJDK 26
- A graphical desktop session

## Build and run

```bash
unzip app-4.zip
mkdir -p build
javac -d build 4/DataDocs.java
java -cp build DataDocs
```

## Usage

1. Select **New Document**.
2. Choose a font and size.
3. Write or paste text into the editor.
4. Use the save control and select a destination.

## Troubleshooting

- If the chosen font is unavailable, select another installed system font.
- If no window appears, confirm the command is running in a graphical desktop session.
- If saving fails, choose a directory where your user account has write permission.
