#!/usr/bin/env python3
"""Build and verify the Desktopcraft Swing downloads with only JDK/Python tools."""

from __future__ import annotations

import shutil
import subprocess
import sys
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "desktop-app" / "src" / "DesktopcraftApp.java"
BUILD = ROOT / "desktop-app" / "build"
DOWNLOADS = ROOT / "downloads"
JAR = DOWNLOADS / "Desktopcraft.jar"
BUNDLE = DOWNLOADS / "Desktopcraft-desktop.zip"
SOURCE_ZIP = DOWNLOADS / "Desktopcraft-source.zip"
INSTALLER = ROOT / "desktop-app" / "installer"


def run(*command: str) -> None:
    subprocess.run(command, cwd=ROOT, check=True)


def add_file(archive: zipfile.ZipFile, source: Path, target: str, executable: bool = False) -> None:
    info = zipfile.ZipInfo.from_file(source, target)
    if executable:
        info.external_attr = (0o100755 << 16)
    with source.open("rb") as handle:
        archive.writestr(info, handle.read(), compress_type=zipfile.ZIP_DEFLATED)


def main() -> None:
    if BUILD.exists():
        shutil.rmtree(BUILD)
    BUILD.mkdir(parents=True)
    DOWNLOADS.mkdir(parents=True, exist_ok=True)

    if shutil.which("javac"):
        run("javac", "--release", "17", "-d", str(BUILD), str(SOURCE))
    else:
        # Some minimal Java installations include the compiler module but omit
        # the javac launcher binary. Invoke the same compiler through java.
        run("java", "-m", "jdk.compiler/com.sun.tools.javac.Main", "--release", "17", "-d", str(BUILD), str(SOURCE))
    run(
        "jar", "--create", "--file", str(JAR), "--main-class", "DesktopcraftApp",
        "-C", str(BUILD), ".", "lessons-extra.js", "desktop-courses.js",
    )
    run("java", "-jar", str(JAR), "--verify")

    if "--verify-only" in sys.argv:
        print(f"Desktop verification passed: {JAR}")
        return

    for target in (BUNDLE, SOURCE_ZIP):
        target.unlink(missing_ok=True)

    with zipfile.ZipFile(BUNDLE, "w") as archive:
        add_file(archive, JAR, "Desktopcraft.jar")
        add_file(archive, INSTALLER / "README-FIRST.txt", "README-FIRST.txt")
        add_file(archive, INSTALLER / "Install-Desktopcraft-Windows.bat", "Install-Desktopcraft-Windows.bat")
        add_file(archive, INSTALLER / "install-desktopcraft.sh", "install-desktopcraft.sh", executable=True)
        add_file(archive, INSTALLER / "Install Desktopcraft.command", "Install Desktopcraft.command", executable=True)

    with zipfile.ZipFile(SOURCE_ZIP, "w") as archive:
        add_file(archive, SOURCE, "desktop-app/src/DesktopcraftApp.java")
        add_file(archive, ROOT / "desktop-app" / "README.md", "desktop-app/README.md")
        add_file(archive, ROOT / "lessons-extra.js", "lessons-extra.js")
        add_file(archive, ROOT / "desktop-courses.js", "desktop-courses.js")
        add_file(archive, ROOT / "scripts" / "build-desktop.py", "scripts/build-desktop.py", executable=True)
        for source in sorted(INSTALLER.iterdir()):
            add_file(archive, source, f"desktop-app/installer/{source.name}", source.suffix in {".sh", ".command"})

    print(f"Desktop app: {JAR}")
    print(f"Desktop installer: {BUNDLE}")
    print(f"Source: {SOURCE_ZIP}")


if __name__ == "__main__":
    main()
