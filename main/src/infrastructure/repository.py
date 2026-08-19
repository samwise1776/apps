#!/usr/bin/env python3
"""Registry-driven Datacenter build, validation, documentation and packaging tool."""
from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
import re
import shutil
import stat
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path
from typing import Iterable

SRC_ROOT = Path(__file__).resolve().parents[1]
ROOT = Path(__file__).resolve().parents[3]
REGISTRY = ROOT / "config/apps.json"
PRODUCTION = {"ACTIVE", "DEVELOPMENT"}

VALID_STATUSES = {"ACTIVE", "DEVELOPMENT", "UNFINISHED", "RETIRED", "ARCHIVED"}

EXCLUDED_PARTS = {
    "node_modules",
    "dist",
    "build",
    "target",
    "bin",
    "obj",
    ".cache",
    ".gradle",
    ".idea",
    ".vscode",
    ".git",
    "runtime",
    "releases",
    "backups",
    "archives",
    "logs",
}

EXCLUDED_SUFFIXES = {".class", ".jar", ".zip", ".log", ".pyc"}


# ============================================================
# BASIC HELPERS
# ============================================================

def registry() -> list[dict]:
    with REGISTRY.open(encoding="utf-8") as handle:
        data = json.load(handle)
    return data["applications"]


def selected(statuses: set[str] = PRODUCTION) -> list[dict]:
    return [app for app in registry() if app["status"] in statuses]


def app(slug: str) -> dict:
    for item in registry():
        if item["slug"] == slug:
            return item
    raise SystemExit(f"Unknown application: {slug}")


def source_path(item: dict) -> Path:
    return SRC_ROOT / item["source"]


def build_output_path(slug: str) -> Path:
    return ROOT / "build" / "apps" / slug


def test_output_path(slug: str) -> Path:
    return ROOT / "build" / "tests" / slug


def run(command: str, *, capture: bool = False, cwd: Path | None = None) -> subprocess.CompletedProcess[str]:
    """Run a shell command from the repo root directory."""
    rg_fallback = r'''
if ! command -v rg >/dev/null 2>&1; then
    rg() {
        if [ "$1" != "--files" ]; then
            echo "Datacenter rg fallback only supports: rg --files <folder> -g <glob>" >&2
            return 127
        fi

        shift
        local base="."
        local pattern="*"

        while [ "$#" -gt 0 ]; do
            case "$1" in
                -g|--glob)
                    shift
                    if [ "$#" -eq 0 ]; then
                        echo "Datacenter rg fallback: missing glob after -g" >&2
                        return 2
                    fi
                    pattern="$1"
                    ;;
                --*)
                    ;;
                *)
                    base="$1"
                    ;;
            esac
            shift
        done

        find "$base" -type f -name "$pattern" -print
    }
    export -f rg
fi
'''

    wrapped_command = rg_fallback + "\n" + command
    working_dir = cwd or ROOT

    return subprocess.run(
        ["bash", "-lc", wrapped_command],
        cwd=working_dir,
        text=True,
        capture_output=capture,
    )


def clean_output(text: str | None) -> str:
    return (text or "").strip()


def process_error_details(
    result: subprocess.CompletedProcess[str],
    *,
    command: str | None = None,
) -> str:
    parts: list[str] = []
    if command:
        parts.append(f"Command: {command}")
    parts.append(f"Exit code: {result.returncode}")
    stdout = clean_output(result.stdout)
    stderr = clean_output(result.stderr)
    if stdout:
        parts.append("STDOUT:\n" + stdout)
    if stderr:
        parts.append("STDERR:\n" + stderr)
    if not stdout and not stderr:
        parts.append("No stdout/stderr was produced.")
    return "\n".join(parts)


def print_process_output(result: subprocess.CompletedProcess[str]) -> None:
    if result.stdout:
        print(result.stdout.rstrip())
    if result.stderr:
        print(result.stderr.rstrip(), file=sys.stderr)


def print_failure_block(title: str, detail: str) -> None:
    print(f"\n--- ERROR: {title} ---", file=sys.stderr)
    print(detail.rstrip(), file=sys.stderr)
    print("--- END ERROR ---\n", file=sys.stderr)


# ============================================================
# BUILDING
# ============================================================

