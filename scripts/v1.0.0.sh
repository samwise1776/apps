#!/usr/bin/env bash
set -Eeuo pipefail

DATA_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="$DATA_DIR/runtime/v1.0.0"
JAVA_BUILD="$BUILD_DIR/java"
MANAGER_BUILD="$BUILD_DIR/manager"
IDS_BUILD="$BUILD_DIR/ids"

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "Missing required command: $1" >&2
        exit 1
    fi
}

require_command java
require_command javac

if [[ -z "${DISPLAY:-}" && -z "${WAYLAND_DISPLAY:-}" ]]; then
    echo "A desktop session is required to run the graphical apps." >&2
    exit 1
fi

rm -rf -- "$JAVA_BUILD" "$MANAGER_BUILD" "$IDS_BUILD"
mkdir -p "$JAVA_BUILD" "$MANAGER_BUILD" "$IDS_BUILD"

echo "Building Datacenter v1.0.0..."
javac -d "$JAVA_BUILD" \
    "$DATA_DIR/apps/datadocs/DataDocs.java" \
    "$DATA_DIR/apps/info/App1.java" \
    "$DATA_DIR/apps/helper/App.java" \
    "$DATA_DIR/apps/helper/Brain.java" \
    "$DATA_DIR/apps/helper/OfflineAssistant.java" \
    "$DATA_DIR/apps/helper/AppDictionary.java" \
    "$DATA_DIR/apps/helper/Dictionary.java" \
    "$DATA_DIR/apps/helper/Incorrectword.java" \
    "$DATA_DIR/apps/helper/Matrix.java" \
    "$DATA_DIR/apps/helper/Slang.java" \
    "$DATA_DIR/apps/helper/Unfinished.java"
javac -d "$MANAGER_BUILD" "$DATA_DIR"/utils/*.java
javac -d "$IDS_BUILD" \
    "$DATA_DIR/ids/IdSocketServer.java" \
    "$DATA_DIR/ids/IdSocketClient.java"

pids=()

start_app() {
    local name="$1"
    shift
    echo "Starting $name..."
    "$@" &
    pids+=("$!")
}

stop_all() {
    echo
    echo "Stopping Datacenter v1.0.0..."
    for pid in "${pids[@]:-}"; do
        kill "$pid" 2>/dev/null || true
    done
    wait 2>/dev/null || true
}
trap stop_all INT TERM EXIT

start_app "ID broker" java -cp "$IDS_BUILD" ids.IdSocketServer --root "$DATA_DIR"
start_app "Manager" java -cp "$MANAGER_BUILD" utils.Manager "$DATA_DIR"
start_app "DataDocs" java -cp "$JAVA_BUILD" DataDocs
start_app "Datapro" java -cp "$JAVA_BUILD" App1
start_app "Helper" java -cp "$JAVA_BUILD" App

echo "Datacenter v1.0.0 is running. Press Ctrl+C to stop everything."
wait
