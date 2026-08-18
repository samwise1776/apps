#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
"$ROOT/scripts/backup.sh"
archive="$(ls -1t "$ROOT"/backups/Datacenter-source-*.zip | head -1)"
temp="$(mktemp -d)"
trap 'rm -rf -- "$temp"' EXIT
unzip -q "$archive" -d "$temp"
"$temp/Datacenter/checker.sh"
echo "Restore test passed: $archive"
