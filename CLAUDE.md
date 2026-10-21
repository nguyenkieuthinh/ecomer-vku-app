# ECOMER — app shop thời trang (Kotlin + Jetpack Compose + Firebase)

## Lệnh nhanh (dùng script trong `scripts/`)

| Việc | Lệnh |
|---|---|
| Build + cài + mở app | `./scripts/dev.sh` |
| Build + cài lại **sạch** (xoá dữ liệu đăng nhập) | `./scripts/dev.sh --clean` |
| Chụp màn hình | `./scripts/shot.sh` hoặc `./scripts/shot.sh ten` |
| Bấm theo **text** (không cần đo pixel) | `./scripts/tap.sh "ĐĂNG KÝ"` |
| Bấm theo toạ độ | `./scripts/tap.sh 720 1370` |
| Xem text + toạ độ đang hiện | `./scripts/ui.sh` |
| Nhập chu / xoá / bấm phím | `./scripts/type.sh "abc"`, `./scripts/type.sh clear`, `./scripts/type.sh key BACK` |
| Xem log lỗi | `./scripts/log.sh errors` |
| Xem log Firestore | `./scripts/log.sh firestore` |
| Chạy cả kịch bản nhiều bước | `./scripts/flow.sh kich_ban.txt` |

Biến môi trường: `PKG` (mặc định `com.ecomer.vku.app`), `WAIT` (giây chờ sau khi mở app).

## Vòng lặp test chuẩn

```bash
./scripts/dev.sh          # build + cài + mở
./scripts/tap.sh "Shop"   # thao tác
./scripts/shot.sh         # chụp xem
./scripts/log.sh errors   # co loi khong
```

## Cấu trúc

```
app/src/main/java/com/ecomer/vku/app/
├── EcomerApp.kt              Application: khoi tao Firebase
├── MainActivity.kt           Entry point
├── data/
│   ├── FirebaseExt.kt        Wrapper Firebase + bao loi
│   ├── ImageEncoder.kt       Resize 1080px + nen <= 700KB + base64
│   ├── Base64Image.kt        Giai ma base64 -> byte (Coil 2 khong ho tro data URI)
│   ├── model/                Product, User, CartItem, Order
│   └── repo/                 Product, Cart, Order, User, Auth, Storage
└── ui/
    ├── theme/                Màu, typography
    ├── components/           EcomerField, PrimaryButton, BottomNav, ProductImage
    ├── nav/                  EcomerNavHost, Screen, ImageViewerViewModel
    └── screens/              auth, home, detail, cart, orders, account, admin
```

## Firebase

- Project: `vku-ecomer` (`app/google-services.json`)
- Firestore collections: `products`, `product_images`, `carts`, `orders`, `users`, `counters`
- Rules: `firestore.rules` (deploy: `firebase deploy --only firestore:rules`)
- Admin được xác định bằng **email trong token** (`request.auth.token.email`), khớp với `User.ADMIN_EMAILS` trong code.
  - Sửa danh sách admin thì sửa **cả hai** nơi.

## Ảnh

Ảnh lưu **base64 trong Firestore**, mỗi ảnh một document riêng (`product_images`) nên giới hạn 1 MiB áp dụng cho từng ảnh — sản phẩm bao nhiêu ảnh cũng không vượt.

Ảnh được resize về cạnh dài tối đa **1080 px** và nén xuống dưới **700 KB** trước khi mã hoá.

**Coil 2 không có `DataUriFetcher`** — truyền chuỗi `data:image/jpeg;base64,...` cho `AsyncImage` sẽ không tải được. Phải giải mã thủ công bằng `Base64Image.decode()` rồi đưa `ByteArray` vào Coil.

## Lưu ý

- `app/google-services.json` chứa API key — **không commit lên GitHub công khai**.
- Firestore Rules chặn user thường ghi vào `products`; bước trừ kho khi đặt hàng cũng ghi `products` nên cần rules cho phép, hoặc chuyển sang Cloud Function.
