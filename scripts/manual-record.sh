#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_MODULE=":testloom-examples:mvc-hello-recorder-demo"
APP_PORT="${APP_PORT:-8080}"
RUN_ID="$(date +%Y%m%d-%H%M%S)"
CAPTURE_DIR="$ROOT_DIR/.testloom/captures/manual-$RUN_ID"
LOG_DIR="$ROOT_DIR/.testloom/logs"
APP_LOG="$LOG_DIR/mvc-hello-recorder-demo-$RUN_ID.log"

mkdir -p "$CAPTURE_DIR" "$LOG_DIR"

APP_PID=""
cleanup() {
  if [[ -n "${APP_PID}" ]] && kill -0 "${APP_PID}" 2>/dev/null; then
    kill "${APP_PID}" 2>/dev/null || true
    wait "${APP_PID}" 2>/dev/null || true
  fi
}
trap cleanup EXIT

cd "$ROOT_DIR"

echo "Starting demo app..."
TESTLOOM_RECORDER_OUTPUT_DIR="$CAPTURE_DIR" \
./gradlew "${APP_MODULE}:bootRun" --quiet >"$APP_LOG" 2>&1 &
APP_PID="$!"

echo "Waiting for startup..."
for _ in {1..90}; do
  if grep -q "Started MvcHelloRecorderDemoApplication" "$APP_LOG" 2>/dev/null; then
    break
  fi
  if ! kill -0 "$APP_PID" 2>/dev/null; then
    echo "ERROR: app exited before startup."
    tail -n 200 "$APP_LOG" || true
    exit 1
  fi
  sleep 1
done

if ! grep -q "Started MvcHelloRecorderDemoApplication" "$APP_LOG" 2>/dev/null; then
  echo "ERROR: timeout waiting for app startup."
  tail -n 200 "$APP_LOG" || true
  exit 1
fi

echo "Sending single manual request..."
RESPONSE="$(curl -fsS "http://127.0.0.1:${APP_PORT}/api/hello")"
if [[ "$RESPONSE" != *'"message":"hello"'* ]]; then
  echo "ERROR: unexpected response: $RESPONSE"
  exit 1
fi

cleanup
APP_PID=""

CAPTURE_FILES=()
while IFS= read -r file; do
  CAPTURE_FILES+=("$file")
done < <(find "$CAPTURE_DIR" -type f -name "*.json" | sort)
if [[ "${#CAPTURE_FILES[@]}" -ne 1 ]]; then
  echo "ERROR: expected exactly 1 capture file, got ${#CAPTURE_FILES[@]}"
  if [[ -d "$CAPTURE_DIR" ]]; then
    find "$CAPTURE_DIR" -type f | sort || true
  fi
  exit 1
fi

CAPTURE_FILE="${CAPTURE_FILES[0]}"
CAPTURE_CONTENT="$(cat "$CAPTURE_FILE")"

require_contains() {
  local pattern="$1"
  local label="$2"
  if ! grep -q "$pattern" <<<"$CAPTURE_CONTENT"; then
    echo "ERROR: missing required field: $label"
    exit 1
  fi
}

require_min_count() {
  local pattern="$1"
  local label="$2"
  local min_count="$3"
  local actual_count
  actual_count="$(grep -o "$pattern" <<<"$CAPTURE_CONTENT" | wc -l | tr -d ' ')"
  if (( actual_count < min_count )); then
    echo "ERROR: missing required field occurrences for $label: expected >= $min_count, got $actual_count"
    exit 1
  fi
}

require_contains '"schemaVersion"' "schemaVersion"
require_contains '"transport"' "transport"
require_contains '"method"' "request.method"
require_contains '"path"' "request.path"
require_contains '"headers"' "request.headers/response.headers"
require_contains '"truncation"' "request.truncation/response.truncation"
require_contains '"status"' "response.status"
require_contains '"durationMs"' "response.durationMs"
require_contains '"recordedAt"' "recordedAt"
require_contains '"bodyTruncated"' "truncation.bodyTruncated"
require_contains '"originalSizeBytes"' "truncation.originalSizeBytes"
require_contains '"capturedSizeBytes"' "truncation.capturedSizeBytes"

require_min_count '"truncation"' "truncation blocks" 2
require_min_count '"bodyTruncated"' "bodyTruncated fields" 2
require_min_count '"originalSizeBytes"' "originalSizeBytes fields" 2
require_min_count '"capturedSizeBytes"' "capturedSizeBytes fields" 2

echo "Capture file: $CAPTURE_FILE"
cat "$CAPTURE_FILE"
