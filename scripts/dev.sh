#!/usr/bin/env bash
# Build + cai + mo app ECOMER. Dung khi code vua doi xong.
#
#   ./scripts/dev.sh            # build debug, cai, mo
#   ./scripts/dev.sh --clean    # xoa app cu (doi tai khoan) roi cai lai
#
# Bien moi truong:
#   PKG       ten package (mac dinh: com.ecomer.vku.app)
#   ACTIVITY  activity chinh    (mac dinh: .MainActivity)

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PKG="${PKG:-com.ecomer.vku.app}"
ACTIVITY="${ACTIVITY:-.MainActivity}"
APK="app/build/outputs/apk/debug/app-debug.apk"

CLEAN=0
for arg in "$@"; do
  case "$arg" in
    --clean) CLEAN=1 ;;
  esac
done

echo "==> Build debug"
./gradlew :app:assembleDebug -q --console=plain

if [ "$CLEAN" = "1" ]; then
  echo "==> Xoa app cu (xoa luon du lieu dang nhap)"
  adb uninstall "$PKG" >/dev/null 2>&1 || true
fi

echo "==> Cai dat"
adb install -r "$APK" | tail -1

echo "==> Khoi dong app"
adb shell am force-stop "$PKG"
adb logcat -c
adb shell am start -n "$PKG/$ACTIVITY" >/dev/null
sleep "${WAIT:-4}"

echo "==> Xong. Xem man hinh: ./scripts/shot.sh"
