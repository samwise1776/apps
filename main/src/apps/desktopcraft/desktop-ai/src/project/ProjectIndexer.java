package project;

import editor.LanguageKind;
import util.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Walks a directory tree, parses every source file, and keeps the SymbolIndexer
 * in sync. Skips binary directories and common build outputs.
 */
public final class ProjectIndexer {
    private static final List<String> SKIPPED_DIRS = List.of(
            ".git", ".svn", ".hg", "node_modules", "target", "build", "out",
            ".gradle", ".idea", ".vscode", "dist", ".cache", "__pycache__", ".venv", "venv");
    private static final List<String> SKIPPED_FILES = List.of(".DS_Store");

    private final Path root;
    private final SymbolIndexer indexer;

    public ProjectIndexer(Path root) {
        this.root = root;
        this.indexer = new SymbolIndexer();
    }

    public Path root() {
        return root;
    }

    public SymbolIndexer indexer() {
        return indexer;
    }

    /** Full (re)index of the project tree. */
    public int reindex() {
        indexer.clear();
        return scanAndIndex(root);
    }

    /** Incremental refresh: adds new files and re-parses changed ones. Returns files parsed. */
    public int refresh() {
        List<Path> files = findSourceFiles(root);
        int parsed = 0;
        for (Path path : files) {
            String absolute = path.toAbsolutePath().normalize().toString();
            if (indexer.file(absolute) != null) {
                continue;
            }
            if (indexFile(path)) {
                parsed++;
            }
        }
        Log.info("Refresh parsed %d new file(s) in %s", parsed, root);
        return parsed;
    }

    private int scanAndIndex(Path dir) {
        int count = 0;
        for (Path path : findSourceFiles(dir)) {
            if (indexFile(path)) {
                count++;
            }
        }
        Log.info("Indexed %d file(s) in %s", count, root);
        return count;
    }

    private boolean indexFile(Path path) {
        try {
            String content = Files.readString(path);
            LanguageKind language = LanguageKind.fromFile(path.getFileName().toString());
            ParsedFile parsed = CodeParser.parse(path.toAbsolutePath().normalize().toString(), content, language);
            indexer.add(parsed);
            return true;
        } catch (IOException e) {
            Log.error("Failed to index " + path, e);
            return false;
        }
    }

    /** Removes a single file from the index (after deletion). */
    public void removeFile(Path path) {
        indexer.remove(path.toAbsolutePath().normalize().toString());
    }

    private static List<Path> findSourceFiles(Path dir) {
        List<Path> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            result = stream
                    .filter(Files::isRegularFile)
                    .filter(ProjectIndexer::notSkipped)
                    .filter(p -> LanguageKind.fromFile(p.getFileName().toString()) != LanguageKind.PLAIN
                            || p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".txt"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            Log.error("Failed to walk " + dir, e);
        }
        return result;
    }

    private static boolean notSkipped(Path path) {
        if (SKIPPED_FILES.contains(path.getFileName().toString())) {
            return false;
        }
        for (Path part : path) {
            if (SKIPPED_DIRS.contains(part.getFileName().toString())) {
                return false;
            }
        }
        return true;
    }
}
