package editor;

import java.util.ArrayList;
import java.util.List;

/**
 * Line-based text document with bounded undo/redo. The UI editor keeps its own
 * caret/selection, while this model owns the text, tabs, and edit history.
 */
public final class DocumentModel {
    public static final int MAX_UNDO = 500;

    private final List<String> lines;
    private final java.util.ArrayDeque<Edit> undoStack = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<Edit> redoStack = new java.util.ArrayDeque<>();
    private final java.util.List<Listener> listeners = new ArrayList<>();
    private String name;
    private boolean dirty;
    private int savedVersion = 0;
    private int version = 0;

    public interface Listener {
        void onTextChanged(DocumentModel document);
    }

    public DocumentModel(String text) {
        this.lines = new ArrayList<>(util.Text.lines(text == null ? "" : text));
    }

    public DocumentModel() {
        this("");
    }

    public void setName(String name) {
        this.name = name;
    }

    public String name() {
        return name == null ? "untitled" : name;
    }

    public int lineCount() {
        return lines.size();
    }

    public String line(int index) {
        if (index < 0 || index >= lines.size()) return "";
        return lines.get(index);
    }

    public List<String> allLines() {
        return new ArrayList<>(lines);
    }

    public String text() {
        return util.Text.joinLines(lines);
    }

    public int textLength() {
        int length = 0;
        for (String line : lines) {
            length += line.length();
        }
        return length + Math.max(0, lines.size() - 1);
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean isModified() {
        return version != savedVersion;
    }

    public void markSaved() {
        savedVersion = version;
        dirty = false;
        notifyChanged();
    }

    /** Inserts a string at a caret position expressed as (line, column). */
    public void insert(int line, int column, String text) {
        if (text.isEmpty()) return;
        Edit edit = new Edit(line, column, text, "");
        replaceRange(line, column, "", text);
        version++;
        dirty = true;
        record(edit);
    }

    private void replaceRange(int startLine, int startColumn, String removed, String inserted) {
        List<String> removedLines = util.Text.lines(removed);
        String firstLine = lines.get(startLine);
        String startText = firstLine.substring(0, startColumn);
        List<String> insertedLines = util.Text.lines(inserted);
        int endLine = startLine + removedLines.size() - 1;
        String endText = lines.get(endLine).substring(removedLines.get(removedLines.size() - 1).length());

        List<String> newLines = new ArrayList<>(lines.subList(0, startLine));
        newLines.add(startText + (insertedLines.size() == 1 ? insertedLines.get(0) + endText : insertedLines.get(0)));
        for (int i = 1; i < insertedLines.size() - 1; i++) {
            newLines.add(insertedLines.get(i));
        }
        if (insertedLines.size() > 1) {
            newLines.add(insertedLines.get(insertedLines.size() - 1) + endText);
        }
        newLines.addAll(lines.subList(endLine + 1, lines.size()));
        lines.clear();
        lines.addAll(newLines);
    }

    /** Removes a range of text and remembers it for undo. */
    public void delete(int startLine, int startColumn, int endLine, int endColumn) {
        String removed = textBetween(startLine, startColumn, endLine, endColumn);
        if (removed.isEmpty()) return;
        Edit edit = new Edit(startLine, startColumn, "", removed);
        replaceRange(startLine, startColumn, removed, "");
        version++;
        dirty = true;
        record(edit);
    }

    /** Deletes an entire line. */
    public void deleteLine(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= lines.size()) return;
        String removed = lines.get(lineIndex);
        lines.remove(lineIndex);
        version++;
        dirty = true;
        record(new Edit(lineIndex, 0, "", removed));
    }

    /** Inserts a blank line after the given index. */
    public void insertLine(int afterIndex) {
        lines.add(Math.min(afterIndex + 1, lines.size()), "");
        version++;
        dirty = true;
        record(new Edit(Math.min(afterIndex + 1, lines.size()) - 1, 0, "", "\n"));
    }

    public String textBetween(int startLine, int startColumn, int endLine, int endColumn) {
        if (startLine == endLine) {
            String line = lines.get(startLine);
            return line.substring(Math.min(startColumn, line.length()), Math.min(endColumn, line.length()));
        }
        StringBuilder out = new StringBuilder();
        out.append(lines.get(startLine).substring(Math.min(startColumn, lines.get(startLine).length()))).append('\n');
        for (int i = startLine + 1; i < endLine; i++) {
            out.append(lines.get(i)).append('\n');
        }
        out.append(lines.get(endLine), 0, Math.min(endColumn, lines.get(endLine).length()));
        return out.toString();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo() {
        Edit edit = undoStack.pollLast();
        if (edit == null) return;
        if (edit.removed.isEmpty()) {
            String current = lines.get(edit.line);
            int column = Math.min(edit.column, current.length());
            lines.set(edit.line, current.substring(0, column) + current.substring(Math.min(column + edit.inserted.length(), current.length())));
        } else {
            replaceRange(edit.line, edit.column, edit.inserted, edit.removed);
        }
        version++;
        dirty = true;
        redoStack.addLast(edit);
        notifyChanged();
    }

    public void redo() {
        Edit edit = redoStack.pollLast();
        if (edit == null) return;
        if (edit.removed.isEmpty()) {
            String current = lines.get(edit.line);
            lines.set(edit.line, current.substring(0, edit.column) + edit.inserted + current.substring(edit.column));
        } else {
            replaceRange(edit.line, edit.column, edit.removed, edit.inserted);
        }
        version++;
        dirty = true;
        undoStack.addLast(edit);
        notifyChanged();
    }

    public void replaceAll(String search, String replacement) {
        String updated = text().replace(search, replacement);
        setText(updated);
    }

    public void setText(String text) {
        Edit edit = new Edit(0, 0, text, text());
        replaceRange(0, 0, edit.removed, edit.inserted);
        version++;
        dirty = true;
        record(edit);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyChanged() {
        for (Listener listener : listeners) {
            listener.onTextChanged(this);
        }
    }

    private void record(Edit edit) {
        undoStack.addLast(edit);
        if (undoStack.size() > MAX_UNDO) {
            undoStack.pollFirst();
        }
        redoStack.clear();
        notifyChanged();
    }

    private static final class Edit {
        final int line;
        final int column;
        final String inserted;
        final String removed;

        Edit(int line, int column, String inserted, String removed) {
            this.line = line;
            this.column = column;
            this.inserted = inserted;
            this.removed = removed;
        }
    }
}
