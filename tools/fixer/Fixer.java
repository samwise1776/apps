import utils.Manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Repairs the Datacenter project's known, safely recoverable problems.
 * Compiler errors that require changing application logic are reported for manual repair.
 */
public final class Fixer {
    private final Path dataDirectory;
    private final List<String> unresolved = new ArrayList<>();
    private int repairs;

    public Fixer(Path dataDirectory) {
        this.dataDirectory = dataDirectory.toAbsolutePath().normalize();
    }

    public static void main(String[] args) {
        Path dataDirectory = args.length == 0
                ? Path.of(System.getProperty("user.home"), "Data")
                : Path.of(args[0]);
        int status = new Fixer(dataDirectory).fixEverything();
        if (status != 0) System.exit(status);
    }

    public int fixEverything() {
        System.out.println("Datacenter Fixer started for " + dataDirectory);
        createRequiredDirectories();
        repairScriptPermissions();
        regenerateRegistryAndVersions();
        rebuildJava();
        rebuildDotNet();
        verifyArchives();
        runFinalChecker();

        System.out.println();
        System.out.println("Repairs completed: " + repairs);
        if (unresolved.isEmpty()) {
            System.out.println("No unresolved errors remain.");
            return 0;
        }

        System.err.println("Unresolved errors: " + unresolved.size());
        for (String error : unresolved) System.err.println("- " + error);
        return 1;
    }

    private void createRequiredDirectories() {
        for (String name : List.of("apps", "build", "docs", ".data/logs", ".data/unfinished",
                "ids", "main", "runtime", "scripts", "utils", "versions")) {
            Path directory = dataDirectory.resolve(name);
            try {
                if (Files.notExists(directory)) {
                    Files.createDirectories(directory);
                    repaired("Created missing directory: " + name);
                }
            } catch (IOException exception) {
                unresolved("Could not create " + directory + ": " + exception.getMessage());
            }
        }
    }

    private void repairScriptPermissions() {
        try (var files = Files.walk(dataDirectory, 3)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".sh"))
                    .forEach(path -> {
                        if (!Files.isExecutable(path)) {
                            if (path.toFile().setExecutable(true, false)) {
                                repaired("Made executable: " + dataDirectory.relativize(path));
                            } else {
                                unresolved("Could not make executable: " + path);
                            }
                        }
                    });
        } catch (IOException exception) {
            unresolved("Could not inspect scripts: " + exception.getMessage());
        }
    }

    private void regenerateRegistryAndVersions() {
        try {
            new Manager(dataDirectory).scan();
            repaired("Regenerated app registry and version metadata");
        } catch (IOException | RuntimeException exception) {
            unresolved("Registry/version repair failed: " + exception.getMessage());
        }
    }

    private void rebuildJava() {
        Path script = dataDirectory.resolve("scripts/build.sh");
        if (!Files.isRegularFile(script)) {
            unresolved("Missing build.sh");
            return;
        }
        if (run("Java rebuild", script.toString()) == 0) repairs++;
    }

    private void rebuildDotNet() {
        Path project = dataDirectory.resolve("apps/2/apps.csproj");
        if (!Files.isRegularFile(project)) {
            unresolved("Missing C# project: " + project);
            return;
        }
        if (run(".NET restore", "dotnet", "restore", project.toString(), "--nologo") != 0) return;
        if (run(".NET build", "dotnet", "build", project.toString(), "--no-restore", "--nologo") == 0) {
            repairs++;
        }
    }

    private void verifyArchives() {
        try (var files = Files.walk(dataDirectory, 3)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".zip"))
                    .filter(path -> !path.startsWith(dataDirectory.resolve(".data/unfinished")))
                    .forEach(path -> run("ZIP check: " + path.getFileName(),
                            "zip", "-T", path.toString()));
        } catch (IOException exception) {
            unresolved("Could not inspect ZIP archives: " + exception.getMessage());
        }
    }

    private void runFinalChecker() {
        Path checker = dataDirectory.resolve("scripts/checker.sh");
        if (!Files.isRegularFile(checker)) checker = dataDirectory.resolve("utils/checker.sh");
        if (!Files.isRegularFile(checker)) {
            unresolved("Missing checker.sh");
            return;
        }
        run("Final project check", checker.toString());
    }

    private int run(String name, String... command) {
        System.out.println();
        System.out.println("-- " + name + " --");
        try {
            Process process = new ProcessBuilder(command)
                    .directory(dataDirectory.toFile())
                    .inheritIO()
                    .start();
            int status = process.waitFor();
            if (status != 0) unresolved(name + " exited with status " + status);
            return status;
        } catch (IOException exception) {
            unresolved(name + " could not start: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            unresolved(name + " was interrupted");
        }
        return 1;
    }

    private void repaired(String message) {
        repairs++;
        System.out.println("[FIXED] " + message);
    }

    private void unresolved(String message) {
        unresolved.add(message);
        System.err.println("[ERROR] " + message);
    }
}
