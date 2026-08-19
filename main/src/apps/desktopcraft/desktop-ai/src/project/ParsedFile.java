package project;

import java.util.ArrayList;
import java.util.List;

/** The parsed structure of one source file. */
public final class ParsedFile {
    private final String path;
    private final String packageName;
    private final List<String> imports = new ArrayList<>();
    private final List<Symbol> symbols = new ArrayList<>();
    private final List<String> lines;
    private final String language;

    public ParsedFile(String path, String packageName, List<String> imports,
                      List<Symbol> symbols, List<String> lines, String language) {
        this.path = path;
        this.packageName = packageName == null ? "" : packageName;
        this.imports.addAll(imports);
        this.symbols.addAll(symbols);
        this.lines = lines;
        this.language = language;
    }

    public String path() {
        return path;
    }

    public String packageName() {
        return packageName;
    }

    public List<String> imports() {
        return imports;
    }

    public List<Symbol> symbols() {
        return symbols;
    }

    public List<String> lines() {
        return lines;
    }

    public String language() {
        return language;
    }

    /** All simple names referenced by imports (for unused-import detection). */
    public List<String> importedSimpleNames() {
        List<String> names = new ArrayList<>();
        for (String imp : imports) {
            int dot = imp.lastIndexOf('.');
            if (dot >= 0) {
                names.add(imp.substring(dot + 1));
            }
        }
        return names;
    }
}
