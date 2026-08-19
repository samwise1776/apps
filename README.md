# Datacenter

Datacenter is a private, self-run software workspace containing desktop applications, web utilities, custom programming languages, and the infrastructure used to build, test, package, and recover them. Application identity, status, versions, source paths, and build commands come from one source: [`config/apps.json`](config/apps.json).

## Quick start

```bash
./datacenter status              # company overview
./datacenter apps                # list all applications
./datacenter build               # build all active apps
./datacenter build datadocs      # build a specific app
./datacenter check               # validate repository and builds
./datacenter test                # run all tests
./datacenter test learner        # test a specific app
./datacenter package datadocs    # package for distribution
./datacenter backup              # create verified source backup
./datacenter audit               # run security audit
./datacenter doctor              # diagnose common problems
./datacenter info <slug>         # show app details
```

## Running applications

Every application can be launched via `bin/<name>` or `run/<name>.sh`:

```bash
bin/datalaunch       # Launch the central control center
bin/datadocs         # Launch DataDocs text editor
bin/learner          # Launch Learner
bin/projecthub       # Launch ProjectHub
bin/helper           # Launch Helper AI chatbot
bin/vexa             # Run Vexa interpreter
bin/velice           # Run Velice interpreter
bin/javagpt          # Launch JavaGPT
bin/canvasally       # Launch Canvasally
bin/utilor           # Launch Utilor
bin/datapro          # Launch Datapro analytics
bin/appcenter        # Launch AppCenter installer
bin/datavault        # Launch DataVault backup manager
bin/logscope         # Launch LogScope log viewer
bin/devpulse         # Launch DevPulse health dashboard
bin/testbench        # Launch TestBench test runner
bin/packforge        # Launch PackForge packaging manager
bin/codeshelf        # Launch CodeShelf snippet manager
bin/assetforge       # Launch AssetForge asset manager
bin/filepilot        # Launch FilePilot file utility
bin/datanotes        # Launch DataNotes wiki
bin/memory           # Run Memory ledger (Python)
```

## Applications

30 registered applications. The full generated table is in [`docs/APPS.md`](docs/APPS.md).

### Active

| App | Language | Description |
|---|---|---|
| DataDocs | Java/Swing | Lightweight text editor |
| Learner | Java/Swing | Modular learning application with 10 subjects |
| ProjectHub | Java/Swing | Project, task, bug, and release planning |
| Trestrio | Node/Electron | Calm personal utility workspace |

### New (Company Tools)

## Web Apps

50 free browser apps — open any `.html` file directly, no server or install required.
Browse the full catalog at [`web/index.html`](web/index.html) (served via GitHub Pages).

### Ported from Desktop

| App | Description | Link |
|---|---|---|
| DataDocs | Document editor with font picker and save/print | [`web/datadocs.html`](web/datadocs.html) |
| DataNotes | Rich notebook with tree, tags, search | [`web/datanotes.html`](web/datanotes.html) |
| Learner | 10,000 lessons, quizzes, achievements | [`web/learner.html`](web/learner.html) |
| ProjectHub | Project, task, bug, release tracker | [`web/projecthub.html`](web/projecthub.html) |
| Trestrio | Tasks, notes, focus timer, 100+ utilities | [`web/trestrio.html`](web/trestrio.html) |
| CodeShelf | Code snippet manager | [`web/codeshelf.html`](web/codeshelf.html) |
| LogScope | Log viewer with filtering and regex | [`web/logscope.html`](web/logscope.html) |
| TestBench | Visual test runner | [`web/testbench.html`](web/testbench.html) |
| DevPulse | Developer health dashboard | [`web/devpulse.html`](web/devpulse.html) |
| Helper | Nova AI chat assistant | [`web/helper.html`](web/helper.html) |
| CanvasAlly | Drawing and painting app | [`web/canvasally.html`](web/canvasally.html) |
| AssetForge | Asset browser with tags | [`web/assetforge.html`](web/assetforge.html) |
| FilePilot | Virtual file manager | [`web/filepilot.html`](web/filepilot.html) |
| DataLaunch | App launcher | [`web/datalaunch.html`](web/datalaunch.html) |
| DataVault | Backup manager | [`web/datavault.html`](web/datavault.html) |
| PackForge | Release & packaging manager | [`web/packforge.html`](web/packforge.html) |
| Utilor | Calculator, password gen, app gen | [`web/utilor.html`](web/utilor.html) |
| JavaGPT | Language model demo | [`web/javagpt.html`](web/javagpt.html) |
| DesktopCraft | Desktop app tutor | [`web/desktopcraft.html`](web/desktopcraft.html) |
| VideoForge | Video editor studio | [`web/videoforge.html`](web/videoforge.html) |

