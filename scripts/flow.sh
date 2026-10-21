#!/usr/bin/env bash
# Chay mot chuoi thao tac theo kich ban, dung de test lai nhieu lan.
#
#   ./scripts/flow.sh kich_ban.txt
#
# Moi dong trong file:
#   tap <text>            bam theo text
#   tapxy <x> <y>         bam theo toa do
#   type <chu>            nhap chu
#   clear                 xoa o dang focus
#   key <KEYCODE>         bam phim
#   hide                  an ban phim
#   sleep <giay>          doi
#   shot <ten>            chup man hinh -> /tmp/<ten>.png
#   log <mode>            xem log
#   text <text>           nhap text vao o (tien xoa truoc)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILE="${1:?Dung: ./scripts/flow.sh <file>}"
PKG="${PKG:-com.ecomer.vku.app}"

step=0
while IFS= read -r line || [ -n "$line" ]; do
  # Bo qua comment va dong trong.
  [[ "$line" =~ ^[[:space:]]*# ]] && continue
  [[ -z "${line// }/" ]] && continue
  step=$((step + 1))
  read -r -a args <<< "$line"
  cmd="${args[0]}"

  case "$cmd" in
    tap)    "$SCRIPT_DIR/tap.sh" "${args[@]:1}" ;;
    tapxy)  adb shell input tap "${args[1]}" "${args[2]}" ;;
    type)   adb shell input text "${args[@]:1}" ;;
    clear)  "$SCRIPT_DIR/type.sh" clear ;;
    key)    adb shell input keyevent "${args[1]}" ;;
    hide)   "$SCRIPT_DIR/type.sh" hide ;;
    sleep)  sleep "${args[1]}" ;;
    shot)   "$SCRIPT_DIR/shot.sh" "${args[1]:-step$step}" ;;
    log)    "$SCRIPT_DIR/log.sh" "${args[1]:-app}" ;;
    *)      echo "[flow] dong $step khong hop le: $line" >&2 ;;
  esac
done < "$FILE"

echo "[flow] xong $step buoc"
