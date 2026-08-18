package editor;

import util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Basic identifier autocomplete. Completions come from (1) tokens already present
 * in the open document and (2) an optional symbol source (project indexer). The
 * UI feeds keywords per language separately.
 */
public final class CompletionEngine {
    private final Set<String> symbols = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    public void addSymbol(String symbol) {
        if (symbol != null && !symbol.isEmpty()) {
            symbols.add(symbol);
        }
    }

    public void addAll(Iterable<String> symbols) {
        for (String symbol : symbols) {
            addSymbol(symbol);
        }
    }

    public void clear() {
        symbols.clear();
    }

    public int size() {
        return symbols.size();
    }

    /** Collects candidate completions for the word at the end of the current line. */
    public List<Completion> complete(String documentText, String lineBeforeCaret, int caretLine, int caretColumn) {
        List<Completion> results = new ArrayList<>();
        String word = trailingWord(lineBeforeCaret);
        if (word.isEmpty()) {
            return results;
        }
        String lowerWord = word.toLowerCase(java.util.Locale.ROOT);
        for (String symbol : symbols) {
            if (symbol.toLowerCase(java.util.Locale.ROOT).startsWith(lowerWord)) {
                results.add(new Completion(symbol, symbol.substring(word.length()), caretLine, caretColumn));
            }
        }
        // Local tokens from the document as a fallback.
        if (results.size() < 8) {
            for (String token : new TreeSet<>(Text.tokens(documentText))) {
                if (token.length() > 2 && token.toLowerCase(java.util.Locale.ROOT).startsWith(lowerWord)
                        && !symbols.contains(token)) {
                    results.add(new Completion(token, token.substring(word.length()), caretLine, caretColumn));
                }
            }
        }
        results.sort((left, right) -> {
            int rank = Integer.compare(left.rank(), right.rank());
            if (rank != 0) return rank;
            return left.display.length() - right.display.length();
        });
        int max = Math.min(results.size(), 30);
        return results.subList(0, max);
    }

    private static String trailingWord(String line) {
        StringBuilder word = new StringBuilder();
        for (int i = line.length() - 1; i >= 0; i--) {
            char c = line.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '.') {
                word.append(c);
            } else {
                break;
            }
        }
        return word.reverse().toString();
    }

    /** A single autocomplete candidate. */
    public static final class Completion {
        private final String display;
        private final String suffix;
        private final int line;
        private final int column;
        private final int rank;

        Completion(String display, String suffix, int line, int column) {
            this(display, suffix, line, column, 0);
        }

        Completion(String display, String suffix, int line, int column, int rank) {
            this.display = display;
            this.suffix = suffix;
            this.line = line;
            this.column = column;
            this.rank = rank;
        }

        public String display() {
            return display;
        }

        public String suffix() {
            return suffix;
        }

        public int line() {
            return line;
        }

        public int column() {
            return column;
        }

        public int rank() {
            return rank;
        }
    }
}
