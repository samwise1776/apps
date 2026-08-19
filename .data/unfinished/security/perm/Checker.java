package security.perm;

import ids.IdSocketClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/**
 * Central permission-policy checker for Datacenter.
 *
 * <p>The checker defaults unknown applications to Level 0, combines permission
 * levels with explicit capabilities and path scopes, canonicalizes paths, and
 * protects security-critical Datacenter directories. It never performs the
 * requested file operation itself.</p>
 *
 * <p><strong>Trust boundary:</strong> the caller must authenticate the process
 * and supply its verified application ID. A string ID supplied by an
 * untrusted application is not proof of identity.</p>
 */
public final class Checker {
    public static final String DEFAULT_CONFIG =
            ".data/unfinished/security/perm/permissions.properties";
    public static final String DEFAULT_AUDIT_LOG = ".data/unfinished/logs/security.log";

    /** Operations understood by the policy engine. */
    public enum Capability {
        FILE_READ_TEXT(1, HostAccess.READ),
        FILE_READ_LINES(1, HostAccess.READ),
        FILE_SEARCH_TEXT(1, HostAccess.READ),
        FILE_COUNT_LINES(1, HostAccess.READ),

        FILE_METADATA_READ(2, HostAccess.READ),
        FILE_EXISTS(2, HostAccess.NONE),
        DIRECTORY_LIST(2, HostAccess.READ),
        DIRECTORY_COUNT(2, HostAccess.READ),

        FILE_READ_BINARY(3, HostAccess.READ),
        FILE_CREATE(3, HostAccess.CREATE),
        FILE_WRITE(3, HostAccess.WRITE),
        FILE_APPEND(3, HostAccess.WRITE),
        FILE_COPY(3, HostAccess.READ),
        FILE_MOVE(3, HostAccess.WRITE_PARENT),
        FILE_RENAME(3, HostAccess.WRITE_PARENT),
        DIRECTORY_CREATE(3, HostAccess.CREATE),
        APP_DATA_READ(3, HostAccess.READ),
        APP_DATA_WRITE(3, HostAccess.WRITE),
        STATE_READ(3, HostAccess.READ),
        STATE_WRITE(3, HostAccess.WRITE),
        LOG_APPEND(3, HostAccess.CREATE),
        LOG_READ_OWN(3, HostAccess.READ),

        FILE_DELETE(4, HostAccess.WRITE_PARENT),
        DIRECTORY_DELETE(4, HostAccess.WRITE_PARENT),
        FILE_EXECUTE(4, HostAccess.EXECUTE),
        REGISTRY_READ(4, HostAccess.READ),
        REGISTRY_MANAGE(4, HostAccess.WRITE),
        PERMISSION_GRANT(4, HostAccess.WRITE),
        PERMISSION_REVOKE(4, HostAccess.WRITE),
        SECURITY_POLICY_READ(4, HostAccess.READ),
        SECURITY_POLICY_WRITE(4, HostAccess.WRITE),
        LOG_READ_SECURITY(4, HostAccess.READ),
        LOG_ADMIN(4, HostAccess.WRITE),
        APPLICATION_MANAGE(4, HostAccess.WRITE),
        RUNTIME_MANAGE(4, HostAccess.WRITE),
        PROCESS_EXECUTE(4, HostAccess.EXECUTE),
        NETWORK_CONNECT(4, HostAccess.NONE),
        NETWORK_LISTEN(4, HostAccess.NONE),
        ENVIRONMENT_READ(4, HostAccess.NONE);

        private final int baselineLevel;
        private final HostAccess hostAccess;

        Capability(int baselineLevel, HostAccess hostAccess) {
            this.baselineLevel = baselineLevel;
            this.hostAccess = hostAccess;
        }

        public int baselineLevel() {
            return baselineLevel;
        }

        public static Capability parse(String value) {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
    }

    /** Stable reason codes that calling applications can safely handle. */
    public enum Reason {
        ALLOWED,
        IDENTITY_INVALID,
        IDENTITY_UNKNOWN,
        IDENTITY_TOKEN_INVALID,
        IDENTITY_SERVICE_UNAVAILABLE,
        APPLICATION_INACTIVE,
        CAPABILITY_DENIED,
        EXPLICIT_DENY,
        SCOPE_DENIED,
        PROTECTED_RESOURCE,
        RESOURCE_INVALID,
        HOST_ACCESS_DENIED,
        SECURITY_CONFIGURATION_INVALID
    }

