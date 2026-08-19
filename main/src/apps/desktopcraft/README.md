# Desktopcraft

An interactive, dependency-free desktop application tutor with **2,500 guided lessons across five courses**. Lessons use short, plain-language teaching text while keeping real API names. Every lesson includes an editable example, browser-based UI simulation, an automatically checked coding challenge, a guided 20-question quiz with answer explanations, and a complete reference covering properties, functions, events, execution flow, vocabulary, and every starter-code line. The site also provides curriculum search, independent course progress, and an XP leaderboard.

The website also includes a five-language quick-start tutorial hub with searchable topics, editable runnable examples, simulator output, code copy/reset controls, and locally saved completion.

The Builder Forum provides login-aware discussions with categories, search, sorting, replies, likes, optional code samples, and profanity-safe posting. Forum drafts currently stay in the browser, and only discussions created by real signed-in accounts are shown.

Desktopcraft includes account creation and sign-in with a Desktopcraft username and a separate Desktopcraft-only password—never an email password. When the site runs with its included SQLite server, accounts, secure sessions, lesson completions, quiz attempts, XP, and leaderboard standings are stored in the database. Passwords are salted and hashed before storage. Account creation is automatic and does not create or download an account file.

Returning learners are signed in automatically from a valid server session or the last account remembered by that browser. Choosing Sign out disables automatic sign-in until the learner signs in again.

A static upload also remains supported. Without the API server, the website automatically stores account records in browser Web Storage, keeping up to 100 records in primary storage and sending later records to IndexedDB overflow storage. Only password verifiers are stored; readable passwords are never saved.

The website and Swing app both include a lesson studio. Creators supply a lesson name, starter code, answer, and expected output; published lessons join the selected local course. A clearly disclosed profanity rule rejects a violating submission, erases its draft and all locally created lessons, and permanently disables creation for that browser/app profile.

App Maker is also available on the website and in the Swing edition. It turns a small visual blueprint into starter source for Java Swing, Python Tkinter, C# WinForms, C++ Qt Widgets, or JavaScript Electron, with an interactive preview plus copy/download or save-source actions. The Swing edition also includes native Tutorials, Builder Forum, Helper AI course guide, and Customization screens so the complete website learning workflow is available offline.

The Community App Library lets signed-in builders publish editable source apps from `make.html`. Everyone can browse other builders' work in `view.html` and download it without signing in. Community apps are source-only and permanently free: the API has no paid listing mode, ignores client-supplied prices, and always reports a price of zero. Published apps and download counts are stored in SQLite when the database server is running.

The Customize page saves a site-wide theme color, brightness level, and concise, balanced, or detailed lesson explanation preference in browser storage. Detailed mode automatically opens the per-line starter-code guide, while concise mode keeps the lesson focused on essential guidance.

Desktopcraft Agent is a real tool-using AI coding tutor built on OpenAI's Responses API. It can consult course references, statically inspect pasted code without executing it, and create course-specific learning plans through a bounded server-side tool loop. Conversation context, the selected course, and the learner's explanation preference carry into each answer. The API key remains server-side. Static hosting and unconfigured servers automatically use the clearly labeled built-in course guide instead.

Projects provides a persistent browser workspace for real source folders. It recognizes 175 programming and project-file languages, detects each open file automatically, supplies starter code for popular languages, and reports project-wide file, language, line, and size statistics. Its safe workspace terminal includes project-scoped discovery, search, inspection, navigation, and file-creation commands without exposing the computer's operating-system shell. The Add File button opens a folder chooser, imports supported text and code files with their paths intact, and lets the learner edit, save back to an authorized folder, export the whole workspace with its history, or download a copy. Each file has explicit version snapshots, comparison selection, line-level added/removed/changed tracking, one-click restoration, copy and comment actions, and keyboard shortcuts.

## Run locally

Create and verify a production build:

```bash
npm run publish:check
```

