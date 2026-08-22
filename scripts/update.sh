#!/usr/bin/env bash

# Publish the website: commit site sources on main, mirror them onto the
# gh-pages branch, and push both.
#
# Site sources live at the repo root of main:
#   index.html app.js guide.js style.css velice.html velice-guide.css
#   data-chips.html data-chips.js assets/ web/
# plus the legacy copies under main/src/public/web/.

set -Eeuo pipefail

ROOT="$(cd -- "$(dirname -- "$(readlink -f -- "${BASH_SOURCE[0]}")")/.." && pwd)"
PAGES_BRANCH="gh-pages"
WORKTREE="$(mktemp -d /tmp/opencode/update-ghpages.XXXXXX)"

SITE_FILES=(
    index.html
    app.js
    guide.js
    style.css
    velice.html
    velice-guide.css
    data-chips.html
    data-chips.js
)
SITE_DIRS=(
    assets
    web
)
LEGACY_SRC="main/src/public/web"

cleanup() {
    git -C "$ROOT" worktree remove --force "$WORKTREE" >/dev/null 2>&1 || rm -rf "$WORKTREE"
}
trap cleanup EXIT

die() { echo "update.sh: $*" >&2; exit 1; }

cd "$ROOT"

# 0. Stay in sync with origin before doing anything.
git fetch origin --quiet || die "'git fetch' failed."
if ! git merge-base --is-ancestor origin/main main; then
    die "main is behind origin/main. Run 'git pull --rebase' first."
fi

# 1. Commit pending site source changes (only when on main).
if [[ "$(git rev-parse --abbrev-ref HEAD)" == "main" ]]; then
    git add -A -- "${SITE_FILES[@]}" "${SITE_DIRS[@]}" "$LEGACY_SRC" ':(exclude)*.backup'
    if ! git diff --cached --quiet; then
        git commit -m "Publish website updates"
        echo "Committed website sources on main."
    else
        echo "Website sources unchanged."
    fi
else
    echo "Not on main; skipping source commit." >&2
fi

# 2. Mirror the site onto gh-pages via a temporary worktree.
git worktree add "$WORKTREE" "$PAGES_BRANCH" >/dev/null
git -C "$WORKTREE" merge --ff-only --quiet "origin/$PAGES_BRANCH" 2>/dev/null \
    || git -C "$WORKTREE" reset --hard --quiet "origin/$PAGES_BRANCH"

for f in "${SITE_FILES[@]}"; do
    [[ -f "$f" ]] && cp -- "$f" "$WORKTREE/$f"
done
for d in "${SITE_DIRS[@]}"; do
    [[ -d "$d" ]] || continue
    rm -rf "${WORKTREE:?}/$d"
    cp -a -- "$d" "$WORKTREE/$d"
done

git -C "$WORKTREE" add -A
if git -C "$WORKTREE" diff --cached --quiet; then
    echo "$PAGES_BRANCH already up to date."
else
    git -C "$WORKTREE" commit -q -m "Update published site $(date +%F)"
    echo "Updated $PAGES_BRANCH."
fi

# 3. Push.
failed=0
if ! git push origin main >/dev/null 2>&1; then
    echo "update.sh: 'git push origin main' failed — push it manually." >&2
    failed=1
else
    echo "Pushed main."
fi
if ! git push origin "$PAGES_BRANCH" >/dev/null 2>&1; then
    echo "update.sh: pushing $PAGES_BRANCH failed — push it manually." >&2
    failed=1
else
    echo "Pushed $PAGES_BRANCH."
fi

[[ $failed -eq 0 ]] || exit 1
echo "Done."
