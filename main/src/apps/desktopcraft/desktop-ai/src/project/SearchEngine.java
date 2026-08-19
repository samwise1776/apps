package project;

import java.util.ArrayList;
import java.util.List;

/** Code search across indexed project files with simple relevance ranking. */
public final class SearchEngine {
    private final SymbolIndexer indexer;

    public SearchEngine(SymbolIndexer indexer) {
        this.indexer = indexer;
    }

    public List<Result> search(String query, boolean wholeWord) {
        List<Result> results = new ArrayList<>();
        if (query == null || query.isBlank()) {
            return results;
        }
        String lowerQuery = query.toLowerCase(java.util.Locale.ROOT);
        for (SymbolIndexer.Occurrence occurrence : indexer.occurrences(query, wholeWord)) {
            results.add(new Result(occurrence.file(), occurrence.line(), occurrence.text(),
                    score(occurrence.text(), lowerQuery)));
        }
        results.sort((left, right) -> Integer.compare(right.score, left.score));
        return results;
    }

    /** Searches only file paths and names. */
    public List<String> searchFiles(String query) {
        String lowerQuery = query.toLowerCase(java.util.Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (ParsedFile file : indexer.files()) {
            if (file.path().toLowerCase(java.util.Locale.ROOT).contains(lowerQuery)) {
                result.add(file.path());
            }
        }
        return result;
    }

    /** Finds all occurrences of a component/API usage, e.g. "JButton". */
    public List<Result> findUsage(String symbolName) {
        List<Result> results = new ArrayList<>();
        String lowerName = symbolName.toLowerCase(java.util.Locale.ROOT);
        for (SymbolIndexer.Occurrence occurrence : indexer.occurrences(symbolName, true)) {
            results.add(new Result(occurrence.file(), occurrence.line(), occurrence.text(),
                    score(occurrence.text(), lowerName)));
        }
        results.sort((left, right) -> Integer.compare(right.score, left.score));
        return results;
    }

    private static int score(String line, String lowerQuery) {
        String lower = line.toLowerCase(java.util.Locale.ROOT);
        int score = 0;
        int occurrences = 0;
        int index = 0;
        while ((index = lower.indexOf(lowerQuery, index)) >= 0) {
            occurrences++;
            index += lowerQuery.length();
        }
        score += occurrences * 10;
        if (lower.contains("class " + lowerQuery) || lower.contains("interface " + lowerQuery)) {
            score += 25;
        }
        if (lower.contains("public") || lower.contains("private")) {
            score += 3;
        }
        if (line.trim().startsWith("//") || line.trim().startsWith("#")) {
            score -= 2;
        }
        return score;
    }

    /** A search hit with its file, line, matching text, and a relevance score. */
    public static final class Result {
        private final String file;
        private final int line;
        private final String text;
        private final int score;

        Result(String file, int line, String text, int score) {
            this.file = file;
            this.line = line;
            this.text = text;
            this.score = score;
        }

        public String file() {
            return file;
        }

        public int line() {
            return line;
        }

        public String text() {
            return text;
        }

        public int score() {
            return score;
        }

        @Override
        public String toString() {
            return file + ":" + line + "  " + text.trim();
        }
    }
}
