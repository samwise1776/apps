#!/usr/bin/env bash
set -Eeuo pipefail

# Create a complete ZIP of the Datacenter workspace without allowing the ZIP
# to include itself while it is being written.
#
# Usage:
#   zip.sh [SOURCE_DIRECTORY] [OUTPUT_ZIP]
#
# Defaults:
#   source: /home/ray/Data (resolved relative to this script)
#   output: SOURCE_DIRECTORY/zip/data.zip

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_SOURCE="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
SOURCE_INPUT="${1:-$DEFAULT_SOURCE}"

if ! command -v zip >/dev/null 2>&1; then
    echo "Error: zip is required but was not found." >&2
    exit 1
fi

if ! command -v unzip >/dev/null 2>&1; then
    echo "Error: unzip is required to verify the finished archive." >&2
    exit 1
fi

if [[ ! -d "$SOURCE_INPUT" ]]; then
    echo "Error: source directory does not exist: $SOURCE_INPUT" >&2
    exit 1
fi

# Resolve the source before deriving its parent and archive entry name.
SOURCE_DIR="$(cd -- "$SOURCE_INPUT" && pwd -P)"
SOURCE_PARENT="$(dirname -- "$SOURCE_DIR")"
SOURCE_NAME="$(basename -- "$SOURCE_DIR")"
OUTPUT_INPUT="${2:-$SOURCE_DIR/zip/data.zip}"

# Make a relative output absolute without requiring the output to exist first.
if [[ "$OUTPUT_INPUT" = /* ]]; then
    OUTPUT_ZIP="$OUTPUT_INPUT"
else
    OUTPUT_ZIP="$PWD/$OUTPUT_INPUT"
fi
OUTPUT_PARENT="$(dirname -- "$OUTPUT_ZIP")"
OUTPUT_NAME="$(basename -- "$OUTPUT_ZIP")"

if [[ "$OUTPUT_NAME" != *.zip ]]; then
    echo "Error: output file must end in .zip: $OUTPUT_ZIP" >&2
    exit 1
fi

mkdir -p -- "$OUTPUT_PARENT"
OUTPUT_PARENT="$(cd -- "$OUTPUT_PARENT" && pwd -P)"
OUTPUT_ZIP="$OUTPUT_PARENT/$OUTPUT_NAME"

if [[ "$OUTPUT_ZIP" == "$SOURCE_DIR" ]]; then
    echo "Error: output ZIP cannot replace the source directory." >&2
    exit 1
fi

# Stage outside the source tree. The validated archive is moved into place only
# after compression and integrity testing both succeed.
TEMP_DIR="$(mktemp -d)"
TEMP_ZIP="$TEMP_DIR/$OUTPUT_NAME"
cleanup() {
    rm -rf -- "$TEMP_DIR"
}
trap cleanup EXIT INT TERM HUP

# Full-workspace archives are generated output. Excluding both formats prevents
# ZIP -> JAR -> ZIP recursion and keeps repeated packaging deterministic.
ZIP_EXCLUDES=(
    "$SOURCE_NAME/zip/data.zip"
    "$SOURCE_NAME/jar/Data-everything.jar"
    "$SOURCE_NAME/ids/*/.id.txt"
    "$SOURCE_NAME/ids/.id-broker.sock"
)
case "$OUTPUT_ZIP" in
    "$SOURCE_DIR"/*)
        # zip patterns are relative to SOURCE_PARENT and therefore include the
        # top-level source directory name.
        OUTPUT_RELATIVE="$SOURCE_NAME/${OUTPUT_ZIP#"$SOURCE_DIR"/}"
        ZIP_EXCLUDES+=("$OUTPUT_RELATIVE")
        ;;
esac

echo "Creating complete workspace archive..."
echo "Source: $SOURCE_DIR"
echo "Output: $OUTPUT_ZIP"

(
    cd -- "$SOURCE_PARENT"
    zip -q -r -9 "$TEMP_ZIP" "$SOURCE_NAME" -x "${ZIP_EXCLUDES[@]}"
)

unzip -tq "$TEMP_ZIP"
mv -f -- "$TEMP_ZIP" "$OUTPUT_ZIP"

ARCHIVE_SIZE="$(du -h "$OUTPUT_ZIP" | cut -f1)"
ENTRY_COUNT="$(unzip -Z1 "$OUTPUT_ZIP" | wc -l)"
CHECKSUM="$(sha256sum "$OUTPUT_ZIP" | cut -d' ' -f1)"

echo "Archive created successfully."
echo "Size: $ARCHIVE_SIZE"
echo "Entries: $ENTRY_COUNT"
echo "SHA-256: $CHECKSUM"
