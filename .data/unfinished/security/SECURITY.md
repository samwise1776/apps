# Datacenter Security Policy and Permission Model

**Security specification version:** 2.0.0  
**Project:** Datacenter  
**Last reviewed:** 2026-08-11  
**Status:** Target security design; enforcement is not yet fully implemented

## 1. Purpose

This document defines the security requirements, permission model, protected
resources, audit rules, vulnerability-reporting process, and implementation
expectations for Datacenter and its applications.

The words **MUST**, **MUST NOT**, **SHOULD**, **SHOULD NOT**, and **MAY** are
used as requirements. A component is not compliant merely because its behavior
is documented here; the required checks must be implemented and tested.

> [!IMPORTANT]
> The current Datacenter Java applications do not yet provide a complete,
> centralized permission-enforcement service. They primarily operate with the
> permissions of the operating-system user that launched them. Until the model
> in this document is implemented, permission levels are design metadata and
> must not be treated as a security boundary.

## 2. Security goals

Datacenter's security model is intended to:

1. Give each application only the access it needs.
2. Deny access unless it has been explicitly granted.
3. Keep applications inside approved resource and path boundaries.
4. Prevent applications from granting themselves additional permissions.
5. Protect the permission registry, logs, application data, and runtime state.
6. Make security decisions consistent, reviewable, and auditable.
7. Fail safely when identity, configuration, or enforcement is unavailable.
8. Let administrators revoke access without reinstalling an application.
9. Avoid exposing secrets through errors, logs, metadata, or diagnostics.

## 3. Scope

This policy applies to:

- Datacenter application registration and identity.
- Files and directories managed through Datacenter.
- Datacenter-managed memory and runtime state.
- Application-specific data.
- Configuration, registries, logs, versions, and runtime resources.
- Process execution, network access, environment access, and external tools
  when those capabilities are added to Datacenter.
- Built-in, third-party, development, and unknown applications.

This model does not replace operating-system security. Filesystem ownership,
process isolation, account permissions, sandboxing, and platform security
remain necessary defense layers.

## 4. Threat model

Datacenter MUST assume that an application can be buggy, compromised, or
malicious. Relevant threats include:

- An unknown program impersonating a trusted application.
- A lower-privileged application reading private files or secrets.
- Path traversal using `..`, absolute paths, alternate separators, or encoded
  path segments.
- Escaping an approved directory through a symbolic link or mount point.
- A time-of-check/time-of-use race that changes a resource after approval.
- Modification of the registry or policy to gain higher permissions.
- Loading or replacing executable code after an identity check.
- Forged, malformed, expired, or replayed permission grants.
- Sensitive data being written to logs or error messages.
- Audit logs being deleted or altered to hide activity.
- Resource exhaustion through excessive reads, writes, logs, processes, or
  memory usage.
- Unsafe deserialization or parsing of application-controlled data.
- A trusted application being used as a confused deputy.

The initial model does not promise protection against an attacker who already
controls the operating-system account or has administrator/root access.

## 5. Core principles

### 5.1 Default deny

Every protected operation MUST be denied unless an authenticated application
has an active grant for the required capability and resource scope.

Unknown applications receive **Level 0**, not Level 1.

### 5.2 Least privilege

Applications MUST receive the smallest set of capabilities, paths, and time
limits necessary for their function. A numeric level is a convenience profile;
it is not a substitute for scoped capabilities.

### 5.3 Complete mediation

Every protected operation MUST pass through the authorization service. A check
performed only when the application starts is insufficient.

### 5.4 Separation of duties

An application that uses permissions MUST NOT also be able to approve its own
registration, edit its own grants, disable auditing, or change security policy.

### 5.5 Fail closed

If application identity, policy, resource scope, or required security state
cannot be verified, the operation MUST be denied.

### 5.6 Defense in depth

Datacenter authorization SHOULD be combined with operating-system accounts,
filesystem permissions, process isolation, sandboxing, and secure defaults.

