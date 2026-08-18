package videoforge.rendering;

import videoforge.config.AppConfig;
import videoforge.effects.BlurEffect;
import videoforge.effects.TextEffect;
import videoforge.logging.AppLog;
import videoforge.timeline.Timeline;
import videoforge.timeline.TimelineClip;
import videoforge.timeline.Track;
import videoforge.utils.TimeUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Preview engine: extracts real video frames through FFmpeg and composites
 * text/images/effects on top with Java 2D, so the preview is genuinely
 * WYSIWYG-ish for the timeline (without a full render).
 *
 * <p>Playback performance comes from decoding short windows of the active
 * source into memory at preview resolution via a single raw-video pipe
 * process, then scrubbing inside the window instantly.</p>
 */
public final class PreviewEngine {

    public enum Quality {
        FULL(1), HALF(2), QUARTER(4);

        public final int divide;

        Quality(int divide) {
            this.divide = divide;
        }
    }

    private static final AppLog LOG = AppLog.get("editor");
    private static final int WINDOW_SECONDS = 3;

    private final FFmpegManager ffmpeg = new FFmpegManager();
    private final AppConfig config = AppConfig.get();

    private Quality quality = Quality.HALF;
    private final ExecutorService decodePool = Executors.newFixedThreadPool(1, r -> {
        Thread t = new Thread(r, "preview-decode");
        t.setDaemon(true);
        return t;
    });

    // current decoded window
    private volatile Window window;
    private Future<?> decodeTask;

    // cached decoded single images per source path
    private final Map<String, BufferedImage> imageCache = new HashMap<>();

    public PreviewEngine() {
        String q = config.getString("previewQuality");
        quality = switch (q == null ? "" : q) {
            case "FULL" -> Quality.FULL;
            case "QUARTER" -> Quality.QUARTER;
            default -> Quality.HALF;
        };
    }

    public void setQuality(Quality q) {
        this.quality = q;
        config.put("previewQuality", q.name());
        window = null;
    }

    public Quality quality() {
        return quality;
    }

    public int previewWidth(int canvasW) {
        return Math.max(2, canvasW / quality.divide);
    }

    public int previewHeight(int canvasH) {
        return Math.max(2, canvasH / quality.divide);
    }

    // ---------- window decoding ----------

    private static final class Window {
        String sourcePath;
        BufferedImage[] frames;
        long startMicros;
        long frameIntervalMicros;
        boolean usable;
    }

    /**
     * Ensure a decoded window exists for the given clip at the given time.
     * Returns the frame nearest to {@code time}, or null while decoding.
     */
    private BufferedImage frameAt(TimelineClip videoClip, long timeMicros, Timeline tl) {
        if (videoClip == null || videoClip.getSourcePath() == null) {
            return null;
        }
        String source = videoClip.getSourcePath();
        Path file = Path.of(source);
        if (!Files.exists(file)) {
            return null;
        }
        Window w = window;
        if (w == null || !w.usable || !w.sourcePath.equals(source)) {
            startWindowDecode(videoClip, timeMicros, tl);
            return null;
        }
        // clip local time in source
        long clipLocal = timeMicros - videoClip.getTimelineStart();
        long sourceTime = videoClip.getSourceStart()
                + (videoClip.isReverse() ? -Math.round(clipLocal * videoClip.getSpeed()) : Math.round(clipLocal * videoClip.getSpeed()));
        if (sourceTime < w.startMicros || sourceTime >= w.startMicros + (long) w.frames.length * w.frameIntervalMicros) {
            startWindowDecode(videoClip, timeMicros, tl);
            return null;
        }
        int idx = (int) ((sourceTime - w.startMicros) / w.frameIntervalMicros);
        idx = Math.max(0, Math.min(idx, w.frames.length - 1));
        return w.frames[idx];
    }

    private void startWindowDecode(TimelineClip clip, long timeMicros, Timeline tl) {
        if (decodeTask != null && !decodeTask.isDone()) {
            return; // let the current decode finish; next frame request will refresh
        }
        final Window target = new Window();
        target.sourcePath = clip.getSourcePath();
        target.frameIntervalMicros = TimeUtils.secondsToMicros(1.0 / previewFps());
        long clipLocal = timeMicros - clip.getTimelineStart();
        long centerSource = clip.getSourceStart()
                + Math.round(clipLocal * clip.getSpeed());
        long halfWindow = TimeUtils.secondsToMicros(WINDOW_SECONDS / 2.0);
        target.startMicros = Math.max(0, centerSource - halfWindow);
        final int frames = WINDOW_SECONDS * previewFps();
        final int outW = previewWidth(tl.canvasWidth());
        final int outH = previewHeight(tl.canvasHeight());

        decodeTask = decodePool.submit(() -> {
            try {
                target.frames = decodeWindow(Path.of(target.sourcePath), target.startMicros, frames, outW, outH);
                target.usable = true;
                window = target;
            } catch (Exception e) {
                LOG.warn("Preview decode failed: " + e.getMessage());
            }
        });
    }

