#!/bin/sh
set -eu

SOURCE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if command -v xdg-user-dir >/dev/null 2>&1; then
  DESKTOP_DIR=$(xdg-user-dir DESKTOP)
else
  DESKTOP_DIR="$HOME/Desktop"
fi
[ -n "$DESKTOP_DIR" ] || DESKTOP_DIR="$HOME/Desktop"
APP_DIR="$DESKTOP_DIR/Desktopcraft"
LAUNCHER="$DESKTOP_DIR/Desktopcraft.desktop"

if ! command -v java >/dev/null 2>&1; then
  echo "Desktopcraft needs Java 17 or newer. Install Java, then run this installer again."
  exit 1
fi

mkdir -p "$APP_DIR"
cp "$SOURCE_DIR/Desktopcraft.jar" "$APP_DIR/Desktopcraft.jar"

{
  echo '[Desktop Entry]'
  echo 'Type=Application'
  echo 'Name=Desktopcraft'
  echo 'Comment=Learn desktop app development by building'
  echo "Exec=java -jar \"$APP_DIR/Desktopcraft.jar\""
  echo 'Terminal=false'
  echo 'Categories=Development;Education;'
} > "$LAUNCHER"
chmod +x "$LAUNCHER"

echo "Desktopcraft is installed at $APP_DIR"
echo "A launcher is available at $LAUNCHER"
java -jar "$APP_DIR/Desktopcraft.jar" >/dev/null 2>&1 &
