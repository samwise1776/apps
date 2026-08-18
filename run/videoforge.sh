#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT/main/src/apps/videoforge" && mvn -q compile exec:java -Dexec.mainClass=videoforge.app.Main "$@"