def build_one(item: dict) -> bool:
    name = item["name"]
    command = item["build_command"]

    if not command:
        print(f"[SKIP] {name}: no build command")
        return True

    print(f"\n== {name} {item['version']} ==", flush=True)

    try:
        result = run(command, capture=True)
    except Exception as exc:
        print(f"[FAIL] {name}")
        print_failure_block(name, f"Could not start build command:\n{exc}")
        return False

    print_process_output(result)

    if result.returncode == 0:
        print(f"[PASS] {name}")
        return True

    print(f"[FAIL] {name}")
    print_failure_block(
        name,
        process_error_details(result, command=command),
    )
    return False


def build(args: argparse.Namespace) -> int:
    items = [app(args.slug)] if args.slug else selected()
    outcomes = [(item, build_one(item)) for item in items]
    passed = sum(ok for _, ok in outcomes)

    print(
        f"\nDatacenter Build: {passed} passed, "
        f"{len(outcomes) - passed} failed"
    )

    return 0 if passed == len(outcomes) else 1


# ============================================================
# TESTING
# ============================================================

def test_one(item: dict) -> bool:
    name = item["name"]
    src_dir = source_path(item)

    if not src_dir.is_dir():
        print(f"[SKIP] {name}: source directory does not exist")
        return True

    if item["language"] == "Java":
        test_dir = src_dir / "tests"
        if test_dir.is_dir():
            test_files = list(test_dir.glob("*Test*.java")) + list(test_dir.glob("*Tests*.java"))
            if test_files:
                output_dir = test_output_path(item["slug"])
                for tf in test_files:
                    class_name = tf.stem
                    ok, detail = run_java_test(
                        label=f"{name} {class_name}",
                        source_dir=src_dir,
                        test_file=tf,
                        test_class=class_name,
                        output_dir=output_dir,
                    )
                    if not ok:
                        print(f"[FAIL] {name}: {class_name}")
                        print(detail, file=sys.stderr)
                        return False
                print(f"[PASS] {name}: all tests passed")
                return True

    if item["language"] in {"Python", "Python/Velice"}:
        test_files = list(src_dir.glob("test_*.py"))
        if test_files:
            result = run(
                f"cd {item['source']} && python3 -m pytest -v",
                capture=True,
                cwd=ROOT,
            )
            if result.returncode == 0:
                print(f"[PASS] {name}: tests passed")
                return True
            else:
                print(f"[FAIL] {name}: tests failed")
                print_process_output(result)
                return False

    if item["language"] == "Node/Electron":
        if (src_dir / "package.json").is_file():
            result = run(
                f"cd {item['source']} && npm test",
                capture=True,
                cwd=ROOT,
            )
            if result.returncode == 0:
                print(f"[PASS] {name}: tests passed")
                return True
            else:
                print(f"[FAIL] {name}: tests failed")
                print_process_output(result)
                return False

    print(f"[SKIP] {name}: no tests found for {item['language']}")
    return True


def test(args: argparse.Namespace) -> int:
    items = [app(args.slug)] if args.slug else selected()
    outcomes = [(item, test_one(item)) for item in items]
    passed = sum(ok for _, ok in outcomes)

    print(
        f"\nDatacenter Test: {passed} passed, "
        f"{len(outcomes) - passed} failed"
    )

    return 0 if passed == len(outcomes) else 1


# ============================================================
# BACKUP CHECK
# ============================================================

def backup_check(args: argparse.Namespace) -> int:
    backups_dir = ROOT / "backups"
    backups = sorted(backups_dir.glob("*.zip"), key=lambda p: p.stat().st_mtime) if backups_dir.is_dir() else []
    if not backups:
        print("No backup found in backups/")
        return 1

    target = backups[-1]
    print(f"Checking backup: {target.name}")

    errors = []

    with tempfile.TemporaryDirectory(prefix="datacenter-backupcheck-") as temp:
        with zipfile.ZipFile(target) as archive:
            archive.extractall(temp)

        base = Path(temp) / "Datacenter"

        if not (base / "config/apps.json").is_file():
            errors.append("config/apps.json not found in backup")
        else:
            with open(base / "config/apps.json") as f:
                data = json.load(f)

            for item in data["applications"]:
                if item["status"] in PRODUCTION:
                    source_path_backup = base / "main/src" / item["source"]
                    if not source_path_backup.is_dir():
                        source_path_legacy = base / item["source"]
                        if not source_path_legacy.is_dir():
                            errors.append(
                                f"{item['name']}: source directory {item['source']} missing from backup"
                            )

        if (base / "build").is_dir():
            errors.append("build/ directory included in backup")
        if (base / "backups").is_dir():
            errors.append("backups/ directory included in backup")
        id_files = list(base.rglob(".id.txt"))
        if id_files:
            errors.append(f"{len(id_files)} secret ID file(s) included in backup")
        if (base / "logs").is_dir():
            errors.append("logs/ directory included in backup")

    if errors:
        print("BACKUP CHECK FAILED:")
        for e in errors:
            print(f"  - {e}")
        return 1

    print("Backup check passed: all registered apps present, no generated/secret files included")
    return 0


