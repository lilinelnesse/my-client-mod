#!/usr/bin/env bash
# Builds the mod jar. Needs JDK 21 and internet access.
set -e
cd "$(dirname "$0")"
if [ ! -f gradlew ]; then
  command -v gradle >/dev/null || { echo "Install Gradle 8.12+ (https://gradle.org/install) or open the project in IntelliJ IDEA."; exit 1; }
  gradle wrapper --gradle-version 8.12
fi
./gradlew build
echo
echo "Done. Your jar: build/libs/my-client-mod-1.11.0.jar"
