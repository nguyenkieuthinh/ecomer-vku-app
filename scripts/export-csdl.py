#!/usr/bin/env python3
"""
Xuat CSDL (Firestore) cua app ECOMER ra file JSON de nop bai.

De bai yeu cau: "Sinh vien nop link github source code va link video demo,
csdl(json)" => can file JSON chua du lieu san pham + anh.

Cach dung:
    ./scripts/export-csdl.py                 # xuat ra docs/csdl.json
    ./scripts/export-csdl.py --out duong/dan # xuat ra file khac
    ./scripts/export-csdl.py --from-export   # doc file export cua Firebase CLI

Cach lay du lieu (lam tren PC):
    1. Firebase Console -> Firestore Database -> chon collection -> Export -> JSON
       (Export ca database de lay het: Products -> Export -> JSON)
    2. Bo file export vao thu muc .export/:
       - CLI:      .export/all_namespaces_all_kinds.json
       - Console:  .export/products.json, .export/product_images.json, ...
    3. Chay: ./scripts/export-csdl.py

    HOAC dung Firebase CLI:
       npm install -g firebase-tools
       firebase login
       firebase firestore:export .export --project vku-ecomer
       ./scripts/export-csdl.py --from-export

Ket qua: docs/csdl.json — dung cho ban nop, khong chua API key.
"""

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_OUT = ROOT / "docs" / "csdl.json"
DEFAULT_EXPORT_DIR = ROOT / ".export"

# Thu tu collection trong file xuat.
# Cac collection trong app ECOMER.
#
# `addresses` khong phai collection tach biet ma la SUBCOLLECTION cua
# `users/{uid}/addresses` — khi export tu Firebase CLI, cac document nay
# bi ghep nham vao collection `users` (ten duong dan chua 3 phan).
#
# Script nay tach rieng `addresses` ra mot collection rieng cho khop voi
# file do app xuat ra (xem `CsdlExporter.kt`).
ADDRESSES_COLLECTION = "addresses"

COLLECTIONS = [
    "products",
    "product_images",
    "hidden_products",
    "carts",
    "orders",
    "users",
    "counters",
    ADDRESSES_COLLECTION,
]

# Cac truong chua thong tin ca nhan cua khach that.
#
# File export chua email, ten, so dien thoai va dia chi giao hang THAT.
# Nop len GitHub cong khai se lo PII nen mac dinh che cac truong nay.
# Bat `mask_personal` neu can du lieu that (chi khi noi file rieng cho
# giao vien, KHONG commit len GitHub).
PERSONAL_FIELDS = {
    "email",
    "fullName",
    "receiverName",
    "displayName",
    "phone",
    "receiverPhone",
    "street",
    "ward",
    "district",
    "city",
    "address",
    "photoUrl",
}

MASKED_NAME = "Khach hang"
MASKED_ADDRESS = "Da an"
MASKED_PHONE = "Da an"

# Tien to chuoi base64 anh.
BASE64_IMAGE_PREFIX = "data:image"


# Cac key bao "wrapper" cua Firestore export: {"stringValue": "..."}.
VALUE_KEYS = (
    "stringValue",
    "integerValue",
    "doubleValue",
    "booleanValue",
    "nullValue",
    "timestampValue",
    "arrayValue",
    "mapValue",
    "referenceValue",
    "bytesValue",
)


def decode_value(value):
    """Giai ma 1 gia tri Firestore export ve kieu JSON thuan.

    Firestore export boc gia tri trong wrapper, vi du:
        {"stringValue": "abc"}          -> "abc"
        {"integerValue": "300000"}      -> 300000
        {"arrayValue": {"values": []}}  -> []
        {"mapValue": {"fields": {}}}    -> {...}
    Gia tri da thuan (chuoi, so) thi giu nguyen.
    """
    if not isinstance(value, dict):
        return value

    # Wrapper co duy nhat 1 key -> gia tri ben trong.
    if len(value) == 1:
        key = next(iter(value))
        if key == "stringValue":
            return value[key]
        if key == "integerValue":
            try:
                return int(value[key])
            except (TypeError, ValueError):
                return value[key]
        if key == "doubleValue":
            try:
                return float(value[key])
            except (TypeError, ValueError):
                return value[key]
        if key == "booleanValue":
            return bool(value[key])
        if key == "nullValue":
            return None
        if key == "timestampValue":
            return value[key]
        if key == "bytesValue":
            return value[key]
        if key == "referenceValue":
            return value[key]
        if key == "arrayValue":
            raw = value[key].get("values") or []
            return [decode_value(v) for v in raw]
        if key == "mapValue":
            raw = value[key].get("fields") or {}
            return {k: decode_value(v) for k, v in raw.items()}

    # Map thuong (nhieu key) -> gia tri moi key cung duoc giai ma.
    return {k: decode_value(v) for k, v in value.items()}


