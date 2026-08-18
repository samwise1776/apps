#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"

# Safety: ensure we are inside the repository
case "$ROOT" in
    /home/*|/root/*|/tmp/*) ;;
    *) echo "Refusing to clean outside expected workspace: $ROOT" >&2; exit 1 ;;
esac

removed=0
for path in \
    "$ROOT/build" \
    "$ROOT/apps/trestrio/dist" \
    "$ROOT/.data/unfinished/apps/projyhub/bin" \
    "$ROOT/.data/unfinished/apps/projyhub/obj" \
    "$ROOT/apps/javagpt/build" \
; do
    [[ -e "$path" ]] || continue
    rm -rf -- "$path"
    echo "Removed: ${path#"$ROOT"/}"
    removed=$((removed + 1))
done

# Clean __pycache__ directories
find "$ROOT" -path "$ROOT/.git" -prune -o -path "$ROOT/node_modules" -prune -o -name __pycache__ -type d -print -exec rm -rf -- {} + 2>/dev/null || true

echo "Generated build outputs removed. Source and user data were preserved."
echo "Cleaned $removed path(s)."
