# Datacenter legacy workspace guide

> Historical reference: this document describes the pre-registry layout from
> August 11, 2026. For current application identity, status, paths, builds, and
> release policy, use `README.md`, `config/apps.json`, and the focused documents
> in `docs/`. Historical app numbers below are not current release metadata.

Last reviewed: **August 11, 2026**  
Workspace: `/home/ray/Data`

## 1. Current state

Datacenter has four active applications: 1, 2, 4, and 5. The active count in
`.apps.txt` is `4`, and matching metadata exists in `versions`. App 3 was an
unfinished Java placeholder and is now isolated under `.data/unfinished`; it
must not be treated as a runnable app.

The workspace supports:

- Java and .NET builds;
- graphical launch of all four active apps;
- application discovery and line-count version metadata;
- repair and full-workspace health checks;
- private application IDs over an owner-only Unix socket;
- a permission-policy prototype with regression tests;
- individual app packages and complete ZIP/JAR snapshots.

## 2. Source-of-truth layout

```text
/home/ray/Data/
├── README.md                 Quick start
├── .apps.txt                 Generated active-app count
├── .data/
│   ├── LICENSE               Proprietary project license
│   ├── logs/                 Active logs
│   ├── resources/zip.sh      Complete ZIP builder
│   └── unfinished/           All incomplete, retired, or experimental work
├── apps/                     Active app source and app-local build output
│   ├── 1/Info.java
│   ├── 2/Program.cs
│   ├── 2/apps.csproj
│   ├── 4/DataDocs.java
│   └── 5/App1.java
├── app-zips/                 Individual distributable app snapshots
├── build/                    Output from scripts/build.sh
├── data/Data.java            Workspace-report utility
├── docs/AllDetails.md        This guide
├── github-dev/               Development Git worktree
├── ids/                      Private IDs, broker, client, and tests
├── jar/                      Complete JAR snapshot output
├── main/Fixer.java           Repair coordinator
├── manager/Manager.java      Legacy single-file manager
├── runtime/                  Generated runtime output
├── scripts/                  Build, check, package, launch, and update tools
├── utils/                    Current modular manager package
├── versions/                 Generated per-app version metadata
└── zip/data.zip              Complete workspace ZIP
```

Git's own `.git` internals remain inside their worktrees. They are repository
metadata, not Datacenter application logs or unfinished work.

## 3. Active applications

### App 1 — Datacenter App Console

- Source: `apps/1/Info.java`
- Main class: `apps.Info`
- Technology: Java Swing
- Role: discover, inspect, compile, repair, launch, and review app logs
- Current version metadata: `0.0.5`

### App 2 — Raylib 3D demo

- Source: `apps/2/Program.cs`
- Project: `apps/2/apps.csproj`
- Technology: .NET 10 and Raylib-cs 8.0.0
- Role: render a simple interactive 3D scene
- Current version metadata: `0.0.0`

`apps/2/bin` and `apps/2/obj` are generated .NET output. The native Raylib
libraries make this app and its package much larger than the Java apps.

### App 4 — DataDocs

- Source: `apps/4/DataDocs.java`
- Main class: `DataDocs`
- Technology: Java Swing
- Role: create, edit, style, and save text documents
- Current version metadata: `0.0.2`

### App 5 — Datapro Analytics

- Source: `apps/5/App1.java`
- Main class: `App1`
- Technology: Java Swing
- Role: show live workspace, storage, file-type, tracker, and system metrics
- Current version metadata: `0.0.5`

Graphical apps require an active desktop display.

## 4. Managers and versioning

`utils` is the current modular manager and uses the `utils` Java package:

- `Registry` scans direct app directories and updates `.apps.txt`.
- `Info` counts supported source lines.
- `Updater` derives and atomically writes `versions/*.version`.
- `Logger` writes terminal output and `.data/logs/system.log`.
- `Manager` repeats the scan every two seconds.

`manager/Manager.java` is the older all-in-one implementation. Only one manager
should run at a time.

One version step represents 100 completed source lines:

```text
major = completed_hundreds / 100
minor = (completed_hundreds / 10) % 10
patch = completed_hundreds % 10
```