    /** Application lifecycle state. Only ACTIVE applications may use grants. */
    public enum Status {
        PENDING, ACTIVE, SUSPENDED, DISABLED, REVOKED, INVALID;

        static Status parse(String value) {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
    }

    /** Immutable result of one authorization decision. */
    public static final class Decision {
        private final String requestId;
        private final String applicationId;
        private final Capability capability;
        private final Path resource;
        private final boolean allowed;
        private final Reason reason;
        private final String message;
        private final boolean auditRecorded;

        private Decision(String requestId, String applicationId, Capability capability,
                Path resource, boolean allowed, Reason reason, String message,
                boolean auditRecorded) {
            this.requestId = requestId;
            this.applicationId = applicationId;
            this.capability = capability;
            this.resource = resource;
            this.allowed = allowed;
            this.reason = reason;
            this.message = message;
            this.auditRecorded = auditRecorded;
        }

        public String requestId() { return requestId; }
        public String applicationId() { return applicationId; }
        public Capability capability() { return capability; }
        public Path resource() { return resource; }
        public boolean allowed() { return allowed; }
        public Reason reason() { return reason; }
        public String message() { return message; }
        public boolean auditRecorded() { return auditRecorded; }

        @Override
        public String toString() {
            return (allowed ? "ALLOWED" : "DENIED") + " [" + reason + "] " + message
                    + " (request " + requestId + ")";
        }
    }

    /** Immutable registered application grant. */
    public static final class Grant {
        private final String applicationId;
        private final int level;
        private final Status status;
        private final Set<Capability> capabilities;
        private final Set<Capability> deniedCapabilities;
        private final List<Path> scopes;

        private Grant(String applicationId, int level, Status status,
                Set<Capability> capabilities, Set<Capability> deniedCapabilities,
                List<Path> scopes) {
            this.applicationId = applicationId;
            this.level = level;
            this.status = status;
            this.capabilities = Collections.unmodifiableSet(EnumSet.copyOf(capabilities));
            this.deniedCapabilities = Collections.unmodifiableSet(EnumSet.copyOf(deniedCapabilities));
            this.scopes = Collections.unmodifiableList(new ArrayList<>(scopes));
        }

        public String applicationId() { return applicationId; }
        public int level() { return level; }
        public Status status() { return status; }
        public Set<Capability> capabilities() { return capabilities; }
        public Set<Capability> deniedCapabilities() { return deniedCapabilities; }
        public List<Path> scopes() { return scopes; }
    }

    private enum HostAccess {
        NONE, READ, WRITE, CREATE, WRITE_PARENT, EXECUTE
    }

    private final Path dataRoot;
    private final Path auditLog;
    private final Map<String, Grant> grants;
    private final List<Path> criticalRoots;

    private Checker(Path dataRoot, Path auditLog, Map<String, Grant> grants) throws IOException {
        this.dataRoot = canonicalExisting(Objects.requireNonNull(dataRoot, "dataRoot"));
        this.auditLog = Objects.requireNonNull(auditLog, "auditLog").toAbsolutePath().normalize();
        this.grants = Collections.unmodifiableMap(new LinkedHashMap<>(grants));
        this.criticalRoots = criticalRoots(this.dataRoot);
    }

    /** Loads {@value #DEFAULT_CONFIG} below the supplied Datacenter root. */
    public static Checker load(Path dataRoot) throws IOException {
        Path absoluteRoot = Objects.requireNonNull(dataRoot, "dataRoot").toAbsolutePath().normalize();
        return load(absoluteRoot, absoluteRoot.resolve(DEFAULT_CONFIG));
    }

