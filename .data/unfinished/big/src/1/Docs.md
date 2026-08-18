# Midnight Text Utility

This project contains a small Java Swing text utility with a midnight-dark-blue
background and a reusable rounded button. It counts the words, characters, and
lines in text entered by the user.

## Files

- `App.java` creates the main `JFrame` and the text statistics utility.
- `RoundedButton.java` paints a simple rounded `JButton`.
- `ai-inf.md` contains the original application requirements.

## Compile and run

Run these commands from `/home/ray/Data`:

```bash
javac big/src/App.java big/src/RoundedButton.java
java big.src.App
```

The window opens at a minimum size of 640 by 420 pixels. Enter text in the main
area and press **Analyze Text** to display its word, character, and line counts.

## Design

The main panel uses RGB color `(8, 18, 38)` for a midnight-dark-blue theme.
Text uses soft white and muted blue shades, while the rounded button uses a
brighter blue accent for contrast.

## Utility behavior

- Words are groups of characters separated by whitespace.
- Characters include spaces and line breaks.
- An empty text area has zero lines; otherwise, each entered line is counted.