# ============================================================
# SOURCE / PACKAGING RULES
# ============================================================

def ignored(path: Path, backup: bool = False) -> bool:
    try:
        rel = path.relative_to(ROOT)
    except ValueError:
        return False

    if any(part in EXCLUDED_PARTS for part in rel.parts):
        return True
    if path.suffix.lower() in EXCLUDED_SUFFIXES:
        return True
    if path.name in {".apps.txt", "apps.zip", "core", "datacenter"}:
        return True
    if rel.parts[:2] == (".data", "logs"):
        return True
    if rel.parts[:2] == (".data", "unfinished") and backup:
        return True
    if path.name in {".id.txt", ".id-broker.sock"}:
        return True
    return False


def source_files(base: Path = ROOT, backup: bool = False) -> Iterable[Path]:
    for directory, names, files in os.walk(base):
        current = Path(directory)
        names[:] = [
            name
            for name in names
            if not ignored(current / name, backup)
        ]
        for name in files:
            path = current / name
            if not ignored(path, backup):
                yield path


def manifest(item: dict, status: str = "PASS") -> str:
    stamp = dt.datetime.now().astimezone().isoformat(timespec="seconds")
    commit = subprocess.run(
        ["git", "rev-parse", "--short", "HEAD"],
        cwd=ROOT,
        text=True,
        capture_output=True,
    ).stdout.strip() or "unversioned"

    return (
        f"App: {item['name']}\n"
        f"Version: {item['version']}\n"
        f"Build date: {stamp}\n"
        f"Language: {item['language']}\n"
        f"Build ID: {commit}\n"
        f"Package status: {status}\n"
    )


def package_one(item: dict) -> Path | None:
    release = ROOT / "releases" / item["name"] / ("v" + item["version"])
    release.mkdir(parents=True, exist_ok=True)

    target = release / f"{item['slug']}-{item['version']}-source.zip"
    src = source_path(item)

    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as archive:
        for path in source_files(src):
            archive.write(
                path,
                Path(item["source"]) / path.relative_to(src),
            )
        archive.writestr("RELEASE-MANIFEST.txt", manifest(item))

    ok = validate_archive(target, item)

    if not ok:
        target.unlink(missing_ok=True)
        return None

    digest = hashlib.sha256(target.read_bytes()).hexdigest()

    target.with_suffix(target.suffix + ".sha256").write_text(
        f"{digest}  {target.name}\n",
        encoding="utf-8",
    )

    provenance = {
        "schema_version": 1,
        "application": item["name"],
        "version": item["version"],
        "artifact": target.name,
        "sha256": digest,
        "built_at": dt.datetime.now()
        .astimezone()
        .isoformat(timespec="seconds"),
        "source_revision": subprocess.run(
            ["git", "rev-parse", "HEAD"],
            cwd=ROOT,
            text=True,
            capture_output=True,
        ).stdout.strip()
        or "unversioned",
        "builder": "infrastructure/repository.py",
        "language": item["language"],
    }

    target.with_suffix(target.suffix + ".provenance.json").write_text(
        json.dumps(provenance, indent=2) + "\n",
        encoding="utf-8",
    )

    return target


