#!/usr/bin/env sh
# Checks the deployed app answers: the page loads, the backend is ready, and a game can be created.
set -eu

URL=${1:-http://localhost:${APP_PORT:-8088}}
check() {
  code=$(curl -s -o /dev/null -w '%{http_code}' "$@")
  echo "$code"
}

page=$(check "$URL/")
game=$(check -X POST "$URL/api/games")
echo "GET  /           -> $page"
echo "POST /api/games  -> $game"
[ "$page" = 200 ] && [ "$game" = 201 ] || { echo "Smoke test failed" >&2; exit 1; }
echo "Smoke test passed"
