package editor;

import java.util.Locale;

/** Supported languages and how to identify them by file extension. */
public enum LanguageKind {
    JAVA("java", "//", new String[]{"java"}),
    PYTHON("python", "#", new String[]{"py", "pyw"}),
    JAVASCRIPT("javascript", "//", new String[]{"js", "mjs", "cjs"}),
    TYPESCRIPT("typescript", "//", new String[]{"ts", "tsx"}),
    HTML("html", "<!--", new String[]{"html", "htm"}),
    CSS("css", "/*", new String[]{"css"}),
    JSON("json", "//", new String[]{"json"}),
    XML("xml", "<!--", new String[]{"xml", "svg"}),
    SQL("sql", "--", new String[]{"sql"}),
    MARKDOWN("markdown", "<!--", new String[]{"md", "markdown"}),
    SHELL("shell", "#", new String[]{"sh", "bash"}),
    PROPERTIES("properties", "#", new String[]{"properties"}),
    PLAIN("plain", "", new String[]{"txt"});

    private final String id;
    private final String lineComment;
    private final String[] extensions;

    LanguageKind(String id, String lineComment, String[] extensions) {
        this.id = id;
        this.lineComment = lineComment;
        this.extensions = extensions;
    }

    public String id() {
        return id;
    }

    public String lineComment() {
        return lineComment;
    }

    public static LanguageKind fromFile(String fileName) {
        if (fileName == null) return PLAIN;
        String lower = fileName.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        if (dot < 0) return PLAIN;
        String ext = lower.substring(dot + 1);
        for (LanguageKind kind : values()) {
            for (String candidate : kind.extensions) {
                if (candidate.equals(ext)) return kind;
            }
        }
        return PLAIN;
    }
}
