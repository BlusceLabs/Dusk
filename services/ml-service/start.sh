#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
exec gunicorn ml_service.wsgi:application \
  --bind "0.0.0.0:${PORT:-8085}" \
  --workers 2 \
  --timeout 30 \
  --log-level info