def doc_id_from_name(name: str) -> str:
    """Lay id document tu duong dan day du trong Firestore export."""
    if not name:
        return ""
    return name.rsplit("/", 1)[-1]


def parse_entities_format(raw: dict) -> dict:
    """Doc file export co dang {"kind": "firestore:export", "entities": [...]}.

    Day la dang cua Firebase CLI va cung la dang cua Firebase Console.
    """
    result: dict[str, list] = {}

    for entity in raw.get("entities") or []:
        name = entity.get("name", "")
        if "/documents/" not in name:
            continue

        # Duong dan sau "/documents/": users/abc123 hoac users/abc123/addresses/xyz
        path = name.split("/documents/", 1)[1]
        parts = path.split("/")
        collection = parts[0]
        doc_id = doc_id_from_name(name)

        fields = entity.get("fields") or {}
        decoded = {k: decode_value(v) for k, v in fields.items()}
        # Giu duong dan day du de tach duoc subcollection `addresses`
        # (Firebase CLI ghep no nham vao collection `users`).
        decoded["_name"] = name

        # Document dia chi lay `userId` tu duong dan (u/uid/addresses/a).
        if collection == "users" and len(parts) >= 4 and parts[2] == ADDRESSES_COLLECTION:
            decoded["userId"] = parts[1]

        result.setdefault(collection, []).append({"id": doc_id, **decoded})

    return result


def parse_plain_list(raw: list) -> list:
    """Doc danh sach document phong (dung khi export tung collection).

    Moi phan tu co the co dang:
        {"fields": {...}, "name": "..."}   -> dung "fields" lam du lieu
        {"tenSanPham": "abc", ...}         -> dung chinh phan tu lam du lieu
    """
    docs = []
    for doc in raw:
        if not isinstance(doc, dict):
            continue
        source = doc.get("fields") if isinstance(doc.get("fields"), dict) else doc
        decoded = {k: decode_value(v) for k, v in source.items()}
        # Bo field "fields" / "name" / "createTime" khi phan tu la ban thuong.
        decoded.pop("fields", None)
        decoded.pop("createTime", None)
        decoded.pop("updateTime", None)
        name = doc.get("name", "")
        doc_id = doc_id_from_name(name) if name else ""
        if doc_id:
            decoded["id"] = doc_id
        docs.append(decoded)
    return docs


def load_export_file(path: Path) -> dict:
    """Doc 1 file export va tra ve {collection: [document, ...]}."""
    with path.open(encoding="utf-8") as f:
        raw = json.load(f)

    if isinstance(raw, dict) and "entities" in raw:
        return parse_entities_format(raw)

    if isinstance(raw, list):
        # Doan collection tu ten file: products.json -> "products".
        collection = path.stem
        return {collection: parse_plain_list(raw)}

    return {}


def load_from_export_dir(export_dir: Path) -> dict:
    """Doc moi file export trong thu muc va gop lai theo collection."""
    data: dict[str, list] = {}

    if not export_dir.exists():
        return data

    # File tong hop (Firebase CLI): all_namespaces_all_kinds.json
    for path in sorted(export_dir.glob("*.json")):
        if path.stem == "all_namespaces_all_kinds":
            for collection, docs in load_export_file(path).items():
                data.setdefault(collection, []).extend(docs)
            continue

        # File tung collection: products.json, product_images.json, ...
        parsed = load_export_file(path)
        for collection, docs in parsed.items():
            data.setdefault(collection, []).extend(docs)

    return data


def mask_email(email: str) -> str:
    """`nguyen@gmail.com` -> `ng****@example.com`."""
    if not email:
        return ""
    at = email.find("@")
    if at <= 0:
        return MASKED_NAME
    return f"{email[:2]}****@example.com"


def mask_phone(phone: str) -> str:
    """`0999991234` -> `09*****234`."""
    digits = "".join(c for c in phone if c.isdigit())
    if len(digits) < 4:
        return MASKED_PHONE
    return f"{digits[:2]}{'*' * (len(digits) - 4)}{digits[-2:]}"


def mask_value(value, key: str):
    """Che gia tri cua truong chua thong tin ca nhan."""
    if not isinstance(value, str):
        return value
    if key == "email":
        return mask_email(value)
    if key in ("phone", "receiverPhone"):
        return mask_phone(value)
    if key in ("fullName", "receiverName", "displayName"):
        return MASKED_NAME
    if key in ("street", "ward", "district", "city", "address"):
        return MASKED_ADDRESS
    if key == "photoUrl":
        return ""
    return value


