package videoforge.rendering;

import videoforge.effects.BackgroundEffect;
import videoforge.effects.BlurEffect;
import videoforge.effects.ChromaKeyEffect;
import videoforge.effects.ColorEffect;
import videoforge.effects.CropEffect;
import videoforge.effects.TextEffect;
import videoforge.logging.AppLog;
import videoforge.media.MediaLibrary;
import videoforge.project.ExportSettings;
import videoforge.project.VideoProject;
import videoforge.timeline.Interpolation;
import videoforge.timeline.Keyframe;
import videoforge.timeline.Timeline;
import videoforge.timeline.TimelineClip;
import videoforge.timeline.Track;
import videoforge.timeline.TransitionType;
import videoforge.utils.TimeUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders a timeline to a media file by building a single FFmpeg filter graph
 * that composites every clip: background, video/image clips (trim, speed,
 * reverse, freeze, crop, color, blur, chroma key, transforms, keyframes), text
 * overlays and the audio mix. Progress and log lines stream back to the UI.
 */
public final class RenderEngine {

    private static final AppLog LOG = AppLog.get("ffmpeg");
    private static final Pattern TIME_PATTERN =
            Pattern.compile("time=(\\d+):(\\d+):(\\d+(?:\\.\\d+)?)");

    public interface Listener {
        void onProgress(double percent, String etaText, String logLine);
    }

    private final AtomicBoolean cancel = new AtomicBoolean();
    private Process process;

    public void cancel() {
        cancel.set(true);
        if (process != null) {
            process.destroyForcibly();
        }
    }

    public boolean isCancelled() {
        return cancel.get();
    }

    public static final class Result {
        public boolean ok;
        public String message;
        public String log;
    }

