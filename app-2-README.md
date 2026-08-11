# App 2 — Raylib 3D Game

**Status:** Runnable development snapshot  
**Download:** [app-2.zip](app-2.zip)  
**Stack:** C#, .NET 10, Raylib-cs 8.0.0

[← All apps](README.md)

App 2 is a compact 3D graphics demo. It opens an 800 × 600 window, renders a beige cube against a sky-blue background, displays a status label and FPS counter, and targets 90 frames per second.

## Requirements

- .NET 10 SDK
- A graphical desktop session
- Internet access for the first NuGet restore
- A graphics environment supported by Raylib

## Build and run

```bash
unzip app-2.zip
dotnet restore 2/apps.csproj
dotnet build 2/apps.csproj
dotnet run --project 2/apps.csproj
```

## Source layout

```text
2/
├── Program.cs       Game loop, camera, cube, and on-screen text
├── apps.csproj      .NET 10 project and Raylib-cs dependency
├── bin/             Existing compiled snapshot
└── obj/             Generated .NET intermediate files
```

The `bin` and `obj` directories are generated and may be removed before a clean rebuild. They make this archive larger than the Java app archives.

## Controls

- Close the game window to exit.
- Camera movement is not enabled in the current snapshot.

## Troubleshooting

- Run `dotnet --version` and confirm it reports version 10 or newer.
- Run `dotnet restore 2/apps.csproj` if `Raylib_cs` cannot be resolved.
- Launch from a desktop session if window or display initialization fails.
