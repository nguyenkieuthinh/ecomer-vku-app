#!/usr/bin/env bash
# Xem cay UI hien tai: text + toa do bounds.
#
#   ./scripts/ui.sh              # tat ca text dang co
#   ./scripts/ui.sh "DANG KY"    # chi tim mot text
#   ./scripts/ui.sh --tap "DANG KY"   # bat dau bang tim + bam

set -euo pipefail

adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
XML="$(adb shell cat /sdcard/ui.xml 2>/dev/null)"

if [ "${1:-}" = "--tap" ]; then
  exec "$(dirname "${BASH_SOURCE[0]}")/tap.sh" "$2" "${3:-1}"
fi

if [ -n "${1:-}" ]; then
  printf '%s' "$XML" | tr '<' '\n' \
    | grep -F "text=\"$1\"" \
    | grep -oE 'text="[^"]*"|bounds="[^"]*"' \
    | paste - - 2>/dev/null \
    || printf '%s' "$XML" | tr '<' '\n' | grep -F "text=\"$1\"" | head -5
  exit 0
fi

printf '%s' "$XML" | tr '<' '\n' \
  | grep -oE 'text="[^"]+".*bounds="[^"]+"' \
  | sed -E 's/.*text="([^"]*)".*bounds="([^"]*)".*/\2  \1/' \
  | grep -vE '^\[0,0\]\[1440,3200\]  $' \
  | head -60
