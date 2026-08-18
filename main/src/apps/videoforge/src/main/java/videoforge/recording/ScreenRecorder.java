package videoforge.recording;

import videoforge.logging.AppLog;
import videoforge.utils.ProcessUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen / microphone / webcam recorder built on FFmpeg. Uses the x11grab
 * input for the desktop, the pulse input for the microphone and a v4l2 input
 * for an optional webcam picture-in-picture overlay. Everything is started and
 * stopped as a normal FFmpeg process; the UI polls elapsed time and file size.
 */
public final class ScreenRecorder {

    private static final AppLog LOG = AppLog.get("recorder");

    private final String ffmpeg;
    private final Path out;
    private final Process process;
    private final long startTime;
    private volatile boolean stopped;

    private ScreenRecorder(String ffmpeg, Path out, Process process) {
        this.ffmpeg = ffmpeg;
        this.out = out;
        this.process = process;
        this.startTime = System.nanoTime();
    }

    /**
     * @param out        output mp4 path
     * @param width      capture width
     * @param height     capture height
     * @param fps        frames per second
     * @param audio      "none", "mic" (pulse default) or "system" (monitor)
     * @param webcam     true to overlay the first v4l2 device
     */
    public static ScreenRecorder start(Path out, int width, int height, double fps,
                                       String audio, boolean webcam) throws IOException {
        String ffmpegBin = videoforge.config.AppConfig.get().ffmpeg();
        if (ffmpegBin == null || ffmpegBin.isBlank()) {
            throw new IOException("FFmpeg not configured");
        }
        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegBin);
        cmd.add("-y");
        cmd.add("-hide_banner");
        cmd.add("-loglevel");
        cmd.add("error");
        cmd.add("-nostdin");

        String display = System.getenv("DISPLAY") == null ? ":0.0" : System.getenv("DISPLAY");
        cmd.add("-f");
        cmd.add("x11grab");
        cmd.add("-framerate");
        cmd.add(String.valueOf(fps));
        cmd.add("-video_size");
        cmd.add(width + "x" + height);
        cmd.add("-i");
        cmd.add(display);

        boolean hasAudio = !"none".equalsIgnoreCase(audio);
        if (hasAudio) {
            cmd.add("-f");
            cmd.add("pulse");
            cmd.add("-ac");
            cmd.add("2");
            if ("system".equalsIgnoreCase(audio)) {
                cmd.add("-i");
                cmd.add("monitor");
            } else {
                cmd.add("-i");
                cmd.add("default");
            }
        }

        boolean hasWebcam = webcam && Files.exists(Path.of("/dev/video0"));
        if (hasWebcam) {
            cmd.add("-f");
            cmd.add("v4l2");
            cmd.add("-video_size");
            cmd.add("320x240");
            cmd.add("-i");
            cmd.add("/dev/video0");
        }

        if (hasWebcam) {
            cmd.add("-filter_complex");
            cmd.add("[2:v]scale=320:-1[wc];[0:v][wc]overlay=x=main_w-overlay_w-16:y=16[vout]");
            cmd.add("-map");
            cmd.add("[vout]");
        } else {
            cmd.add("-map");
            cmd.add("0:v");
        }
        if (hasAudio) {
            cmd.add("-map");
            cmd.add("1:a");
        }

        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-preset");
        cmd.add("ultrafast");
        cmd.add("-crf");
        cmd.add("20");
        cmd.add("-pix_fmt");
        cmd.add("yuv420p");
        if (hasAudio) {
            cmd.add("-c:a");
            cmd.add("aac");
        }
        cmd.add("-movflags");
        cmd.add("+faststart");
        cmd.add(out.toString());

        LOG.raw("Recording: " + ProcessUtils.commandLine(cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);
        Process p = pb.start();
        return new ScreenRecorder(ffmpegBin, out, p);
    }

    public Path outputPath() {
        return out;
    }

    public double elapsedSeconds() {
        return (System.nanoTime() - startTime) / 1e9;
    }

    public long fileSize() {
        try {
            return Files.exists(out) ? Files.size(out) : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    public boolean isRunning() {
        return !stopped && process.isAlive();
    }

    /** Stop and finalize the recording. Returns the output file. */
    public Path stop() {
        stopped = true;
        if (process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(8, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
        LOG.info("Recording stopped: " + out);
        return out;
    }

    public void cancel() {
        stopped = true;
        process.destroyForcibly();
        try {
            Files.deleteIfExists(out);
        } catch (IOException ignored) {
        }
    }
}