### New Web Apps

| App | Description | Link |
|---|---|---|
| ScrapZone (Web) | 5-stage browser game | [`web/game.html`](web/game.html) |
| Weatherly | Weather dashboard | [`web/weatherly.html`](web/weatherly.html) |
| BudgetFlow | Personal finance tracker | [`web/budgetflow.html`](web/budgetflow.html) |
| HabitForge | Habit tracker with streaks | [`web/habitforge.html`](web/habitforge.html) |
| FlashLearn | Flashcard study app | [`web/flashlearn.html`](web/flashlearn.html) |
| TaskFlow | Kanban board | [`web/taskflow.html`](web/taskflow.html) |
| NoteCraft | Markdown note editor | [`web/notecraft.html`](web/notecraft.html) |
| TimeBlock | Weekly planner | [`web/timeblock.html`](web/timeblock.html) |
| PomoFocus | Pomodoro timer | [`web/pomofocus.html`](web/pomofocus.html) |
| Playlistly | Music playlist player | [`web/playlistly.html`](web/playlistly.html) |
| RecipeBox | Recipe manager | [`web/recipebox.html`](web/recipebox.html) |
| ColorPal | Color palette generator | [`web/colorpal.html`](web/colorpal.html) |
| MarkdownView | Markdown renderer | [`web/markdownview.html`](web/markdownview.html) |
| Notepad | Simple notepad | [`web/notepad.html`](web/notepad.html) |
| CalorieCounter | Calorie counter | [`web/caloriecounter.html`](web/caloriecounter.html) |
| PasswordVault | Password manager | [`web/passwordvault.html`](web/passwordvault.html) |
| Whiteboard | Infinite whiteboard | [`web/whiteboard.html`](web/whiteboard.html) |
| JSONBuddy | JSON editor | [`web/jsonbuddy.html`](web/jsonbuddy.html) |
| SprintLog | Sprint planning board | [`web/sprintlog.html`](web/sprintlog.html) |
| MindMap | Mind map builder | [`web/mindmap.html`](web/mindmap.html) |
| MusicPlayer | Music player | [`web/musicplayer.html`](web/musicplayer.html) |
| Budget | Budget calculator | [`web/budget.html`](web/budget.html) |
| FitnessLog | Workout tracker | [`web/fitnesslog.html`](web/fitnesslog.html) |
| BookShelf | Reading tracker | [`web/bookshelf.html`](web/bookshelf.html) |
| ContactBook | Contact book | [`web/contactbook.html`](web/contactbook.html) |
| Stopwatch | Advanced stopwatch | [`web/stopwatch.html`](web/stopwatch.html) |
| Trivia | Trivia quiz game | [`web/trivia.html`](web/trivia.html) |
| DailyLog | Daily journal | [`web/dailylog.html`](web/dailylog.html) |
| TypeMaster | Typing speed test | [`web/typemaster.html`](web/typemaster.html) |

| App | Language | Description |
|---|---|---|
| DataLaunch | Java/Swing | Central control center and application launcher |
| DataVault | Java/Swing | Backup and recovery manager with SHA-256 verification |
| LogScope | Java/Swing | Company-wide log viewer with search and statistics |
| DevPulse | Java/Swing | Company health dashboard with real metrics |
| TestBench | Java/Swing | Visual test runner for the entire company |
| PackForge | Java/Swing | Release and packaging manager |
| CodeShelf | Java/Swing | Code snippet manager (9 languages) |
| AssetForge | Java/Swing | Project asset manager with duplicate detection |
| FilePilot | Java/Swing | Safe developer file utility |
| DataNotes | Java/Swing | Local wiki with Markdown and auto-save |

