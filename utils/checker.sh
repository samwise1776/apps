#!/usr/bin/env bash
set -Eeuo pipefail

DATA_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
exec "$DATA_DIR/scripts/checker.sh"
