#!/bin/bash
# Compile and launch JavaGPT GUI
set -e
cd "$(dirname "$0")"
if [ ! -d build ] || [ ! -f build/javagpt/GUI.class ]; then
    echo "Compiling JavaGPT..."
    mkdir -p build
    javac -d build -sourcepath src src/javagpt/*.java
fi
java -cp build javagpt.Main gui
