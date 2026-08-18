package project;

import util.Log;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Top-level facade for opening projects, keeping them indexed, and exposing
 * analysis, search, and code context through a single object.
 */
public final class ProjectManager {
    private final Map<String, ProjectContext> projects = new HashMap<>();

    /** Opens (or reopens) a project rooted at the given directory and indexes it. */
    public ProjectContext open(Path root) {
        Path normalized = root.toAbsolutePath().normalize();
        String key = normalized.toString();
        ProjectContext existing = projects.get(key);
        if (existing != null) {
            refreshProject(existing);
            return existing;
        }
        ProjectIndexer indexer = new ProjectIndexer(normalized);
        int files = indexer.reindex();
        ProjectContext context = new ProjectContext(key, indexer.indexer());
        projects.put(key, context);
        Log.info("Opened project %s with %d file(s)", key, files);
        return context;
    }

    public ProjectContext get(Path root) {
        return projects.get(root.toAbsolutePath().normalize().toString());
    }

    /** Re-scans and re-parses all files in a project, then rebuilds the dependency graph. */
    public void refreshProject(ProjectContext context) {
        ProjectIndexer indexer = new ProjectIndexer(Path.of(context.root()));
        int count = indexer.reindex();
        context.indexer().clear();
        for (ParsedFile file : indexer.indexer().files()) {
            context.indexer().add(file);
        }
        context.dependencies().rebuild();
        Log.info("Refreshed project %s (%d file(s))", context.root(), count);
    }

    public Map<String, ProjectContext> projects() {
        return projects;
    }

    public void close(Path root) {
        projects.remove(root.toAbsolutePath().normalize().toString());
    }
}