    /** Loads a specific permission registry. A missing registry means no grants. */
    public static Checker load(Path dataRoot, Path configFile) throws IOException {
        Path absoluteRoot = canonicalExisting(Objects.requireNonNull(dataRoot, "dataRoot"));
        Path absoluteConfig = Objects.requireNonNull(configFile, "configFile").toAbsolutePath().normalize();
        Properties properties = new Properties();
        if (Files.exists(absoluteConfig, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isRegularFile(absoluteConfig, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Permission registry is not a regular file: " + absoluteConfig);
            }
            try (var reader = Files.newBufferedReader(absoluteConfig, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }
        Map<String, Grant> grants = parseGrants(absoluteRoot, properties);
        return new Checker(absoluteRoot, absoluteRoot.resolve(DEFAULT_AUDIT_LOG), grants);
    }

    public Path dataRoot() { return dataRoot; }
    public Map<String, Grant> grants() { return grants; }

    /**
     * Checks Datacenter policy only. This is the preferred check before asking
     * the host filesystem to perform an operation.
     */
    public Decision check(String applicationId, Capability capability, Path resource) {
        return decide(applicationId, capability, resource, false);
    }

    /**
     * Checks both Datacenter policy and the current host filesystem access bits.
     * Host checks are advisory and do not replace handling an IOException at the
     * time the operation is performed.
     */
    public Decision checkEffective(String applicationId, Capability capability, Path resource) {
        return decide(applicationId, capability, resource, true);
    }

    /**
     * Resolves a short-lived socket token and checks policy without accepting a
     * caller-supplied application ID.
     */
    public Decision checkToken(String sessionToken, Capability capability, Path resource) {
        return decideToken(sessionToken, capability, resource, false);
    }

    /** Resolves a socket token and checks both policy and host access. */
    public Decision checkTokenEffective(String sessionToken, Capability capability, Path resource) {
        return decideToken(sessionToken, capability, resource, true);
    }

    public boolean can(String applicationId, Capability capability, Path resource) {
        return check(applicationId, capability, resource).allowed();
    }

    public boolean canRead(String applicationId, Path resource) {
        return can(applicationId, Capability.FILE_READ_TEXT, resource);
    }

    public boolean canWrite(String applicationId, Path resource) {
        return can(applicationId, Capability.FILE_WRITE, resource);
    }

    public boolean canCreate(String applicationId, Path resource) {
        return can(applicationId, Capability.FILE_CREATE, resource);
    }

    public boolean canDelete(String applicationId, Path resource) {
        return can(applicationId, Capability.FILE_DELETE, resource);
    }

    /** Throws AccessDeniedException when policy denies the operation. */
    public void require(String applicationId, Capability capability, Path resource)
            throws AccessDeniedException {
        Decision decision = check(applicationId, capability, resource);
        if (!decision.allowed()) {
            throw new AccessDeniedException(resource == null ? "<null>" : resource.toString(),
                    null, decision.reason() + ": " + decision.message());
        }
    }

    /** Throws AccessDeniedException when a socket token is invalid or policy denies the operation. */
    public void requireToken(String sessionToken, Capability capability, Path resource)
            throws AccessDeniedException {
        Decision decision = checkToken(sessionToken, capability, resource);
        if (!decision.allowed()) {
            throw new AccessDeniedException(resource == null ? "<null>" : resource.toString(),
                    null, decision.reason() + ": " + decision.message());
        }
    }

    private Decision decideToken(String sessionToken, Capability capability, Path resource,
            boolean checkHost) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return finish(UUID.randomUUID().toString(), "unknown", capability, null, false,
                    Reason.IDENTITY_TOKEN_INVALID, "Socket identity token is missing");
        }
        try {
            String applicationId = new IdSocketClient(dataRoot).resolveApplication(sessionToken);
            return decide(applicationId, capability, resource, checkHost);
        } catch (IOException exception) {
            Reason reason = Files.exists(dataRoot.resolve("ids").resolve(".id-broker.sock"))
                    ? Reason.IDENTITY_TOKEN_INVALID
                    : Reason.IDENTITY_SERVICE_UNAVAILABLE;
            return finish(UUID.randomUUID().toString(), "unknown", capability, null, false,
                    reason, reason == Reason.IDENTITY_SERVICE_UNAVAILABLE
                            ? "ID socket service is unavailable"
                            : "ID socket token is invalid or expired");
        }
    }

    private Decision decide(String rawApplicationId, Capability capability, Path rawResource,
            boolean checkHost) {
        String requestId = UUID.randomUUID().toString();
        String applicationId = normalizeId(rawApplicationId);
        if (applicationId == null) {
            return finish(requestId, "unknown", capability, null, false,
                    Reason.IDENTITY_INVALID, "Application ID is missing or invalid");
        }
        if (capability == null) {
            return finish(requestId, applicationId, null, null, false,
                    Reason.CAPABILITY_DENIED, "Capability is missing");
        }

        Path resource;
        try {
            resource = canonicalTarget(rawResource);
        } catch (IOException | InvalidPathException | SecurityException exception) {
            return finish(requestId, applicationId, capability, null, false,
                    Reason.RESOURCE_INVALID, "Resource could not be resolved safely");
        }

        Grant grant = grants.get(applicationId);
        if (grant == null) {
            return finish(requestId, applicationId, capability, resource, false,
                    Reason.IDENTITY_UNKNOWN, "Unknown applications default to Level 0");
        }
        if (grant.status != Status.ACTIVE) {
            return finish(requestId, applicationId, capability, resource, false,
                    Reason.APPLICATION_INACTIVE, "Application status is " + grant.status);
        }
        if (grant.deniedCapabilities.contains(capability)) {
            return finish(requestId, applicationId, capability, resource, false,
                    Reason.EXPLICIT_DENY, "An explicit deny overrides other grants");
        }
        if (!grant.capabilities.contains(capability)) {
            return finish(requestId, applicationId, capability, resource, false,
                    Reason.CAPABILITY_DENIED,
                    "Level " + grant.level + " does not grant " + capability);
        }
        if (!insideAnyScope(resource, grant.scopes)) {
            return finish(requestId, applicationId, capability, resource, false,
                    Reason.SCOPE_DENIED, "Resource is outside the application's approved scopes");
        }
        if (isCritical(resource) && !mayAccessCritical(grant, capability)) {
            return finish(requestId, applicationId, capability, resource, false,
                    Reason.PROTECTED_RESOURCE,
                    "Critical Datacenter resources require Level 4 and an administrative capability");
        }
        if (checkHost && !hostAllows(resource, capability.hostAccess)) {
            return finish(requestId, applicationId, capability, resource, false,
                    Reason.HOST_ACCESS_DENIED,
                    "Datacenter policy allows the request but the host filesystem does not");
        }
        return finish(requestId, applicationId, capability, resource, true,
                Reason.ALLOWED, "Capability and resource scope are allowed");
    }

    private Decision finish(String requestId, String applicationId, Capability capability,
            Path resource, boolean allowed, Reason reason, String message) {
        boolean audited = writeAudit(requestId, applicationId, capability, resource, allowed, reason);
        return new Decision(requestId, applicationId, capability, resource, allowed, reason,
                message, audited);
    }

    private synchronized boolean writeAudit(String requestId, String applicationId,
            Capability capability, Path resource, boolean allowed, Reason reason) {
        String entry = "timestamp=" + Instant.now()
                + " event=AUTHORIZATION_DECISION"
                + " request_id=" + safeLog(requestId)
                + " app_id=" + safeLog(applicationId)
                + " capability=" + (capability == null ? "NONE" : capability)
                + " resource_hash=" + hashResource(resource)
                + " result=" + (allowed ? "ALLOWED" : "DENIED")
                + " reason=" + reason
                + System.lineSeparator();
        try {
            Path parent = auditLog.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(auditLog, entry, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return true;
        } catch (IOException | SecurityException exception) {
            return false;
        }
    }

    private Path canonicalTarget(Path raw) throws IOException {
        if (raw == null) throw new InvalidPathException("", "Resource is missing");
        Path absolute = raw.isAbsolute() ? raw.normalize() : dataRoot.resolve(raw).normalize();
        if (Files.exists(absolute, LinkOption.NOFOLLOW_LINKS)) return absolute.toRealPath();

        // Resolve the nearest existing parent so a new target cannot escape an
        // approved scope through a symlink in one of its parent directories.
        List<Path> missing = new ArrayList<>();
        Path cursor = absolute;
        while (cursor != null && !Files.exists(cursor, LinkOption.NOFOLLOW_LINKS)) {
            Path name = cursor.getFileName();
            if (name != null) missing.add(name);
            cursor = cursor.getParent();
        }
        if (cursor == null) throw new IOException("No existing parent for resource");
        Path resolved = cursor.toRealPath();
        for (int i = missing.size() - 1; i >= 0; i--) resolved = resolved.resolve(missing.get(i));
        return resolved.normalize();
    }

    private boolean isCritical(Path resource) {
        for (Path root : criticalRoots) if (resource.startsWith(root)) return true;
        return false;
    }

    private static boolean mayAccessCritical(Grant grant, Capability capability) {
        if (grant.level != 4) return false;
        return capability == Capability.SECURITY_POLICY_READ
                || capability == Capability.SECURITY_POLICY_WRITE
                || capability == Capability.REGISTRY_READ
                || capability == Capability.REGISTRY_MANAGE
                || capability == Capability.PERMISSION_GRANT
                || capability == Capability.PERMISSION_REVOKE
                || capability == Capability.LOG_READ_SECURITY
                || capability == Capability.LOG_ADMIN
                || capability == Capability.APPLICATION_MANAGE
                || capability == Capability.RUNTIME_MANAGE;
    }

    private static boolean insideAnyScope(Path resource, List<Path> scopes) {
        for (Path scope : scopes) if (resource.equals(scope) || resource.startsWith(scope)) return true;
        return false;
    }

    private static boolean hostAllows(Path resource, HostAccess access) {
        switch (access) {
            case NONE: return true;
            case READ: return Files.exists(resource) && Files.isReadable(resource);
            case WRITE: return Files.exists(resource)
                    ? Files.isWritable(resource)
                    : writableParent(resource);
            case CREATE: return writableParent(resource);
            case WRITE_PARENT:
                return Files.exists(resource) && writableParent(resource);
            case EXECUTE: return Files.exists(resource) && Files.isExecutable(resource);
            default: return false;
        }
    }

    private static boolean writableParent(Path resource) {
        Path parent = resource.getParent();
        while (parent != null && !Files.exists(parent)) parent = parent.getParent();
        return parent != null && Files.isDirectory(parent) && Files.isWritable(parent);
    }

    private static Map<String, Grant> parseGrants(Path dataRoot, Properties properties)
            throws IOException {
        String format = properties.getProperty("format.version", "1").trim();
        if (!"1".equals(format)) throw new IOException("Unsupported permission format: " + format);

        Set<String> applicationIds = new LinkedHashSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith("app.")) continue;
            int fieldSeparator = key.lastIndexOf('.');
            if (fieldSeparator <= 4) throw new IOException("Invalid application property: " + key);
            String id = normalizeId(key.substring(4, fieldSeparator));
            if (id == null) throw new IOException("Invalid application ID in property: " + key);
            applicationIds.add(id);
        }

        Map<String, Grant> parsed = new LinkedHashMap<>();
        for (String id : applicationIds) {
            String prefix = "app." + id + ".";
            int level = parseLevel(properties.getProperty(prefix + "level", "0"), id);
            Status status;
            try {
                status = Status.parse(properties.getProperty(prefix + "status", "PENDING"));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid status for " + id, exception);
            }

            EnumSet<Capability> allowed = baseline(level);
            allowed.addAll(parseCapabilities(properties.getProperty(prefix + "capabilities", ""), id));
            EnumSet<Capability> denied = parseCapabilities(
                    properties.getProperty(prefix + "deny", ""), id);
            allowed.removeAll(denied);
            List<Path> scopes = parseScopes(dataRoot,
                    properties.getProperty(prefix + "scopes", ""), id, level);
            parsed.put(id, new Grant(id, level, status, allowed, denied, scopes));
        }
        return parsed;
    }

