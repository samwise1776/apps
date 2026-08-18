package utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Coordinates application discovery, information, logging, and version updates. */
public final class Manager {
    private static final long CHECK_INTERVAL_MS = 2_000;

    private final Registry registry;
    private final Info info;
    private final Logger logger;
    private final Updater updater;

    public Manager(Path dataDirectory) throws IOException {
        logger = new Logger(dataDirectory);
        registry = new Registry(dataDirectory);
        info = new Info(logger);
        updater = new Updater(dataDirectory, logger);
    }

    public static void main(String[] args) {
        Path dataDirectory = args.length == 0
                ? Path.of(System.getProperty("user.home"), "Data")
                : Path.of(args[0]).toAbsolutePath().normalize();
        try {
            new Manager(dataDirectory).run();
        } catch (IOException exception) {
            System.err.println("Could not start Datacenter Manager: " + exception.getMessage());
            System.exit(1);
        }
    }

    public void run() {
        logger.info("Datacenter Manager started. One version step = 100 completed lines.");
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> logger.info("Datacenter Manager stopped."), "manager-shutdown"));

        while (!Thread.currentThread().isInterrupted()) {
            try {
                scan();
                Thread.sleep(CHECK_INTERVAL_MS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (IOException | RuntimeException exception) {
                logger.error("Manager scan failed", exception);
                sleepAfterFailure();
            }
        }
    }

    public void scan() throws IOException {
        List<Path> applications = registry.refresh();
        for (Path application : applications) {
            Info.AppDetails details = info.read(application);
            updater.update(details);
        }
        updater.removeStaleMetadata(applications);
    }

    private void sleepAfterFailure() {
        try {
            Thread.sleep(CHECK_INTERVAL_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
