# App 1 — Datacenter App Console

**Status:** Runnable development snapshot  
**Download:** [app-1.zip](app-1.zip)  
**Stack:** Java Swing

[← All apps](README.md)

App 1 contains `Info.java`, the desktop control center for the Datacenter workspace. It combines application discovery, source inspection, compilation/repair tools, launching, and log viewing in one interface.

## Features

- Discovers applications under `~/Data/apps`
- Previews readable application files
- Compiles and repairs Java applications
- Checks C# project files and build results
- Launches supported applications
- Displays application and system logs
- Reports basic Java, operating-system, memory, and process information

## Requirements

- A current Java Development Kit; tested with OpenJDK 26
- A graphical desktop session
- The Datacenter workspace at `~/Data`
- .NET SDK if App 1 will inspect or repair C# applications

## Build and run

```bash
unzip app-1.zip
mkdir -p build
javac -d build 1/Info.java
java -cp build apps.Info
```

## Expected workspace

```text
~/Data/
├── apps/
├── logs/
└── runtime/
```

The app creates required working directories when possible.

## Troubleshooting

- If compilation is unavailable, verify `javac -version` works.
- If an app fails to launch, inspect `~/Data/logs/system.log` and the app-specific log.
- If no apps appear, confirm they are stored directly beneath `~/Data/apps`.
