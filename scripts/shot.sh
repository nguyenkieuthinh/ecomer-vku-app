#!/usr/bin/env bash
# Chup man hinh hien tai luu vao /tmp, in ra duong dan.
#
#   ./scripts/shot.sh           # -> /tmp/ecomer-<timestamp>.png
#   ./scripts/shot.sh ten      # -> /tmp/ten.png

set -euo pipefail

NAME="${1:-ecomer-$(date +%H%M%S)}"
OUT="/tmp/${NAME}.png"

adb exec-out screencap -p > "$OUT"
echo "$OUT"
