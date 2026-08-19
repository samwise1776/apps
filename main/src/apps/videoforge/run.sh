#!/usr/bin/env bash
#
# VideoForge Studio - build and run script.
#
# Requires: JDK 21+, Maven 3.8+, FFmpeg (with libx264, aac, x11grab, pulse,
# v4l2, drawtext). FFmpeg paths can be set in Settings or auto-detected.
#
# Usage:
#   ./run.sh            build and launch the editor
#   ./run.sh compile    compile only
#   ./run.sh package    build a jlink self-contained runtime + jpackage image
#   ./run.sh clean      clean build output
#
# The workspace (projects, cache, recordings, exports, config, logs) lives in
# $VIDEOF_BASE (default: ~/.videoforge). Override it to keep it elsewhere, e.g.
#   VIDEOF_BASE="$HOME/.local/share/videoforge" ./run.sh

set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE="${VIDEOF_BASE:-$HOME/.videoforge}"
MAIN_CLASS="videoforge.app.Main"

mkdir -p "$BASE"
mkdir -p "$BASE/projects" "$BASE/cache" "$BASE/autosave" "$BASE/exports" \
         "$BASE/recordings" "$BASE/temp" "$BASE/logs" "$BASE/config"

# ---------------------------------------------------------------------------
# FFmpeg auto-detection (also performed at runtime by the app)
# ---------------------------------------------------------------------------
detect_ffmpeg() {
    if ! command -v ffmpeg >/dev/null 2>&1; then
        echo "WARNING: ffmpeg not found on PATH."
        echo "  Install it (sudo apt install ffmpeg) or set paths in the app's Settings window."
    fi
}

case "${1:-}" in
    compile)
        cd "$DIR"
        mvn -q -B compile
        echo "compile OK"
        ;;
    package)
        detect_ffmpeg
        cd "$DIR"
        mvn -q -B clean
        mvn -q -B javafx:jlink
        echo "jlink runtime created in $DIR/target/ ..."
        ;;
    clean)
        cd "$DIR"
        mvn -q -B clean
        ;;
    "")
        detect_ffmpeg
        cd "$DIR"
        mvn -q -B compile
        exec mvn -q -B exec:java \
            -Dexec.mainClass="$MAIN_CLASS" \
            -Dvideoforge.base="$BASE"
        ;;
    *)
        echo "Unknown command: $1"
        echo "Usage: ./run.sh [compile|package|clean]"
        exit 1
        ;;
esac