### Development

| App | Language | Description |
|---|---|---|
| Datapro | Java/Swing | Workspace analytics dashboard |
| Helper | Java/Swing | Offline neural-network AI chatbot |
| Memory | Python | Account and download ledger |
| Desktopcraft | HTML/JS/Python | Interactive course platform |
| VideoForge | Java/Maven | Video editor and screen recorder |
| AppCenter | Java | One-click Datacenter installer |
| Velice | Python/Velice | Dynamic programming language with GUI |
| Vexa | Java | Language interpreter |
| Canvasally | Java/Swing | Drawing and painting application |
| JavaGPT | Java | Generative transformer language model |
| Utilor | Java/Swing | Utility toolkit |

## Programming Languages

Custom languages live under `languages/`:

- **Velice** (`languages/velice/`) — Python-based language with interpreter, GUI, 147 tests, examples, and VS Code support
- **Vexa** (`languages/vexa/`) — Java-based language interpreter with examples

## Repository structure

```text
Data/
├── config/            canonical application registry (apps.json)
├── main/src/          all application and language sources
│   ├── apps/          application source code
│   ├── installer/     AppCenter installer
│   ├── info/          Datapro analytics
│   ├── memory/        Memory ledger
│   ├── languages/     Velice and Vexa
│   └── infrastructure/ build/check/package tooling
├── scripts/           build, test, package, backup scripts
├── build/             generated build output (gitignored)
├── releases/          validated per-version packages
├── run/               runnable scripts for every app
├── bin/               symlinks to run/ for quick launch
├── docs/              engineering and operating documentation
├── tools/             standalone utility programs
├── utils/             Java utility classes
├── ids/               identity and security system
├── logs/              centralized logging
├── backups/           source backups (generated)
├── archives/          historical and legacy content
├── datacenter         company management CLI
├── LICENSE            license
└── README.md          this file
```

See [`docs/REPOSITORY-STRUCTURE.md`](docs/REPOSITORY-STRUCTURE.md) for detailed directory descriptions.

## CLI commands

```bash
./datacenter status               # company overview
./datacenter apps                 # list all applications
./datacenter info <slug>          # app details
./datacenter build [slug]         # build apps
./datacenter test [slug]          # run tests
./datacenter check                # validate repository (30+ checks)
./datacenter package [slug]       # package for distribution
./datacenter release [slug]       # create releases
./datacenter backup               # create verified backup
./datacenter backup-check         # verify backup integrity
./datacenter audit                # security audit
./datacenter docs                 # generate documentation
./datacenter clean                # remove build outputs
./datacenter doctor               # diagnose common problems
./datacenter fix                  # run automated repair
```

## Build instructions

```bash
./datacenter build                # build all active/development apps
./datacenter build datadocs       # build a specific app
./datacenter clean                # clean build outputs
```

See [`docs/BUILDING.md`](docs/BUILDING.md) for detailed build instructions.

## Testing

```bash
./datacenter check                # full validation (30+ checks)
./datacenter test                 # run all tests
./datacenter test learner         # run tests for a specific app
```

Tests include Java compilation, Python unit tests, Node.js tests, Velice language tests (147), and website validation.

## Release system

```bash
./datacenter package datadocs     # package a specific app
./datacenter release datadocs     # create a release
```

Packages include source ZIP, release manifest, SHA256 checksum, and provenance metadata. See [`docs/PACKAGING.md`](docs/PACKAGING.md).

## Development workflow

```text
Idea → Project Setup → Development → Testing → Build → Package → Release
```

See [`docs/DEVELOPMENT-WORKFLOW.md`](docs/DEVELOPMENT-WORKFLOW.md) for the full workflow.

## License

Private and proprietary. See [`LICENSE`](LICENSE).
