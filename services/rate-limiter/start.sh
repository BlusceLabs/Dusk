#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
if [ ! -f target/release/rate-limiter ]; then
  echo "[rate-limiter] Building Rust binary..."
  cargo build --release
fi
exec ./target/release/rate-limiter
