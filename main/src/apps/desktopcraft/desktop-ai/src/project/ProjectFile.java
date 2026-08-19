package project;

import editor.LanguageKind;
import util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** A file that belongs to a project, with cached content and parsed structure. */
public final class ProjectFile {
    private final Path path;
    private final LanguageKind language;
    private String content = "";
    private ParsedFile parsed;
    private boolean dirty;

    public ProjectFile(Path path) {
        this.path = path;
        this.language = LanguageKind.fromFile(path.getFileName().toString());
    }

    public Path path() {
        return path;
    }

    public String absolutePath() {
        return path.toAbsolutePath().normalize().toString();
    }

    public LanguageKind language() {
        return language;
    }

    public boolean isDirty() {
        return dirty;
    }

    public String content() {
        if (content == null) {
            return "";
        }
        return content;
    }

    /** Loads content from disk (if not already cached) and returns it. */
    public String load() {
        try {
            if (!dirty && content == null) {
                content = Files.readString(path, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            Log.error("Failed to read " + path, e);
            content = "";
        }
        return content();
    }

    /** Updates content in memory and marks the file dirty (used by the editor). */
    public void update(String newContent) {
        this.content = newContent;
        this.dirty = true;
        this.parsed = null;
    }

    public ParsedFile parsed() {
        if (parsed == null) {
            parsed = CodeParser.parse(absolutePath(), load(), language);
        }
        return parsed;
    }

    public void invalidate() {
        this.parsed = null;
    }
}
