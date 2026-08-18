package project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds a file-to-file dependency graph from imports and reference scanning, and
 * finds unreferenced (dead) classes.
 */
public final class DependencyAnalyzer {
    private final SymbolIndexer indexer;
    private final Map<String, Set<String>> dependencies = new HashMap<>();

    public DependencyAnalyzer(SymbolIndexer indexer) {
        this.indexer = indexer;
    }

    /** Recomputes the dependency graph from the current index. */
    public void rebuild() {
        dependencies.clear();
        for (ParsedFile file : indexer.files()) {
            Set<String> edges = new HashSet<>();
            for (String imported : file.imports()) {
                String target = resolve(imported);
                if (target != null && !target.equals(file.path())) {
                    edges.add(target);
                }
            }
            dependencies.put(file.path(), edges);
        }
    }

    private String resolve(String imported) {
        String simple = imported.substring(imported.lastIndexOf('.') + 1).replace("*", "");
        for (Symbol match : indexer.byName(simple)) {
            if (match.isType()) {
                return match.file();
            }
        }
        return null;
    }

    public Set<String> dependentsOf(String file) {
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            if (entry.getValue().contains(file)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public Set<String> dependenciesOf(String file) {
        return dependencies.getOrDefault(file, new HashSet<>());
    }

    /** Files whose types are never referenced by imports anywhere else. */
    public List<String> findUnusedFiles() {
        Set<String> referenced = new HashSet<>();
        for (Set<String> edges : dependencies.values()) {
            referenced.addAll(edges);
        }
        List<String> result = new ArrayList<>();
        for (String file : dependencies.keySet()) {
            if (!referenced.contains(file)) {
                result.add(file);
            }
        }
        return result;
    }

    /** Classes declared in a file that no other file references by import or symbol. */
    public List<Symbol> findUnusedClasses() {
        Set<String> referenced = new HashSet<>();
        for (Set<String> edges : dependencies.values()) {
            referenced.addAll(edges);
        }
        List<Symbol> result = new ArrayList<>();
        for (ParsedFile file : indexer.files()) {
            for (Symbol symbol : file.symbols()) {
                if (!symbol.isType()) continue;
                boolean referencedByImport = referenced.contains(file.path());
                boolean referencedByName = false;
                if (!referencedByImport) {
                    for (ParsedFile other : indexer.files()) {
                        if (other.path().equals(file.path())) continue;
                        for (Symbol candidate : other.symbols()) {
                            if (candidate.name().equals(symbol.name())) {
                                referencedByName = true;
                                break;
                            }
                        }
                        if (referencedByName) break;
                    }
                }
                if (!referencedByImport && !referencedByName) {
                    result.add(symbol);
                }
            }
        }
        return result;
    }

    public String describe() {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            out.append(entry.getKey()).append(" depends on: ");
            out.append(entry.getValue().isEmpty() ? "(nothing)" : String.join(", ", entry.getValue()));
            out.append('\n');
        }
        return out.toString();
    }
}
