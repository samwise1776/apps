package editor;

import java.util.List;

/** A file-level diff result between two revisions. */
public final class DiffResult {
    private final String file;
    private final List<DiffHunk> hunks;
    private final boolean unchanged;

    public DiffResult(String file, List<DiffHunk> hunks) {
        this.file = file;
        this.hunks = hunks;
        this.unchanged = hunks.isEmpty();
    }

    public String file() {
        return file;
    }

    public List<DiffHunk> hunks() {
        return hunks;
    }

    public boolean isUnchanged() {
        return unchanged;
    }

    public int addedLines() {
        int count = 0;
        for (DiffHunk hunk : hunks) {
            count += hunk.addedCount();
        }
        return count;
    }

    public int removedLines() {
        int count = 0;
        for (DiffHunk hunk : hunks) {
            count += hunk.removedCount();
        }
        return count;
    }
}
