package project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** An aggregated snapshot of everything the analyzers know about a project. */
public final class ProjectContext {
    private final String root;
    private final SymbolIndexer indexer;
    private final SearchEngine search;
    private final DependencyAnalyzer dependencies;
    private final ImportAnalyzer imports;

    public ProjectContext(String root, SymbolIndexer indexer) {
        this.root = root;
        this.indexer = indexer;
        this.search = new SearchEngine(indexer);
        this.dependencies = new DependencyAnalyzer(indexer);
        this.imports = new ImportAnalyzer(indexer);
    }

    public String root() {
        return root;
    }

    public SymbolIndexer indexer() {
        return indexer;
    }

    public SearchEngine search() {
        return search;
    }

    public DependencyAnalyzer dependencies() {
        return dependencies;
    }

    public ImportAnalyzer imports() {
        return imports;
    }

    /** Full project summary as a formatted report (for AI context / CLI). */
    public String summarize() {
        StringBuilder out = new StringBuilder();
        out.append("Project root: ").append(root).append('\n');
        out.append("Files: ").append(indexer.fileCount()).append("  Symbols: ").append(indexer.symbolCount()).append('\n');
        List<Symbol> classes = indexer.classes();
        if (!classes.isEmpty()) {
            out.append("\nTypes (").append(classes.size()).append("):\n");
            for (Symbol symbol : classes) {
                out.append("  ").append(symbol.kind().name().toLowerCase(java.util.Locale.ROOT))
                        .append(' ').append(symbol.name()).append("  ").append(symbol.file()).append(':').append(symbol.line()).append('\n');
            }
        }
        return out.toString();
    }

    public List<Symbol> classes() {
        return indexer.classes();
    }

    public List<Symbol> symbols() {
        return indexer.all();
    }

    public List<Symbol> symbolsIn(String file) {
        ParsedFile parsed = indexer.file(file);
        if (parsed == null) return Collections.emptyList();
        return parsed.symbols();
    }

    /** All import issues across the project. */
    public List<ImportAnalyzer.ImportIssue> importIssues() {
        List<ImportAnalyzer.ImportIssue> issues = new ArrayList<>();
        for (ParsedFile file : indexer.files()) {
            issues.addAll(imports.analyze(file));
        }
        return issues;
    }
}
