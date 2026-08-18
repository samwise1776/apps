package videoforge.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Process helpers built around {@link ProcessBuilder} with argument lists.
 *
 * <p>Security rule: filenames and user strings are ALWAYS passed as separate list
 * arguments, never concatenated into a shell command line. Shells are never used.</p>
 */
public final class ProcessUtils {

    private ProcessUtils() {}

    /** Result of running a process: exit code plus combined captured output. */
    public static final class Result {
        public final int exitCode;
        public final String stdout;
        public final String stderr;

        Result(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public String all() {
            return stdout + "\n" + stderr;
        }
    }

    /** Run a command and capture output, waiting for it to finish (with timeout). */
    public static Result run(List<String> command, Duration timeout) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);
        Process p = pb.start();

        StringBuilder out = new StringBuilder();
        StringBuilder err = new StringBuilder();
        Thread outThread = drain(p.getInputStream(), out);
        Thread errThread = drain(p.getErrorStream(), err);

        boolean finished = p.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        outThread.join(2000);
        errThread.join(2000);
        if (!finished) {
            p.destroyForcibly();
            throw new IOException("Process timed out after " + timeout.getSeconds() + "s: " + command.get(0));
        }
        return new Result(p.exitValue(), out.toString(), err.toString());
    }

    /** Run and return the merged output regardless of exit code. */
    public static Result runMerged(List<String> command, Duration timeout) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder merged = new StringBuilder();
        Thread t = drain(p.getInputStream(), merged);
        boolean finished = p.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        t.join(2000);
        if (!finished) {
            p.destroyForcibly();
            throw new IOException("Process timed out after " + timeout.getSeconds() + "s");
        }
        return new Result(p.exitValue(), merged.toString(), "");
    }

    private static Thread drain(java.io.InputStream in, StringBuilder sink) {
        Thread t = new Thread(() -> {
            byte[] buf = new byte[8192];
            try (var is = in) {
                int n;
                while ((n = is.read(buf)) != -1) {
                    sink.append(new String(buf, 0, n, StandardCharsets.UTF_8));
                }
            } catch (IOException ignored) {
            }
        }, "process-drain");
        t.setDaemon(true);
        t.start();
        return t;
    }

    /** Flatten a command list into a single debug string. */
    public static String commandLine(List<String> command) {
        return String.join(" ", command);
    }

    public static List<String> buildList(Object... parts) {
        List<String> out = new ArrayList<>();
        for (Object part : parts) {
            out.add(String.valueOf(part));
        }
        return out;
    }
}