    /** Renders the project timeline to {@code outFile}. Synchronous; listener on render thread. */
    public Result render(VideoProject project, ExportSettings settings, Path outFile,
                         MediaLibrary library, Listener listener) {
        cancel.set(false);
        Result result = new Result();
        try {
            Files.createDirectories(outFile.toAbsolutePath().getParent());
        } catch (IOException e) {
            result.ok = false;
            result.message = "Cannot create output folder: " + e.getMessage();
            return result;
        }

        double duration = Math.max(0.1, TimeUtils.microsToSeconds(project.timeline().duration()));
        String ffmpeg = videoforge.config.AppConfig.get().ffmpeg();
        if (ffmpeg == null || ffmpeg.isBlank()) {
            result.ok = false;
            result.message = "FFmpeg not found. Install FFmpeg and set its path in Settings.";
            return result;
        }

        GraphBuilder gb = new GraphBuilder(project, settings, duration, library);
        if (!gb.build()) {
            result.ok = false;
            result.message = gb.error;
            return result;
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpeg);
        cmd.add("-y");
        cmd.add("-hide_banner");
        cmd.add("-loglevel");
        cmd.add("info");
        cmd.add("-nostdin");
        for (Path input : gb.inputs) {
            cmd.add("-i");
            cmd.add(input.toString());
        }
        cmd.add("-filter_complex");
        cmd.add(gb.filterGraph);
        cmd.add("-map");
        cmd.add("[vout]");
        cmd.add("-map");
        cmd.add("[aout]");
        cmd.add("-c:v");
        cmd.add(gb.videoCodec);
        if ("crf".equals(settings.qualityMode)) {
            cmd.add("-crf");
            cmd.add(String.valueOf(settings.crf));
        } else {
            cmd.add("-b:v");
            cmd.add(String.valueOf(settings.bitrate));
        }
        cmd.add("-preset");
        cmd.add(gb.preset);
        cmd.add("-c:a");
        cmd.add(settings.audioCodec);
        cmd.add("-pix_fmt");
        cmd.add("yuv420p");
        cmd.add("-r");
        cmd.add(String.valueOf(settings.fps));
        cmd.add("-t");
        cmd.add(String.format(Locale.ROOT, "%.3f", duration));
        cmd.add(outFile.toString());

        LOG.raw("Render: " + videoforge.utils.ProcessUtils.commandLine(cmd));
        LOG.raw("Filtergraph: " + gb.filterGraph);

        StringBuilder log = new StringBuilder();
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        try {
            process = pb.start();
            long startNs = System.nanoTime();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (log.length() < 200_000) {
                        log.append(line).append('\n');
                    }
                    Matcher m = TIME_PATTERN.matcher(line);
                    if (m.find()) {
                        double secs = Double.parseDouble(m.group(1)) * 3600
                                + Double.parseDouble(m.group(2)) * 60
                                + Double.parseDouble(m.group(3));
                        double pct = Math.min(100, secs / duration * 100);
                        double elapsed = (System.nanoTime() - startNs) / 1e9;
                        String eta = secs > 0.1 && pct > 1
                                ? formatDuration(elapsed / pct * (100 - pct)) : "";
                        listener.onProgress(pct, eta, line);
                    }
                    if (cancel.get()) {
                        process.destroyForcibly();
                        break;
                    }
                }
            }
            int code = process.waitFor();
            process = null;
            result.log = log.toString();
            if (cancel.get()) {
                result.ok = false;
                result.message = "Render cancelled by user";
                try {
                    Files.deleteIfExists(outFile);
                } catch (IOException ignored) {
                }
            } else if (code == 0 && Files.exists(outFile)) {
                result.ok = true;
                result.message = "Rendered to " + outFile;
                LOG.info("Render OK: " + outFile);
            } else {
                result.ok = false;
                result.message = "FFmpeg exited with code " + code;
                LOG.error("Render failed: " + tail(log.toString()), null);
            }
        } catch (IOException e) {
            result.ok = false;
            result.message = "Could not run FFmpeg: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.ok = false;
            result.message = "Render interrupted";
        }
        return result;
    }

    private static String tail(String s) {
        String[] lines = s == null ? new String[0] : s.split("\n");
        return String.join("\n", java.util.Arrays.copyOfRange(lines, Math.max(0, lines.length - 12), lines.length));
    }

    private static String formatDuration(double secs) {
        long total = Math.round(secs);
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    // ======================================================================
    //  Filter graph builder
    // ======================================================================

    private static final class GraphBuilder {
        final List<Path> inputs = new ArrayList<>();
        final Map<String, Integer> inputIndex = new LinkedHashMap<>();
        final List<String> filters = new ArrayList<>();
        final int outW;
        final int outH;
        final double fps;
        final String videoCodec;
        final String preset;
        final double duration;
        final Timeline tl;
        final BackgroundEffect bg;
        final ExportSettings settings;

        String filterGraph;
        String error;
        String mainLabel = "bg";
        int labelId = 0;
        final FFmpegManager ffmpegProbe = new FFmpegManager();

        GraphBuilder(VideoProject project, ExportSettings settings, double duration, MediaLibrary library) {
            this.tl = project.timeline();
            this.bg = project.background();
            this.settings = settings;
            this.outW = settings.width;
            this.outH = settings.height;
            this.fps = settings.fps;
            this.duration = duration;
            switch (settings.videoCodec) {
                case "h264" -> { videoCodec = "libx264"; preset = "medium"; }
                case "h265", "hevc" -> { videoCodec = "libx265"; preset = "medium"; }
                case "vp9" -> { videoCodec = "libvpx-vp9"; preset = "good"; }
                case "av1" -> { videoCodec = "libsvtav1"; preset = "8"; }
                default -> { videoCodec = settings.videoCodec; preset = "medium"; }
            }
        }

        boolean build() {
            try {
                buildBackground();
                if (!buildVideoClips()) {
                    return false;
                }
                if (!buildImageClips()) {
                    return false;
                }
                buildTextOverlays();
                buildAudio();
                buildOutput();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < filters.size(); i++) {
                    if (i > 0) {
                        sb.append(';');
                    }
                    sb.append(filters.get(i));
                }
                this.filterGraph = sb.toString();
                return true;
            } catch (Exception e) {
                error = "Filter graph error: " + e.getMessage();
                LOG.error(error, e);
                return false;
            }
        }

        private void buildBackground() {
            switch (bg.getType()) {
                case GRADIENT -> {
                    // geq-based vertical/horizontal gradient (no "gradients" filter needed)
                    int[] a = rgb(bg.getGradientColorA());
                    int[] b = rgb(bg.getGradientColorB());
                    String coord = bg.isGradientVertical() ? "Y" : "X";
                    String g = "geq=r='lerp(" + a[0] + "," + b[0] + "," + coord + "/H)':"
                            + "g='lerp(" + a[1] + "," + b[1] + "," + coord + "/H)':"
                            + "b='lerp(" + a[2] + "," + b[2] + "," + coord + "/H)'";
                    filters.add("color=c=white:s=" + outW + "x" + outH + ":r=" + fps + ":d=" + duration + "[bgbase]");
                    filters.add("[bgbase]" + g + "[bg]");
                }
                case IMAGE -> {
                    if (bg.getImagePath() != null && Files.exists(Path.of(bg.getImagePath()))) {
                        int i = inputFor(Path.of(bg.getImagePath()));
                        filters.add("[" + i + ":v]scale=" + outW + ":" + outH
                                + ":force_original_aspect_ratio=increase,crop=" + outW + ":" + outH
                                + ",loop=loop=-1:size=1,fps=" + fps + ",setpts=PTS-STARTPTS[bg]");
                    } else {
                        filters.add("color=c=black:s=" + outW + "x" + outH + ":r=" + fps + ":d=" + duration + "[bg]");
                    }
                }
                default -> filters.add("color=c=" + hexColor(bg.getSolidColor())
                        + ":s=" + outW + "x" + outH + ":r=" + fps + ":d=" + duration + "[bg]");
            }
        }

        private int inputFor(Path file) {
            String key = file.toAbsolutePath().normalize().toString();
            Integer idx = inputIndex.get(key);
            if (idx != null) {
                return idx;
            }
            int i = inputs.size();
            inputs.add(file);
            inputIndex.put(key, i);
            return i;
        }

        /** Video clips. Returns false if a filter graph could not be built. */
        private boolean buildVideoClips() {
            List<Track> videoTracks = new ArrayList<>(tl.tracksOf(Track.Kind.VIDEO));
            videoTracks.sort(Comparator.comparingInt(Track::getZOrder));
            List<TimelineClip> clips = new ArrayList<>();
            for (Track t : videoTracks) {
                clips.addAll(t.clips());
            }

            boolean blurUsed = false;
            for (int ci = 0; ci < clips.size(); ci++) {
                TimelineClip clip = clips.get(ci);
                if (!clip.isEnabled() || clip.isHidden() || clip.getSourcePath() == null) {
                    continue;
                }
                Path file = Path.of(clip.getSourcePath());
                if (!Files.exists(file)) {
                    continue;
                }
                int src = inputFor(file);
                double sStart = TimeUtils.microsToSeconds(clip.getSourceStart());
                double sEnd = TimeUtils.microsToSeconds(clip.getSourceEnd());
                double cStart = TimeUtils.microsToSeconds(clip.getTimelineStart());
                double cEnd = TimeUtils.microsToSeconds(clip.timelineEnd());
                double cDur = cEnd - cStart;

                double srcW = probeWidth(file);
                double srcH = probeHeight(file);

                // underlying blurred stream for the bottom-most visible video clip
                boolean blurBehind = !blurUsed && bg.getFillMode() == BackgroundEffect.FillMode.BLUR_BEHIND
                        && Math.abs(clip.getRotation()) < 0.01;
                if (blurBehind) {
                    blurUsed = true;
                    String bb = "bb" + labelId++;
                    filters.add("[" + src + ":v]trim=start=" + sStart + ":end=" + sEnd
                            + ",setpts=PTS-STARTPTS,scale=" + outW + ":" + outH
                            + ":force_original_aspect_ratio=increase,crop=" + outW + ":" + outH
                            + ",gblur=sigma=30[" + bb + "]");
                    String comp = "c" + labelId++;
                    filters.add("[" + mainLabel + "][" + bb + "]overlay=0:0[" + comp + "]");
                    mainLabel = comp;
                }

                String label = "v" + labelId++;
                List<String> chain = new ArrayList<>();
                chain.add("trim=start=" + sStart + ":end=" + sEnd);
                if (clip.isReverse()) {
                    chain.add("reverse");
                }
                if (clip.isFreezeFrame()) {
                    chain.add("loop=loop=-1:size=1");
                    chain.add("trim=duration=" + cDur);
                    chain.add("setpts=PTS-STARTPTS+" + cStart + "/TB");
                } else {
                    chain.add("setpts=(PTS-STARTPTS)/" + format(clip.getSpeed()) + "+" + cStart + "/TB");
                }

                chain = buildVisualChain(chain, clip, srcW, srcH, cStart, cDur);
                filters.add("[" + src + ":v]" + String.join(",", chain) + "[" + label + "]");

                String xExpr = ffExpr("positionX", clip, clipX(clip)) + "-overlay_w/2";
                String yExpr = ffExpr("positionY", clip, clipY(clip)) + "-overlay_h/2";
                String comp = "c" + labelId++;
                filters.add("[" + mainLabel + "][" + label + "]overlay=x='" + xExpr + "':y='" + yExpr
                        + "':eval=frame:enable='between(t," + format(cStart) + "," + format(cEnd) + ")'[" + comp + "]");
                mainLabel = comp;
            }
            return true;
        }

        private List<String> buildVisualChain(List<String> chain, TimelineClip clip, double srcW, double srcH,
                                              double cStart, double cDur) {
            CropEffect crop = clip.cropEffect();
            ColorEffect color = clip.colorEffect();
            BlurEffect blur = clip.blurEffect();
            ChromaKeyEffect chroma = clip.chromaKeyEffect();
            double baseFit = Math.min(outW / srcW, outH / srcH);
            double scale = baseFit * clip.getScale() * clip.getScaleX() * clip.getScaleY();
            double effW = srcW * scale;
            double effH = srcH * scale;

            if (crop != null && crop.isEnabled() && !crop.isNeutral()) {
                double left = effW * crop.getLeft();
                double top = effH * crop.getTop();
                double w = effW * (1 - crop.getLeft() - crop.getRight());
                double h = effH * (1 - crop.getTop() - crop.getBottom());
                chain.add("crop=" + round(w) + ":" + round(h) + ":" + round(left) + ":" + round(top));
            }
            chain.add("scale=" + ffExpr("scale", clip, effW) + ":" + ffExpr("scale", clip, effH) + ":eval=frame");

            if (chroma != null && chroma.isEnabled()) {
                chain.add("chromakey=color=" + String.format("0x%06X", chroma.getKeyColor())
                        + ":similarity=" + format(chroma.getTolerance() * 2)
                        + ":blend=" + format(chroma.getSoftness() * 2));
            }
            if (blur != null && blur.isEnabled() && blur.getMode() == BlurEffect.Mode.WHOLE && blur.getStrength() > 0) {
                chain.add("gblur=sigma=" + format(Math.max(0.1, blur.getStrength() / 8)));
            }
            if (color != null && color.isEnabled() && !color.isNeutral()) {
                String cf = colorFilter(color);
                if (!cf.isBlank()) {
                    chain.add(cf);
                }
            }
            boolean rotKf = clip.keyframes("rotation") != null && !clip.keyframes("rotation").isEmpty();
            if (Math.abs(clip.getRotation()) > 0.01 || rotKf) {
                String rot = "(" + ffExpr("rotation", clip, clip.getRotation()) + ")*PI/180";
                chain.add("rotate=a='" + rot + "'");
            }

            double opacity = clip.getOpacity();
            double fadeIn = clip.getTransitionIn() == TransitionType.NONE ? 0 : clip.getTransitionInDuration();
            double fadeOut = clip.getTransitionOut() == TransitionType.NONE ? 0 : clip.getTransitionOutDuration();
            boolean hasFade = false;
            if (fadeOut > 0) {
                chain.add("fade=t=out:st=" + format(Math.max(0, cStart + cDur - fadeOut)) + ":d=" + fadeOut + ":alpha=1");
                hasFade = true;
            }
            if (fadeIn > 0) {
                chain.add("fade=t=in:st=" + format(cStart) + ":d=" + fadeIn + ":alpha=1");
                hasFade = true;
            }
            if (opacity < 0.999 || hasFade) {
                chain.add("format=rgba,colorchannelmixer=aa=" + format(opacity));
            }
            return chain;
        }

        private String colorFilter(ColorEffect c) {
            List<String> parts = new ArrayList<>();
            if (Math.abs(c.getContrast()) > 0.1 || Math.abs(c.getBrightness()) > 0.1
                    || Math.abs(c.getSaturation()) > 0.1 || Math.abs(c.getGamma() - 1) > 0.01
                    || Math.abs(c.getExposure()) > 0.01) {
                parts.add("eq=contrast=" + format(1 + c.getContrast() / 100.0)
                        + ":brightness=" + format(c.getBrightness() / 100.0 * 0.3)
                        + ":saturation=" + format(1 + c.getSaturation() / 100.0)
                        + ":gamma=" + format(c.getGamma())
                        + ":gamma_r=" + format(c.getGamma() + c.getTint() / 400.0)
                        + ":gamma_b=" + format(c.getGamma() - c.getTint() / 400.0));
            }
            if (c.getPreset() == ColorEffect.Preset.BLACK_WHITE) {
                parts.add("hue=s=0");
            } else if (c.getPreset() == ColorEffect.Preset.SEPIA) {
                parts.add("colorchannelmixer=rr=0.393:rg=0.769:rb=0.189:gr=0.349:gg=0.686:gb=0.168:br=0.272:bg=0.534:bb=0.131");
            }
            if (Math.abs(c.getTemperature()) > 0.1) {
                double t = c.getTemperature() / 100.0;
                parts.add("colorbalance=rs=" + format(t * 0.3) + ":bs=" + format(-t * 0.3));
            }
            return String.join(",", parts);
        }

        /** Builds an FFmpeg expression evaluating a keyframed property at time t (global seconds). */
        private String ffExpr(String property, TimelineClip clip, double fallback) {
            List<Keyframe> frames = clip.keyframes(property);
            if (frames == null || frames.isEmpty()) {
                return format(fallback);
            }
            double clipStart = TimeUtils.microsToSeconds(clip.getTimelineStart());
            String expr = format(fallback);
            for (int i = frames.size() - 1; i >= 0; i--) {
                Keyframe k = frames.get(i);
                double t = clipStart + TimeUtils.microsToSeconds(k.getTimeMicros());
                double v = k.getValue();
                if (i < frames.size() - 1) {
                    Keyframe next = frames.get(i + 1);
                    double t1 = clipStart + TimeUtils.microsToSeconds(next.getTimeMicros());
                    expr = "if(between(t," + format(t) + "," + format(t1) + "),"
                            + interp(k.getInterpolation(), t, v, t1, next.getValue()) + "," + expr + ")";
                } else {
                    expr = "if(lt(t," + format(t) + ")," + expr + "," + format(v) + ")";
                }
            }
            return expr;
        }

        private static String interp(Interpolation interp, double t0, double v0, double t1, double v1) {
            double span = Math.max(1e-6, t1 - t0);
            String p = "(t-" + t0 + ")/" + span;
            return switch (interp) {
                case EASE_IN -> "(" + v0 + "+(" + v1 + "-" + v0 + ")*(" + p + "*" + p + "))";
                case EASE_OUT -> "(" + v0 + "+(" + v1 + "-" + v0 + ")*(1-(1-" + p + ")*(1-" + p + ")))";
                case EASE_IN_OUT -> "(" + v0 + "+(" + v1 + "-" + v0 + ")*(" + p + "*" + p + "*(3-2*" + p + ")))";
                default -> "(" + v0 + "+(" + v1 + "-" + v0 + ")*" + p + ")";
            };
        }

        private boolean buildImageClips() {
            List<Track> imgTracks = new ArrayList<>(tl.tracksOf(Track.Kind.IMAGE));
            imgTracks.sort(Comparator.comparingInt(Track::getZOrder));
            for (Track track : imgTracks) {
                for (TimelineClip clip : track.clips()) {
                    if (!clip.isEnabled() || clip.isHidden() || clip.getSourcePath() == null) {
                        continue;
                    }
                    Path file = Path.of(clip.getSourcePath());
                    if (!Files.exists(file)) {
                        continue;
                    }
                    int src = inputFor(file);
                    double cStart = TimeUtils.microsToSeconds(clip.getTimelineStart());
                    double cEnd = TimeUtils.microsToSeconds(clip.timelineEnd());
                    double srcW = probeWidth(file);
                    double srcH = probeHeight(file);
                    double baseFit = Math.min(outW / srcW, outH / srcH);
                    double scale = baseFit * clip.getScale() * clip.getScaleX() * clip.getScaleY();
                    String label = "img" + labelId++;
                    List<String> chain = new ArrayList<>();
                    chain.add("scale=" + ffExpr("scale", clip, srcW * scale)
                            + ":" + ffExpr("scale", clip, srcH * scale) + ":eval=frame");
                    chain.add("loop=loop=-1:size=1");
                    chain.add("fps=" + fps);
                    chain.add("setpts=PTS-STARTPTS");
                    chain.add("trim=duration=" + format(cEnd - cStart));
                    chain.add("setpts=PTS-STARTPTS+" + cStart + "/TB");
                    if (clip.getOpacity() < 0.999) {
                        chain.add("format=rgba,colorchannelmixer=aa=" + format(clip.getOpacity()));
                    }
                    filters.add("[" + src + ":v]" + String.join(",", chain) + "[" + label + "]");
                    String xExpr = ffExpr("positionX", clip, clipX(clip)) + "-overlay_w/2";
                    String yExpr = ffExpr("positionY", clip, clipY(clip)) + "-overlay_h/2";
                    String comp = "c" + labelId++;
                    filters.add("[" + mainLabel + "][" + label + "]overlay=x='" + xExpr + "':y='" + yExpr
                            + "':eval=frame:enable='between(t," + format(cStart) + "," + format(cEnd) + ")'[" + comp + "]");
                    mainLabel = comp;
                }
            }
            return true;
        }

        private void buildTextOverlays() {
            List<Track> textTracks = new ArrayList<>(tl.tracksOf(Track.Kind.TEXT));
            textTracks.sort(Comparator.comparingInt(Track::getZOrder));
            for (Track track : textTracks) {
                for (TimelineClip clip : track.clips()) {
                    if (!clip.isEnabled() || clip.isHidden()) {
                        continue;
                    }
                    TextEffect t = clip.getText();
                    if (t == null || t.getText().isBlank()) {
                        continue;
                    }
                    Path textFile;
                    try {
                        textFile = Files.createTempFile("vforge-text-", ".txt");
                        Files.writeString(textFile, t.getText(), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        continue;
                    }
                    textFile.toFile().deleteOnExit();
                    Path fontFile = findFontFile(t.getFont());
                    double cStart = TimeUtils.microsToSeconds(clip.getTimelineStart());
                    double cEnd = TimeUtils.microsToSeconds(clip.timelineEnd());

                    List<String> params = new ArrayList<>();
                    params.add("textfile=" + textFile);
                    if (fontFile != null) {
                        params.add("fontfile=" + fontFile);
                    } else {
                        params.add("font=" + t.getFont());
                    }
                    params.add("fontsize=" + round(t.getFontSize()));
                    params.add("fontcolor=" + hexColor(t.getColor()));

                    double cx = clipX(clip);
                    double cy = clipY(clip);
                    String xExpr = ffExpr("positionX", clip, cx) + "-text_w/2";
                    String yExpr = ffExpr("positionY", clip, cy) + "-text_h/2";

                    double anim = Math.max(0.1, t.getAnimationDuration());
                    String alpha = null;
                    if (t.isFadeIn() || t.isFadeOut() || t.isPopIn()) {
                        String in = (t.isFadeIn() || t.isPopIn())
                                ? "if(lt(t," + format(cStart + anim) + "),(t-" + format(cStart) + ")/" + anim + ",1)"
                                : "1";
                        String out = t.isFadeOut()
                                ? "if(gt(t," + format(cEnd - anim) + "),max(0,1-(t-" + format(cEnd - anim) + ")/" + anim + ")," + in + ")"
                                : in;
                        alpha = out;
                    }
                    if (alpha != null) {
                        params.add("alpha='" + alpha + "'");
                    }
                    if (t.isSlideIn() || t.isSlideOut()) {
                        double dx = t.getSlideDistance();
                        String slide = "";
                        if (t.isSlideIn()) {
                            slide = "if(lt(t," + format(cStart + anim) + ")," + dx + "*(1-(t-" + format(cStart) + ")/" + anim + "),0)";
                        }
                        if (t.isSlideOut()) {
                            slide += (slide.isEmpty() ? "" : "+") + "if(gt(t," + format(cEnd - anim) + ")," + dx + "*(t-" + format(cEnd - anim) + ")/" + anim + ",0)";
                        }
                        xExpr = "(" + ffExpr("positionX", clip, cx) + "-text_w/2)-(" + slide + ")";
                    }
                    params.add("x='" + xExpr + "'");
                    params.add("y='" + yExpr + "'");
                    params.add("enable='between(t," + format(cStart) + "," + format(cEnd) + ")'");

                    if (t.isShadowEnabled()) {
                        params.add("shadowx=" + format(t.getShadowDistance()));
                        params.add("shadowy=" + format(t.getShadowDistance()));
                        params.add("shadowcolor=" + hexColor(t.getShadowColor()) + "@0.6");
                    }
                    if (t.isStrokeEnabled()) {
                        params.add("borderw=" + format(Math.max(0.5, t.getStrokeWidth())));
                        params.add("bordercolor=" + hexColor(t.getStrokeColor()));
                    }
                    if (t.isBackgroundEnabled()) {
                        params.add("box=1");
                        params.add("boxcolor=" + hexColor(t.getBackgroundColor()) + "@" + format(t.getBackgroundOpacity()));
                    }

                    String label = "txt" + labelId++;
                    filters.add("[" + mainLabel + "]drawtext=" + String.join(":", params) + "[" + label + "]");
                    mainLabel = label;
                }
            }
        }

        private void buildAudio() {
            List<String> labels = new ArrayList<>();
            for (Track track : tl.tracks()) {
                boolean isAudioTrack = track.getKind() == Track.Kind.AUDIO;
                for (TimelineClip clip : track.clips()) {
                    if (!clip.isEnabled() || clip.isHidden() || clip.getSourcePath() == null) {
                        continue;
                    }
                    boolean useAudio;
                    if (isAudioTrack) {
                        useAudio = !clip.isMuted();
                    } else if (clip.getKind() == TimelineClip.Kind.VIDEO) {
                        useAudio = clip.isHasAudio() && !clip.isAudioDetached() && !clip.isMuted();
                    } else {
                        useAudio = false;
                    }
                    if (!useAudio) {
                        continue;
                    }
                    Path file = Path.of(clip.getSourcePath());
                    if (!Files.exists(file)) {
                        continue;
                    }
                    int src = inputFor(file);
                    double sStart = TimeUtils.microsToSeconds(clip.getSourceStart());
                    double sEnd = TimeUtils.microsToSeconds(clip.getSourceEnd());
                    double cStart = TimeUtils.microsToSeconds(clip.getTimelineStart());
                    double cDur = TimeUtils.microsToSeconds(clip.duration());
                    String volExpr = "max(0," + ffExpr("volume", clip, clip.getVolume()) + ")";
                    List<String> chain = new ArrayList<>();
                    chain.add("atrim=start=" + sStart + ":end=" + sEnd);
                    chain.add("asetpts=PTS-STARTPTS");
                    chain.add("volume='" + volExpr + "':eval=frame");
                    double fadeIn = clip.getTransitionIn() == TransitionType.NONE ? 0 : clip.getTransitionInDuration();
                    double fadeOut = clip.getTransitionOut() == TransitionType.NONE ? 0 : clip.getTransitionOutDuration();
                    if (fadeIn > 0) {
                        chain.add("afade=t=in:st=0:d=" + fadeIn);
                    }
                    if (fadeOut > 0) {
                        chain.add("afade=t=out:st=" + format(Math.max(0, cDur - fadeOut)) + ":d=" + fadeOut);
                    }
                    chain.add("adelay=" + Math.round(cStart * 1000) + ":all=1");
                    String label = "a" + labelId++;
                    labels.add(label);
                    filters.add("[" + src + ":a]" + String.join(",", chain) + "[" + label + "]");
                }
            }
            if (labels.isEmpty()) {
                filters.add("anullsrc=r=48000:cl=stereo,atrim=duration=" + format(duration) + "[aout]");
            } else {
                StringBuilder mix = new StringBuilder();
                for (String l : labels) {
                    mix.append('[').append(l).append(']');
                }
                mix.append("amix=inputs=").append(labels.size()).append(":normalize=0");
                mix.append(",aformat=sample_fmts=fltp:sample_rates=48000:channel_layouts=stereo[aout]");
                filters.add(mix.toString());
            }
        }

        private void buildOutput() {
            filters.add("[" + mainLabel + "]format=yuv420p[vout]");
        }

        private double clipX(TimelineClip clip) {
            return Double.isNaN(clip.getPositionX()) ? outW / 2.0 : clip.getPositionX();
        }

        private double clipY(TimelineClip clip) {
            return Double.isNaN(clip.getPositionY()) ? outH / 2.0 : clip.getPositionY();
        }

        private double probeWidth(Path file) {
            var md = ffmpegProbe.probe(file);
            return md != null && md.getWidth() > 0 ? md.getWidth() : outW;
        }

        private double probeHeight(Path file) {
            var md = ffmpegProbe.probe(file);
            return md != null && md.getHeight() > 0 ? md.getHeight() : outH;
        }

        private static String hexColor(int rgb) {
            return String.format("0x%06X", rgb & 0xFFFFFF);
        }

        private static int[] rgb(int color) {
            return new int[]{(color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF};
        }

        private static String format(double v) {
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                return "0";
            }
            return String.format(Locale.ROOT, "%.4f", v);
        }

        private static long round(double v) {
            return Math.max(1, Math.round(v));
        }

        private static Path findFontFile(String family) {
            String[] candidates = {
                    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
                    "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
                    "/usr/share/fonts/TTF/DejaVuSans.ttf",
            };
            for (String c : candidates) {
                if (Files.exists(Path.of(c))) {
                    return Path.of(c);
                }
            }
            for (String dir : new String[]{"/usr/share/fonts/truetype", "/usr/share/fonts/TTF", "/usr/local/share/fonts"}) {
                Path d = Path.of(dir);
                if (!Files.isDirectory(d)) {
                    continue;
                }
                try (var stream = Files.walk(d, 3)) {
                    var found = stream.filter(p -> p.toString().endsWith(".ttf")).findFirst();
                    if (found.isPresent()) {
                        return found.get();
                    }
                } catch (IOException ignored) {
                }
            }
            return null;
        }
    }
}
