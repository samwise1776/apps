#!/usr/bin/env bash
set -Eeuo pipefail

DATA_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_DIR="$DATA_DIR/github-dev"
APPS_DIR="$DATA_DIR/apps"

if ! command -v git >/dev/null 2>&1; then
    echo "Git is required." >&2
    exit 1
fi

read -r -p "File inside apps to add to GitHub (example: learner/src/Learner.java): " app_file
app_file="${app_file#apps/}"

if [[ -z "$app_file" ]]; then
    echo "No file was entered." >&2
    exit 1
fi

selected_file="$(realpath -m -- "$APPS_DIR/$app_file")"
apps_root="$(realpath -m -- "$APPS_DIR")"

case "$selected_file" in
    "$apps_root"/*) ;;
    *)
        echo "The selected file must be inside $APPS_DIR." >&2
        exit 1
        ;;
esac

if [[ ! -f "$selected_file" ]]; then
    echo "File not found: $selected_file" >&2
    exit 1
fi

if ! git -C "$REPO_DIR" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "Development repository not found: $REPO_DIR" >&2
    exit 1
fi

relative_file="${selected_file#"$DATA_DIR"/}"
repo_file="$REPO_DIR/apps/${relative_file#apps/}"
mkdir -p -- "$(dirname -- "$repo_file")"
cp -- "$selected_file" "$repo_file"
default_message="Update $relative_file"
read -r -p "Commit message [$default_message]: " commit_message
commit_message="${commit_message:-$default_message}"

git -C "$REPO_DIR" add -- "$relative_file"
if git -C "$REPO_DIR" diff --cached --quiet -- "$relative_file"; then
    echo "Nothing changed in $relative_file."
    exit 0
fi

git -C "$REPO_DIR" commit -m "$commit_message" -- "$relative_file"
branch="$(git -C "$REPO_DIR" branch --show-current)"
git -C "$REPO_DIR" push -u origin "$branch"

echo "Added $relative_file to GitHub successfully."
