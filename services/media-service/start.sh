#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
if [ ! -f build/media_service ]; then
  echo "[media] Compiling C++ service..."
  cmake -B build -DCMAKE_BUILD_TYPE=Release
  cmake --build build --config Release -j$(nproc)
fi
exec ./build/media_service
