#!/bin/sh
set -eu

SOURCE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DESKTOP_DIR="$HOME/Desktop"
APP_DIR="$DESKTOP_DIR/Desktopcraft"
LAUNCHER="$DESKTOP_DIR/Launch Desktopcraft.command"

if ! command -v java >/dev/null 2>&1; then
  echo "Desktopcraft needs Java 17 or newer. Install Java, then run this installer again."
  read -r _
  exit 1
fi

mkdir -p "$APP_DIR"
cp "$SOURCE_DIR/Desktopcraft.jar" "$APP_DIR/Desktopcraft.jar"

{
  echo '#!/bin/sh'
  echo "exec java -jar \"$APP_DIR/Desktopcraft.jar\""
} > "$LAUNCHER"
chmod +x "$LAUNCHER"

echo "Desktopcraft is installed on your Desktop."
java -jar "$APP_DIR/Desktopcraft.jar" >/dev/null 2>&1 &
