#!/usr/bin/env bash
# Builds the mod jar. Needs JDK 21 and internet access.
set -e
cd "$(dirname "$0")"
if [ ! -f gradlew ]; then
  command -v gradle >/dev/null || { echo "Install Gradle 9.5+ (https://gradle.org/install) or open the project in IntelliJ IDEA."; exit 1; }
  gradle wrapper --gradle-version 9.8.0
fi
./gradlew build
echo
echo "Done. Your jar: build/libs/my-client-mod-mc1.21.11-1.2.0.jar"
