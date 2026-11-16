#!/usr/bin/env bash
# Lactea benchmark: cycles through every bundled preset at several render resolutions
# and saves the LACTEA logcat for each resolution in docs/bench/.
# Usage: tools/bench.sh <adb-serial> [seconds-per-preset] [resolutions...]
set -u
SERIAL="$1"; shift
PER="${1:-5}"; shift || true
RES=("${@:-720 1080 1440 2160}")
[ $# -eq 0 ] && RES=(720 1080 1440 2160)
ADB="${ADB:-$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe}"
OUT="$(dirname "$0")/../docs/bench"
mkdir -p "$OUT"
COUNT=60

for r in "${RES[@]}"; do
  echo "== ${r}p"
  "$ADB" -s "$SERIAL" shell am start -n fr.lactea/.VisualizerActivity --ei res "$r" >/dev/null
  sleep 4
  "$ADB" -s "$SERIAL" logcat -c
  for ((i = 0; i < COUNT; i++)); do
    "$ADB" -s "$SERIAL" shell input keyevent KEYCODE_DPAD_RIGHT
    sleep "$PER"
  done
  "$ADB" -s "$SERIAL" logcat -d -s LACTEA:I LACTEA_NATIVE:W > "$OUT/bench-${r}p.log"
done
echo "done"