    /** Decode {@code count} frames starting at {@code startSeconds} via a raw pipe. */
    private BufferedImage[] decodeWindow(Path source, long startMicros, int count, int w, int h) throws Exception {
        if (count <= 0) {
            return new BufferedImage[0];
        }
        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpeg.ffmpegPath());
        cmd.addAll(List.of("-v", "error", "-ss", String.valueOf(TimeUtils.microsToSeconds(startMicros)),
                "-i", source.toString(), "-frames:v", String.valueOf(count),
                "-fps_mode", "passthrough",
                "-vf", "scale=" + w + ":" + h + ",fps=" + previewFps(),
                "-f", "rawvideo", "-pix_fmt", "rgb24", "-"));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        int frameBytes = w * h * 3;
        BufferedImage[] frames = new BufferedImage[count];
        int index = 0;
        byte[] buf = new byte[frameBytes];
        try (InputStream in = p.getInputStream()) {
            int filled = 0;
            int b;
            while ((b = in.read()) != -1 && index < count) {
                buf[filled++] = (byte) b;
                if (filled == frameBytes) {
                    frames[index++] = rawToImage(buf, w, h);
                    filled = 0;
                }
            }
        }
        p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
        return index == count ? frames : java.util.Arrays.copyOf(frames, index);
    }

    private static BufferedImage rawToImage(byte[] rgb24, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] row = new int[w];
        int p = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = rgb24[p++] & 0xff;
                int g = rgb24[p++] & 0xff;
                int b = rgb24[p++] & 0xff;
                row[x] = (r << 16) | (g << 8) | b;
            }
            img.setRGB(0, y, w, 1, row, 0, w);
        }
        return img;
    }

    private int previewFps() {
        double f = config.getDouble("previewPlaybackFps");
        return (int) Math.round(f > 0 ? f : 30);
    }

    // ---------- compositing ----------

    /**
     * Render the timeline state at {@code timeMicros} into a BufferedImage at
     * preview resolution. When {@code allowBlocking} is true, falls back to a
     * blocking per-frame FFmpeg extraction when the decoded window is not ready
     * (used for scrubbing); during playback pass false to avoid UI stalls and
     * reuse the last composed frame instead.
     */
    public BufferedImage renderFrame(Timeline tl, long timeMicros, BufferedImage fallback, boolean allowBlocking) {
        int w = previewWidth(tl.canvasWidth());
        int h = previewHeight(tl.canvasHeight());
        BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        paintBackground(g, tl, w, h);

        // video clips from bottom track to top
        List<Track> videoTracks = tl.tracksOf(Track.Kind.VIDEO);
        videoTracks.sort(java.util.Comparator.comparingInt(Track::getZOrder));
        for (Track track : videoTracks) {
            if (track.isHidden()) {
                continue;
            }
            for (TimelineClip clip : track.clips()) {
                if (!clip.isEnabled() || !clip.isClipAt(timeMicros)) {
                    continue;
                }
                BufferedImage frame = frameAt(clip, timeMicros, tl);
                if (frame == null && allowBlocking) {
                    frame = singleFrameFallback(clip, timeMicros, tl, w, h);
                }
                if (frame != null) {
                    paintVideoClip(g, clip, frame, w, h, timeMicros);
                }
            }
        }

        // image clips
        for (Track track : tl.tracksOf(Track.Kind.IMAGE)) {
            if (track.isHidden()) {
                continue;
            }
            for (TimelineClip clip : track.clips()) {
                if (clip.isEnabled() && clip.isClipAt(timeMicros)) {
                    paintImageClip(g, clip, w, h, timeMicros);
                }
            }
        }

        // text clips
        for (Track track : tl.tracksOf(Track.Kind.TEXT)) {
            if (track.isHidden()) {
                continue;
            }
            for (TimelineClip clip : track.clips()) {
                if (clip.isEnabled() && clip.isClipAt(timeMicros)) {
                    paintTextClip(g, clip, w, h, timeMicros);
                }
            }
        }

        // effect overlay blur regions for all video clips
        for (Track track : videoTracks) {
            for (TimelineClip clip : track.clips()) {
                if (clip.isEnabled() && clip.isClipAt(timeMicros)) {
                    paintRegionBlur(canvas, clip, w, h, timeMicros);
                }
            }
        }

        g.dispose();
        return canvas;
    }

    private void paintBackground(Graphics2D g, Timeline tl, int w, int h) {
        var bg = tlOwnerBackground(tl);
        switch (bg.getType()) {
            case GRADIENT -> {
                java.awt.GradientPaint gp = bg.isGradientVertical()
                        ? new java.awt.GradientPaint(0, 0, color(bg.getGradientColorA()), 0, h, color(bg.getGradientColorB()))
                        : new java.awt.GradientPaint(0, 0, color(bg.getGradientColorA()), w, 0, color(bg.getGradientColorB()));
                g.setPaint(gp);
                g.fillRect(0, 0, w, h);
            }
            case IMAGE -> {
                BufferedImage img = loadImage(bg.getImagePath(), w, h);
                if (img != null) {
                    g.drawImage(img, 0, 0, w, h, null);
                } else {
                    g.setColor(color(bg.getSolidColor()));
                    g.fillRect(0, 0, w, h);
                }
            }
            case TRANSPARENT -> {
                g.setColor(new Color(0, 0, 0, 0));
                g.fillRect(0, 0, w, h);
            }
            default -> {
                g.setColor(color(bg.getSolidColor()));
                g.fillRect(0, 0, w, h);
            }
        }
    }

    private videoforge.effects.BackgroundEffect tlOwnerBackground(Timeline tl) {
        // The background lives on the project; the UI passes it in via a holder.
        videoforge.effects.BackgroundEffect bg = backgroundOverride;
        return bg != null ? bg : new videoforge.effects.BackgroundEffect();
    }

    private videoforge.effects.BackgroundEffect backgroundOverride;

    public void setBackgroundOverride(videoforge.effects.BackgroundEffect background) {
        this.backgroundOverride = background;
    }

    private void paintVideoClip(Graphics2D g, TimelineClip clip, BufferedImage frame,
                                int w, int h, long timeMicros) {
        double cx = centerX(clip, w);
        double cy = centerY(clip, h);
        double baseScale = Math.min((double) w / frame.getWidth(), (double) h / frame.getHeight());
        double scale = baseScale * clip.getScale() * clip.getScaleX() * clip.getScaleY();
        double rotation = clip.evaluate(TimelineClip.P_ROTATION, timeMicros - clip.getTimelineStart(), clip.getRotation());
        double opacity = clip.evaluate(TimelineClip.P_OPACITY, timeMicros - clip.getTimelineStart(), clip.getOpacity());
        AffineTransform at = new AffineTransform();
        at.translate(cx, cy);
        at.rotate(Math.toRadians(rotation));
        at.scale(scale, scale);
        at.translate(-frame.getWidth() / 2.0, -frame.getHeight() / 2.0);
        g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
                (float) Math.max(0, Math.min(1, opacity))));
        g.drawImage(frame, at, null);
        g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1f));
    }

    private BufferedImage singleFrameFallback(TimelineClip clip, long timeMicros, Timeline tl, int w, int h) {
        long clipLocal = timeMicros - clip.getTimelineStart();
        double sourceTime = TimeUtils.microsToSeconds(clip.getSourceStart()
                + Math.round(clipLocal * clip.getSpeed()));
        byte[] png = ffmpeg.extractFrameBytes(Path.of(clip.getSourcePath()), sourceTime, w, false);
        if (png == null) {
            return null;
        }
        try {
            return javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        } catch (IOException e) {
            return null;
        }
    }

    private void paintImageClip(Graphics2D g, TimelineClip clip, int w, int h, long timeMicros) {
        if (clip.getSourcePath() == null) {
            return;
        }
        BufferedImage img = imageCache.computeIfAbsent(clip.getSourcePath(), p -> {
            try {
                return javax.imageio.ImageIO.read(Path.of(p).toFile());
            } catch (IOException e) {
                return null;
            }
        });
        if (img == null) {
            return;
        }
        long local = timeMicros - clip.getTimelineStart();
        double baseScale = Math.min((double) w / img.getWidth(), (double) h / img.getHeight());
        double scale = baseScale * clip.getScale() * clip.getScaleX() * clip.getScaleY();
        double rotation = clip.evaluate(TimelineClip.P_ROTATION, local, clip.getRotation());
        double opacity = clip.evaluate(TimelineClip.P_OPACITY, local, clip.getOpacity());
        AffineTransform at = new AffineTransform();
        at.translate(centerX(clip, w), centerY(clip, h));
        at.rotate(Math.toRadians(rotation));
        at.scale(scale, scale);
        at.translate(-img.getWidth() / 2.0, -img.getHeight() / 2.0);
        g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
                (float) Math.max(0, Math.min(1, opacity))));
        g.drawImage(img, at, null);
        g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1f));
    }

    private void paintTextClip(Graphics2D g, TimelineClip clip, int w, int h, long timeMicros) {
        TextEffect t = clip.getText();
        if (t == null || t.getText() == null || t.getText().isEmpty()) {
            return;
        }
        long local = timeMicros - clip.getTimelineStart();
        double opacity = clip.evaluate(TimelineClip.P_OPACITY, local, clip.getOpacity()) * t.getOpacity();
        double scale = clip.evaluate(TimelineClip.P_SCALE, local, clip.getScale());
        double rotation = clip.evaluate(TimelineClip.P_ROTATION, local, clip.getRotation());

        int style = java.awt.Font.PLAIN;
        if (t.isBold() && t.isItalic()) style = java.awt.Font.BOLD | java.awt.Font.ITALIC;
        else if (t.isBold()) style = java.awt.Font.BOLD;
        else if (t.isItalic()) style = java.awt.Font.ITALIC;
        double size = t.getFontSize() * scale * (Math.min(w, h) / 720.0);
        java.awt.Font font = new java.awt.Font(safeFont(t.getFont()), style, Math.max(8, (int) size));

        double baseX = centerX(clip, w);
        double baseY = centerY(clip, h);
        java.awt.FontMetrics fm = g.getFontMetrics(font);
        String[] lines = t.getText().split("\n");
        double lineHeight = fm.getHeight() + t.getLineSpacing();
        double totalH = lines.length * lineHeight;
        double totalW = 0;
        for (String line : lines) {
            totalW = Math.max(totalW, fm.stringWidth(line) + t.getLetterSpacing() * (line.length() - 1));
        }
        // animations
        double anim = t.getAnimationDuration();
        double clipStart = clip.getTimelineStart();
        double clipEnd = clip.timelineEnd();
        double inT = Math.min(1, (timeMicros - clipStart) / (anim * 1_000_000.0));
        double outT = Math.min(1, (clipEnd - timeMicros) / (anim * 1_000_000.0));
        double alpha = opacity;
        double slideX = 0, slideY = 0;
        if (t.isFadeIn()) alpha *= Math.max(0, Math.min(1, inT));
        if (t.isFadeOut()) alpha *= Math.max(0, Math.min(1, outT));
        if (t.isSlideIn()) slideX -= (1 - inT) * t.getSlideDistance() * (Math.min(w, h) / 720.0);
        if (t.isSlideOut()) slideX += (1 - outT) * t.getSlideDistance() * (Math.min(w, h) / 720.0);
        if (t.isPopIn()) scale *= 0.5 + 0.5 * inT;

        g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
                (float) Math.max(0, Math.min(1, alpha))));

        double startY = baseY - totalH / 2.0 + fm.getAscent();
        for (int i = 0; i < lines.length; i++) {
            double lineW = fm.stringWidth(lines[i]) + t.getLetterSpacing() * (lines[i].length() - 1);
            double x = baseX - lineW / 2.0 + slideX;
            if ("left".equals(t.getAlign())) x = 8;
            else if ("right".equals(t.getAlign())) x = w - lineW - 8;
            double y = startY + i * lineHeight + slideY;
            drawTextWithStyle(g, lines[i], font, (int) x, (int) y, t, scale, rotation, baseX, baseY);
        }
        g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1f));
    }

    private void drawTextWithStyle(Graphics2D g, String text, java.awt.Font font, int x, int y,
                                   TextEffect t, double scale, double rotation, double cx, double cy) {
        AffineTransform orig = g.getTransform();
        g.rotate(Math.toRadians(rotation), cx, cy);
        int r = (t.getColor() >> 16) & 0xff;
        int gg = (t.getColor() >> 8) & 0xff;
        int b = t.getColor() & 0xff;
        g.setFont(font);

        if (t.isShadowEnabled()) {
            int sr = (t.getShadowColor() >> 16) & 0xff;
            int sg = (t.getShadowColor() >> 8) & 0xff;
            int sb = t.getShadowColor() & 0xff;
            g.setColor(new Color(sr, sg, sb));
            double dist = t.getShadowDistance() * scale;
            g.drawString(text, x + (int) dist, y + (int) dist);
        }
        if (t.isBackgroundEnabled()) {
            int br = (t.getBackgroundColor() >> 16) & 0xff;
            int bgc = (t.getBackgroundColor() >> 8) & 0xff;
            int bb = t.getBackgroundColor() & 0xff;
            java.awt.FontMetrics fm = g.getFontMetrics();
            int pad = 6;
            g.setColor(new Color(br, bgc, bb, (int) (t.getBackgroundOpacity() * 255)));
            g.fillRoundRect(x - pad, y - fm.getAscent() - pad / 2, fm.stringWidth(text) + 2 * pad, fm.getHeight() + pad, 8, 8);
        }
        if (t.isStrokeEnabled()) {
            int sr = (t.getStrokeColor() >> 16) & 0xff;
            int sg = (t.getStrokeColor() >> 8) & 0xff;
            int sb = t.getStrokeColor() & 0xff;
            g.setColor(new Color(sr, sg, sb));
            java.awt.font.GlyphVector gv = font.createGlyphVector(g.getFontRenderContext(), text);
            java.awt.Shape shape = gv.getOutline(x, y);
            g.setStroke(new BasicStroke((float) Math.max(1, t.getStrokeWidth() * scale)));
            g.draw(shape);
        }
        g.setColor(new Color(r, gg, b));
        g.drawString(text, x, y);
        if (t.isUnderline()) {
            java.awt.FontMetrics fm = g.getFontMetrics();
            g.drawLine(x, y + 2, x + fm.stringWidth(text), y + 2);
        }
        g.setTransform(orig);
    }

    private void paintRegionBlur(BufferedImage canvas, TimelineClip clip, int w, int h, long timeMicros) {
        BlurEffect blur = clip.blurEffect();
        if (blur == null || !blur.isEnabled() || blur.getMode() != BlurEffect.Mode.REGION) {
            return;
        }
        long local = timeMicros - clip.getTimelineStart();
        double strength = clip.evaluate(TimelineClip.P_BLUR, local, blur.getStrength());
        if (strength <= 0.5) {
            return;
        }
        int rx = (int) (blur.getRegionX() * w);
        int ry = (int) (blur.getRegionY() * h);
        int rw = (int) (blur.getRegionW() * w);
        int rh = (int) (blur.getRegionH() * h);
        if (rx < 0 || ry < 0 || rw <= 1 || rh <= 1 || rx + rw > w || ry + rh > h) {
            return;
        }
        int region = Math.max(4, (int) (strength * 1.6));
        float[] weights = new float[region * region];
        java.util.Arrays.fill(weights, 1f / (region * region));
        ConvolveOp op = new ConvolveOp(new Kernel(region, region, weights), ConvolveOp.EDGE_NO_OP, null);
        BufferedImage regionImg = canvas.getSubimage(rx, ry, rw, rh);
        BufferedImage copy = new BufferedImage(rw, rh, BufferedImage.TYPE_INT_RGB);
        Graphics2D cg = copy.createGraphics();
        cg.drawImage(regionImg, 0, 0, null);
        cg.dispose();
        BufferedImage blurred = op.filter(copy, null);
        Graphics2D g = canvas.createGraphics();
        g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
                (float) Math.max(0.2f, Math.min(1f, (float) (0.4 + blur.getFeather())))));
        g.drawImage(blurred, rx, ry, null);
        g.dispose();
    }

    // ---------- helpers ----------

    private static Color color(int rgb) {
        return new Color((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
    }

    private double centerX(TimelineClip clip, int w) {
        return Double.isNaN(clip.getPositionX()) ? w / 2.0 : clip.getPositionX() * (w / 1920.0);
    }

    private double centerY(TimelineClip clip, int h) {
        return Double.isNaN(clip.getPositionY()) ? h / 2.0 : clip.getPositionY() * (h / 1080.0);
    }

    private static String safeFont(String font) {
        java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String name : ge.getAvailableFontFamilyNames()) {
            if (name.equalsIgnoreCase(font)) {
                return name;
            }
        }
        return java.awt.Font.SANS_SERIF;
    }

    private BufferedImage loadImage(String path, int w, int h) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        try {
            BufferedImage img = javax.imageio.ImageIO.read(Path.of(path).toFile());
            if (img == null) {
                return null;
            }
            BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, w, h, null);
            g.dispose();
            return scaled;
        } catch (IOException e) {
            return null;
        }
    }

    public void shutdown() {
        decodePool.shutdownNow();
    }
}
