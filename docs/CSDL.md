# CSDL (json) — file nộp bài

Đề bài yêu cầu: *"Sinh viên nộp link github source code và link video demo, csdl(json)"*.

→ CSDL là **một file JSON** chứa toàn bộ dữ liệu của app. Không phải màn hình trong app.

---

## 1. CSDL nằm ở đâu?

| | |
|---|---|
| Hệ quản trị CSDL | **Firebase Cloud Firestore** (project `vku-ecomer`) |
| Xem trên máy | Firebase Console → Firestore Database → thấy collection `products`, `product_images`, ... |
| File nộp | `docs/csdl.json` — sinh ra bằng script `scripts/export-csdl.py` |

Firestore lưu document dạng JSON thuần, nên **file export chính là dữ liệu thật của app** — không phải bản sao được dựng lại.

```
Firestore (trên cloud)  ──export──▶  .export/*.json  ──script──▶  docs/csdl.json  ──▶ nộp bài
```

---

## 2. Các collection trong app

| Collection | Nội dung | Ghi chú |
|---|---|---|
| `products` | Sản phẩm — **đúng 4 trường** `tenSanPham`, `loaiSanPham`, `gia`, `image` | `image` là **id ảnh**, không phải base64 |
| `product_images` | Mỗi ảnh 1 document: `productId`, `tieuDe`, `data` (base64), `tenFile`, `order`, `sizeBytes` | `data` là base64 → **file JSON nặng** |
| `product_owners` | Chủ sở hữu sản phẩm: `userId`, `userEmail` | tách riêng để **không** thêm field `userId` vào `products` |
| `users` | Hồ sơ: `uid`, `email`, `displayName`, `photoUrl`, `role`, `lastLoginAt` | |
| `counters` | Bộ đếm sinh id (vd `ao-nam`) | |

> **Vì sao ảnh và chủ sở hữu tách riêng?** Firestore giới hạn **1 MiB mỗi document**. Một ảnh base64 ~955 KB → để trong `products` là vượt ngay. Nên:
> - `product_images`: mỗi ảnh 1 document → sản phẩm bao nhiêu ảnh cũng không vượt.
> - `product_owners`: chỉ lưu uid/email, giữ `products` đúng 4 trường như đề yêu cầu.
> - Quyền: mọi tài khoản đã đăng nhập đều **xem** tất cả sản phẩm; user chỉ sửa/xoá sản phẩm của mình, admin sửa/xoá tất cả.

---

## 3. Cách xuất file JSON

### Cách A — Firebase CLI (khuyến nghị, xuất được cả database)

```bash
npm install -g firebase-tools     # cài 1 lần
firebase login                    # đăng nhập bằng tài khoản Google (mở trình duyệt)
firebase firestore:export .export --project vku-ecomer
./scripts/export-csdl.py
```

Kết quả: `.export/all_namespaces_all_kinds.json` → `docs/csdl.json`.

### Cách B — Firebase Console (không cài gì)

1. Vào [Firebase Console](https://console.firebase.google.com) → project `vku-ecomer` → **Firestore Database**
2. Chọn collection → nút **⋮** → **Export** → chọn **JSON**
3. Lặp lại cho từng collection
4. Tải file về, đặt vào thư mục `.export/` với đúng tên: `products.json`, `product_images.json`, `product_owners.json`, ...
5. `./scripts/export-csdl.py`

> Console chỉ export được **từng collection một**, nên phải làm lặp lại 7 lần. Cách A nhanh hơn.

---

## 4. File JSON trông như thế nào?

Sau khi script chạy, `docs/csdl.json` có cấu trúc: **mỗi collection là một mảng JSON**, mỗi phần tử là một document đã bỏ wrapper `{"integerValue": ...}` của Firestore.

```jsonc
{
  "_doc": "CSDL app ECOMER (Firebase Cloud Firestore). ...",

  "products": [
    {
      "id": "ao-nam-001",
      "tenSanPham": "Áo VKU",
      "loaiSanPham": "Áo nam",
      "gia": 1000000,
      "image": "ao-nam-001-1"          // ← id ảnh, trỏ sang product_images
    }
  ],

  "product_images": [
    {
      "id": "ao-nam-001-1",
      "productId": "ao-nam-001",
      "tieuDe": "Áo VKU",
      "data": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ...",  // ← ảnh thật
      "tenFile": "ao-vku.jpg",
      "order": 0,
      "sizeBytes": 183420
    }
  ],

  "product_owners": [
    {
      "id": "ao-nam-001",
      "userId": "abc123uid",
      "userEmail": "user@example.com",
      "createdAt": "2026-10-22T09:31:00.000000Z"
    }
  ],

  "users": [
    { "id": "abc123uid", "uid": "abc123uid", "email": "admin@ecomer.vn",
      "displayName": "admin", "photoUrl": "", "role": "admin",
      "lastLoginAt": "2026-10-22T09:00:00.000000Z" }
  ],

  "counters": [ { "id": "ao-nam", "count": 1 } ]
}
```

---

## 5. File nặng — xử lý sao?

`product_images.data` là base64 nên **1 ảnh ~955 KB**. 20 ảnh ⇒ file `csdl.json` ~19 MB.

| Nhu cầu | Lệnh |
|---|---|
| File đầy đủ (kèm ảnh) | `./scripts/export-csdl.py` |
| File gọn (bỏ base64, giữ metadata) | `./scripts/export-csdl.py --no-images` |

**Nộp bài:** thường nộp file gọn (metadata + cấu trúc đầy đủ) vì file nặng khó up lên GitHub. Ảnh thật vẫn nằm trên Firebase và có trong video demo.

---

## 6. Lưu ý bảo mật

- File export **không chứa API key** → an toàn để nộp.
- `app/google-services.json` chứa API key → **đã nằm trong `.gitignore`**, không được commit lên GitHub công khai.
- Thư mục `.export/` cũng đã được thêm vào `.gitignore` (file thô, không cần commit).

---

## 7. Checklist nộp bài

- [ ] Link GitHub source code
- [ ] Link video demo (Google Drive / YouTube — nhớ bật quyền xem)
- [ ] `docs/csdl.json` (sinh từ Firestore)
- [ ] Firestore Rules đã deploy: `firebase deploy --only firestore:rules`
- [ ] `google-services.json` **không** có trong repo
