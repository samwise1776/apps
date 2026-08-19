# Development Workflow

This document describes the standard workflow for developing, testing, building, and releasing software in the Datacenter workspace.

## Workflow Overview

```
Idea
  ↓
Project Setup
  ↓
Development
  ↓
Testing
  ↓
Build
  ↓
Package
  ↓
Release
```

## Detailed Steps

### 1. Idea
- Document the idea in `ideas/ideas.md` or a project-specific location
- Determine if it's an app, game, tool, or library
- Choose the appropriate language and framework

### 2. Project Setup
- Create the project directory under the appropriate location:
  - `apps/` for applications
  - `languages/` for programming languages
  - `tools/` for standalone utilities
- Register the project in `config/apps.json` with a unique ID (format: `DC-XXX-NNN`)
- Create a build script in `scripts/build/`
- Add a README.md to the project directory

### 3. Development
- Write source code in the project directory
- Follow the project's language conventions
- Keep generated files out of version control (see `.gitignore`)
- Use the Datacenter utilities (`utils/`) for common operations

### 4. Testing
- Write tests in the project's `tests/` directory
- Run tests for your project:
  ```bash
  ./datacenter test <project-slug>
  ```
- Run all tests:
  ```bash
  ./datacenter test
  ```

### 5. Build
- Build your project:
  ```bash
  ./datacenter build <project-slug>
  ```
- Build all active projects:
  ```bash
  ./datacenter build
  ```
- Build output goes to `build/apps/<slug>/`

### 6. Package
- Package your project for distribution:
  ```bash
  ./datacenter package <project-slug>
  ```
- Packages are created in `releases/<AppName>/v<version>/`
- Each package includes:
  - Source ZIP
  - Release manifest
  - SHA256 checksum
  - Provenance metadata

### 7. Release
- Update the version in `config/apps.json`
- Run the full validation:
  ```bash
  ./datacenter check
  ```
- Create the release package:
  ```bash
  ./datacenter release <project-slug>
  ```
- The release is stored in `releases/<AppName>/v<version>/`

## Quick Reference

### Common Commands

| Command | Description |
|---|---|
| `./datacenter status` | Show company status |
| `./datacenter apps` | List all applications |
| `./datacenter build` | Build all active apps |
| `./datacenter build <slug>` | Build specific app |
| `./datacenter test` | Run all tests |
| `./datacenter check` | Validate repository |
| `./datacenter package <slug>` | Package app for distribution |
| `./datacenter backup` | Create source backup |
| `./datacenter audit` | Run security audit |
| `./datacenter clean` | Remove build outputs |

### Application States

| State | Description |
|---|---|
| `ACTIVE` | Released and distributable |
| `DEVELOPMENT` | In active development, built and checked |
| `UNFINISHED` | Incomplete, isolated from production |
| `RETIRED` | No longer maintained |
| `ARCHIVED` | Historical, preserved but not active |

### Versioning

- Semantic versioning: `MAJOR.MINOR.PATCH`
- Major: Breaking changes or major features
- Minor: New features, backward compatible
- Patch: Bug fixes, backward compatible

### Code Organization

- Source code goes in the project directory
- Build output goes in `build/`
- Generated runtime goes in `runtime/`
- Release packages go in `releases/`
- Backups go in `backups/`
- Logs go in `logs/`

## Best Practices

1. **Always run `./datacenter check` before committing**
2. **Never commit generated files** (build output, node_modules, __pycache__)
3. **Keep the registry updated** when adding or moving projects
4. **Test before building** — run tests first, then build
5. **Use semantic versions** for releases
6. **Document your changes** in CHANGELOG.md
7. **Keep backups** before major changes
8. **Follow the naming conventions** for IDs and slugs