This is the release gate, not merely a build command. It compiles and verifies the Swing app, creates the public site, validates every internal link and script, enforces accessibility and performance budgets, checks deployment security headers, exercises browser persistence features, and runs the database API integration suite.

For a verification of an existing build, use `npm run test:all`. For individual areas, use `npm run test:quality`, `npm run test:desktop`, or any focused test command in `package.json`.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for subsystem boundaries and [`docs/QUALITY.md`](docs/QUALITY.md) for the enforced release standard and manual release checklist.

Preview the resulting site:

```bash
npm run preview
```

Then visit `http://localhost:4173`. The deployable site is written to `dist/` and contains only public website files, assets, and the downloadable desktop packages.

## Run with the database

Initialize SQLite, build the site, and start the database-backed server:

```bash
npm run db:init
npm run build
npm start
```

Then visit `http://localhost:8000`. The default database file is `database/desktopcraft.db`. Set `DESKTOPCRAFT_DB_PATH` to choose another location. The server uses same-origin, HTTP-only session cookies and does not expose password hashes to the browser.

Open `http://localhost:8000/make.html` to publish an app and `http://localhost:8000/view.html` to browse or download community apps. Publishing requires a signed-in Desktopcraft account; browsing and free downloads do not.

### Feedback email

`feedback.html` sends each message to the server. The server appends it to the private `texta.txt` file and sends the same message to the configured owner address through SMTP. Copy `.env.example` to `.env`, then replace the example recipient and SMTP values before starting the server:

```bash
cp .env.example .env
npm run build
npm start
```

Use an app-specific SMTP password when your email provider supports one; do not put your normal email password in browser code. The `.env` and `texta.txt` files are ignored by Git, Docker builds, and the public site build. In Docker, the feedback log is stored at `/data/texta.txt` with the database.

Run the database integration test with:

```bash
npm run test:database
```

## Publish

For shared accounts and rankings, deploy the included `Dockerfile` and mount persistent storage at `/data`. The container serves the site and API on port `8000`, with SQLite stored at `/data/desktopcraft.db`.

Netlify and Vercel configuration files are also included for a static-only edition. Import the repository into either service and its build system will run `npm run build` and publish `dist/`. On static hosting, Desktopcraft uses its browser-storage fallback, so accounts and rankings are limited to that browser.

## Courses

- Java Swing — 500 lessons
- Python Tkinter — 500 lessons
- C# Windows Forms — 500 lessons
- C++ Qt Widgets — 500 lessons
- JavaScript Electron — 500 lessons

## Simulator coverage

The local simulator reads code in the editor and recreates a practical desktop UI subset in the browser:

- Java Swing frames, components, layouts, models, and common listeners
- Tkinter windows, ttk controls, commands, and geometry-managed interfaces
- WinForms controls and Click events, Qt widgets and signals, and Electron DOM controls
- Labels, buttons, text controls, choices, lists, tables, trees, and progress feedback
- Console feedback, lightweight syntax checks, and editable/resettable lesson code

This is a safe teaching interpreter rather than a language runtime, so arbitrary libraries and operating-system APIs are intentionally not executed. Lesson snippets can be copied into the appropriate local development environment for full execution.

## Curriculum

Use the course picker in the sidebar to switch tracks. Each course remembers its own lesson, progress, and edited code. Press `/` anywhere outside the editor to search the current curriculum.

## Leaderboard

Open `leaderboard.html` or use the trophy link in the tutor. Completed lessons award 100 XP. With the SQLite server, rankings are shared by all registered learners. The rankings contain only real Desktopcraft accounts and recorded course completions—there are no sample builders or invented scores.

## Website icon and desktop download

The shared Desktopcraft icon is used as the website favicon and brand mark. The Java Swing edition draws the same mark for its application windows and login dialogs. Download `Desktopcraft-desktop.zip` for Windows, Linux, and macOS installer scripts that copy the app and a launcher to the Desktop. The runnable JAR and source ZIP remain available separately.
# desktopcraft
# desktopcraft
# desktopcraft
# desktopcraftwebsite
