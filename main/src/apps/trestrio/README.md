# Trestrio

A calm, full-stack personal utility for tasks, notes, focus sessions, and quick everyday tools.

## Run

Requires Node.js 18 or newer.

```bash
npm start
```

Open http://127.0.0.1:3000. The server binds only to the local machine by default,
and data is saved atomically to `data.json`. Set `TRESTRIO_HOST` only when remote
network access is intentional and protected by an authenticated reverse proxy.

## Desktop app

Install dependencies once, then launch the desktop edition:

```bash
npm install
npm run desktop
```

The desktop app uses the same interface and all 103 utilities. Its tasks, notes,
and focus history are stored in the operating system's app-data folder.

Build an installable app for the current platform with `npm run desktop:build`,
or create an unpacked build with `npm run desktop:pack`.