def strip_images(value):
    """Bo chuoi base64 anh o moi cap (ke ca long nhau).

    Du lieu cu co the con base64 o nhung cho khac — vi du `orders[].items[].imageUrl`.
    """
    if isinstance(value, str):
        return None if value.startswith(BASE64_IMAGE_PREFIX) else value
    if isinstance(value, list):
        return [strip_images(v) for v in value]
    if isinstance(value, dict):
        return {k: strip_images(v) for k, v in value.items()}
    return value


def apply_mask(value, key: str, mask: bool):
    """Che thong tin ca nhan o moi cap cua du lieu."""
    if not mask:
        return value
    if isinstance(value, dict):
        return {k: apply_mask(v, k, mask) for k, v in value.items()}
    if isinstance(value, list):
        return [apply_mask(v, key, mask) for v in value]
    if key in PERSONAL_FIELDS:
        return mask_value(value, key)
    return value


def build_csdl(data: dict, mask_personal: bool = True, include_images: bool = True) -> dict:
    """Dung file JSON de nop: moi collection la 1 danh sach document."""
    csdl = {
        "_doc": (
            "CSDL app ECOMER (Firebase Cloud Firestore). "
            "products: tenSanPham, loaiSanPham, gia, image (id anh). "
            "product_images: moi anh mot document, truong 'data' la base64."
        ),
    }

    for collection in COLLECTIONS:
        docs = data.get(collection)
        if not docs:
            continue
        prepared = []
        for doc in docs:
            item = {k: v for k, v in doc.items() if k != "_name"}
            if not include_images:
                item = strip_images(item)
                # Bo truong rong sau khi strip.
                item = {k: v for k, v in item.items() if v is not None}
            item = apply_mask(item, "", mask_personal)
            prepared.append(item)
        csdl[collection] = prepared

    return csdl


def main() -> int:
    parser = argparse.ArgumentParser(description="Xuat CSDL ECOMER ra JSON de nop bai.")
    parser.add_argument(
        "--out", type=Path, default=DEFAULT_OUT, help="File JSON dau ra."
    )
    parser.add_argument(
        "--export-dir",
        type=Path,
        default=DEFAULT_EXPORT_DIR,
        help="Thu muc chua file export cua Firebase.",
    )
    parser.add_argument(
        "--no-images",
        action="store_true",
        help="Khong gom du lieu base64 cua anh (file JSON gon nhe).",
    )
    parser.add_argument(
        "--no-mask",
        action="store_true",
        help="KHONG che thong tin ca nhan (email, ten, so dien thoai, dia chi).",
    )
    args = parser.parse_args()

    data = load_from_export_dir(args.export_dir)

    if not data:
        print(
            "Chua co du lieu de xuat.\n"
            "\n"
            "Lam theo huong dan:\n"
            "  1. Firebase Console -> Firestore Database -> Export -> JSON\n"
            "  2. Bo file export vao thu muc .export/ voi ten dung:\n"
            "       products.json, product_images.json, ...\n"
            "  3. Chay lai: ./scripts/export-csdl.py\n"
            "\n"
            "  HOAC dung Firebase CLI:\n"
            "       npm install -g firebase-tools\n"
            "       firebase login\n"
            "       firebase firestore:export .export --project vku-ecomer\n"
            "       ./scripts/export-csdl.py",
            file=sys.stderr,
        )
        return 1

    # Tach dia chi (subcollection users/{uid}/addresses) ra collection rieng.
    #
    # Duong dan Firebase CLI: projects/X/databases/Y/documents/users/UID/addresses/AID
    # Phan sau "/documents/" la: users/UID/addresses/AID (4 phan).
    addresses = []
    users = []
    for doc in data.get("users", []):
        name = doc.get("_name", "")
        path = name.split("/documents/", 1)[-1]
        parts = path.split("/")
        if len(parts) >= 4 and parts[2] == ADDRESSES_COLLECTION:
            addresses.append(doc)
        else:
            users.append(doc)
    if addresses:
        data["users"] = users
        data[ADDRESSES_COLLECTION] = addresses

    csdl = build_csdl(
        data,
        mask_personal=not args.no_mask,
        include_images=not args.no_images,
    )
    args.out.parent.mkdir(parents=True, exist_ok=True)
    with args.out.open("w", encoding="utf-8") as f:
        json.dump(csdl, f, ensure_ascii=False, indent=2)

    print(f"==> Da xuat {args.out}")
    for collection, docs in csdl.items():
        if collection == "_doc":
            continue
        print(f"    {collection}: {len(docs)} documents")
    size_mb = args.out.stat().st_size / (1024 * 1024)
    print(f"    Tong dung luong file: {size_mb:.2f} MB")
    return 0


if __name__ == "__main__":
    sys.exit(main())