## 6. Security terminology

| Term | Meaning |
| --- | --- |
| Subject | The authenticated application or Datacenter component requesting an operation. |
| Capability | A specific operation such as `FILE_READ` or `LOG_APPEND`. |
| Resource | The exact file, directory, data namespace, service, or runtime object being accessed. |
| Scope | The limits attached to a grant, such as an approved directory or application namespace. |
| Grant | An authorization record binding a subject to a capability and scope. |
| Level | A predefined group of capabilities used as a starting profile. |
| Protected resource | A resource that requires an explicit authorization decision. |
| Security authority | The trusted component allowed to register identities and manage grants. |

## 7. Authorization decision

Every authorization decision MUST evaluate all of the following:

```text
authenticated subject
        + requested capability
        + canonical resource
        + resource scope
        + grant status and expiry
        + runtime context
        + applicable deny rules
        = ALLOW or DENY
```

A level check alone is not sufficient. An application with `FILE_WRITE` for
its own data directory must still be denied when it requests a registry file.

Explicit deny rules MUST take precedence over allow rules.

## 8. Permission levels

Datacenter defines five states: Level 0 plus four grant profiles. Higher levels
include the baseline capabilities of lower levels, but every capability remains
limited by resource scope and explicit deny rules.

### 8.1 Level 0 — No protected access

Level 0 is the mandatory default for unknown, disabled, invalid, expired, or
unverified applications.

Level 0 applications:

- MUST NOT access protected Datacenter files, metadata, memory, or logs.
- MAY display their own user interface without protected access.
- MAY request registration or user approval through the trusted security UI.
- MUST NOT repeatedly prompt for a permission after it has been denied.

### 8.2 Level 1 — Scoped text reading

Level 1 MAY include:

- `FILE_READ_TEXT`
- `FILE_READ_LINES`
- `FILE_SEARCH_TEXT`
- `FILE_COUNT_LINES`

Level 1 access MUST be limited to explicitly approved text files or directories.
It MUST NOT include secrets, credentials, arbitrary binary files, security
configuration, the registry, private logs, or another application's data.

### 8.3 Level 2 — Scoped metadata inspection

Level 2 includes scoped Level 1 capabilities and MAY include:

- `FILE_METADATA_READ`
- `FILE_EXISTS`
- `DIRECTORY_LIST`
- `DIRECTORY_COUNT`

Metadata may include file name, type, size, extension, timestamps, and whether
a path is a file or directory. Exact locations and names can be sensitive and
MUST remain subject to scope and redaction rules.

Level 2 MUST NOT modify files.

### 8.4 Level 3 — Scoped file and application-data access

Level 3 includes scoped Level 2 capabilities and MAY include individually
approved capabilities such as:

- `FILE_CREATE`
- `FILE_WRITE`
- `FILE_RENAME`
- `FILE_MOVE`
- `FILE_COPY`
- `DIRECTORY_CREATE`
- `APP_DATA_READ`
- `APP_DATA_WRITE`
- `STATE_READ`
- `STATE_WRITE`

Level 3 MUST NOT imply access to arbitrary system files, security policy,
registry data, another application's private data, raw process memory, code,
executables, startup configuration, or protected logs.

Deletion is not part of the default Level 3 profile. It requires a separate
capability and, for important data, confirmation or recovery support.

### 8.5 Level 4 — Administrative capabilities

Level 4 is reserved for trusted Datacenter security and management components.
It MAY include explicitly scoped administrative capabilities such as:

- `FILE_DELETE`
- `DIRECTORY_DELETE`
- `REGISTRY_MANAGE`
- `PERMISSION_GRANT`
- `PERMISSION_REVOKE`
- `SECURITY_POLICY_READ`
- `SECURITY_POLICY_WRITE`
- `PROTECTED_RESOURCE_MANAGE`
- `LOG_ADMIN`
- `APPLICATION_MANAGE`
- `RUNTIME_MANAGE`

