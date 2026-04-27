#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
JAR="target/realtime-service-0.1.0.jar"
if [ ! -f "$JAR" ]; then
  echo "[realtime] Building Java service..."
  mvn package -q -DskipTests
fi
exec java -jar "$JAR"
