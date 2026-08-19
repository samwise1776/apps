package videoforge.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;

/**
 * File helpers: safe names, atomic writes, unique files and extension filtering.
 */
public final class FileUtils {

    private FileUtils() {}

    public static final Set<String> VIDEO_EXT = Set.of(
            "mp4", "mov", "mkv", "avi", "webm", "m4v", "mts", "m2ts", "ts", "flv", "wmv", "mpeg", "mpg", "3gp");
    public static final Set<String> AUDIO_EXT = Set.of(
            "mp3", "wav", "flac", "ogg", "oga", "m4a", "aac", "opus", "wma", "aiff", "aif", "mka");
    public static final Set<String> IMAGE_EXT = Set.of(
            "png", "jpg", "jpeg", "webp", "bmp", "gif", "tiff", "tif");
    public static final Set<String> SUBTITLE_EXT = Set.of("srt");

    /** Normalize a string to a safe file name component. */
    public static String sanitizeFileName(String name) {
        String safe = name == null ? "" : name.replaceAll("[^A-Za-z0-9 _\\-()]+", "_").trim();
        if (safe.isBlank()) {
            safe = "untitled";
        }
        return safe;
    }

    /** Guess the "kind" of a media file from its extension. */
    public static String kindOf(Path path) {
        String ext = extension(path);
        if (VIDEO_EXT.contains(ext)) return "video";
        if (AUDIO_EXT.contains(ext)) return "audio";
        if (IMAGE_EXT.contains(ext)) return "image";
        if (SUBTITLE_EXT.contains(ext)) return "subtitle";
        return "other";
    }

    public static String extension(Path path) {
        if (path == null) return "";
        String n = path.getFileName().toString();
        int i = n.lastIndexOf('.');
        if (i < 0) return "";
        return n.substring(i + 1).toLowerCase();
    }

    public static boolean isSupportedMedia(Path path) {
        return !kindOf(path).equals("other");
    }

    /** Return a non-existing file inside {@code dir}: name.ext, name (1).ext, ... */
    public static Path uniqueFile(Path dir, String baseName, String extension) {
        String base = sanitizeFileName(baseName);
        Path candidate = dir.resolve(base + extension);
        int n = 1;
        while (Files.exists(candidate)) {
            candidate = dir.resolve(base + " (" + n + ")" + extension);
            n++;
        }
        return candidate;
    }

    /**
     * Atomically write text to a file: write a temp sibling then move it over the target.
     * This prevents corrupt project files when the process dies mid-write.
     */
    public static void writeTextAtomic(Path target, String content) throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path tmp = Files.createTempFile(parent, ".tmp-", ".part");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /** Read a UTF-8 text file; returns null if missing or unreadable. */
    public static String readText(Path path) {
        try {
            if (Files.exists(path)) {
                return Files.readString(path, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    public static String humanBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }

    /** Open the system file manager on a directory/file when possible. */
    public static void revealInFileManager(Path path) {
        try {
            List<String> cmd = new java.util.ArrayList<>();
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("linux")) {
                String opener = System.getenv("VFS_OPEN_COMMAND") != null
                        ? System.getenv("VFS_OPEN_COMMAND")
                        : (Files.isDirectory(path) ? "xdg-open" : "xdg-open");
                cmd.add(opener);
                cmd.add(path.getParent() == null ? path.toString() : path.getParent().toString());
            } else if (os.contains("mac")) {
                cmd.add("open");
                cmd.add(path.getParent() == null ? path.toString() : path.getParent().toString());
            } else {
                cmd.add("explorer");
                cmd.add("/select," + path.toString());
            }
            new ProcessBuilder(cmd).start();
        } catch (IOException ignored) {
        }
    }
}
