package videoforge.rendering;

import videoforge.config.AppConfig;
import videoforge.logging.AppLog;
import videoforge.media.MediaMetadata;
import videoforge.utils.ProcessUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Wrapper around the FFmpeg / FFprobe executables.
 *
 * <p>Responsibilities: locating the binaries, probing media metadata, extracting
 * single frames, and reporting available encoders. Every invocation uses a
 * {@code ProcessBuilder} argument list - never a shell.</p>
 */
public final class FFmpegManager {

    private static final AppLog LOG = AppLog.get("ffmpeg");

    public static final class Availability {
        public boolean ffmpegOk;
        public boolean ffprobeOk;
        public String ffmpegVersion = "";
        public String ffprobeVersion = "";
        public String message = "";
    }

    private final AppConfig config = AppConfig.get();

    /** Return full availability info (used by the dependency checker). */
    public Availability checkAvailability() {
        Availability a = new Availability();
        a.ffmpegOk = version(config.ffmpeg(), true) != null;
        a.ffprobeOk = version(config.ffprobe(), false) != null;
        if (a.ffmpegOk) {
            a.ffmpegVersion = version(config.ffmpeg(), true);
        }
        if (a.ffprobeOk) {
            a.ffprobeVersion = version(config.ffprobe(), false);
        }
        if (!a.ffmpegOk && !a.ffprobeOk) {
            a.message = "Neither ffmpeg nor ffprobe were found on PATH. "
                    + "Install FFmpeg or set the paths in Settings > External Tools.";
        } else if (!a.ffmpegOk) {
            a.message = "ffprobe was found but ffmpeg is missing.";
        } else if (!a.ffprobeOk) {
            a.message = "ffmpeg was found but ffprobe is missing.";
        }
        return a;
    }

    private String version(String bin, boolean isFfmpeg) {
        try {
            ProcessUtils.Result r = ProcessUtils.run(List.of(bin, "-version"), Duration.ofSeconds(10));
            if (r.exitCode != 0) {
                return null;
            }
            String first = r.stdout.isEmpty() ? r.stderr : r.stdout;
            String line = first.split("\n")[0].trim();
            return line.isEmpty() ? null : line;
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }

    /** True when the ffmpeg executable responds. */
    public boolean ffmpegAvailable() {
        return version(config.ffmpeg(), true) != null;
    }

    public boolean ffprobeAvailable() {
        return version(config.ffprobe(), false) != null;
    }

    // ---------- probing ----------

    /**
     * Probe a media file and populate metadata. Returns null on failure
     * (callers should surface the message to the user).
     */
    public MediaMetadata probe(Path file) {
        MediaMetadata meta = new MediaMetadata();
        if (file == null || !Files.exists(file)) {
            return meta;
        }
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(config.ffprobe());
            cmd.addAll(List.of("-v", "error", "-print_format", "json",
                    "-show_format", "-show_streams", file.toString()));
            ProcessUtils.Result r = ProcessUtils.run(cmd, Duration.ofSeconds(60));
            if (r.exitCode != 0) {
                LOG.warn("ffprobe failed for " + file + ": " + r.stderr);
                return meta;
            }
            parseProbe(r.stdout, meta);
        } catch (IOException | InterruptedException e) {
            LOG.warn("ffprobe error for " + file + ": " + e.getMessage());
        }
        return meta;
    }