    private static int parseLevel(String raw, String applicationId) throws IOException {
        try {
            int level = Integer.parseInt(raw.trim());
            if (level < 0 || level > 4) throw new NumberFormatException();
            return level;
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid level for " + applicationId + ": " + raw, exception);
        }
    }

    private static EnumSet<Capability> baseline(int level) {
        EnumSet<Capability> result = EnumSet.noneOf(Capability.class);
        if (level == 0) return result;
        for (Capability capability : Capability.values()) {
            if (capability.baselineLevel <= level) result.add(capability);
        }
        return result;
    }

    private static EnumSet<Capability> parseCapabilities(String raw, String applicationId)
            throws IOException {
        EnumSet<Capability> result = EnumSet.noneOf(Capability.class);
        if (raw.trim().isEmpty()) return result;
        for (String item : raw.split(",")) {
            try {
                result.add(Capability.parse(item));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Unknown capability for " + applicationId + ": " + item,
                        exception);
            }
        }
        return result;
    }

    private static List<Path> parseScopes(Path dataRoot, String raw, String applicationId,
            int level) throws IOException {
        List<Path> scopes = new ArrayList<>();
        if (raw.trim().isEmpty()) return scopes;
        for (String item : raw.split(";")) {
            String value = item.trim();
            if (value.isEmpty()) continue;
            if ("*".equals(value)) {
                if (level != 4) throw new IOException("Only Level 4 may use wildcard scope: " + applicationId);
                scopes.add(dataRoot);
                continue;
            }
            try {
                Path requested = Path.of(value);
                Path scope = requested.isAbsolute()
                        ? requested.toAbsolutePath().normalize()
                        : dataRoot.resolve(requested).normalize();
                if (!scope.startsWith(dataRoot)) {
                    throw new IOException("External scope is not allowed in the registry: " + value);
                }
                scopes.add(canonicalFuture(scope));
            } catch (InvalidPathException exception) {
                throw new IOException("Invalid scope for " + applicationId + ": " + value, exception);
            }
        }
        return scopes;
    }

