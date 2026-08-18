#!/usr/bin/env bash
set -Eeuo pipefail

DATA_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="$DATA_DIR/build"
MARKER_FILE="$BUILD_DIR/.data-java-build"

if [[ ! -e "$BUILD_DIR" ]]; then
    echo "Nothing to undo; $BUILD_DIR does not exist."
    exit 0
fi

if [[ ! -f "$MARKER_FILE" ]]; then
    echo "Refusing to remove $BUILD_DIR because it was not created by build.sh." >&2
    exit 1
fi

rm -rf -- "$BUILD_DIR"
echo "Removed Java build output: $BUILD_DIR"
