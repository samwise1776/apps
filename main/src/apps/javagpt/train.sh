#!/bin/bash
# Compile and train JavaGPT
set -e
cd "$(dirname "$0")"
echo "Compiling JavaGPT..."
mkdir -p build
javac -d build -sourcepath src src/javagpt/*.java
echo "Training..."
java -cp build javagpt.Main train "${1:-tiny}"