Level 4 is not an unconditional bypass. Protected operations still require
identity verification, capability checks, scope checks, policy checks, and
auditing. Destructive or security-sensitive actions SHOULD require explicit
administrator confirmation or a narrowly authorized service identity.

### 8.6 Level summary

| Level | Profile | Default access |
| ---: | --- | --- |
| 0 | Untrusted | No protected access |
| 1 | Text reader | Approved text content only |
| 2 | Inspector | Approved text and metadata only |
| 3 | Application | Approved file, state, and app-data operations |
| 4 | Administrator | Explicit administrative capabilities |

## 9. Capability catalog

Implementations SHOULD use stable capability identifiers rather than comparing
human-readable names.

### 9.1 Files and directories

```text
FILE_READ_TEXT
FILE_READ_BINARY
FILE_READ_LINES
FILE_SEARCH_TEXT
FILE_COUNT_LINES
FILE_METADATA_READ
FILE_EXISTS
FILE_CREATE
FILE_WRITE
FILE_APPEND
FILE_TRUNCATE
FILE_COPY
FILE_MOVE
FILE_RENAME
FILE_DELETE
DIRECTORY_LIST
DIRECTORY_COUNT
DIRECTORY_CREATE
DIRECTORY_DELETE
```

### 9.2 Application data and state

```text
APP_DATA_READ
APP_DATA_WRITE
APP_DATA_DELETE
STATE_READ
STATE_WRITE
STATE_DELETE
```

`STATE_*` refers only to Datacenter-managed structured state. It MUST NOT grant
raw access to the JVM, native process memory, pointers, or another process.

### 9.3 Logs

```text
LOG_APPEND
LOG_READ_OWN
LOG_READ_SECURITY
LOG_EXPORT
LOG_ADMIN
```

Ordinary applications SHOULD normally receive only `LOG_APPEND` to a service
that assigns trusted timestamps and application identity.

### 9.4 Registry, applications, and policy

```text
REGISTRY_READ
REGISTRY_MANAGE
APPLICATION_REGISTER
APPLICATION_ENABLE
APPLICATION_DISABLE
APPLICATION_REMOVE
PERMISSION_REQUEST
PERMISSION_GRANT
PERMISSION_REVOKE
SECURITY_POLICY_READ
SECURITY_POLICY_WRITE
PROTECTED_RESOURCE_MANAGE
```

### 9.5 Runtime and future capabilities

```text
RUNTIME_READ
RUNTIME_MANAGE
PROCESS_EXECUTE
NETWORK_CONNECT
NETWORK_LISTEN
ENVIRONMENT_READ
CLIPBOARD_READ
CLIPBOARD_WRITE
```

Process, network, environment, and clipboard access MUST default to denied.
They require separate design review before implementation and MUST NOT be
silently included in Level 3 or Level 4.

## 10. Application registration and identity

Applications MUST be registered before receiving protected access. A registry
record SHOULD contain:

```text
application ID
display name
version
publisher or owner
canonical installation location
entry point
content hash or signing identity
requested capabilities
approved capabilities and scopes
permission profile
status
registration time
grant expiry, if any
last security review
```

Display names are not identities. An application MUST NOT be trusted merely
because it supplies a known name or ID.

Before granting access, Datacenter MUST:

1. Authenticate the requesting process through a trusted launch or IPC channel.
2. Match it to an active registry record.
3. Verify its installation path and entry point.
4. Verify an approved content hash, signature, or equivalent identity control.
5. Reject identity data supplied only by the untrusted application.
6. detect changed executable content and require reapproval when appropriate.

The registry MUST be writable only by the security authority. Applications
MUST NOT directly edit their own records.

## 11. Application status

Valid application states are:

