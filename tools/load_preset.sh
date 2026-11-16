#!/usr/bin/env bash
# Load a preset on the TV by its key, whatever characters its name contains.
# Usage: tools/load_preset.sh <adb-serial> "Category/sub/name"
set -eu
SERIAL="$1"; KEY="$2"
ADB="${ADB:-$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe}"
B64=$(printf '%s' "$KEY" | base64 -w0)
MSYS_NO_PATHCONV=1 "$ADB" -s "$SERIAL" shell am start -n fr.lactea/.VisualizerActivity --es preset_b64 "$B64" </dev/null >/dev/null 2>&1
