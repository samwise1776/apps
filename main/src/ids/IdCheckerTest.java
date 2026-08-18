package ids;

import security.perm.Checker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/** End-to-end identity-socket and permission-checker integration test. */
public final class IdCheckerTest {
    private IdCheckerTest() { }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("datacenter-id-checker-test-").toRealPath();
        Path idDirectory = root.resolve("ids/DataDocs");
        Path dataDirectory = root.resolve("data/datadocs");
        Path config = root.resolve(".data/unfinished/security/perm/permissions.properties");
        Files.createDirectories(idDirectory);
        Files.createDirectories(dataDirectory);
        Files.createDirectories(config.getParent());
        Files.writeString(idDirectory.resolve(".id.txt"), "private-test-id", StandardCharsets.UTF_8);
        Files.writeString(config, """
                format.version=1
                app.datadocs.status=ACTIVE
                app.datadocs.level=1
                app.datadocs.scopes=data/datadocs
                """, StandardCharsets.UTF_8);

        IdSocketClient.Token token;
        Checker checker = Checker.load(root, config);
        try (IdSocketServer server = new IdSocketServer(root, Duration.ofSeconds(30))) {
            Thread thread = new Thread(() -> {
                try { server.serve(); } catch (IOException exception) {
                    if (!Thread.currentThread().isInterrupted()) throw new RuntimeException(exception);
                }
            }, "id-checker-test-server");
            thread.start();

            IdSocketClient client = new IdSocketClient(root);
            token = client.requestToken("DataDocs");
            Checker.Decision allowed = checker.checkToken(token.value(),
                    Checker.Capability.FILE_READ_TEXT, dataDirectory.resolve("note.txt"));
            expect(allowed.allowed(), "valid socket token and scope must be allowed");

            Checker.Decision capabilityDenied = checker.checkToken(token.value(),
                    Checker.Capability.FILE_WRITE, dataDirectory.resolve("note.txt"));
            expect(capabilityDenied.reason() == Checker.Reason.CAPABILITY_DENIED,
                    "socket identity must still enforce permission levels");

            Checker.Decision invalid = checker.checkToken("not-a-real-token",
                    Checker.Capability.FILE_READ_TEXT, dataDirectory.resolve("note.txt"));
            expect(!invalid.allowed(), "invalid socket token must fail closed");
            expect(invalid.reason() == Checker.Reason.IDENTITY_TOKEN_INVALID,
                    "invalid socket token must be denied");
        }

        Checker.Decision offline = checker.checkToken(token.value(),
                Checker.Capability.FILE_READ_TEXT, dataDirectory.resolve("note.txt"));
        expect(!offline.allowed(), "offline ID service must fail closed");
        expect(offline.reason() == Checker.Reason.IDENTITY_SERVICE_UNAVAILABLE,
                "checker must fail closed when ID service is offline");
        System.out.println("ID/checker integration tests passed: 6");
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
