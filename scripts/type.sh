#!/usr/bin/env bash
# Nhap text vao o dang focus, hoac bam phim.
#
#   ./scripts/type.sh "admin@ecomer.vn"   # nhap chu
#   ./scripts/type.sh "clear"             # xoa het o dang focus
#   ./scripts/type.sh key BACK             # bam phim
#   ./scripts/type.sh tap 720 1030         # bam toa do
#   ./scripts/type.sh hide                 # an ban phim

set -euo pipefail

case "${1:-}" in
  clear)
    adb shell input keyevent KEYCODE_MOVE_END
    for _ in $(seq 1 60); do adb shell input keyevent KEYCODE_DEL; done
    ;;
  key)
    adb shell input keyevent "$2"
    ;;
  tap)
    adb shell input tap "$2" "$3"
    ;;
  hide)
    adb shell input keyevent KEYCODE_BACK
    ;;
  "")
    echo "Thieu noi dung. Xem usage trong file script." >&2
    exit 1
    ;;
  *)
    adb shell input text "$1"
    ;;
esac