| Status | Meaning |
| --- | --- |
| `PENDING` | Registered request awaiting review; Level 0 applies. |
| `ACTIVE` | Identity and grants are valid. |
| `SUSPENDED` | Temporarily denied pending review. |
| `DISABLED` | Administratively disabled; Level 0 applies. |
| `REVOKED` | Grants are invalid and must not be reused. |
| `INVALID` | Identity or registry data failed validation. |

Only `ACTIVE` applications may use grants.

## 12. Resource scopes

Every resource grant MUST define a scope. Examples include:

```text
application-data://datadocs/**
user-selected-file://<opaque-handle>
workspace://apps/4/**
log://datadocs/append
state://datadocs/preferences
```

Opaque resource handles are preferred for user-selected files because they
avoid giving an application unrestricted path access.

Scopes MUST specify whether they apply to one resource, direct children, or a
recursive subtree. Ambiguous prefix matching is forbidden. For example,
`/data/app` MUST NOT accidentally match `/data/application-secrets`.

## 13. Protected resources

The following Datacenter areas are protected by default:

```text
.data/unfinished/security/
registry/
manager/
runtime/
versions/
logs/
data/
apps/
utils/
main/
scripts/
build/
```

Protection is based on canonical resource identity, not only a textual path.

### 13.1 Critical resources

These resources require administrative capabilities and SHOULD be read-only to
ordinary applications:

- Security policy and permission grants.
- Application identity registry.
- Trusted executable code and startup scripts.
- Security audit logs.
- Credential, token, key, and secret storage.
- Update metadata and trusted version information.
- Runtime configuration that can change enforcement behavior.

### 13.2 Application-private data

Each application SHOULD receive a separate data namespace. One application
MUST NOT access another application's private namespace without a specific
cross-application grant approved by the security authority.

## 14. Safe path handling

For every filesystem operation, Datacenter MUST:

1. Reject missing, malformed, or unsupported paths.
2. Resolve the requested path against the approved root.
3. Normalize separators and `.` or `..` segments.
4. Resolve symbolic links for the target and existing parent directories.
5. Compare canonical paths using path-segment-aware operations.
6. Confirm that the resolved target remains inside the approved scope.
7. Revalidate immediately before the operation when race conditions are
   possible.
8. Avoid following links during destructive recursive operations.
9. Apply filesystem permissions that agree with Datacenter policy.

String prefix checks such as `path.startsWith(allowedText)` MUST NOT be used as
the sole containment test.

Archive extraction MUST reject absolute paths, `..` traversal, link escape,
device files, and duplicate entries designed to overwrite prior output.

## 15. Permission requests and approval

Permission requests MUST show:

- The verified application identity and publisher.
- The exact capability requested.
- The affected resource or scope.
- Why the application says it needs access.
- Whether the grant is one-time, session-only, time-limited, or persistent.
- The risk of granting the request.

Security-sensitive approval MUST occur in trusted Datacenter UI that an
application cannot imitate or modify. Approval prompts MUST NOT use vague text
such as “allow all access.”

Denial MUST remain usable; an application MUST NOT trap the user in repeated
approval prompts.

## 16. Grant lifecycle

A permission grant MUST include:

```text
grant ID
application identity
capability
scope
issuer
issued time
expiry or duration
status
reason
```

Grants MAY be one-time, session-only, time-limited, or persistent. High-risk
grants SHOULD expire and require renewed approval.

Permission changes MUST:

1. Be performed only by the security authority.
2. Validate all fields against a strict schema.
3. Reject unknown levels, capabilities, and statuses.
4. Be written atomically.
5. Be logged before taking effect when feasible.
6. Invalidate relevant authorization caches immediately.
7. Take effect for active applications without requiring a restart.

Revoked grants MUST NOT be reusable, even from a stale session or cached token.

## 17. Authorization flow

