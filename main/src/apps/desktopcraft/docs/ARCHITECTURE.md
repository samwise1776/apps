# Desktopcraft architecture

Desktopcraft is one product with three delivery modes that share curriculum and product language while keeping runtime responsibilities separate.

## Browser application

The browser edition is dependency-free ES modules and page-specific scripts. `app.js` owns the primary lesson studio. Feature areas such as authentication, tutorials, forum, projects, customization, review, App Maker, and community publishing live in focused scripts so they can be tested without a browser framework. Curriculum factories in `lessons-extra.js`, `desktop-courses.js`, and `curriculum-expansion.js` generate consistent lesson objects consumed by the tutor and simulator.

The production build copies an explicit allowlist into `dist/`. This prevents server files, configuration, database contents, and development-only material from entering static deployments.

## Database server

`database/server.py` serves the production bundle and a same-origin JSON API backed by SQLite. It owns account credentials, sessions, progress, quiz attempts, leaderboard records, community apps, feedback, and authenticated agent requests. `database/ai_agent.py` owns the OpenAI Responses API contract and a bounded function-tool loop for course retrieval, safe static code analysis, and learning plans. Passwords use scrypt with unique salts. Only hashed session tokens are stored. API responses are non-cacheable and deployment headers prevent MIME confusion, framing, and overly broad browser capabilities.

Static deployments intentionally fall back to local browser storage. That mode is per-browser rather than shared and never presents itself as server-backed identity.

## Swing application

`desktop-app/src/DesktopcraftApp.java` provides the offline Java 17 edition. Curriculum source files are packaged as read-only JAR resources. Java Preferences stores local accounts, settings, progress, projects, and creator content. The `--verify` entry point checks course counts, quizzes, lesson creation, source generation, and credential storage without opening a window.

## Release flow

`npm run publish:check` is the sole release gate. It builds and verifies desktop artifacts, builds the static site, checks links and JavaScript syntax, audits accessibility/security/performance, validates generated curriculum, tests browser persistence features, and runs backend integration tests. GitHub Pages CI runs this complete gate before uploading an artifact.

## Boundaries for future changes

- Add new browser features in a focused script rather than expanding `app.js` unless the behavior is intrinsic to the lesson studio.
- Add backend endpoints through small handler methods and cover every authorization boundary with an integration test.
- Keep generated curriculum deterministic and make every challenge solution pass its checker.
- Never add a public file by copying the entire repository; extend the build allowlist explicitly.
- Keep the Swing `--verify` path graphical-environment independent.
