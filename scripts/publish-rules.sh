#!/usr/bin/env bash
# Publish Firestore Security Rules len Firebase.
#
#   ./scripts/publish-rules.sh          # dung Firebase CLI (neu co)
#   ./scripts/publish-rules.sh --show   # in rules ra man hinh de copy tay
#
# Firebase CLI cai dat: npm install -g firebase-tools
# Dang nhap: firebase login

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RULES="$ROOT/firestore.rules"

if [ "${1:-}" = "--show" ]; then
  echo "=== Copy toan bo noi dung duoi day, dan vao"
  echo "=== Firebase Console -> Firestore Database -> Rules -> Publish ==="
  echo
  cat "$RULES"
  exit 0
fi

if ! command -v firebase >/dev/null 2>&1; then
  echo "Chua co Firebase CLI. Cai dat bang:"
  echo "  npm install -g firebase-tools"
  echo
  echo "Hoac dung cach copy tay: ./scripts/publish-rules.sh --show"
  exit 1
fi

cd "$ROOT"
firebase login
firebase deploy --only firestore:rules
echo "==> Da publish rules thanh cong."
