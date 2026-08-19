#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd -- "$(dirname -- "$0")/.." && pwd)"
java -cp "$ROOT/build/apps/datanotes" apps.datanotes.App "$@"
