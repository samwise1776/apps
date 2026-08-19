# Security

Datacenter is a local private workspace, not an authentication boundary. Application IDs provide stable identity and organization; secrecy of an ID is not authorization. Owner-only Unix permissions protect local ID files where POSIX permissions exist, and the checker verifies mode `600`. Windows installations should use equivalent account ACLs.

Passwords, access tokens, and API keys must not be committed. User data belongs in application-specific local state, logs in `.data/logs`, and secrets in an external secret store or environment. Release packages and backups exclude raw ID files, sockets, runtime state, and logs. Security errors should identify the affected component without printing secret values.

Trestrio binds to `127.0.0.1` by default, applies browser security headers,
rejects cross-origin state changes, and replaces its data file atomically.
Remote binding is not a supported security boundary; it requires an
authenticated reverse proxy and an explicit threat review.

ProjectHub stores new data in a bounded, versioned binary format using atomic
replacement. Existing Java-serialized data is migrated once through a strict
class allow-list and resource limits; new data is never written with Java
object serialization.

Run `./scripts/security-audit.sh` before release. It checks for obvious secret
material, private-ID permissions, and symbolic links escaping the workspace.
