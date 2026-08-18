#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# rg fallback if not installed
if ! command -v rg >/dev/null 2>&1; then
    _rg_hidden=""
    _rg_globs=()
    _rg_patterns=()
    _rg_search_dirs=()

    rg() {
        local args=("$@")
        local positional=()
        local i=0
        while [[ $i -lt ${#args[@]} ]]; do
            case "${args[$i]}" in
                --hidden) _rg_hidden="--hidden" ;;
                --glob) i=$((i+1)); _rg_globs+=("${args[$i]}") ;;
                -e) i=$((i+1)); _rg_patterns+=("${args[$i]}") ;;
                --) ;;
                -*) ;;
                *) positional+=("${args[$i]}") ;;
            esac
            i=$((i+1))
        done

        # Build grep arguments
        local grep_args=()
        grep_args+=(-rn)
        grep_args+=(--include='*.java' --include='*.js' --include='*.py' --include='*.sh' --include='*.json')
        grep_args+=(--exclude-dir='.git' --exclude-dir='node_modules' --exclude-dir='dist')
        grep_args+=(--exclude-dir='build' --exclude-dir='runtime' --exclude-dir='releases')
        grep_args+=(--exclude-dir='backups')

        # Convert glob exclusions to grep --exclude-dir
        for g in "${_rg_globs[@]}"; do
            if [[ "$g" == '!'* ]]; then
                local dir="${g#'!'}"
                dir="${dir#'**/'}"
                dir="${dir%/**}"
                grep_args+=(--exclude-dir="$dir")
            fi
        done

        # Add .data/unfinished and archives as additional exclusions
        grep_args+=(--exclude-dir='.data' --exclude-dir='archives')

        # Build OR pattern from -e patterns
        if [[ ${#_rg_patterns[@]} -gt 0 ]]; then
            local or_pattern=""
            for p in "${_rg_patterns[@]}"; do
                if [[ -n "$or_pattern" ]]; then
                    or_pattern="$or_pattern|$p"
                else
                    or_pattern="$p"
                fi
            done
            grep_args+=(-E "$or_pattern")
        fi

        # Add search directories
        grep_args+=("${positional[@]+"${positional[@]}"}")

        local rc=0
        grep "${grep_args[@]}" 2>/dev/null || rc=$?
        return $rc
    }
    export -f rg
fi

failed=0

echo "DATACENTER SECURITY AUDIT"
echo ""

# --- Secrets ---
echo "--- Checking for committed secrets ---"
if rg -n --hidden --glob '!.git/**' --glob '!**/node_modules/**' --glob '!**/dist/**' --glob '!build/**' --glob '!runtime/**' --glob '!releases/**' --glob '!backups/**' --glob '!.data/unfinished/**' --glob '!archives/**' \
  -e '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----' \
  -e '(?i)(api[_-]?key|access[_-]?token|password)\s*[=:]\s*[^[:space:]]{8,}' .; then
  echo "[FAIL] Possible committed secret detected"
  failed=1
else
  echo "[PASS] No obvious committed secrets"
fi

# --- ID permissions ---
echo ""
echo "--- Checking ID file permissions ---"
if [[ -d ids ]]; then
    if find ids -name '.id.txt' -type f ! -perm 600 -print | grep -q .; then
        echo "[FAIL] Private ID files must have mode 600"
        failed=1
    else
        echo "[PASS] Private ID permissions"
    fi
else
    echo "[SKIP] No ids/ directory found"
fi

# --- Symlink escapes ---
echo ""
echo "--- Checking for escaping symlinks ---"
escapes_found=0
while IFS= read -r link; do
    target="$(readlink -f -- "$link" 2>/dev/null || true)"
    case "$target" in
        "$ROOT"/*) ;;
        *) echo "  ESCAPE: $link -> $target"; escapes_found=1 ;;
    esac
done < <(find . -path './.git' -prune -o -type l -print 2>/dev/null)

if [[ $escapes_found -eq 1 ]]; then
    echo "[FAIL] Symlink escapes the workspace"
    failed=1
else
    echo "[PASS] No escaping symlinks"
fi

# --- Hardcoded paths ---
echo ""
echo "--- Checking for hardcoded absolute paths in source ---"
hardcoded_found=0
for ext in java py sh; do
    while IFS= read -r line; do
        # Skip documentation, .git, node_modules, build
        case "$line" in
            *.md:*|*node_modules*|*build/*|*backups/*|.git/*|.data/*|archives/*) continue ;;
        esac
        echo "  $line"
        hardcoded_found=1
    done < <(grep -rn --include="*.$ext" '/home/ray/Data' . 2>/dev/null | grep -v 'node_modules\|build/\|backups/\|\.git/\|\.data/\|archives/' | head -20)
done

if [[ $hardcoded_found -eq 1 ]]; then
    echo "[WARN] Hardcoded paths found in source (see above)"
else
    echo "[PASS] No hardcoded absolute paths in source"
fi

echo ""
exit "$failed"
