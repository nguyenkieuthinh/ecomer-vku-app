# ECOMER — hệ thống nhập liệu sản phẩm (Kotlin + Jetpack Compose + Firebase)

> Đây **không phải** app mua hàng. Chỉ có 2 màn hình: đăng nhập/đăng ký → **Dữ liệu sản phẩm**.

## Lệnh nhanh (dùng script trong `scripts/`)

| Việc | Lệnh |
|---|---|
| Build + cài + mở app | `./scripts/dev.sh` |
| Build + cài lại **sạch** (xoá dữ liệu đăng nhập) | `./scripts/dev.sh --clean` |
| Chụp màn hình | `./scripts/shot.sh` hoặc `./scripts/shot.sh ten` |
| Bấm theo **text** (không cần đo pixel) | `./scripts/tap.sh "ĐĂNG KÝ"` |
| Bấm theo toạ độ | `./scripts/tap.sh 720 1370` |
| Xem text + toạ độ đang hiện | `./scripts/ui.sh` |
| Nhập chữ / xoá / bấm phím | `./scripts/type.sh "abc"`, `./scripts/type.sh clear`, `./scripts/type.sh key BACK` |
| Xem log lỗi | `./scripts/log.sh errors` |
| Xem log Firestore | `./scripts/log.sh firestore` |
| Chạy cả kịch bản nhiều bước | `./scripts/flow.sh kich_ban.txt` |

Biến môi trường: `PKG` (mặc định `com.ecomer.vku.app`), `WAIT` (giây chờ sau khi mở app).

## Vòng lặp test chuẩn

```bash
./scripts/dev.sh              # build + cài + mở
./scripts/tap.sh "Tên sản phẩm"
./scripts/shot.sh
./scripts/log.sh errors       # co loi khong
```

## Cấu trúc

```
app/src/main/java/com/ecomer/vku/app/
├── EcomerApp.kt              Application: khoi tao Firebase
├── MainActivity.kt           Entry point, chon man hinh dau theo trang thai dang nhap
├── data/
│   ├── FirebaseExt.kt        Wrapper Firebase + bao loi
│   ├── ImageEncoder.kt       Resize 1080px + nen <= 700KB + base64
│   ├── Base64Image.kt        Giai ma base64 -> byte (Coil 2 khong ho tro data URI)
│   ├── model/                Models.kt (Product, ProductImage, User)
│   └── repo/                 ProductRepository, UserRepository, AuthRepository
└── ui/
    ├── theme/                Màu, typography
    ├── components/           EcomerField, PrimaryButton, OutlineButton, ProductImage, banner
    ├── nav/                  EcomerNavHost, Screen
    └── screens/
        ├── auth/             SignInScreen, SignUpScreen, AuthViewModel
        └── products/         ProductsScreen, ProductsViewModel
```

## Luồng app

1. Mở app: chưa đăng nhập → `SignInScreen`; đã đăng nhập → thẳng `ProductsScreen`.
2. Đăng nhập / đăng ký → `ProductsScreen` (xoá sạch back stack).
3. Menu tài khoản (góc phải trên): email + badge `admin`/`khách` + **Đăng xuất** → về `SignInScreen`.

## Phân quyền sản phẩm

- **Mọi tài khoản đã đăng nhập đều thấy tất cả sản phẩm** (của mình và của người khác).
- **User thường**: thêm sản phẩm; chỉ sửa/xoá sản phẩm **do mình nhập** (dòng của người khác không hiện nút sửa/xoá).
- **Admin**: sửa/xoá **tất cả** sản phẩm.
- Danh sách chỉ hiện **3 thông tin chính** (Tên sp, Giá sp, Loại sp) + ảnh. **Không** hiện người nhập.
- Mỗi dòng có bút chì (sửa) và thùng rác đỏ (xoá); xoá phải xác nhận qua `AlertDialog`.
- Quyền được kiểm tra ở **server** bằng Firestore Rules; app chỉ ẩn nút cho gọn.

## Firestore

- Project: `vku-ecomer` (`app/google-services.json`)
- Collections:

| Collection | Nội dung |
|---|---|
| `products` | **đúng 4 trường**: `tenSanPham`, `loaiSanPham`, `gia`, `image` |
| `product_images` | mỗi ảnh 1 document: `productId`, `tieuDe`, `data` (base64), `tenFile`, `order`, `sizeBytes` |
| `product_owners` | chủ sở hữu: `userId`, `userEmail`, `createdAt` — tách riêng để `products` vẫn đúng 4 trường |
| `users` | hồ sơ: `uid`, `email`, `displayName`, `photoUrl`, `role`, `lastLoginAt` |
| `counters` | bộ đếm sinh id sản phẩm (vd `ao-nam`) |

- Rules: `firestore.rules`. **Phải publish sau mỗi lần sửa**:
  `firebase login` rồi `firebase deploy --only firestore:rules`, hoặc Firebase Console → Firestore → Rules → dán → Publish.
- Admin xác định bằng **email trong token** (`request.auth.token.email`), khớp với `User.ADMIN_EMAILS` trong code.
  - Sửa danh sách admin thì sửa **cả hai** nơi.

## Ảnh

Ảnh lưu **base64 trong Firestore**, mỗi ảnh một document riêng (`product_images`) nên giới hạn 1 MiB áp dụng cho từng ảnh — sản phẩm bao nhiêu ảnh cũng không vượt.

Ảnh được resize về cạnh dài tối đa **1080 px** và nén xuống dưới **700 KB** trước khi mã hoá.

**Coil 2 không có `DataUriFetcher`** — truyền chuỗi `data:image/jpeg;base64,...` cho `AsyncImage` sẽ không tải được. Phải giải mã thủ công bằng `Base64Image.decode()` rồi đưa `ByteArray` vào Coil.

Mỗi ảnh có trường **`tieuDe`** (tiêu đề ảnh). Ảnh đầu tiên là ảnh đại diện; `products.image` lưu **id** của ảnh đó (chuỗi ngắn), không lưu base64.

### Không nhét base64 vào `products`

Một ảnh base64 ~955 KB, Firestore giới hạn **1 MiB mỗi document**. `products` chỉ lưu `image` = **id ảnh**; ảnh tải lại từ `product_images` khi hiển thị.

## Lưu ý

- `app/google-services.json` chứa API key — **không commit lên GitHub công khai**.
- Đề thi yêu cầu sản phẩm chỉ có **4 trường** — chủ sở hữu lưu ở `product_owners`, **không** thêm trường vào document sản phẩm.
- Code cũ (giỏ hàng, đơn hàng, admin dashboard...) đã xoá khỏi source; nằm trong lịch sử git nếu cần xem lại.

## CSDL (json) — file nộp bài

- CSDL là **Firebase Cloud Firestore**. File nộp bài: `docs/csdl.json` (sinh ra từ Firestore, **không** phải màn hình trong app).
- Chi tiết + hướng dẫn xuất: `docs/CSDL.md`.

```bash
npm install -g firebase-tools
firebase login
firebase firestore:export .export --project vku-ecomer
./scripts/export-csdl.py              # file đầy đủ (kèm base64)
./scripts/export-csdl.py --no-images  # file gọn (bỏ base64) — nộp bài
```

- `.export/` và `app/google-services.json` đã nằm trong `.gitignore`.
