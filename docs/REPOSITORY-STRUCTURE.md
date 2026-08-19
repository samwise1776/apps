# Repository Structure

This document explains what belongs in every major directory of the Datacenter workspace.

## Top-Level Directories

### `apps/`
All application source code. Each application has its own subdirectory containing its source files, tests, and documentation.

```
apps/
├── canvasally/          # Canvasally - Drawing/painting application (Java)
├── datadocs/            # DataDocs - Swing text editor
├── desktopcraft/        # Desktopcraft - Course platform
├── helper/              # Helper - AI chatbot (Java)
├── info/                # Datapro - Analytics dashboard (Java)
├── javagpt/             # JavaGPT - Transformer language model (Java)
├── learner/             # Learner - Learning application
├── memory/              # Memory - Account ledger (Python)
├── projecthub/          # ProjectHub - Project management
├── projyhub/            # ProjyHub - C#/Raylib prototype (UNFINISHED)
├── trestrio/            # Trestrio - Node/Electron workspace
├── utilor/              # Utilor - Utility toolkit (Java)
└── videoforge/          # VideoForge - Video editor (Maven)
```

### `languages/`
Custom programming language implementations.

```
languages/
├── velice/            # Velice - Python-based language
│   ├── velice/        # Interpreter source
│   ├── tests/         # Language tests
│   ├── examples/      # Example programs
│   ├── editor/        # VS Code support
│   └── docs/          # Language documentation
├── vexa/              # Vexa - Java-based language
│   ├── src/           # Interpreter source
│   ├── tests/         # Language tests
│   └── examples/      # Example programs
└── src/               # (unused)
```

### `infrastructure/`
Core build, check, and packaging tooling.

```
infrastructure/
└── repository.py      # Registry-driven build/check/package tool
```

### `utils/`
Java utility classes used by the Datacenter system.

```
utils/
├── Manager.java       # Application discovery and versioning
├── Registry.java      # Application registry
├── Logger.java        # Logging utility
├── Info.java          # Source information reader
├── Updater.java       # Update utility
└── checker.sh         # Checker wrapper
```

### `ids/`
Identity and security system.

```
ids/
├── IdSocketServer.java    # Unix domain socket ID broker
├── IdSocketClient.java    # Client for ID broker
├── IdCheckerTest.java     # Permission tests
├── IdSocketTest.java      # Socket tests
├── start-id-socket.sh     # Launch script
├── DataDocs/              # App identity data
├── Learner/               # App identity data
└── Projecthub/            # App identity data
```

### `scripts/`
Company-wide scripts organized by category.

```
scripts/
├── build/             # Individual app build scripts
│   ├── datadocs.sh
│   ├── learner.sh
│   ├── projecthub.sh
│   ├── trestrio.sh
│   └── ...
├── test/              # Test scripts
│   ├── velice-guide.py
│   ├── website.py
│   └── restore-backup.sh
├── package/           # Packaging scripts
│   └── validate-all.sh
├── build.sh           # Main build dispatcher
├── build-all.sh       # Build all active apps
├── checker.sh         # Repository validation
├── package.sh         # Package dispatcher
├── backup.sh          # Source backup
├── status.sh          # Company status
├── security-audit.sh  # Security checks
├── clean.sh           # Clean build outputs
├── generate-docs.sh   # Documentation generator
├── update.sh          # GitHub updater
├── jar.sh             # JAR creator
├── unbuild.sh         # Undo build
├── v1.0.0.sh          # Legacy v1.0.0 launcher
├── datacenter         # Company management CLI
└── maintenance/       # Maintenance scripts
```

### `config/`
Machine-readable configuration.

```
config/
└── apps.json          # Canonical application registry
```

### `docs/`
Engineering and operating documentation.

```
docs/
├── README.md              # Documentation index
├── ARCHITECTURE.md        # System architecture
├── BUILDING.md            # Build instructions
├── SECURITY.md            # Security policies
├── VERSIONING.md          # Versioning scheme
├── PACKAGING.md           # Packaging guide
├── CONTRIBUTING.md        # Contribution guide
├── APPS.md                # Generated app table
├── RELEASE-CHECKLIST.md   # Release process
├── THIRD_PARTY.md         # Third-party credits
├── REPOSITORY-AUDIT.md    # This audit
├── REPOSITORY-STRUCTURE.md # This document
├── DEVELOPMENT-WORKFLOW.md # Development process
└── REORGANIZATION-REPORT.md # Migration report
```

### `build/`
Generated build output. Never edit directly.

```
build/
├── apps/              # Compiled application classes
├── tests/             # Compiled test classes
└── .data-java-build   # Build marker file
```

### `runtime/`
Generated runtime output.

```
runtime/
├── classes/           # Runtime class files
├── csharp/            # C# runtime output
└── v1.0.0/            # Legacy v1.0.0 build
```

### `releases/`
Validated per-version release packages.

```
releases/
├── DataDocs/
│   └── v1.0.0/        # DataDocs 1.0.0 release
├── Learner/
│   └── v1.0.0/        # Learner 1.0.0 release
├── ProjectHub/
│   └── v1.0.0/        # ProjectHub 1.0.0 release
└── Trestrio/
    └── v1.0.0/        # Trestrio 1.0.0 release
```

### `backups/`
Source backups (generated).

```
backups/
├── automatic/         # Automatic backups
├── manual/            # Manual backups
└── migrations/        # Migration backups
```

### `archives/`
Historical and legacy content.

```
archives/
├── legacy/            # Legacy snapshots
│   ├── github-dev/    # Historical GitHub snapshot
│   ├── github/        # Old README files
│   ├── app-zips/      # Old app ZIPs
│   └── registry/      # Old registry
└── jar/               # JAR archives
```

### `tools/`
Standalone utility programs.

```
tools/
├── fixer/             # Datacenter Fixer utility
└── data-reporter/     # Workspace size reporter
```

### `public/`
Public-facing content and distribution.

```
public/
├── web/               # Static website
│   ├── index.html
│   ├── style.css
│   └── velice.html
└── zip/               # Public distribution ZIPs
    ├── apps/
    └── games/
```

### `database/`
Database files.

```
database/
└── data.sql           # SQL data
```

### `logs/`
Centralized logging.

```
logs/
├── apps/              # Application logs
├── build/             # Build logs
├── system/            # System logs
└── errors/            # Error logs
```

### `.data/`
Internal workspace data.

```
.data/
├── unfinished/        # Isolated incomplete work
│   ├── apps/          # Unfinished app prototypes
│   ├── archives/      # Historical archives
│   └── ...
├── logs/              # System logs (gitignored)
└── resources/         # Internal resources
```

## Files

| File | Purpose |
|---|---|
| `README.md` | Repository overview |
| `LICENSE` | License file |
| `.gitignore` | Git ignore rules |
| `.gitattributes` | Git attributes |
| `.apps.txt` | Current app count |
| `.packageignore` | Packaging exclusions |
| `.backupignore` | Backup exclusions |
| `company-tree.txt` | Full directory tree |
| `company-sizes.txt` | Directory sizes |
| `datacenter` | Company CLI (symlink) |
