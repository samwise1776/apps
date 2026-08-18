package project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Analyzes imports: which are unused (Java), which are wildcard, and which reference
 * other files in the same project (for dependency graphs).
 */
public final class ImportAnalyzer {
    private final SymbolIndexer indexer;

    public ImportAnalyzer(SymbolIndexer indexer) {
        this.indexer = indexer;
    }

    public List<ImportIssue> analyze(ParsedFile file) {
        List<ImportIssue> issues = new ArrayList<>();
        if (file == null || !file.language().equals("java")) {
            return issues;
        }
        StringBuilder body = new StringBuilder();
        boolean inHeader = true;
        for (String line : file.lines()) {
            if (line.trim().startsWith("import ") || line.trim().startsWith("package ")) {
                continue;
            }
            inHeader = false;
            body.append(line).append('\n');
        }
        Set<String> simpleNames = new HashSet<>();
        for (String name : file.importedSimpleNames()) {
            if (!name.equals("*")) {
                simpleNames.add(name);
            }
        }
        int importLine = 0;
        for (String line : file.lines()) {
            importLine++;
            String trimmed = line.trim();
            if (!trimmed.startsWith("import ")) {
                continue;
            }
            String imported = trimmed.replaceFirst("^import\\s+(?:static\\s+)?([\\w.*]+)\\s*;", "$1");
            String simpleName = imported.substring(imported.lastIndexOf('.') + 1);
            if (simpleName.equals("*")) {
                issues.add(new ImportIssue(file.path(), importLine, imported, "Wildcard import. Import the specific types to keep dependencies explicit."));
                continue;
            }
            if (!body.toString().contains(simpleName)) {
                issues.add(new ImportIssue(file.path(), importLine, imported, "This import does not appear to be used in the file."));
            }
        }
        return issues;
    }

    /** Resolves an imported type name to a project file path, or null if external. */
    public String resolveToProjectFile(ParsedFile file, String imported) {
        String simple = imported.substring(imported.lastIndexOf('.') + 1).replace("*", "");
        List<Symbol> matches = indexer.byName(simple);
        for (Symbol match : matches) {
            if (match.isType()) {
                return match.file();
            }
        }
        return null;
    }

    public static final class ImportIssue {
        private final String file;
        private final int line;
        private final String imported;
        private final String message;

        ImportIssue(String file, int line, String imported, String message) {
            this.file = file;
            this.line = line;
            this.imported = imported;
            this.message = message;
        }

        public String file() {
            return file;
        }

        public int line() {
            return line;
        }

        public String imported() {
            return imported;
        }

        public String message() {
            return message;
        }
    }
}