def validate_archive(path: Path, item: dict) -> bool:
    try:
        with tempfile.TemporaryDirectory(prefix="datacenter-package-") as temp:
            with zipfile.ZipFile(path) as archive:
                names = archive.namelist()
                if any(
                    any(part in EXCLUDED_PARTS for part in Path(name).parts)
                    for name in names
                ):
                    raise RuntimeError("archive contains generated dependencies/output")
                archive.extractall(temp)

            base = Path(temp)
            source = base / item["source"]

            if item["language"] == "Java":
                files = list(source.rglob("*.java"))
                if not files:
                    raise RuntimeError("no Java sources")
                out = base / "classes"
                out.mkdir()
                result = subprocess.run(
                    ["javac", "-d", str(out), *[str(x) for x in files]],
                    text=True,
                    capture_output=True,
                )
                if result.returncode:
                    raise RuntimeError(
                        process_error_details(result, command="javac <all package Java sources>")
                    )
                if not any(out.rglob("*.class")):
                    raise RuntimeError("compiler produced no classes")

            elif item["language"] == "Node/Electron":
                result = subprocess.run(
                    ["npm", "test"],
                    cwd=source,
                    text=True,
                    capture_output=True,
                )
                if result.returncode:
                    raise RuntimeError(process_error_details(result, command="npm test"))

            print(f"[PASS] Package {item['name']}: extracted and validated")
            return True

    except Exception as exc:
        print(f"[FAIL] Package {item['name']}: {exc}", file=sys.stderr)
        return False


def package(args: argparse.Namespace) -> int:
    items = (
        [app(args.slug)]
        if args.slug
        else [x for x in selected({"ACTIVE"}) if x["distributable"]]
    )

    results = [(x, package_one(x)) for x in items]
    return 0 if all(path for _, path in results) else 1


# ============================================================
# BACKUP
# ============================================================

def backup(args: argparse.Namespace) -> int:
    output = ROOT / "backups"
    output.mkdir(exist_ok=True)

    stamp = dt.datetime.now().strftime("%Y-%m-%d-%H%M%S")
    target = output / f"Datacenter-source-{stamp}.zip"
    files = list(source_files(backup=True))

    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as archive:
        for path in files:
            archive.write(
                path,
                Path("Datacenter") / path.relative_to(ROOT),
            )

    with zipfile.ZipFile(target) as archive:
        bad = archive.testzip()

    if bad:
        target.unlink()
        print(f"Backup integrity failed at {bad}", file=sys.stderr)
        return 1

    print(
        "Backup completed\n"
        f"Path: {target}\n"
        f"Files: {len(files):,}\n"
        f"Size: {target.stat().st_size / 1024 / 1024:.2f} MB\n"
        "Integrity: PASS"
    )

    return 0


# ============================================================
# DOCUMENTATION
# ============================================================

def generate_docs() -> None:
    lines = [
        "<!-- BEGIN GENERATED APPS -->",
        "| App | Version | Status | Language | Source |",
        "|---|---:|---|---|---|",
    ]

    for item in registry():
        lines.append(
            f"| {item['name']} | {item['version']} | {item['status']} | "
            f"{item['language']} | `{item['source']}` |"
        )

    lines.append("<!-- END GENERATED APPS -->")

    path = ROOT / "docs/APPS.md"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        "# Application Registry\n\n"
        "Generated from `config/apps.json`. Do not edit the table by hand.\n\n"
        + "\n".join(lines)
        + "\n",
        encoding="utf-8",
    )


# ============================================================
# CHECK HELPERS
# ============================================================

def duplicate_values(values: list[str]) -> list[str]:
    seen: set[str] = set()
    duplicates: set[str] = set()
    for value in values:
        if value in seen:
            duplicates.add(value)
        seen.add(value)
    return sorted(duplicates)


def mode_string(path: Path) -> str:
    return oct(stat.S_IMODE(path.stat().st_mode))


def repair_private_id_permissions(ids: list[Path]) -> tuple[list[str], list[str]]:
    fixed: list[str] = []
    failed: list[str] = []

    if os.name != "posix":
        return fixed, failed

    for path in ids:
        try:
            current = stat.S_IMODE(path.stat().st_mode)
            if current != 0o600:
                path.chmod(0o600)
                fixed.append(
                    f"{path.relative_to(ROOT)}: {oct(current)} -> 0o600"
                )
        except OSError as exc:
            failed.append(f"{path}: {exc}")

    return fixed, failed


