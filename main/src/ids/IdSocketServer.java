package ids;

import jdk.net.ExtendedSocketOptions;
import jdk.net.UnixDomainPrincipal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Owner-only Unix-domain-socket broker for Datacenter application identities.
 *
 * <p>Raw values from {@code ids/APP/.id.txt} never leave this process. Clients
 * receive random, short-lived session tokens. A trusted permission checker can
 * exchange a valid token for the normalized application key, but never for the
 * raw stored ID.</p>
 */
public final class IdSocketServer implements AutoCloseable {
    public static final String SOCKET_NAME = ".id-broker.sock";
    private static final int MAX_REQUEST_BYTES = 4_096;
    private static final int TOKEN_BYTES = 32;
    private static final Duration DEFAULT_TOKEN_LIFETIME = Duration.ofMinutes(2);
    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS =
            PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> FILE_PERMISSIONS =
            PosixFilePermissions.fromString("rw-------");

    private final Path dataRoot;
    private final Path idsDirectory;
    private final Path socketPath;
    private final Path auditLog;
    private final Duration tokenLifetime;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Identity> identities;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final ExecutorService clients = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "datacenter-id-client");
        thread.setDaemon(true);
        return thread;
    });
    private final ServerSocketChannel server;
    private volatile boolean running;

    public IdSocketServer(Path dataRoot) throws IOException {
        this(dataRoot, DEFAULT_TOKEN_LIFETIME);
    }

    public IdSocketServer(Path dataRoot, Duration tokenLifetime) throws IOException {
        this.dataRoot = Objects.requireNonNull(dataRoot, "dataRoot").toAbsolutePath().normalize();
        if (!Files.isDirectory(this.dataRoot)) {
            throw new IOException("Datacenter root is not a directory: " + this.dataRoot);
        }
        this.idsDirectory = this.dataRoot.resolve("ids");
        this.socketPath = idsDirectory.resolve(SOCKET_NAME);
        this.auditLog = this.dataRoot.resolve(".data/logs/id-socket.log");
        this.tokenLifetime = Objects.requireNonNull(tokenLifetime, "tokenLifetime");
        if (tokenLifetime.isZero() || tokenLifetime.isNegative()
                || tokenLifetime.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("Token lifetime must be between 1 ns and 1 hour");
        }

        secureDirectory(idsDirectory);
        this.identities = Map.copyOf(loadIdentities(idsDirectory, random));
        prepareSocketPath(socketPath);
        this.server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        try {
            server.bind(UnixDomainSocketAddress.of(socketPath));
            secureFile(socketPath);
        } catch (IOException exception) {
            try { server.close(); } catch (IOException ignored) { }
            throw exception;
        }
    }

    public Path socketPath() { return socketPath; }
    public int identityCount() { return identities.size(); }

    /** Blocks while accepting local socket clients. */
    public void serve() throws IOException {
        if (running) throw new IllegalStateException("ID socket server is already running");
        running = true;
        audit("SERVER_STARTED", "system", "ALLOWED", "identities=" + identities.size());
        try {
            while (running) {
                SocketChannel channel;
                try {
                    channel = server.accept();
                } catch (IOException exception) {
                    if (!running) break;
                    throw exception;
                }
                clients.execute(() -> handle(channel));
            }
        } finally {
            running = false;
        }
    }

    private void handle(SocketChannel channel) {
        String peer = "unknown";
        try (channel) {
            peer = peerUser(channel);
            String request = readLineLimited(channel);
            String response = process(request, peer);
            writeLine(channel, response);
        } catch (IOException | RuntimeException exception) {
            audit("CLIENT_ERROR", "unknown", "DENIED", "peer=" + safe(peer));
        }
    }

    private String process(String request, String peer) {
        purgeExpired();
        if (request == null || request.isBlank()) return error("INVALID_REQUEST");
        String[] parts = request.split("\\t", -1);
        String command = parts[0].toUpperCase(Locale.ROOT);

        switch (command) {
            case "PING":
                if (parts.length != 1) return error("INVALID_REQUEST");
                return "OK\tPONG";
            case "TOKEN":
                if (parts.length != 2) return error("INVALID_REQUEST");
                return issueToken(parts[1], peer);
            case "IDENTITY":
                if (parts.length != 2) return error("INVALID_REQUEST");
                return resolveToken(parts[1], peer);
            case "REVOKE":
                if (parts.length != 2) return error("INVALID_REQUEST");
                return revokeToken(parts[1], peer);
            default:
                return error("UNKNOWN_COMMAND");
        }
    }

    private String issueToken(String requestedApplication, String peer) {
        String key = normalizeApplicationKey(requestedApplication);
        Identity identity = key == null ? null : identities.get(key);
        if (identity == null) {
            audit("TOKEN_ISSUE", key == null ? "unknown" : key, "DENIED", "peer=" + safe(peer));
            return error("IDENTITY_UNKNOWN");
        }

        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expires = Instant.now().plus(tokenLifetime);
        sessions.put(token, new Session(identity.applicationKey, identity.idDigest, peer, expires));
        audit("TOKEN_ISSUE", identity.applicationKey, "ALLOWED", "peer=" + safe(peer));
        return "OK\t" + token + "\t" + expires.getEpochSecond();
    }

    private String resolveToken(String token, String peer) {
        Session session = sessions.get(token);
        if (session == null || session.expires.isBefore(Instant.now())) {
            if (session != null) sessions.remove(token);
            audit("TOKEN_RESOLVE", "unknown", "DENIED", "reason=TOKEN_INVALID");
            return error("TOKEN_INVALID");
        }
        if (!MessageDigest.isEqual(session.peerUser.getBytes(StandardCharsets.UTF_8),
                peer.getBytes(StandardCharsets.UTF_8))) {
            audit("TOKEN_RESOLVE", session.applicationKey, "DENIED", "reason=PEER_MISMATCH");
            return error("PEER_MISMATCH");
        }

        Identity current = identities.get(session.applicationKey);
        if (current == null || !MessageDigest.isEqual(current.idDigest, session.idDigest)) {
            sessions.remove(token);
            audit("TOKEN_RESOLVE", session.applicationKey, "DENIED", "reason=IDENTITY_CHANGED");
            return error("IDENTITY_CHANGED");
        }
        audit("TOKEN_RESOLVE", session.applicationKey, "ALLOWED", "peer=" + safe(peer));
        return "OK\t" + session.applicationKey;
    }

    private String revokeToken(String token, String peer) {
        Session session = sessions.get(token);
        if (session == null) return error("TOKEN_INVALID");
        if (!MessageDigest.isEqual(session.peerUser.getBytes(StandardCharsets.UTF_8),
                peer.getBytes(StandardCharsets.UTF_8))) {
            return error("PEER_MISMATCH");
        }
        sessions.remove(token);
        audit("TOKEN_REVOKE", session.applicationKey, "ALLOWED", "peer=" + safe(peer));
        return "OK\tREVOKED";
    }

    private static Map<String, Identity> loadIdentities(Path idsDirectory, SecureRandom random)
            throws IOException {
        Map<String, Identity> result = new HashMap<>();
        Set<String> uniqueIds = new HashSet<>();
        try (var entries = Files.list(idsDirectory)) {
            for (Path directory : entries.filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                    .sorted().toList()) {
                String key = normalizeApplicationKey(directory.getFileName().toString());
                if (key == null) throw new IOException("Invalid application ID directory: " + directory);
                secureDirectory(directory);
                Path idFile = directory.resolve(".id.txt");
                if (Files.notExists(idFile, LinkOption.NOFOLLOW_LINKS)) {
                    createPrivateId(idFile, random);
                }
                if (!Files.isRegularFile(idFile, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("Missing regular ID file: " + idFile);
                }
                secureFile(idFile);
                String rawId = Files.readString(idFile, StandardCharsets.UTF_8).strip();
                validateRawId(rawId, idFile);
                String idHash = Base64.getEncoder().encodeToString(digest(rawId));
                if (!uniqueIds.add(idHash)) throw new IOException("Duplicate application ID detected");
                result.put(key, new Identity(key, digest(rawId)));
            }
        }
        if (result.isEmpty()) throw new IOException("No application IDs found in " + idsDirectory);
        return result;
    }

    private static void createPrivateId(Path idFile, SecureRandom random) throws IOException {
        byte[] generated = new byte[TOKEN_BYTES];
        random.nextBytes(generated);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(generated);
        try {
            try {
                Files.createFile(idFile,
                        PosixFilePermissions.asFileAttribute(FILE_PERMISSIONS));
            } catch (UnsupportedOperationException exception) {
                Files.createFile(idFile);
                secureFile(idFile);
            }
            Files.writeString(idFile, value, StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(idFile);
            throw exception;
        }
    }

    private static void validateRawId(String id, Path source) throws IOException {
        if (id.length() < 4 || id.length() > 256) throw new IOException("Invalid ID length: " + source);
        for (int index = 0; index < id.length(); index++) {
            if (Character.isISOControl(id.charAt(index)) || Character.isWhitespace(id.charAt(index))) {
                throw new IOException("ID contains whitespace or control characters: " + source);
            }
        }
    }

    private static byte[] digest(String value) throws IOException {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-256 is unavailable", impossible);
        }
    }

    private static String peerUser(SocketChannel channel) throws IOException {
        UnixDomainPrincipal principal = channel.getOption(ExtendedSocketOptions.SO_PEERCRED);
        if (principal == null || principal.user() == null) throw new IOException("Peer identity unavailable");
        return principal.user().getName();
    }

    private static String readLineLimited(SocketChannel channel) throws IOException {
        BufferedInputStream input = new BufferedInputStream(Channels.newInputStream(channel));
        byte[] bytes = new byte[MAX_REQUEST_BYTES];
        int length = 0;
        while (length < bytes.length) {
            int next = input.read();
            if (next < 0 || next == '\n') break;
            if (next == '\r') continue;
            bytes[length++] = (byte) next;
        }
        if (length == bytes.length) throw new IOException("Socket request is too large");
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }

    private static void writeLine(SocketChannel channel, String value) throws IOException {
        BufferedOutputStream output = new BufferedOutputStream(Channels.newOutputStream(channel));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.write('\n');
        output.flush();
    }

    private static void prepareSocketPath(Path socketPath) throws IOException {
        if (!Files.exists(socketPath, LinkOption.NOFOLLOW_LINKS)) return;
        try (SocketChannel probe = SocketChannel.open(StandardProtocolFamily.UNIX)) {
            probe.connect(UnixDomainSocketAddress.of(socketPath));
            throw new IOException("An ID socket server is already running: " + socketPath);
        } catch (IOException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("An ID socket")) {
                throw exception;
            }
            Files.delete(socketPath);
        }
    }

    private static void secureDirectory(Path directory) throws IOException {
        Files.createDirectories(directory);
        try {
            Files.setPosixFilePermissions(directory, DIRECTORY_PERMISSIONS);
        } catch (UnsupportedOperationException exception) {
            if (!directory.toFile().setReadable(false, false)
                    || !directory.toFile().setReadable(true, true)
                    || !directory.toFile().setWritable(false, false)
                    || !directory.toFile().setWritable(true, true)
                    || !directory.toFile().setExecutable(false, false)
                    || !directory.toFile().setExecutable(true, true)) {
                throw new IOException("Could not secure directory permissions: " + directory);
            }
        }
    }

    private static void secureFile(Path file) throws IOException {
        try {
            Files.setPosixFilePermissions(file, FILE_PERMISSIONS);
        } catch (UnsupportedOperationException exception) {
            if (!file.toFile().setReadable(false, false)
                    || !file.toFile().setReadable(true, true)
                    || !file.toFile().setWritable(false, false)
                    || !file.toFile().setWritable(true, true)) {
                throw new IOException("Could not secure file permissions: " + file);
            }
        }
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> entry.getValue().expires.isBefore(now));
    }

    private synchronized void audit(String event, String application, String result, String details) {
        String line = "timestamp=" + Instant.now()
                + " event=" + safe(event)
                + " app=" + safe(application)
                + " result=" + safe(result)
                + " " + details.replaceAll("[\\r\\n]", "_")
                + System.lineSeparator();
        try {
            Files.createDirectories(auditLog.getParent());
            Files.writeString(auditLog, line, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Authentication does not expose IDs when audit storage is unavailable.
        }
    }

    private static String safe(String value) {
        if (value == null) return "unknown";
        return value.replaceAll("[^A-Za-z0-9._=@:-]", "_");
    }

    private static String normalizeApplicationKey(String value) {
        if (value == null) return null;
        String key = value.strip().toLowerCase(Locale.ROOT);
        return key.matches("[a-z0-9][a-z0-9._-]{0,63}") ? key : null;
    }

    private static String error(String code) { return "ERR\t" + code; }

    @Override
    public void close() throws IOException {
        running = false;
        sessions.clear();
        clients.shutdownNow();
        try {
            server.close();
        } finally {
            Files.deleteIfExists(socketPath);
            audit("SERVER_STOPPED", "system", "ALLOWED", "sessions=0");
        }
    }

    @SuppressWarnings("try")
    public static void main(String[] args) {
        Path root = Path.of(System.getProperty("user.home"), "Data");
        for (int index = 0; index < args.length; index++) {
            if ("--root".equals(args[index]) && index + 1 < args.length) {
                root = Path.of(args[++index]);
            } else {
                System.err.println("Usage: java ids.IdSocketServer [--root PATH]");
                System.exit(64);
            }
        }

        try (IdSocketServer broker = new IdSocketServer(root)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { broker.close(); } catch (IOException ignored) { }
            }, "datacenter-id-shutdown"));
            System.out.println("Datacenter ID socket ready: " + broker.socketPath());
            System.out.println("Loaded identities: " + broker.identityCount());
            broker.serve();
        } catch (IOException exception) {
            System.err.println("ID socket server failed: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static final class Identity {
        final String applicationKey;
        final byte[] idDigest;
        Identity(String applicationKey, byte[] idDigest) {
            this.applicationKey = applicationKey;
            this.idDigest = idDigest.clone();
        }
    }

    private static final class Session {
        final String applicationKey;
        final byte[] idDigest;
        final String peerUser;
        final Instant expires;
        Session(String applicationKey, byte[] idDigest, String peerUser, Instant expires) {
            this.applicationKey = applicationKey;
            this.idDigest = idDigest.clone();
            this.peerUser = peerUser;
            this.expires = expires;
        }
    }
}
