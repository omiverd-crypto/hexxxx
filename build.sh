#!/bin/sh
# Builds the mod jar. Needs JDK 21 and Gradle 8.10+ on PATH (internet required on first run).
set -e
cd "$(dirname "$0")"
command -v gradle >/dev/null || { echo "Gradle not found. Install Gradle 8.10+ (https://gradle.org/install) and JDK 21."; exit 1; }
[ -x ./gradlew ] || gradle wrapper --gradle-version 8.10.2
./gradlew build
echo; echo "Done: $(ls build/libs/hexpowers-*[0-9].jar)"