    private static Path canonicalFuture(Path path) throws IOException {
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return path.toRealPath();
        List<Path> missing = new ArrayList<>();
        Path cursor = path;
        while (cursor != null && !Files.exists(cursor, LinkOption.NOFOLLOW_LINKS)) {
            if (cursor.getFileName() != null) missing.add(cursor.getFileName());
            cursor = cursor.getParent();
        }
        if (cursor == null) throw new IOException("Scope has no existing parent: " + path);
        Path result = cursor.toRealPath();
        for (int i = missing.size() - 1; i >= 0; i--) result = result.resolve(missing.get(i));
        return result.normalize();
    }

    private static Path canonicalExisting(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        if (!Files.isDirectory(absolute)) throw new IOException("Datacenter root is not a directory: " + absolute);
        return absolute.toRealPath();
    }

    private static List<Path> criticalRoots(Path root) {
        String[] names = {
            ".data/unfinished/security", "registry", "manager", "runtime", "main", "scripts", "utils"
        };
        List<Path> paths = new ArrayList<>();
        for (String name : names) paths.add(root.resolve(name).normalize());
        return Collections.unmodifiableList(paths);
    }

    private static String normalizeId(String raw) {
        if (raw == null) return null;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return value.matches("[a-z0-9][a-z0-9._-]{0,63}") ? value : null;
    }

