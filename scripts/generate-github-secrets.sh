#!/usr/bin/env bash
# Run this in the Replit shell to generate GitHub Secret values.
# Usage: bash scripts/generate-github-secrets.sh

set -e

echo ""
echo "=== GitHub Secret Values for Dusk ==="
echo ""
echo "You only need TWO secrets to build the APK:"
echo ""

# google-services.json
if [ -f "artifacts/dusk/google-services.json" ]; then
  echo "──────────────────────────────────────────"
  echo "Secret name:  GOOGLE_SERVICES_JSON_BASE64"
  echo "Secret value:"
  echo ""
  base64 -w 0 artifacts/dusk/google-services.json
  echo ""
  echo "──────────────────────────────────────────"
  echo ""
else
  echo "❌  artifacts/dusk/google-services.json not found"
fi

echo "Secret name:  EXPO_TOKEN"
echo "Secret value: Get it from https://expo.dev/accounts/<you>/settings/access-tokens"
echo ""
echo "Optional (only needed if your API server runs in prod):"
echo "  NEON_DATABASE_URL"
echo ""
echo "Paste each value into:"
echo "  GitHub repo → Settings → Secrets and variables → Actions → New repository secret"
echo ""
echo "All Firebase values (API key, project ID, etc.) are extracted"
echo "automatically from GOOGLE_SERVICES_JSON_BASE64 during the build."
