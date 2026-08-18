#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/build/appcenter-native"
CLASSES="$ROOT/build/appcenter-classes"
JAR="$ROOT/build/appcenter/AppCenter.jar"
PACKAGE_OUT="$(mktemp -d "$ROOT/build/appcenter-package.XXXXXX")"

mkdir -p "$OUT" "$CLASSES" "$(dirname -- "$JAR")"
javac -Xlint:all -d "$CLASSES" "$ROOT/installer/App.java"
jar --create --file "$JAR" --main-class App -C "$CLASSES" .

# jpackage creates a normal native application and bundles a private Java
# runtime. Users do not need to find, download, configure, or update Java.
jpackage \
  --type app-image \
  --name AppCenter \
  --description "The Datacenter app installer" \
  --vendor "Datacenter" \
  --app-version 1.0.0 \
  --input "$(dirname -- "$JAR")" \
  --main-jar "$(basename -- "$JAR")" \
  --main-class App \
  --dest "$PACKAGE_OUT" \
  --java-options "-Dfile.encoding=UTF-8"

if [[ -d "$OUT/AppCenter" ]]; then
  mv "$OUT/AppCenter" "$OUT/AppCenter.previous.$(date +%s)"
fi
mv "$PACKAGE_OUT/AppCenter" "$OUT/AppCenter"

printf 'AppCenter built with Java included:\n%s\n' "$OUT/AppCenter"