    private void parseProbe(String json, MediaMetadata meta) {
        try {
            org.json.JSONObject root = new org.json.JSONObject(json);
            org.json.JSONObject format = root.optJSONObject("format");
            if (format != null) {
                meta.setFormat(format.optString("format_name", ""));
                meta.setBitrate(format.optLong("bit_rate", 0));
                meta.setDurationSeconds(format.optDouble("duration", 0));
            }
            org.json.JSONArray streams = root.optJSONArray("streams");
            if (streams != null) {
                for (int i = 0; i < streams.length(); i++) {
                    org.json.JSONObject s = streams.getJSONObject(i);
                    String codecType = s.optString("codec_type", "");
                    if ("video".equals(codecType)) {
                        meta.setHasVideo(true);
                        meta.setWidth(s.optInt("width", 0));
                        meta.setHeight(s.optInt("height", 0));
                        meta.setVideoCodec(s.optString("codec_name", ""));
                        double rate = parseFrameRate(s.optString("avg_frame_rate", s.optString("r_frame_rate", "")));
                        if (rate > 0) {
                            meta.setFps(rate);
                        }
                        if (meta.getDurationSeconds() <= 0) {
                            meta.setDurationSeconds(s.optDouble("duration", 0));
                        }
                    } else if ("audio".equals(codecType)) {
                        meta.setHasAudio(true);
                        meta.setAudioCodec(s.optString("codec_name", ""));
                        if (meta.getDurationSeconds() <= 0) {
                            meta.setDurationSeconds(s.optDouble("duration", 0));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not parse ffprobe output: " + e.getMessage());
        }
    }

    private static double parseFrameRate(String rate) {
        if (rate == null || rate.isBlank() || "0/0".equals(rate)) {
            return 0;
        }
        try {
            if (rate.contains("/")) {
                String[] parts = rate.split("/");
                double num = Double.parseDouble(parts[0]);
                double den = Double.parseDouble(parts[1]);
                return den != 0 ? num / den : 0;
            }
            return Double.parseDouble(rate);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ---------- frame extraction ----------

    /**
     * Extract a single frame as PNG. When {@code fastSeek} is true an input seek
     * is used (fast, may land on a keyframe); otherwise an output seek gives the
     * exact time but is slower. Width is scaled (aspect preserved), height -1.
     */
    public boolean extractFrame(Path source, double timeSeconds, int width, Path outPng, boolean fastSeek) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(config.ffmpeg());
            cmd.addAll(List.of("-y", "-hide_banner", "-loglevel", "error"));
            if (fastSeek) {
                cmd.addAll(List.of("-ss", String.valueOf(timeSeconds)));
            }
            cmd.addAll(List.of("-i", source.toString()));
            if (!fastSeek) {
                cmd.addAll(List.of("-ss", String.valueOf(timeSeconds)));
            }
            cmd.addAll(List.of("-frames:v", "1", "-qscale:v", "2"));
            if (width > 0) {
                cmd.addAll(List.of("-vf", "scale=" + width + ":-2"));
            }
            cmd.addAll(List.of("-f", "image2", outPng.toString()));
            ProcessUtils.Result r = ProcessUtils.run(cmd, Duration.ofSeconds(60));
            return r.exitCode == 0 && Files.exists(outPng);
        } catch (IOException | InterruptedException e) {
            LOG.warn("Frame extraction failed for " + source + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Extract a frame into a byte array (PNG). Used by the preview engine.
     */
    public byte[] extractFrameBytes(Path source, double timeSeconds, int width, boolean fastSeek) {
        Path tmp;
        try {
            tmp = Files.createTempFile(config.tempDir(), "frame-", ".png");
        } catch (IOException e) {
            return null;
        }
        if (!extractFrame(source, timeSeconds, width, tmp, fastSeek)) {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
            }
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(tmp);
            Files.deleteIfExists(tmp);
            return bytes;
        } catch (IOException e) {
            return null;
        }
    }

    // ---------- encoders ----------

    private Set<String> cachedEncoders;

    /** Names of video encoders ffmpeg reports as available. */
    public Set<String> availableVideoEncoders() {
        if (cachedEncoders != null) {
            return cachedEncoders;
        }
        Set<String> out = new LinkedHashSet<>();
        try {
            ProcessUtils.Result r = ProcessUtils.run(
                    List.of(config.ffmpeg(), "-hide_banner", "-encoders"), Duration.ofSeconds(20));
            if (r.exitCode == 0) {
                for (String line : r.stdout.split("\n")) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("Encoders")) {
                        continue;
                    }
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        out.add(parts[1]);
                    }
                }
            }
        } catch (IOException | InterruptedException ignored) {
        }
        cachedEncoders = out;
        return out;
    }

    public boolean hasEncoder(String name) {
        return availableVideoEncoders().contains(name);
    }

    // ---------- misc ----------

    public String ffmpegPath() { return config.ffmpeg(); }
    public String ffprobePath() { return config.ffprobe(); }

    public boolean testExtract(Path outPng) {
        try {
            Path tmp = Files.createTempFile("vforge-test", ".png");
            Files.deleteIfExists(tmp);
            List<String> cmd = List.of(config.ffmpeg(), "-y", "-hide_banner",
                    "-f", "lavfi", "-i", "color=c=black:s=64x64:d=0.1",
                    "-frames:v", "1", tmp.toString());
            ProcessUtils.Result r = ProcessUtils.run(cmd, Duration.ofSeconds(20));
            return r.exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