def java_sources(source_dir: Path) -> list[Path]:
    if not source_dir.is_dir():
        return []
    return sorted(source_dir.rglob("*.java"))


def run_java_test(
    *,
    label: str,
    source_dir: Path,
    test_file: Path,
    test_class: str,
    output_dir: Path,
) -> tuple[bool, str]:
    sources = java_sources(source_dir)

    if not source_dir.is_dir():
        return False, f"Source directory does not exist: {source_dir.relative_to(ROOT)}"

    if not sources:
        return False, f"No Java source files found under: {source_dir.relative_to(ROOT)}"

    if not test_file.is_file():
        return False, f"Test file does not exist: {test_file.relative_to(ROOT)}"

    javac = shutil.which("javac")
    java = shutil.which("java")

    if not javac:
        return False, "javac was not found in PATH. Install/configure a JDK."
    if not java:
        return False, "java was not found in PATH. Install/configure a JDK."

    try:
        if output_dir.exists():
            shutil.rmtree(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
    except OSError as exc:
        return False, f"Could not prepare test output directory: {exc}"

    compile_command = [
        javac,
        "-d",
        str(output_dir),
        *[str(path) for path in sources],
        str(test_file),
    ]

    compile_result = subprocess.run(
        compile_command,
        cwd=ROOT,
        text=True,
        capture_output=True,
    )

    if compile_result.returncode != 0:
        return False, (
            f"{label} compile failed.\n"
            + process_error_details(compile_result, command=" ".join(compile_command))
        )

    run_command = [
        java,
        "-ea",
        "-cp",
        str(output_dir),
        test_class,
    ]

    test_result = subprocess.run(
        run_command,
        cwd=ROOT,
        text=True,
        capture_output=True,
    )

    if test_result.returncode != 0:
        return False, (
            f"{label} execution failed.\n"
            + process_error_details(test_result, command=" ".join(run_command))
        )

    success_output = clean_output(test_result.stdout or test_result.stderr)
    return True, success_output


# ============================================================
# DOCTOR
# ============================================================

def doctor(args: argparse.Namespace) -> int:
    """Diagnose common Datacenter problems and suggest fixes."""
    problems = []

    print("DATACENTER DOCTOR\n")

    # Check Java
    javac = shutil.which("javac")
    java = shutil.which("java")
    if not javac:
        problems.append(("CRITICAL", "javac not found", "Install a JDK (e.g. sudo apt install openjdk-21-jdk)"))
    if not java:
        problems.append(("CRITICAL", "java not found", "Install a JDK (e.g. sudo apt install openjdk-21-jdk)"))

    # Check Python3
    python3 = shutil.which("python3")
    if not python3:
        problems.append(("CRITICAL", "python3 not found", "Install Python 3 (e.g. sudo apt install python3)"))

    # Check registry
    if not REGISTRY.is_file():
        problems.append(("CRITICAL", "config/apps.json missing", "Restore from backup or recreate"))
    else:
        try:
            apps = registry()
            print(f"[OK] Registry: {len(apps)} applications registered")
        except Exception as exc:
            problems.append(("CRITICAL", f"Registry parse error: {exc}", "Check config/apps.json syntax"))

    # Check source directories
    if SRC_ROOT.is_dir():
        print(f"[OK] Source root: {SRC_ROOT}")
    else:
        problems.append(("CRITICAL", f"Source root missing: {SRC_ROOT}", "Restore source directory"))

    # Check build directory
    build_dir = ROOT / "build"
    if build_dir.is_dir():
        print(f"[OK] Build directory: {build_dir}")
    else:
        problems.append(("WARN", "Build directory missing", "Run: ./datacenter build"))

    # Check scripts
    for script_name in ["build.sh", "checker.sh", "clean.sh", "package.sh", "security-audit.sh"]:
        script_path = ROOT / "scripts" / script_name
        if not script_path.is_file():
            problems.append(("WARN", f"Script missing: scripts/{script_name}", "Restore from backup"))

    # Check datacenter CLI
    cli = ROOT / "datacenter"
    if not cli.exists():
        problems.append(("CRITICAL", "datacenter CLI symlink missing", "Create symlink to scripts/datacenter"))
    elif not os.access(cli, os.X_OK):
        problems.append(("WARN", "datacenter CLI not executable", "Run: chmod +x datacenter"))

    # Check for stale build artifacts
    for app_dir in (ROOT / "build" / "apps").glob("*") if (ROOT / "build" / "apps").is_dir() else []:
        if not any(app_dir.iterdir()):
            problems.append(("WARN", f"Empty build output: build/apps/{app_dir.name}", "Rebuild or clean"))

    # Check for duplicate doc directories
    root_docs = ROOT / "docs"
    src_docs = SRC_ROOT / "docs"
    if root_docs.is_dir() and src_docs.is_dir():
        root_files = set(f.name for f in root_docs.iterdir() if f.is_file())
        src_files = set(f.name for f in src_docs.iterdir() if f.is_file())
        common = root_files & src_files
        if common:
            problems.append(("WARN", f"Duplicate docs in docs/ and main/src/docs/ ({len(common)} files)", "main/src/docs/ should be removed or synced"))

    # Check config consistency
    if REGISTRY.is_file():
        try:
            apps = registry()
            for item in apps:
                src_dir = source_path(item)
                if item["status"] in PRODUCTION and not src_dir.is_dir():
                    problems.append(("WARN", f"{item['name']}: source dir missing ({item['source']})", "Create directory or update apps.json"))
                if item["build_script"] and not (ROOT / item["build_script"]).is_file():
                    problems.append(("WARN", f"{item['name']}: build script missing ({item['build_script']})", "Create build script"))
        except Exception:
            pass

    # Summary
    if not problems:
        print("\nNo problems detected. Datacenter is healthy.")
        return 0

    critical = [p for p in problems if p[0] == "CRITICAL"]
    warnings = [p for p in problems if p[0] == "WARN"]

    print(f"\nFound {len(critical)} critical issue(s) and {len(warnings)} warning(s):\n")
    for level, problem, fix in problems:
        print(f"  [{level}] {problem}")
        print(f"         Fix: {fix}")

    return 1 if critical else 0


# ============================================================
# CHECKER
# ============================================================

def checks() -> int:
    results: list[bool] = []

    def check(name: str, condition: bool, detail: str = "") -> bool:
        ok = bool(condition)
        results.append(ok)
        print(f"[{'PASS' if ok else 'FAIL'}] {name}")
        if detail:
            prefix = "INFO" if ok else "ERROR"
            print(f"    {prefix}: {detail.replace(chr(10), chr(10) + '    ')}")
        return ok

    print("DATACENTER CHECKER\n")

    # STRUCTURE
    print("STRUCTURE")

    try:
        apps = registry()
        check("App registry parses", True)
    except Exception as exc:
        check("App registry parses", False, str(exc))
        print("\nRESULT\n0/1 checks passed\nDATACENTER HEALTH: 0%")
        return 1

    required = {
        "id", "slug", "name", "version", "status", "language",
        "source", "build_script", "build_command", "main",
        "visibility", "distributable", "description",
    }

    missing_fields: list[str] = []
    for item in apps:
        missing = sorted(required - set(item))
        if missing:
            missing_fields.append(
                f"{item.get('name', item.get('slug', '<unknown>'))}: {', '.join(missing)}"
            )

    check("Registry fields", not missing_fields, "; ".join(missing_fields))

    duplicate_ids = duplicate_values([x["id"] for x in apps])
    check("Unique IDs", not duplicate_ids, ", ".join(duplicate_ids))

    duplicate_slugs = duplicate_values([x["slug"] for x in apps])
    check("Unique slugs", not duplicate_slugs, ", ".join(duplicate_slugs))

    bad_ids = [
        f"{x['name']}={x['id']}"
        for x in apps
        if not re.fullmatch(r"DC-[A-Z]+-[0-9]{3}", x["id"])
    ]
    check("ID format", not bad_ids, ", ".join(bad_ids))

    bad_versions = [
        f"{x['name']}={x['version']}"
        for x in apps
        if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", x["version"])
    ]
    check("Semantic versions", not bad_versions, ", ".join(bad_versions))

    bad_statuses = [
        f"{x['name']}={x['status']}"
        for x in apps
        if x["status"] not in VALID_STATUSES
    ]
    check(
        "Valid lifecycle states",
        not bad_statuses,
        "Invalid: " + ", ".join(bad_statuses) if bad_statuses else "",
    )

    buildable_statuses = {"ACTIVE", "DEVELOPMENT"}
    missing_sources = [
        f"{x['name']}: {x['source']}"
        for x in apps
        if x["status"] in buildable_statuses and not source_path(x).is_dir()
    ]
    check(
        "Source folders exist",
        not missing_sources,
        "; ".join(missing_sources),
    )

    missing_scripts = [
        f"{x['name']}: {x['build_script']}"
        for x in apps
        if x["build_script"] and not (ROOT / x["build_script"]).is_file()
    ]
    check(
        "Build scripts exist",
        not missing_scripts,
        "; ".join(missing_scripts),
    )

    misplaced_unfinished = [
        f"{x['name']}: {x['source']}"
        for x in apps
        if x["status"] == "UNFINISHED"
        and not x["source"].startswith(".data/unfinished/")
    ]
    check(
        "No unfinished apps in production",
        not misplaced_unfinished,
        "; ".join(misplaced_unfinished),
    )

    # SECURITY
    print("\nSECURITY")

    ids_dir = ROOT / "ids"
    ids = list(ids_dir.glob("*/.id.txt")) if ids_dir.is_dir() else []
    fixed_permissions, permission_fix_errors = repair_private_id_permissions(ids)

    bad_permissions: list[str] = []
    if os.name == "posix":
        for path in ids:
            try:
                if stat.S_IMODE(path.stat().st_mode) != 0o600:
                    bad_permissions.append(
                        f"{path.relative_to(ROOT)} is {mode_string(path)}, expected 0o600"
                    )
            except OSError as exc:
                bad_permissions.append(f"{path}: {exc}")

    permission_detail_parts: list[str] = []
    if fixed_permissions:
        permission_detail_parts.append("Automatically fixed: " + "; ".join(fixed_permissions))
    if permission_fix_errors:
        permission_detail_parts.append("Could not fix: " + "; ".join(permission_fix_errors))
    if bad_permissions:
        permission_detail_parts.append("Still incorrect: " + "; ".join(bad_permissions))

    check(
        "Private ID permissions",
        os.name != "posix" or not bad_permissions,
        " | ".join(permission_detail_parts),
    )

    secret_pattern = re.compile(
        r'''(?i)\b(api[_-]?key|password|access[_-]?token)\b\s*[=:]\s*['"][^'"\r\n]+['"]'''
    )

    exposed: list[str] = []
    for path in source_files():
        if path.suffix.lower() in {".java", ".js", ".py", ".sh", ".json", ".properties"}:
            try:
                if secret_pattern.search(path.read_text(errors="ignore")):
                    exposed.append(str(path.relative_to(ROOT)))
            except OSError:
                pass

    check(
        "No obvious hardcoded secrets",
        not exposed,
        ", ".join(exposed[:10]),
    )

    # APPLICATION BUILDS
    print("\nAPPLICATIONS")

    for item in selected():
        ok = build_one(item)
        check(item["name"] + " build", ok)

    # JAVA TESTS
    learner_src = ROOT / "apps/learner/src"
    if not learner_src.is_dir():
        learner_src = SRC_ROOT / "apps/learner/src"
    learner_test = ROOT / "apps/learner/tests/LearnerTests.java"
    if not learner_test.is_file():
        learner_test = SRC_ROOT / "apps/learner/tests/LearnerTests.java"

    if learner_src.is_dir() and learner_test.is_file():
        learner_ok, learner_detail = run_java_test(
            label="Learner model/storage tests",
            source_dir=learner_src,
            test_file=learner_test,
            test_class="LearnerTests",
            output_dir=test_output_path("learner"),
        )
        check("Learner model/storage tests", learner_ok, learner_detail)
    else:
        check("Learner model/storage tests", True, "Skipped: test files not found")

    projecthub_src = ROOT / "apps/projecthub/src"
    if not projecthub_src.is_dir():
        projecthub_src = SRC_ROOT / "apps/projecthub/src"
    projecthub_test = ROOT / "apps/projecthub/tests/ProjectHubStoreTests.java"
    if not projecthub_test.is_file():
        projecthub_test = SRC_ROOT / "apps/projecthub/tests/ProjectHubStoreTests.java"

    if projecthub_src.is_dir() and projecthub_test.is_file():
        projecthub_ok, projecthub_detail = run_java_test(
            label="ProjectHub persistence tests",
            source_dir=projecthub_src,
            test_file=projecthub_test,
            test_class="ProjectHubStoreTests",
            output_dir=test_output_path("projecthub"),
        )
        check("ProjectHub persistence tests", projecthub_ok, projecthub_detail)
    else:
        check("ProjectHub persistence tests", True, "Skipped: test files not found")

    # PACKAGING
    print("\nPACKAGING")

    packaging_examples = [
        "trestrio/node_modules",
        "trestrio/dist",
        "projyhub/bin",
        "projyhub/obj",
    ]

    not_excluded = [
        value
        for value in packaging_examples
        if not ignored(ROOT / value)
    ]

    check(
        "Source rules exclude dependencies",
        not not_excluded,
        "Not excluded: " + ", ".join(not_excluded) if not_excluded else "",
    )

    # DOCUMENTATION
    print("\nDOCUMENTATION")

    try:
        generate_docs()
        doc = (ROOT / "docs/APPS.md").read_text(encoding="utf-8")
        missing_doc_apps = [x["name"] for x in apps if x["name"] not in doc]
        check(
            "Generated app list synchronized",
            not missing_doc_apps,
            ", ".join(missing_doc_apps),
        )
    except Exception as exc:
        check("Generated app list synchronized", False, str(exc))

    required_docs = [
        "README.md", "ARCHITECTURE.md", "BUILDING.md", "SECURITY.md",
        "VERSIONING.md", "PACKAGING.md", "APPS.md", "CONTRIBUTING.md",
    ]

    missing_docs = [
        name
        for name in required_docs
        if not (ROOT / "docs" / name).is_file()
    ]

    check(
        "Required documentation",
        not missing_docs,
        ", ".join(missing_docs),
    )

    website_test = run("python3 scripts/test/website.py", capture=True, cwd=ROOT)

    website_detail = ""
    if website_test.returncode != 0:
        website_detail = process_error_details(
            website_test,
            command="python3 scripts/test/website.py",
        )

    check(
        "Website guide and links",
        website_test.returncode == 0,
        website_detail,
    )

    # RESULT
    passed = sum(results)
    total = len(results)
    health = round(100 * passed / total) if total else 0

    print(
        f"\nRESULT\n"
        f"{passed}/{total} checks passed\n"
        f"DATACENTER HEALTH: {health}%"
    )

    if passed != total:
        print(
            "\nOne or more checks failed. Read the ERROR details directly "
            "under each failed check above."
        )

    return 0 if all(results) else 1


# ============================================================
# STATUS
# ============================================================

def status(args: argparse.Namespace) -> None:
    apps = registry()

    states = ["ACTIVE", "DEVELOPMENT", "UNFINISHED", "RETIRED", "ARCHIVED"]
    counts = {state: sum(item["status"] == state for item in apps) for state in states}

    releases = [x for x in apps if x["status"] == "ACTIVE"]

    print("DATACENTER STATUS\n")
    for key, value in counts.items():
        print(f"{key.title()}: {value}")
    print(f"\nTotal: {len(apps)}")

    if releases:
        print("\nCurrent releases:")
        for x in releases:
            print(f"  {x['name']} {x['version']} ({x['language']})")


# ============================================================
# CLI
# ============================================================

def main() -> int:
    parser = argparse.ArgumentParser()
    subs = parser.add_subparsers(dest="command", required=True)

    for name in ["build", "package", "test"]:
        sub = subs.add_parser(name)
        sub.add_argument("slug", nargs="?")

    subs.add_parser("check")
    subs.add_parser("backup")
    subs.add_parser("backup-check")
    subs.add_parser("status")
    subs.add_parser("docs")
    subs.add_parser("doctor")

    args = parser.parse_args()

    if args.command == "build":
        return build(args)
    if args.command == "package":
        return package(args)
    if args.command == "test":
        return test(args)
    if args.command == "check":
        return checks()
    if args.command == "backup":
        return backup(args)
    if args.command == "backup-check":
        return backup_check(args)
    if args.command == "status":
        status(args)
        return 0
    if args.command == "doctor":
        return doctor(args)

    generate_docs()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
