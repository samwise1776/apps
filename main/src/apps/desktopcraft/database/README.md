# Desktopcraft database

Desktopcraft uses SQLite through Python's standard library. No database package needs to be installed.

The schema stores users, salted password hashes, sessions, per-course progress, exact lesson completions, 20-question quiz attempts, custom-lesson records, forum records, and free community source apps. The current API connects accounts, sessions, course progress, quiz attempts, XP, the shared leaderboard, app publishing, and free source downloads.

From the project root:

```bash
npm run db:init
npm run build
npm start
```

Useful environment variables:

- `DESKTOPCRAFT_DB_PATH` — SQLite file path; defaults to `database/desktopcraft.db`
- `DESKTOPCRAFT_SITE_ROOT` — built website directory; defaults to `dist`
- `HOST` and `PORT` — listening address and port
- `DESKTOPCRAFT_SECURE_COOKIES=true` — require HTTPS for the session cookie in production
- `OPENAI_API_KEY` — enables server-side Helper AI for signed-in learners; never place this value in browser JavaScript
- `OPENAI_HELPER_MODEL` — optional Responses API model override; defaults to `gpt-5.2`
- `DESKTOPCRAFT_FEEDBACK_TO` — email address that receives feedback; use commas for multiple recipients
- `DESKTOPCRAFT_SMTP_HOST`, `DESKTOPCRAFT_SMTP_PORT`, `DESKTOPCRAFT_SMTP_USER`, and `DESKTOPCRAFT_SMTP_PASSWORD` — SMTP delivery settings
- `DESKTOPCRAFT_SMTP_FROM` — sender address used for feedback email
- `DESKTOPCRAFT_SMTP_SECURITY` — `starttls` (default), `ssl`, or `none`
- `DESKTOPCRAFT_FEEDBACK_FILE` — feedback log path; defaults to `texta.txt` in the project root

For local development, these settings can be placed in the ignored project-root `.env` file; see `.env.example`. Real environment variables take precedence.

Back up the database by stopping the server and copying the `.db` file. Docker deployments should mount persistent storage at `/data`.