```text
Request received
       |
       v
Authenticate application identity ---- failure ---> DENY
       |
       v
Parse and validate request ------------ failure ---> DENY
       |
       v
Resolve canonical resource ------------ failure ---> DENY
       |
       v
Load active grants and deny rules ------ failure ---> DENY
       |
       v
Check capability + scope + context ----- mismatch --> DENY
       |
       v
Record security decision
       |
       v
Perform operation through trusted service
       |
       v
Record completion or failure
```

Example decision logic:

```text
authorize(subject, capability, resource, context):
    identity = authenticate(subject)
    if identity is invalid: deny(IDENTITY_INVALID)

    target = canonicalize(resource)
    if target is invalid: deny(RESOURCE_INVALID)

    if explicit_deny(identity, capability, target, context):
        deny(EXPLICIT_DENY)

    grant = find_active_grant(identity, capability, target, context)
    if grant is absent: deny(GRANT_NOT_FOUND)

    allow(grant.id)
```

## 18. Denied operations

Denied operations MUST stop before protected data is read or changed. Errors
returned to applications MUST be useful without revealing secret paths, policy
internals, or the existence of inaccessible resources.

Recommended stable result codes include:

```text
IDENTITY_UNKNOWN
IDENTITY_INVALID
APPLICATION_INACTIVE
REQUEST_INVALID
CAPABILITY_DENIED
SCOPE_DENIED
RESOURCE_INVALID
GRANT_EXPIRED
GRANT_REVOKED
EXPLICIT_DENY
SECURITY_UNAVAILABLE
RATE_LIMITED
```

Example application response:

```text
SECURITY_DENIED
REQUEST_ID: 7c2f...
CODE: CAPABILITY_DENIED
OPERATION: FILE_WRITE
```

The detailed audit record may contain additional protected diagnostic data.

## 19. Security logging and auditing

The security service MUST log important events, including:

- Authentication success and failure.
- Permission allow and deny decisions.
- Registration, enablement, suspension, and removal.
- Grant creation, change, expiry, and revocation.
- Access to critical protected resources.
- Security-policy and registry changes.
- Invalid or malformed security data.
- Audit service failures and suspected tampering.
- Repeated denials, rate limits, and suspicious request patterns.

Each audit record SHOULD include:

```text
trusted UTC timestamp
event ID
request ID
verified application ID
verified application version or content identity
capability
normalized resource identifier or safe hash
grant ID, when applicable
result
reason code
trusted component handling the request
```

Example:

```text
timestamp=2026-08-11T18:00:00Z
event=AUTHORIZATION_DECISION
request_id=7c2f...
app_id=datadocs
capability=FILE_WRITE
resource_hash=sha256:...
grant_id=19a1...
result=DENIED
reason=SCOPE_DENIED
```

Audit requirements:

- Applications MUST NOT choose their trusted identity or timestamp fields.
- Logs MUST NOT contain passwords, access tokens, private keys, full document
  contents, or unnecessary personal data.
- Sensitive resource names SHOULD be redacted or hashed where possible.
- Security logs MUST be append-only to ordinary applications.
- Rotation and retention limits MUST be defined.
- Access to logs MUST itself be authorized and audited.
- Loss of audit capability during a critical administrative operation SHOULD
  cause that operation to fail closed.

## 20. Secrets and sensitive data

Credentials, tokens, encryption keys, and similar secrets MUST NOT be stored in
source code, ordinary configuration, archives, or general logs.

Secrets SHOULD be stored using an operating-system credential store or a
dedicated encrypted secret service. Applications SHOULD receive short-lived,
narrowly scoped credentials when direct access is unavoidable.

Secret values MUST be redacted from:

- Logs and exception messages.
- Crash reports and diagnostics.
- UI screenshots and clipboard output.
- Exported support bundles.
- Generated archives and Git commits.

## 21. Data integrity and recovery

Security-critical files MUST use strict schemas and reject unknown or malformed
values. Updates SHOULD be atomic and preserve a recoverable prior version.

