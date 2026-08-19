package videoforge.media;

import videoforge.config.AppConfig;
import videoforge.logging.AppLog;
import videoforge.rendering.FFmpegManager;
import videoforge.utils.FileUtils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Generates and caches thumbnails for media assets.
 *
 * <p>Video/audio assets get a frame extracted by FFmpeg into
 * {@code cache/thumbnails/}. Images are downscaled with Java 2D. Generation runs
 * on a small background pool so importing a big batch never blocks the UI.</p>
 */
public final class ThumbnailGenerator {

    private static final AppLog LOG = AppLog.get("editor");
    public static final int THUMB_SIZE = 160;

    private final FFmpegManager ffmpeg = new FFmpegManager();
    private final ExecutorService pool = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "thumbnail-generator");
        t.setDaemon(true);
        return t;
    });

    public interface Callback {
        void thumbnailReady(MediaFile media, Path thumb);
    }

    private final List<Callback> callbacks = new ArrayList<>();

    public void addCallback(Callback c) {
        callbacks.add(c);
    }

    private Path thumbDir() {
        return AppConfig.get().cacheDir("thumbnails");
    }

    private Path thumbFile(MediaFile media) {
        return thumbDir().resolve(media.getId() + ".png");
    }

    /** Returns a cached thumbnail, generating it synchronously if missing. */
    public Path thumbnail(MediaFile media) {
        Path cached = thumbFile(media);
        if (Files.exists(cached)) {
            media.setThumbnailPath(cached.toString());
            return cached;
        }
        if (media.getPath() == null || !Files.exists(Path.of(media.getPath()))) {
            return null;
        }
        Path generated = generate(media);
        if (generated != null) {
            media.setThumbnailPath(generated.toString());
        }
        return generated;
    }

    /** Schedule asynchronous generation; callbacks fire on completion. */
    public void generateAsync(MediaFile media) {
        pool.execute(() -> {
            Path thumb = generate(media);
            if (thumb != null) {
                media.setThumbnailPath(thumb.toString());
                for (Callback c : new ArrayList<>(callbacks)) {
                    try {
                        c.thumbnailReady(media, thumb);
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    private Path generate(MediaFile media) {
        try {
            Files.createDirectories(thumbDir());
        } catch (IOException e) {
            return null;
        }
        Path out = thumbFile(media);
        if (Files.exists(out)) {
            return out;
        }
        try {
            switch (media.getKind()) {
                case "video" -> {
                    if (!ffmpeg.extractFrame(Path.of(media.getPath()), 0.1, THUMB_SIZE, out, true)) {
                        return null;
                    }
                }
                case "audio" -> drawAudioPlaceholder(out);
                case "image" -> drawImageThumb(media, out);
                default -> drawPlaceholder(out, media.getKind());
            }
        } catch (Exception e) {
            LOG.warn("Thumbnail failed for " + media.getName() + ": " + e.getMessage());
            return null;
        }
        return Files.exists(out) ? out : null;
    }

    private void drawImageThumb(MediaFile media, Path out) {
        try {
            BufferedImage src = javax.imageio.ImageIO.read(Path.of(media.getPath()).toFile());
            if (src == null) {
                drawPlaceholder(out, media.getKind());
                return;
            }
            double scale = Math.min((double) THUMB_SIZE / src.getWidth(), (double) THUMB_SIZE / src.getHeight());
            int w = Math.max(1, (int) Math.round(src.getWidth() * scale));
            int h = Math.max(1, (int) Math.round(src.getHeight() * scale));
            BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = dst.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, w, h, null);
            g.dispose();
            javax.imageio.ImageIO.write(dst, "png", out.toFile());
        } catch (IOException e) {
            drawPlaceholder(out, media.getKind());
        }
    }

    private void drawAudioPlaceholder(Path out) {
        BufferedImage img = new BufferedImage(THUMB_SIZE, THUMB_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(30, 34, 42));
        g.fillRect(0, 0, THUMB_SIZE, THUMB_SIZE);
        g.setColor(new Color(0, 170, 255));
        int mid = THUMB_SIZE / 2;
        for (int i = 0; i < 8; i++) {
            int y = 40 + i * 12;
            int x = 20 + i * 18;
            g.drawLine(x, mid, x + 12, mid - 14 + (i % 3) * 10);
        }
        g.dispose();
        writePng(img, out);
    }

    private void drawPlaceholder(Path out, String label) {
        BufferedImage img = new BufferedImage(THUMB_SIZE, THUMB_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(30, 34, 42));
        g.fillRect(0, 0, THUMB_SIZE, THUMB_SIZE);
        g.setColor(new Color(140, 150, 165));
        String text = label.toUpperCase(Locale.ROOT).isEmpty() ? "MEDIA" : label.toUpperCase(Locale.ROOT);
        g.drawString(text, 8, THUMB_SIZE / 2);
        g.dispose();
        writePng(img, out);
    }

    private void writePng(BufferedImage img, Path out) {
        try {
            javax.imageio.ImageIO.write(img, "png", out.toFile());
        } catch (IOException ignored) {
        }
    }

    public long cacheSizeBytes() {
        try {
            return Files.walk(thumbDir()).filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0;
                        }
                    }).sum();
        } catch (IOException e) {
            return 0;
        }
    }

    public void clearCache() {
        try {
            Files.walk(thumbDir()).sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    public void shutdown() {
        pool.shutdown();
    }
}