These versions measure source size; they are not semantic release versions.

## 5. IDs and permission checks

The `ids` directory is mode `700`, and each `.id.txt` is mode `600`.
`IdSocketServer` reads those raw IDs and exposes an owner-only Unix-domain
socket. A client requests a random, short-lived token and can resolve it only
to a normalized application key. Raw stored IDs are never returned or logged.
They are excluded from release archives. If an extracted workspace retains an
application's ID directory but not its ID file, the broker generates a new
random owner-only ID on first startup.

Start the broker:

```bash
/home/ray/Data/ids/start-id-socket.sh
```

The policy prototype is stored in `.data/unfinished/security/perm`. Its
`Checker` defaults unknown apps to Level 0, enforces capability and path scopes,
canonicalizes paths to resist traversal and symlink escapes, protects critical
workspace roots, records decisions, and fails closed for invalid tokens or an
offline ID service.

The policy is not yet a complete production boundary because the graphical
apps have not routed every protected operation through it. Processes running
as the same operating-system user also need OS-level sandboxing for strong
mutual isolation.

## 6. Build and repair commands

From `/home/ray/Data`:

```bash
# Compile active Java source
./scripts/build.sh

# Validate the complete workspace
./scripts/checker.sh

# Remove generated Java build output safely
./scripts/unbuild.sh

# Repair recoverable state and run the checks
java -cp build/classes/main:build/classes/utils Fixer /home/ray/Data

# Build the C# app directly
dotnet build apps/2/apps.csproj --nologo

# Launch all active graphical apps
./scripts/v1.0.0.sh
```

`scripts/checker.sh` is the authoritative test command. It checks shell syntax,
active Java and .NET compilation, app counts and metadata, 25 ID/permission
assertions, private ID modes, internal layout, unfinished isolation, archive
integrity, and packaged-source freshness.

## 7. Logs and generated state

Active logs belong in `.data/logs`. Logs from removed or unfinished code belong
in `.data/unfinished/logs`. The manager uses append-only text logs, so old logs
may need manual rotation if they grow large.

These paths are generated and rebuildable:

- `build`;
- `runtime`;
- `.data/Data/Compiled_folders`;
- `apps/2/bin` and `apps/2/obj`;
- `.apps.txt` and `versions/*.version`;
- `zip/data.zip` and `jar/Data-everything.jar`.

Do not treat generated output as the only copy of important source or data.

## 8. Unfinished-work policy

Everything incomplete, retired, experimental, placeholder-only, or awaiting
security integration stays under `.data/unfinished`. The main build, launcher,
registry, and app archives exclude it. Promote an item only after it has working
behavior, documentation, tests, and an explicit status review.

`GameCs.java` has been removed. Other retired game artifacts and historical
snapshots remain under `.data/unfinished` where they cannot enter production
builds accidentally.

## 9. Packaging and backups

Rebuild the complete ZIP:

```bash
./.data/resources/zip.sh
```

The script stages outside the source, excludes generated full ZIP/JAR snapshots
to prevent recursive archive growth, excludes raw IDs and the live socket,
tests the completed ZIP, then moves it into `zip/data.zip`.

Rebuild the complete JAR snapshot:

```bash
./jar.sh
```

The JAR is a storage container, not an executable Java application. It also
excludes generated full ZIP/JAR snapshots, raw IDs, and the live socket.
Individual app archives live in
`app-zips`; the health checker verifies their embedded sources against the
active workspace.

For disaster recovery, keep a dated verified copy outside `/home/ray/Data`.

## 10. Maintenance checklist

1. Run `./scripts/checker.sh` after source, policy, or packaging changes.
2. Confirm `.apps.txt` is `4` unless the active catalog intentionally changes.
3. Keep unfinished work under `.data/unfinished`.
4. Keep `ids` and raw ID files owner-only; never commit or print raw IDs.
5. Run only one manager implementation at a time.
6. Rebuild archives before publishing a release.
7. Review `.data/logs/system.log` and security logs for unexpected denials or
   repeated errors.
8. Keep the Git worktrees and external release in sync through reviewed commits.
9. Preserve `.data/LICENSE` with private distributions.
