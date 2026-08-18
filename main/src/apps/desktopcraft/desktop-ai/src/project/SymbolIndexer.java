package project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Indexes parsed symbols across a project and answers questions like
 * "which classes depend on Theme.java" and "find every JButton".
 */
public final class SymbolIndexer {
    private final Map<String, ParsedFile> files = new LinkedHashMap<>();
    private final Map<String, List<Symbol>> symbolsByName = new HashMap<>();
    private final List<Symbol> allSymbols = new ArrayList<>();

    public void add(ParsedFile file) {
        files.put(file.path(), file);
        for (Symbol symbol : file.symbols()) {
            allSymbols.add(symbol);
            symbolsByName.computeIfAbsent(symbol.name(), key -> new ArrayList<>()).add(symbol);
        }
    }

    public void remove(String path) {
        ParsedFile file = files.remove(path);
        if (file == null) return;
        allSymbols.removeAll(file.symbols());
        for (Symbol symbol : file.symbols()) {
            List<Symbol> list = symbolsByName.get(symbol.name());
            if (list != null) {
                list.removeIf(candidate -> candidate.file().equals(path));
            }
        }
    }

    public void clear() {
        files.clear();
        symbolsByName.clear();
        allSymbols.clear();
    }

    public int fileCount() {
        return files.size();
    }

    public int symbolCount() {
        return allSymbols.size();
    }

    public ParsedFile file(String path) {
        return files.get(path);
    }

    public List<ParsedFile> files() {
        return new ArrayList<>(files.values());
    }

    public List<Symbol> byName(String name) {
        return symbolsByName.getOrDefault(name, Collections.emptyList());
    }

    public List<Symbol> all() {
        return new ArrayList<>(allSymbols);
    }

    public List<Symbol> ofKind(Symbol.Kind kind) {
        List<Symbol> result = new ArrayList<>();
        for (Symbol symbol : allSymbols) {
            if (symbol.kind() == kind) {
                result.add(symbol);
            }
        }
        return result;
    }

    public List<Symbol> classes() {
        return ofKind(Symbol.Kind.CLASS);
    }

    public List<Symbol> methods() {
        return ofKind(Symbol.Kind.METHOD);
    }

    public List<Symbol> fields() {
        return ofKind(Symbol.Kind.FIELD);
    }

    /** Every occurrence of a term in any indexed file (symbols plus token scan). */
    public List<Occurrence> occurrences(String term, boolean wholeWord) {
        List<Occurrence> result = new ArrayList<>();
        java.util.regex.Pattern whole = wholeWord
                ? java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(term) + "\\b")
                : null;
        for (ParsedFile file : files.values()) {
            List<String> lines = file.lines();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                boolean hit = whole == null ? line.contains(term) : whole.matcher(line).find();
                if (hit) {
                    result.add(new Occurrence(file.path(), i + 1, line));
                }
            }
        }
        return result;
    }

    /** A single match: file, line number, and the matching source line. */
    public static final class Occurrence {
        private final String file;
        private final int line;
        private final String text;

        Occurrence(String file, int line, String text) {
            this.file = file;
            this.line = line;
            this.text = text;
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
    }
}
