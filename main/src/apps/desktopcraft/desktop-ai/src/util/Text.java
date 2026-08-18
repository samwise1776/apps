package util;

import java.util.ArrayList;
import java.util.List;

/** Text helpers used across the platform: line handling, indentation, tokenizing, templates. */
public final class Text {
    private Text() {}

    /** Splits text into lines, preserving trailing empty line handling. */
    public static List<String> lines(String text) {
        List<String> result = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                result.add(text.substring(start, i));
                start = i + 1;
            }
        }
        result.add(text.substring(start));
        return result;
    }

    public static String joinLines(List<String> lines) {
        return String.join("\n", lines);
    }

    public static String trimTrailing(String text) {
        return text.replaceAll("[ \\t]+\\r?\\n", "\n").replaceAll("[ \\t]+$", "");
    }

    /** Counts occurrences of a substring. */
    public static int count(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    /** Simple tokenizer: splits on whitespace and punctuation while keeping identifiers intact. */
    public static List<String> tokens(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                current.append(c);
            } else {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                if (!Character.isWhitespace(c)) {
                    result.add(String.valueOf(c));
                }
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    /** Returns true if the text contains any of the given words (word-boundary). */
    public static boolean containsAnyWord(String text, String... words) {
        for (String word : words) {
            if (text.matches("(?s).*\\b" + java.util.regex.Pattern.quote(word) + "\\b.*")) {
                return true;
            }
        }
        return false;
    }

    public static String indent(String text, int spaces) {
        String prefix = " ".repeat(Math.max(0, spaces));
        StringBuilder out = new StringBuilder();
        for (String line : lines(text)) {
            if (!line.isEmpty()) {
                out.append(prefix);
            }
            out.append(line).append('\n');
        }
        return out.toString();
    }

    public static String wrap(String text, int width) {
        StringBuilder out = new StringBuilder();
        for (String paragraph : text.split("\n")) {
            String[] words = paragraph.split("\\s+");
            int column = 0;
            for (String word : words) {
                if (word.isEmpty()) continue;
                if (column + word.length() > width && column > 0) {
                    out.append('\n');
                    column = 0;
                }
                if (column > 0) {
                    out.append(' ');
                    column++;
                }
                out.append(word);
                column += word.length();
            }
            out.append('\n');
            column = 0;
        }
        return out.toString();
    }

    /** Minimal template: replaces {{name}} placeholders from a map. */
    public static String template(String text, java.util.Map<String, String> values) {
        String out = text;
        for (java.util.Map.Entry<String, String> entry : values.entrySet()) {
            out = out.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return out;
    }

    public static String truncate(String text, int max) {
        if (text == null || text.length() <= max) return text == null ? "" : text;
        return text.substring(0, max) + "…";
    }

    /** Levenshtein distance, used by autocomplete and fuzzy search. */
    public static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] swap = prev;
            prev = curr;
            curr = swap;
        }
        return prev[b.length()];
    }
}
