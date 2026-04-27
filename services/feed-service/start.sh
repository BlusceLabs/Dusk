#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
JAR="target/scala-3.3.4/feed-service.jar"
if [ ! -f "$JAR" ]; then
  echo "[feed-service] Building Scala service (this may take a few minutes)..."
  sbt assembly
fi
exec java -jar "$JAR"
