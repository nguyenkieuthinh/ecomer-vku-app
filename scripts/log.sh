#!/usr/bin/env bash
# Xem log app, chi loc phan lien quan.
#
#   ./scripts/log.sh              # log dang chay (Ctrl+C de dung)
#   ./scripts/log.sh errors       # chi loi/crash
#   ./scripts/log.sh firestore    # log Firestore
#   ./scripts/log.sh raw         # toan bo log app

set -euo pipefail

PKG="${PKG:-com.ecomer.vku.app}"
MODE="${1:-app}"

case "$MODE" in
  errors)
    adb logcat -v brief \
      | grep -E "AndroidRuntime|JavaExceptionHandler|${PKG}.*(E/|FATAL)|PERMISSION_DENIED|ANR" \
      | grep -vE "^\s+at " || true
    ;;
  firestore)
    adb logcat -v brief | grep -E "W/Firestore|FirebaseFirestoreException|PERMISSION_DENIED" || true
    ;;
  raw)
    adb logcat -v brief | grep -E "$PKG|Firestore|Firebase" || true
    ;;
  *)
    adb logcat -v brief | grep -E "$PKG|Firestore|Firebase|AndroidRuntime" || true
    ;;
esac
