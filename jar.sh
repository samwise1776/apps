#!/usr/bin/env bash
set -euo pipefail

# Datacenter project folder.
# You can override it by running: ./jar.sh /some/other/Data
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT="${1:-$SCRIPT_DIR}"

if ! command -v jar >/dev/null 2>&1; then
    echo "Error: the Java 'jar' command was not found. Install/use a JDK first."
    exit 1
fi

if [[ ! -d "$ROOT" ]]; then
    echo "Error: Datacenter folder does not exist: $ROOT"
    exit 1
fi

ROOT="$(cd -- "$ROOT" && pwd -P)"
JAR_DIR="$ROOT/archives/jar"
JAR_FILE="$JAR_DIR/Data-everything.jar"

mkdir -p "$JAR_DIR"

# Build in /tmp so the output jar never gets packed inside itself.
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT

echo "Copying Datacenter files..."
cp -a "$ROOT"/. "$TEMP_DIR"/

# Full-workspace archives are generated output. Excluding both formats prevents
# JAR -> ZIP -> JAR recursion and keeps repeated packaging deterministic.
rm -rf "$TEMP_DIR/jar"
rm -f "$TEMP_DIR/zip/data.zip"
rm -f "$TEMP_DIR/ids/.id-broker.sock"
for id_file in "$TEMP_DIR"/ids/*/.id.txt; do
    [[ -e "$id_file" ]] && rm -f "$id_file"
done

echo "Creating: $JAR_FILE"
jar --create --file "$JAR_FILE" -C "$TEMP_DIR" .
jar --list --file "$JAR_FILE" >/dev/null

echo
echo "Done!"
echo "JAR: $JAR_FILE"
echo "Size: $(du -h "$JAR_FILE" | cut -f1)"
echo "Files: $(jar --list --file "$JAR_FILE" | wc -l)"
echo "SHA-256: $(sha256sum "$JAR_FILE" | cut -d' ' -f1)"
