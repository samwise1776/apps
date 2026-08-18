package ids;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;

/** Dependency-free regression tests for the ID socket broker. */
public final class IdSocketTest {
    private IdSocketTest() { }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("datacenter-id-test-").toRealPath();
        createId(root, "DataDocs", "private-doc-id");
        createId(root, "DataAnalytics", "private-analytics-id");
        Path generatedId = root.resolve("ids/AppForge/.id.txt");
        Files.createDirectories(generatedId.getParent());

        try (IdSocketServer server = new IdSocketServer(root, Duration.ofSeconds(30))) {
            Thread thread = new Thread(() -> {
                try { server.serve(); } catch (IOException exception) {
                    if (!Thread.currentThread().isInterrupted()) throw new RuntimeException(exception);
                }
            }, "id-socket-test-server");
            thread.start();

            IdSocketClient client = new IdSocketClient(root);
            expect(client.ping(), "broker must answer ping");
            IdSocketClient.Token token = client.requestToken("DataDocs");
            expect(!token.expired(), "new token must be active");
            expect(!token.value().contains("private-doc-id"), "token must not reveal raw ID");
            expect("datadocs".equals(client.resolveApplication(token.value())),
                    "token must resolve to normalized application key");
            client.revoke(token.value());
            expectDenied(() -> client.resolveApplication(token.value()), "revoked token must fail");
            expectDenied(() -> client.requestToken("missing"), "unknown application must fail");
            expect(ownerOnly(root.resolve("ids/DataDocs/.id.txt")), "ID file must be owner-only");
            expect(ownerOnly(root.resolve("ids")), "ID directory must be owner-only");
            expect(Files.isRegularFile(generatedId), "broker must provision a missing private ID");
            expect(ownerOnly(generatedId), "generated ID file must be owner-only");
        }

        System.out.println("ID socket tests passed: 10");
    }

    private static void createId(Path root, String application, String id) throws IOException {
        Path directory = root.resolve("ids").resolve(application);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(".id.txt"), id, StandardCharsets.UTF_8);
    }

    private static boolean ownerOnly(Path path) throws IOException {
        try {
            return Files.getPosixFilePermissions(path).equals(
                    Files.isDirectory(path)
                            ? PosixFilePermissions.fromString("rwx------")
                            : PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException exception) {
            return true;
        }
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectDenied(CheckedRunnable action, String message) throws Exception {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IOException expected) {
            // Expected denial.
        }
    }

    @FunctionalInterface
    private interface CheckedRunnable { void run() throws Exception; }
}
