# Building

Prerequisites: Bash, Python 3, Java 17+, .NET 10 SDK, and Node 18+ with npm.

## Quick build

```bash
./datacenter build               # build all active/development apps
./datacenter build datadocs      # build a specific app
```

## Individual build scripts

Each registered application with a `build_script` field has a script under `scripts/build/`. These scripts delegate to `scripts/build.sh`, which calls `infrastructure/repository.py build <slug>`, which runs the `build_command` from `config/apps.json`.

## Build output

Build output belongs in `build/` and is never authoritative source. Use `./datacenter clean` to remove generated outputs.

## Dependencies

- **Java apps**: JDK 17+ (javac and java in PATH)
- **Trestrio**: `npm --prefix apps/trestrio ci`
- **Velice**: Python 3 with standard library only
- **VideoForge**: Maven (`mvn compile`)
- **Desktopcraft**: Node.js (`npm run build`)
- **ProjyHub**: .NET 10 SDK (`dotnet build`)

## Security

`./datacenter audit` performs the security gate. The workflow in `.github/workflows/quality.yml` recreates dependencies, runs that audit, runs the complete checker, and verifies that generated application documentation is committed. Releases must be produced from a clean, reviewed commit after this workflow passes.
