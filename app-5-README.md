# App 5 — Datapro Analytics

**Status:** Runnable development snapshot  
**Download:** [app-5.zip](app-5.zip)  
**Stack:** Java Swing

[← All apps](README.md)

Datapro is a live analytics dashboard for the local Datacenter workspace. It scans in the background and presents file, storage, category, and system information without freezing the interface.

## Features

- Live workspace scanning approximately every two seconds
- Summary metric cards
- Searchable file tracker table
- File-category and storage-category charts
- Basic processor, memory, and filesystem information
- Scan status and last-updated time
- Graceful background-worker shutdown when the window closes

## Requirements

- A current Java Development Kit; tested with OpenJDK 26
- A graphical desktop session
- The Datacenter workspace at `~/Data`

## Build and run

```bash
unzip app-5.zip
mkdir -p build
javac -d build 5/App1.java
java -cp build App1
```

## What it scans

Datapro uses `~/Data` as its workspace root. Results become more useful as that directory gains applications, documentation, logs, builds, and version metadata.

## Troubleshooting

- If the dashboard is empty, confirm `/home/<user>/Data` exists and contains readable files.
- If a directory is missing from results, check filesystem read permissions.
- If metrics refresh slowly, large generated directories may be increasing scan time.
- Close the window normally so the scheduled scanner can shut down cleanly.