Destructive operations SHOULD use recoverable deletion when practical. Before
recursive deletion, Datacenter MUST resolve and display the exact target,
confirm its scope, and reject broad or ambiguous roots.

Backups containing protected data MUST receive protections equivalent to the
original data.

## 22. Rate limits and resource safety

Datacenter SHOULD enforce per-application limits for:

- Requests per time interval.
- Concurrent operations.
- Bytes read or written.
- Log volume.
- Memory and storage usage.
- Process execution time and count.
- Network requests when networking is supported.

Limit violations SHOULD produce `RATE_LIMITED`, be audited, and must not cause
other applications or the security service to fail.

## 23. Updates and executable integrity

Updates to trusted code, policy, or the application registry MUST be obtained
from an authenticated source and verified before installation.

An application update that changes its verified content identity or requests
new capabilities SHOULD require renewed approval. Updates MUST NOT silently
expand resource scopes.

Rollback behavior and last-known-good recovery SHOULD be tested before the
update mechanism is considered production-ready.

## 24. Current application assignments

No application should be considered securely sandboxed until centralized
enforcement is implemented.

| Application | Legacy/design level | Current enforcement status | Required review |
| --- | ---: | --- | --- |
| Datadocs | 4 | Not centrally enforced | Replace broad Level 4 access with user-selected document access and an application-private data scope. |

New applications default to Level 0 until registered, reviewed, and explicitly
granted capabilities.

## 25. Implementation status

This document intentionally separates the desired model from current code.

| Control | Status as of 2026-08-11 |
| --- | --- |
| Central authorization service | Policy engine implemented in `.data/unfinished/security/perm/Checker.java`; applications are not yet wired through it |
| Authenticated application identity | Owner-only Unix socket issues short-lived tokens from private IDs; same-user process isolation remains pending |
| Capability and scope registry | Initial file-backed registry implemented in `.data/unfinished/security/perm/permissions.properties` |
| Safe canonical path broker | Canonical scope checking implemented in `Checker`; file operations are not yet brokered centrally |
| Trusted permission approval UI | Not implemented |
| Grant revocation and expiry | Not implemented |
| Structured security audit log | Initial authorization-decision log implemented; retention and tamper resistance remain pending |
| Operating-system process sandbox | Not implemented by Datacenter |
| Ordinary OS filesystem permission checks | Provided by the host platform |

This table MUST be updated as controls are implemented and verified. A control
must not be marked implemented until it has automated tests and a documented
failure mode.

## 26. Required security tests

Before the permission system is considered enforceable, automated tests MUST
cover at least:

### Identity and grants

- Unknown, disabled, revoked, and modified applications receive Level 0.
- An application cannot impersonate another application by name or ID.
- Expired and revoked grants fail immediately.
- Grant caches cannot preserve revoked access.
- Invalid levels and unknown capabilities are rejected.
- An application cannot change its own permissions.

### Paths and resources

- Relative and absolute path traversal is denied.
- Symlink and mount-point escape is denied.
- Sibling paths with common string prefixes are not confused.
- Case and separator behavior is correct on supported platforms.
- Archive extraction cannot escape its destination.
- Race-condition tests cannot swap an approved target for a protected target.

### Authorization

- Every capability has allow and deny tests.
- Scope boundaries work for files, direct children, and recursive trees.
- Explicit deny overrides an allow profile.
- Level inheritance does not bypass scope.
- Failure of identity, registry, policy, or auditing fails closed.

### Logs and sensitive data

- Untrusted applications cannot forge audit identity or timestamps.
- Applications cannot alter protected audit records.
- Secrets and document contents are redacted.
- Log rotation cannot be used to erase a recent security event.

### Abuse and recovery

- Rate limits prevent request, storage, and log exhaustion.
- Interrupted policy updates recover atomically.
- Destructive operations reject broad or ambiguous targets.
- Backup and rollback procedures restore a usable trusted state.

