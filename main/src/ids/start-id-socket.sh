#!/usr/bin/env bash
set -Eeuo pipefail

DATA_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="$DATA_ROOT/.data/Data/Compiled_folders/ids"

if ! command -v javac >/dev/null 2>&1 || ! command -v java >/dev/null 2>&1; then
    echo "Error: a current JDK is required." >&2
    exit 1
fi

mkdir -p -- "$BUILD_DIR"
javac -Xlint:all -d "$BUILD_DIR" \
    "$DATA_ROOT/ids/IdSocketServer.java" \
    "$DATA_ROOT/ids/IdSocketClient.java"

exec java -cp "$BUILD_DIR" ids.IdSocketServer --root "$DATA_ROOT"
