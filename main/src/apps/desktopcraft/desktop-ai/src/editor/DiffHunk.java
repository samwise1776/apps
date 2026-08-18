package editor;

import java.util.ArrayList;
import java.util.List;

/** A single changed hunk within a file diff. */
public final class DiffHunk {
    public enum Kind { UNCHANGED, ADDED, REMOVED }

    private final int oldStart;
    private final int newStart;
    private final List<DiffLine> lines;

    public DiffHunk(int oldStart, int newStart, List<DiffLine> lines) {
        this.oldStart = oldStart;
        this.newStart = newStart;
        this.lines = lines;
    }

    public int oldStart() {
        return oldStart;
    }

    public int newStart() {
        return newStart;
    }

    public List<DiffLine> lines() {
        return lines;
    }

    public int addedCount() {
        return count(Kind.ADDED);
    }

    public int removedCount() {
        return count(Kind.REMOVED);
    }

    private int count(Kind kind) {
        int count = 0;
        for (DiffLine line : lines) {
            if (line.kind == kind) count++;
        }
        return count;
    }

    public boolean isEmpty() {
        return addedCount() == 0 && removedCount() == 0;
    }

    /** Rebuilds this hunk's new-side content from its lines. */
    public String appliedContent() {
        StringBuilder out = new StringBuilder();
        for (DiffLine line : lines) {
            if (line.kind != Kind.REMOVED) {
                out.append(line.text).append('\n');
            }
        }
        return out.toString();
    }

    public static final class DiffLine {
        private final Kind kind;
        private final String text;

        DiffLine(Kind kind, String text) {
            this.kind = kind;
            this.text = text;
        }

        public Kind kind() {
            return kind;
        }

        public String text() {
            return text;
        }
    }
}
