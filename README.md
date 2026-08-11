# Datacenter Apps

[![Java](https://img.shields.io/badge/Java-26-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![.NET](https://img.shields.io/badge/.NET-10-512BD4?logo=dotnet&logoColor=white)](https://dotnet.microsoft.com/)
[![Apps](https://img.shields.io/badge/apps-5-22c55e)](#app-catalog)

Five independently packaged desktop experiments from the **Datacenter** project. Download one ZIP, extract it, and build only the app you want—no monorepo setup required.

> These are development snapshots. App 3 is intentionally a placeholder; the other four apps are runnable in a graphical desktop session.

## App catalog

| App | Download | What it does | Stack | Status |
|---|---|---|---|---|
| 1 | [app-1.zip](app-1.zip) | Launches, inspects, repairs, and logs Datacenter apps | Java Swing | Runnable |
| 2 | [app-2.zip](app-2.zip) | Renders a 3D cube with an FPS display | C#, .NET 10, Raylib-cs | Runnable |
| 3 | [app-3.zip](app-3.zip) | Starting point for a Java game | Java | Placeholder |
| 4 | [app-4.zip](app-4.zip) | Creates and saves styled text documents | Java Swing | Runnable |
| 5 | [app-5.zip](app-5.zip) | Displays live workspace and system analytics | Java Swing | Runnable |

Detailed guides: [App 1](app-1-README.md) · [App 2](app-2-README.md) · [App 3](app-3-README.md) · [App 4](app-4-README.md) · [App 5](app-5-README.md)

## Quick start

1. Download an archive from the table.
2. Extract it into a new working directory.
3. Open that app's guide and run its build commands.

Example for App 4:

```bash
unzip app-4.zip
mkdir -p build
javac -d build 4/DataDocs.java
java -cp build DataDocs
```

## Requirements

| Apps | Requirement |
|---|---|
| 1, 3, 4, 5 | A current Java Development Kit; snapshots are tested with OpenJDK 26 |
| 2 | .NET 10 SDK; NuGet access on the first dependency restore |
| 1, 2, 4, 5 | A graphical desktop session |
| 1, 5 | Best results with the full Datacenter workspace at `~/Data` |

Confirm installed tools with:

```bash
java -version
javac -version
dotnet --version
```

## Archive layout

Each archive preserves its numbered source folder:

```text
app-1.zip  →  1/Info.java
app-2.zip  →  2/Program.cs, 2/apps.csproj, build output
app-3.zip  →  3/Game.java
app-4.zip  →  4/DataDocs.java
app-5.zip  →  5/App1.java
```

App 2 is larger because its snapshot includes existing .NET and native Raylib build artifacts for several platforms.

## Troubleshooting

- **`javac: command not found`** — install a JDK, not only a Java runtime.
- **No window appears** — launch from an active desktop session; SSH/headless sessions need display forwarding.
- **Raylib package errors** — run `dotnet restore 2/apps.csproj` with internet access.
- **App 1 or App 5 shows an empty workspace** — place the full project at `~/Data` or create the expected folders.
- **App 3 will not launch** — it has no `main` method yet; it is a clean placeholder by design.

## Project notes

- Versions are packaged snapshots rather than formal releases.
- Generated files may be rebuilt with the documented Java or .NET commands.
- Report reproducible problems with the app number, operating system, tool version, command, and complete error output.

## License

No license file has been added yet. Add one before accepting outside contributions or redistributing the source under specific terms.
