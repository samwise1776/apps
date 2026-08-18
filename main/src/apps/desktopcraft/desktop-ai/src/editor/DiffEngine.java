package editor;

import util.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Real line-based diff using a Longest Common Subsequence (LCS) dynamic program.
 * Hunks of added/removed/unchanged lines are produced with surrounding context.
 */
public final class DiffEngine {
    public static final int CONTEXT_LINES = 3;

    private DiffEngine() {}

    /** Diffs two texts and returns hunks (with context). */
    public static DiffResult diff(String file, String before, String after) {
        List<String> oldLines = Text.lines(before == null ? "" : before);
        List<String> newLines = Text.lines(after == null ? "" : after);
        List<int[]> operations = lcs(oldLines, newLines);
        List<DiffHunk> hunks = groupHunks(operations, oldLines, newLines);
        return new DiffResult(file, hunks);
    }

    /** Returns a list of [operation, oldIndex, newIndex]; 0=keep, 1=remove, 2=add. */
    public static List<int[]> lcs(List<String> oldLines, List<String> newLines) {
        int n = oldLines.size();
        int m = newLines.size();
        int[][] table = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                if (oldLines.get(i).equals(newLines.get(j))) {
                    table[i][j] = table[i + 1][j + 1] + 1;
                } else {
                    table[i][j] = Math.max(table[i + 1][j], table[i][j + 1]);
                }
            }
        }
        List<int[]> operations = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < n && j < m) {
            if (oldLines.get(i).equals(newLines.get(j))) {
                operations.add(new int[]{0, i, j});
                i++;
                j++;
            } else if (table[i + 1][j] >= table[i][j + 1]) {
                operations.add(new int[]{1, i, -1});
                i++;
            } else {
                operations.add(new int[]{2, -1, j});
                j++;
            }
        }
        while (i < n) {
            operations.add(new int[]{1, i, -1});
            i++;
        }
        while (j < m) {
            operations.add(new int[]{2, -1, j});
            j++;
        }
        return operations;
    }

    /** Applies the LCS operations to produce a unified-style diff text. */
    public static String unified(String before, String after) {
        List<String> oldLines = Text.lines(before == null ? "" : before);
        List<String> newLines = Text.lines(after == null ? "" : after);
        List<int[]> operations = lcs(oldLines, newLines);
        StringBuilder out = new StringBuilder();
        for (int[] op : operations) {
            switch (op[0]) {
                case 1: out.append('-').append(oldLines.get(op[1])).append('\n'); break;
                case 2: out.append('+').append(newLines.get(op[2])).append('\n'); break;
                default: out.append(' ').append(oldLines.get(op[1])).append('\n');
            }
        }
        return out.toString();
    }

    private static List<DiffHunk> groupHunks(List<int[]> operations, List<String> oldLines, List<String> newLines) {
        List<DiffHunk> hunks = new ArrayList<>();
        List<DiffHunk.DiffLine> current = new ArrayList<>();
        int oldIndex = 0;
        int newIndex = 0;
        boolean inHunk = false;
        int oldStart = 0;
        int newStart = 0;

        for (int[] op : operations) {
            if (op[0] == 0) {
                if (inHunk) {
                    // Keep context lines until the window is full, then split.
                    if (countSinceLastChange(current) < CONTEXT_LINES) {
                        current.add(new DiffHunk.DiffLine(DiffHunk.Kind.UNCHANGED, oldLines.get(op[1])));
                    } else {
                        flush(hunks, current, oldStart, newStart);
                        oldStart = op[1];
                        newStart = op[2];
                    }
                }
                oldIndex = op[1] + 1;
                newIndex = op[2] + 1;
            } else {
                if (!inHunk) {
                    // Include up to CONTEXT_LINES unchanged lines before this change.
                    int need = Math.min(CONTEXT_LINES, current.size());
                    if (need > 0) {
                        current = new ArrayList<>(current.subList(Math.max(0, current.size() - need), current.size()));
                    }
                    oldStart = oldIndex - need;
                    newStart = newIndex - need;
                    inHunk = true;
                }
                if (op[0] == 1) {
                    current.add(new DiffHunk.DiffLine(DiffHunk.Kind.REMOVED, oldLines.get(op[1])));
                    oldIndex = op[1] + 1;
                } else {
                    current.add(new DiffHunk.DiffLine(DiffHunk.Kind.ADDED, newLines.get(op[2])));
                    newIndex = op[2] + 1;
                }
            }
        }
        if (inHunk) {
            flush(hunks, current, oldStart, newStart);
        }
        return hunks;
    }

    private static int countSinceLastChange(List<DiffHunk.DiffLine> lines) {
        int count = 0;
        for (int i = lines.size() - 1; i >= 0 && lines.get(i).kind() == DiffHunk.Kind.UNCHANGED; i--) {
            count++;
        }
        return count;
    }

    private static void flush(List<DiffHunk> hunks, List<DiffHunk.DiffLine> lines, int oldStart, int newStart) {
        if (lines.isEmpty()) return;
        boolean hasChange = false;
        for (DiffHunk.DiffLine line : lines) {
            if (line.kind() != DiffHunk.Kind.UNCHANGED) {
                hasChange = true;
                break;
            }
        }
        if (!hasChange) return;
        hunks.add(new DiffHunk(oldStart, newStart, new ArrayList<>(lines)));
        lines.clear();
    }
}