Security regressions MUST block a production release.

## 27. Secure development requirements

Changes affecting authentication, authorization, path handling, updates,
registry data, secrets, or audit logging require security-focused review.

Developers MUST:

- Validate all data crossing a trust boundary.
- Use parameterized, structured formats rather than building commands or paths
  from untrusted strings.
- Avoid executing shell commands with application-controlled content.
- Avoid unsafe native memory access and unsafe deserialization.
- Keep dependencies supported and review security advisories.
- Write negative tests, not only successful-operation tests.
- Never commit credentials, tokens, private keys, or sensitive local logs.
- Document any accepted security risk and its planned remediation.

## 28. Incident response

When a security incident is suspected:

1. Preserve relevant logs and evidence without modifying the originals.
2. Suspend affected application identities and revoke their grants.
3. Rotate exposed credentials and signing material.
4. Identify affected versions, systems, resources, and users.
5. Contain the issue before restoring service.
6. Develop and test a fix, including a regression test.
7. Notify affected users when appropriate.
8. Publish a security advisory after coordinated remediation.
9. Record lessons learned and update this policy or threat model.

Incident data MUST be shared only with people who need it and handled according
to its sensitivity.

## 29. Reporting a vulnerability

Do not disclose a suspected vulnerability in a public issue, discussion, log,
or pull request.

Preferred reporting method:

1. Open the `samwise1776/datacenter-dev` repository on GitHub.
2. Select **Security** and then **Report a vulnerability** if private
   vulnerability reporting is enabled.
3. If that option is unavailable, contact the repository owner privately
   through an established trusted channel and request a private reporting path.

A useful report includes:

- A concise description of the vulnerability.
- Affected versions, files, or components.
- Reproduction steps or a minimal proof of concept.
- The expected and actual security behavior.
- Potential impact and required preconditions.
- Suggested mitigation, if known.
- Whether the issue has been disclosed elsewhere.

Do not include real credentials, personal data, or another person's private
files in a report. Use test data and redact secrets.

The project SHOULD acknowledge a complete report within five business days,
provide periodic status updates, and coordinate disclosure with the reporter.
Response timing may vary based on severity and maintainer availability.

## 30. Supported versions

Until a longer-term support policy is published, security fixes are provided
for:

- The latest release of Datacenter.
- The current default development branch when a fix is being prepared.

Older snapshots and development builds may not receive security fixes. Users
SHOULD update to the latest fixed release when a security update is published.

## 31. Policy changes

Changes to this security model require review by the Datacenter maintainer or
designated security authority. Material changes SHOULD include:

- A reason for the change.
- Compatibility and migration impact.
- Updated tests.
- Updated capability and threat-model documentation.
- A security-version increment.

Permission profiles MUST NOT be expanded silently.

## 32. Security checklist

Before granting a capability, confirm all of the following:

- [ ] The application identity is authenticated.
- [ ] The application is active and its code identity is current.
- [ ] The capability is necessary for the application feature.
- [ ] The resource scope is explicit and minimal.
- [ ] The grant has an appropriate duration or expiry.
- [ ] Explicit deny rules have been evaluated.
- [ ] The resource has been canonicalized safely.
- [ ] The decision and subsequent operation will be audited.
- [ ] Errors will not expose protected information.
- [ ] Revocation will take effect immediately.

Before marking the security system production-ready, confirm:

- [ ] Central enforcement is used for every protected operation.
- [ ] Applications cannot directly bypass the enforcement service.
- [ ] Level 0 is the default for unknown or invalid applications.
- [ ] Security policy and the registry are protected from applications.
- [ ] Required tests in this document pass on supported platforms.
- [ ] Incident response and recovery procedures have been exercised.

---

**Security version:** 2.0.0  
**Permission profiles:** 0 through 4  
**Authorization basis:** authenticated identity + capability + resource scope + context  
**Default decision:** deny