    private static String safeLog(String value) {
        if (value == null) return "unknown";
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String hashResource(Path resource) {
        if (resource == null) return "none";
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(resource.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder("sha256:");
            for (byte value : bytes) result.append(String.format("%02x", value));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            return "unavailable";
        }
    }

    /**
     * Command-line permission check.
     *
     * <pre>
     * java security.perm.Checker [--root PATH] [--effective] [--token] ID CAPABILITY RESOURCE
     * </pre>
     */
    public static void main(String[] args) {
        int index = 0;
        Path root = Path.of(System.getProperty("user.dir"));
        boolean effective = false;
        boolean tokenIdentity = false;
        try {
            while (index < args.length && args[index].startsWith("--")) {
                if ("--root".equals(args[index]) && index + 1 < args.length) {
                    root = Path.of(args[index + 1]);
                    index += 2;
                } else if ("--effective".equals(args[index])) {
                    effective = true;
                    index++;
                } else if ("--token".equals(args[index])) {
                    tokenIdentity = true;
                    index++;
                } else {
                    usageAndExit("Unknown or incomplete option: " + args[index], 64);
                    return;
                }
            }
            if (args.length - index != 3) {
                usageAndExit("Expected APP CAPABILITY RESOURCE", 64);
                return;
            }
            String applicationId = args[index];
            Capability capability = Capability.parse(args[index + 1]);
            Path resource = Path.of(args[index + 2]);
            Checker checker = Checker.load(root);
            Decision decision;
            if (tokenIdentity) {
                decision = effective
                        ? checker.checkTokenEffective(applicationId, capability, resource)
                        : checker.checkToken(applicationId, capability, resource);
            } else {
                decision = effective
                        ? checker.checkEffective(applicationId, capability, resource)
                        : checker.check(applicationId, capability, resource);
            }
            System.out.println(decision);
            if (!decision.auditRecorded()) {
                System.err.println("WARNING: security decision could not be written to the audit log");
            }
            if (!decision.allowed()) System.exit(2);
        } catch (IllegalArgumentException | IOException exception) {
            System.err.println("Security checker error: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static void usageAndExit(String error, int status) {
        System.err.println(error);
        System.err.println("Usage: java security.perm.Checker [--root PATH] [--effective] [--token]"
                + " ID CAPABILITY RESOURCE");
        System.exit(status);
    }
}
