package security.perm;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free security regression tests. Run with assertions enabled. */
public final class CheckerTest {
    private CheckerTest() {}

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("datacenter-checker-test-").toRealPath();
        Path config = root.resolve(".data/unfinished/security/perm/permissions.properties");
        Path reader = root.resolve("data/reader");
        Path editor = root.resolve("data/editor");
        Files.createDirectories(config.getParent());
        Files.createDirectories(reader);
        Files.createDirectories(editor);
        Files.writeString(reader.resolve("note.txt"), "hello", StandardCharsets.UTF_8);
        Files.writeString(config, """
                format.version=1
                app.reader.status=ACTIVE
                app.reader.level=1
                app.reader.scopes=data/reader
                app.editor.status=ACTIVE
                app.editor.level=3
                app.editor.scopes=data/editor
                app.editor.deny=FILE_DELETE
                app.admin.status=ACTIVE
                app.admin.level=4
                app.admin.scopes=*
                """, StandardCharsets.UTF_8);

        Checker checker = Checker.load(root, config);
        expect(checker.check("reader", Checker.Capability.FILE_READ_TEXT,
                reader.resolve("note.txt")), true, Checker.Reason.ALLOWED);
        expect(checker.check("reader", Checker.Capability.FILE_WRITE,
                reader.resolve("note.txt")), false, Checker.Reason.CAPABILITY_DENIED);
        expect(checker.check("unknown", Checker.Capability.FILE_READ_TEXT,
                reader.resolve("note.txt")), false, Checker.Reason.IDENTITY_UNKNOWN);
        expect(checker.check("reader", Checker.Capability.FILE_READ_TEXT,
                editor.resolve("private.txt")), false, Checker.Reason.SCOPE_DENIED);
        expect(checker.check("editor", Checker.Capability.FILE_WRITE,
                editor.resolve("new.txt")), true, Checker.Reason.ALLOWED);
        expect(checker.check("editor", Checker.Capability.FILE_DELETE,
                editor.resolve("new.txt")), false, Checker.Reason.EXPLICIT_DENY);
        expect(checker.check("editor", Checker.Capability.FILE_WRITE,
                root.resolve("security/policy.txt")), false, Checker.Reason.SCOPE_DENIED);
        expect(checker.check("admin", Checker.Capability.SECURITY_POLICY_WRITE,
                root.resolve("security/policy.txt")), true, Checker.Reason.ALLOWED);
        expect(checker.check("reader", Checker.Capability.FILE_READ_TEXT,
                reader.resolve("../editor/private.txt")), false, Checker.Reason.SCOPE_DENIED);

        System.out.println("Checker tests passed: 9");
    }

    private static void expect(Checker.Decision decision, boolean allowed,
            Checker.Reason reason) {
        if (decision.allowed() != allowed || decision.reason() != reason) {
            throw new AssertionError("Expected allowed=" + allowed + " reason=" + reason
                    + " but got " + decision);
        }
    }
}
