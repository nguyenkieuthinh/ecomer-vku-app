#!/usr/bin/env bash
# Bam vao man hinh theo TOA DO, theo TEXT, hoac theo THU TU O NHAP.
#
#   ./scripts/tap.sh 720 1370           # theo toa do
#   ./scripts/tap.sh "DANG KY"          # theo text hien tren man hinh
#   ./scripts/tap.sh "DANG KY" 2       # bat dau bang lan thu 2
#   ./scripts/tap.sh --field 2         # theo o nhap thu 2 (1 = o dau tien)
#
# Luu y: o mat khau co password="true" nen uiautomator KHONG hien placeholder —
# dung --field cho cac o do.

set -euo pipefail

dump_ui() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb shell cat /sdcard/ui.xml 2>/dev/null
}

tap_xy() {
  adb shell input tap "$1" "$2"
  echo "tap ($1, $2)"
}

# --- Bam theo thu tu o nhap ---
if [ "${1:-}" = "--field" ]; then
  N="${2:-1}"
  XML="$(dump_ui)"
  BOUNDS="$(printf '%s' "$XML" | tr '<' '\n' \
    | grep 'class="android.widget.EditText"' \
    | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' \
    | sed -n "${N}p")"
  if [ -z "$BOUNDS" ]; then
    echo "KHONG co o nhap thu $N" >&2
    exit 1
  fi
  X1="$(printf '%s' "$BOUNDS" | grep -oE '[0-9]+' | sed -n 1p)"
  Y1="$(printf '%s' "$BOUNDS" | grep -oE '[0-9]+' | sed -n 2p)"
  X2="$(printf '%s' "$BOUNDS" | grep -oE '[0-9]+' | sed -n 3p)"
  Y2="$(printf '%s' "$BOUNDS" | grep -oE '[0-9]+' | sed -n 4p)"
  tap_xy $(( (X1 + X2) / 2 )) $(( (Y1 + Y2) / 2 ))
  exit 0
fi

# --- Bam theo toa do ---
if [ $# -ge 2 ] && [[ "$1" =~ ^[0-9]+$ ]]; then
  tap_xy "$1" "$2"
  exit 0
fi

# --- Bam theo text ---
TEXT="$1"
NTH="${2:-1}"
XML="$(dump_ui)"

BOUNDS="$(printf '%s' "$XML" | tr '<' '\n' | grep -F "text=\"$TEXT\"" \
  | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | sed -n "${NTH}p")"

if [ -z "$BOUNDS" ]; then
  echo "KHONG tim thay text: $TEXT" >&2
  echo "Danh sach text dang hien:" >&2
  printf '%s' "$XML" | tr '<' '\n' | grep -oE 'text="[^"]+"' | sort -u | head -40 >&2
  echo "Dung --field <n> de bam theo thu tu o nhap." >&2
  exit 1
fi

X1="$(printf '%s' "$BOUNDS" | grep -oE '[0-9]+' | sed -n 1p)"
Y1="$(printf '%s' "$BOUNDS" | grep -oE '[0-9]+' | sed -n 2p)"
X2="$(printf '%s' "$BOUNDS" | grep -oE '[0-9]+' | sed -n 3p)"
Y2="$(printf '%s' "$BOUNDS" | grep -oE '[0-9]+' | sed -n 4p)"
tap_xy $(( (X1 + X2) / 2 )) $(( (Y1 + Y2) / 2 ))
