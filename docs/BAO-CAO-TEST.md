# BÁO CÁO KIỂM THỂ — App ECOMER

**Ngày kiểm thử:** 22/10/2026
**Thiết bị:** Xiaomi 25019PNF3C (Android, HyperOS), màn 1440×3200
**Phiên bản build:** `app-debug.apk` — package `com.ecomer.vku.app`
**Người thực hiện:** Claude Code (đóng vai người dùng thật, thao tác qua adb)
**Backend:** Firebase project `vku-ecomer`

---

## 1. Tổng quan

| Hạng mục | Kết quả |
|---|---|
| Build | ✅ `./gradlew assembleDebug` thành công, không cảnh báo |
| Cài đặt & khởi động | ✅ Không crash |
| Đăng ký bằng email | ✅ Thành công, tự chuyển sang màn Shop |
| Đăng nhập bằng email | ✅ Thành công |
| Phân quyền admin/khách | ✅ Đúng — admin thấy "Quản lý sản phẩm", khách không thấy |
| Đăng nhập bằng Gmail | ✅ Bảng chọn tài khoản mở được, không treo |
| Xem danh sách sản phẩm | ✅ Ảnh hiển thị đúng |
| Xem chi tiết sản phẩm | ✅ Nhiều ảnh, có ảnh đại diện |
| Xem ảnh phóng to | ✅ Vuốt ngang chuyển ảnh, có chỉ số "1 / 2" |
| Thêm / giảm số lượng giỏ hàng | ✅ Tổng tiền tính đúng (66.666 × 3 = 199.998) |
| **Đặt hàng** | ❌ **KHÔNG HOẠT ĐỘNG** (BUG-01) |
| Quản lý sản phẩm (admin) | ⚠️ Cần test thêm (chưa xác minh được hết) |

**Tổng: 15 lỗi phát hiện — 2 nghiêm trọng, 7 trung bình, 6 nhẹ.**

> **Cập nhật lần 2** (sau khi thêm tính năng địa chỉ + thanh toán): thêm BUG-13, BUG-14, BUG-15
> và mục "Tính năng mới" ở mục 7.

---

## 2. Danh sách lỗi

### BUG-01 · Đặt hàng không thành công — *Nghiêm trọng*

**Mô tả:** Khách thêm sản phẩm vào giỏ, bấm **ĐẶT HÀNG** → không có gì xảy ra. Giỏ hàng vẫn nguyên, tab "Đơn hàng" hiện *"Bạn chưa có đơn hàng nào"*.

**Bước tái hiện:**
1. Đăng nhập tài khoản khách (`khach1@test.com`)
2. Shop → bấm sản phẩm → **THÊM VÀO GIỎ**
3. Tab Giỏ hàng → bấm **ĐẶT HÀNG**
4. → Không có phản hồi, vẫn ở màn Giỏ hàng

**Nguyên nhân:** Bước trừ kho (`reserveStock`) chạy transaction ghi vào collection `products`. Nhưng `firestore.rules` đang đặt:
```
match /products/{productId} {
  allow write: if isAdmin();
}
```
→ Khách đăng nhập không có quyền ghi `products` → transaction thất bại → `placeOrder` dừng lại, không tạo đơn.

**Cách sửa (chọn 1):**
- **(a) Nới rules** cho phép user đã đăng nhập giảm kho:
  ```
  match /products/{productId} {
    allow read: if true;
    allow write: if isAdmin();
    // Cho phep giam ton kho khi dat hang.
    allow update: if isAdmin()
      || (request.auth != null
          && request.resource.data.diff(resource.data).affectedKeys()
             .hasOnly(['availableStock', 'soldCount']));
  }
  ```
  → Hạn chế chỉ cho đổi `availableStock`/`soldCount`, không sửa được giá/tên.
- **(b) Chuyển việc trừ kho sang Cloud Function** (đúng chuẩn production, an toàn hơn).

**File liên quan:**
- `app/src/main/java/com/ecomer/vku/app/ui/screens/home/ShopViewModel.kt:placeOrder()`
- `app/src/main/java/com/ecomer/vku/app/data/repo/ProductRepository.kt:reserveStock()`
- `firestore.rules:30-33`

---

### BUG-02 · Thông báo lỗi bị nuốt im lặng — *Trung bình*

**Mô tả:** Nhiều màn hình có trạng thái `message` trong ViewModel nhưng **không hề render ra UI**. Người dùng bấm nút không có phản hồi, dễ tưởng app treo.

**Chi tiết:** Đếm số lần `state.message` xuất hiện trong từng màn:

| Màn hình | Có render `message`? |
|---|---|
| `HomeScreen` | ❌ Không |
| `CartScreen` | ❌ Không |
| `OrdersScreen` | ❌ Không |
| `ProductDetailScreen` | ❌ Không |
| `AccountScreen` | ❌ Không |
| `AdminUsersScreen` | ❌ Không |
| `AdminProductsScreen` | ✅ Có |
| `AdminDashboardScreen` | ✅ Có |
| `AdminOrdersScreen` | ✅ Có |

**Cách sửa:** Thêm `ErrorBanner`/`ToastMessage` vào 6 màn còn thiếu, hoặc bật/tắt hiển thị qua một `Snackbar` dùng chung.

---

### BUG-03 · Chip danh mục bị bóp méo — *Trung bình*

**Mô tả:** Trên màn Shop, các chip danh mục hiển thị sai kích thước. Chip có tên ngắn ("k") bị bóp thành hình tròn, chip kế tiếp hiện thành **tròn xanh không có chữ**.

**Nguyên nhân:** `CategoryChip` dùng `Box` với `Modifier.height(36.dp)` cố định, không có ràng buộc bề rộng tối thiểu. Trong `LazyRow`, item được đo với `minWidth = 0` nên `Box` co về 0.

**Cách sửa:** Thêm `Modifier.widthIn(min = 44.dp)` + `wrapContentWidth()`, hoặc đổi `Box` → `Surface`/`Text` có padding tự co giãn.

**File:** `app/src/main/java/com/ecomer/vku/app/ui/components/Widgets.kt` — hàm `CategoryChip`

---

### BUG-04 · Ảnh bị letterbox, không lấp đầy khung — *Trung bình*

**Mô tả:** Ảnh sản phẩm luôn hiện nền xám 2 bên (ảnh vuông trong khung chữ nhật), ở cả:
- Màn chi tiết (khung 390×300)
- Giỏ hàng (ô 64×64)
- Lưới sản phẩm

**Nguyên nhân:** `AsyncImage` mặc định `contentScale = ContentScale.Fit` (vừa khung, giữ tỉ lệ). Bản thiết kế gốc dùng ảnh **cover** lấp đầy khung.

**Cách sửa:** Thêm `contentScale = ContentScale.Crop` cho `AsyncImage` trong `ProductImage`.

**File:** `app/src/main/java/com/ecomer/vku/app/ui/components/Common.kt` — hàm `ProductImage`

---

### BUG-05 · Avatar hiện "?" thay vì chữ cái đầu — *Trung bình*

**Mô tả:** Ở màn Tài khoản, avatar hiển thị dấu "?" thay vì 2 chữ cái đầu của tên/email (đã xác nhận với cả `admin@ecomer.vn`).

**Nguyên nhân:** `AccountScreen` truyền `name = user?.displayName ?: user?.email.orEmpty()`. Với tài khoản đăng ký bằng email, `displayName` là **chuỗi rỗng** (không phải `null`) nên toán tử `?:` không kích hoạt → `name = ""` → `UserAvatar` fallback về "?".

**Cách sửa:** Dùng `displayName.ifBlank { email }` thay vì `?:`.

**File:** `app/src/main/java/com/ecomer/vku/app/ui/screens/account/AccountScreen.kt:78`

---

### BUG-06 · Nút đóng màn xem ảnh nằm giữa màn hình — *Nhẹ*

**Mô tả:** Ở màn xem ảnh phóng to, nút **✕** nằm ở **giữa** phía trên thay vì ở góc phải.

**Nguyên nhân:** `Column` có `fillMaxWidth()` + `Alignment.CenterHorizontally` → nút bị canh giữa.

**Cách sửa:** Đặt nút trong `Box` với `Modifier.align(Alignment.TopEnd)`.

**File:** `app/src/main/java/com/ecomer/vku/app/ui/screens/detail/ImageViewerScreen.kt`

---

### BUG-07 · Placeholder của ô mật khẩu không hiện với trình đọc màn hình — *Nhẹ*

**Mô tả:** Ô mật khẩu bị đánh dấu `password="true"` trong accessibility tree → nhãn placeholder ("Mật khẩu") **không được đọc bởi screen reader**, và automation tool không tìm được ô này bằng text.

**Cách sửa:** Dùng `Modifier.semantics { contentDescription = placeholder }` cho ô mật khẩu, hoặc thêm `passwordVisualTransformation` cùng `contentDescription`.

**Ghi chú:** Đây cũng là lý do `scripts/tap.sh` cần thêm chế độ `--field` (bấm theo thứ tự ô nhập).

---

### BUG-08 · Banner lỗi đẩy toàn bộ form xuống dưới — *Nhẹ*

**Mô tả:** Khi đăng ký sai, banner lỗi *"Hai mật khẩu không khớp"* hiện **phía trên** các ô → toàn bộ form bị đẩy xuống ~90px. Với bàn phím mở, nội dung bị tràn khỏi màn hình và người dùng không nhìn thấy ô đang nhập.

**Cách sửa:** Đặt banner lỗi **dưới** form, hoặc dùng `Snackbar` không ảnh hưởng layout.

**File:** `app/src/main/java/com/ecomer/vku/app/ui/screens/auth/SignUpScreen.kt`

---

### BUG-09 · Tổng tiền hiển thị lẻ (199.998 đ) — *Nhẹ*

**Mô tả:** Giá 66.666 đ × 3 = **199.998 đ**. Với tiền VND, người dùng quen thấy số tròn.

**Cách sửa:** Làm tròn khi hiển thị (`price / 1000 * 1000`) hoặc khi lưu giá cho sẵn làm tròn.

---

### BUG-10 · Sản phẩm lưu trước đó không có `createdAt` bị ẩn khỏi trang chủ — *Trung bình (đã sửa)*

**Mô tả:** Sản phẩm tạo trước khi thêm trường `createdAt` **không bao giờ hiện** ở trang chủ.

**Nguyên nhân:** Firestore loại bỏ khỏi kết quả mọi document **không có field** được dùng trong `orderBy`. Query cũ là `orderBy("createdAt")` → sản phẩm cũ (thiếu field) biến mất im lặng.

**Đã sửa:** Bỏ `orderBy`, chuyển sang sắp xếp tại app theo `createdAt`; đồng thời đổi `get()` (đọc 1 lần) thành `addSnapshotListener` (live) để danh sách tự cập nhật khi admin thêm/sửa/xoá.

**File:** `app/src/main/java/com/ecomer/vku/app/data/repo/ProductRepository.kt:observeProducts()`

---

### BUG-11 · Trừ kho dùng `set()` sẽ xoá field của sản phẩm — *Trung bình (đã sửa)*

**Mô tả:** `updateProduct()` dùng `set()` (ghi đè toàn bộ document) nên làm mất các field không nằm trong map — gồm `createdAt`, `soldCount`.

**Cách sửa:** Đổi sang `update()` (chỉ ghi các field được chỉ định).

**File:** `app/src/main/java/com/ecomer/vku/app/data/repo/ProductRepository.kt:updateProduct()`

---

### BUG-12 · `ensureUser()` xoá field `email`/`role` của tài khoản — *Trung bình (đã sửa)*

**Môtả:** `ensureUser()` dùng `set(mapOf("lastLoginAt" to ...))` khi tài khoản đã tồn tại → ghi đè document, mất `email` và `role`. Điều này làm tài khoản admin **hiện thành "khách"** vì app không đọc được email để đối chiếu danh sách admin.

**Cách sửa:** Đổi sang `update()`; đồng thời mọi thông tin email/role giờ lấy từ **Firebase Auth** (nguồn chính xác, giống cách Firestore Rules kiểm tra `request.auth.token.email`).

**File:** `app/src/main/java/com/ecomer/vku/app/data/repo/UserRepository.kt:ensureUser()`

### BUG-13 · Đổi tài khoản nhưng vẫn thấy dữ liệu tài khoản cũ — *Nghiêm trọng*

**Mô tả:** Tài khoản A đặt hàng → đăng xuất → đăng nhập tài khoản B → tab "Đơn hàng" của B **vẫn hiện đơn hàng của A** (tương tự cho Giỏ hàng và Địa chỉ).

**Nguyên nhân (2 tầng):**
1. `ShopViewModel` / `CheckoutViewModel` / `AddressViewModel` **cache `uid` lúc khởi tạo**:
   ```kotlin
   private val userId: String? = FirebaseExt.currentUserId   // ← sai
   ```
   ViewModel còn sống sau khi đổi tài khoản → vẫn đọc dữ liệu của uid cũ.
2. Bottom nav dùng `popUpTo { saveState = true }` + `restoreState = true` → ViewModelStore được **lưu lại và khôi phục**, giữ nguyên instance cũ.

**Đã sửa:**
- uid đọc **động** (biến `var`, không phải `val`), kèm listener `addAuthStateListener` → khi uid đổi thì xoá dữ liệu cũ và tải lại.
- Bỏ `saveState`/`restoreState` ở bottom nav → ViewModel cũ bị hủy đúng cách.
- `onCleared()` gỡ listener.

**File:** `ui/screens/home/ShopViewModel.kt`, `ui/screens/checkout/CheckoutViewModel.kt`, `ui/screens/address/AddressViewModel.kt`, `ui/nav/EcomerNavHost.kt`

---

### BUG-14 · Hiện thông báo lỗi tiếng Anh của Firebase cho người dùng — *Trung bình*

**Mô tả:** Khi lưu địa chỉ lỗi, app hiện thẳng chuỗi `"PERMISSION_DENIED: Missing or insufficient permissions."` — người dùng không hiểu phải làm gì.

**Đã sửa:** Thêm `FirebaseExt.friendlyMessage()` dịch các lỗi Firestore sang tiếng Việt, kèm hướng dẫn cụ thể (ví dụ *"Kiểm tra lại Security Rules trên Firebase Console (nhớ bấm Publish)"*).

**File:** `data/FirebaseExt.kt`

---

### BUG-15 · Không có trạng thái "Hủy" cho đơn hàng — *Nhẹ*

**Mô tả:** Màn quản lý đơn hàng chỉ có 3 trạng thái (Chờ xử lý / Đang giao / Hoàn thành), không có cách hủy đơn.

**Đã sửa:** Thêm nút **"Hủy"** (màu đỏ). Badge "Đã hủy" đã có sẵn trong `StatusBadge`.

**File:** `ui/screens/admin/AdminOrdersScreen.kt`

---

---

## 3. Lỗi đã phát hiện & sửa trong quá trình test

| Lỗi | Nguyên nhân | Cách sửa |
|---|---|---|
| Ảnh không hiện (ô xám) | **Coil 2 không có `DataUriFetcher`** → không tải được chuỗi `data:image/jpeg;base64,...` | Tự giải mã base64 → `ByteArray` bằng `Base64Image.decode()` rồi đưa vào Coil (có `ByteArrayMapper` + `ByteBufferFetcher`) |
| Mọi ảnh mã hoá thất bại | `ImageEncoder.decodeScaled()` gộp `openInputStream()?.use { decodeStream(...) }` — `decodeStream` với `inJustDecodeBounds=true` **luôn trả null** → elvis `?: return null` làm hỏng mọi ảnh | Tách phần mở stream ra khỏi `use`, kiểm tra null riêng |
| App crash khi mở (Firestore lỗi) | Exception từ `flow{}` không được bắt → lan ra `viewModelScope` | Bọc `runCatching` trong mọi repository + fallback về dữ liệu mẫu |
| Bottom nav bị đè xuống dưới system nav bar | Thứ tự modifier sai | `.navigationBarsPadding()` **trước** `.height(64.dp)` |
| Nội dung tràn lên status bar | Bật `enableEdgeToEdge()` nhưng không xử lý insets | `contentWindowInsets = WindowInsets.safeDrawing` + `statusBarsPadding()` |
| Đăng nhập Gmail bị treo 25s | Luồng Play Services không có timeout | Thêm timeout 120s cho luồng legacy, 20s cho Credential Manager |
| Banner lỗi Firestore không hiện | `lastError` là `@Volatile` var, không phải Compose `State` | Đổi sang `mutableStateOf` |
| Ảnh trong danh sách admin không hiện | `imageUrl = ""` hardcode | Truyền thumbnail thật từ `AdminViewModel` |

---

## 4. Vấn đề cấu hình Firebase (cần người dùng xử lý)

Không sửa được từ code — cần thao tác trên Firebase Console:

| Việc | Tác động |
|---|---|
| **Tạo Firestore database** cho project `vku-ecomer` | Trước đó app chỉ chạy được dữ liệu mẫu |
| Bật provider **Google** trong Authentication | Đăng nhập Gmail cần provider này |
| Bật provider **Email/Password** trong Authentication | Đăng ký/đăng nhập email cần provider này |
| **Publish lại `firestore.rules`** | Bản hiện tại chặn khách trừ kho → BUG-01 |
| Sửa danh sách admin ở **2 nơi** khi đổi: `User.ADMIN_EMAILS` (code) + `isAdmin()` trong `firestore.rules` | Lệch → admin không vào được trang quản trị |

**Rủi ro bảo mật:** `app/google-services.json` chứa API key của project. Nếu repo này được push lên GitHub công khai thì key bị lộ. Nên thêm `app/google-services.json` vào `.gitignore` và dùng `google-services.json` mẫu (`google-services.json.example`) cho người khác clone.

---

## 5. Cần test thêm

| # | Hành trình | Mục đích |
|---|---|---|
| 1 | Admin → Quản lý sản phẩm → **THÊM SẢN PHẨM** chọn 2 ảnh | Kiểm tra ảnh hiện, hiển thị tổng dung lượng, chọn ảnh đại diện |
| 2 | Admin → bấm **Sửa** → thêm 1 ảnh, xoá 1 ảnh → **CẬP NHẬT** | Kiểm tra thêm/xoá ảnh khi sửa |
| 3 | Admin → bấm vào sản phẩm trong danh sách | Kiểm tra mở màn chi tiết |
| 4 | Admin → **Quản trị → Người dùng** | Kiểm tra danh sách khách + số đơn |
| 5 | Admin → **Quản trị → Đơn hàng** → đổi trạng thái | Kiểm tra cập nhật trạng thái |
| 6 | Khách → **ĐẶT HÀNG** (sau khi sửa rules) | Xác nhận BUG-01 đã hết |
| 7 | Tài khoản → **Đổi mật khẩu** (nhập sai mật khẩu cũ) | Kiểm tra thông báo lỗi |
| 8 | Khách → tìm kiếm / lọc danh mục | Kiểm tra ô tìm kiếm và chip lọc |
| 9 | **Đăng xuất rồi đăng nhập tài khoản khác** (không restart app) | Kiểm tra ViewModel có bị cache lại không |

---

## 6. Tính năng mới (đợt 2)

| Tính năng | Mô tả | File |
|---|---|---|
| **Địa chỉ giao hàng** | Thêm/sửa/xoá, đặt địa chỉ mặc định. Lưu trong subcollection `users/{uid}/addresses` | `data/model/Address.kt`, `data/repo/AddressRepository.kt`, `ui/screens/address/` |
| **Mục trong Tài khoản** | "Địa chỉ giao hàng" (không bắt buộc khi đăng ký) | `AccountScreen.kt` |
| **Màn thanh toán** | Chọn địa chỉ, xem sản phẩm, tổng tiền, xác nhận | `ui/screens/checkout/` |
| **Bắt buộc địa chỉ khi mua** | Chưa có địa chỉ → hiện empty state kèm nút "Thêm địa chỉ"; nút xác nhận bị vô hiệu hoá | `CheckoutScreen.kt` |
| **2 nút ở chi tiết sản phẩm** | **THÊM VÀO GIỎ** và **MUA NGAY** (MUA NGAY đi thẳng màn thanh toán) | `ProductDetailScreen.kt` |
| **Giao diện riêng cho admin** | Bottom nav chỉ 2 tab (Shop, Tài khoản) — admin xem đơn qua trang "Quản trị cửa hàng" | `BottomNav.kt`, `EcomerNavHost.kt` |
| **Lưu địa chỉ trong đơn** | Đơn hàng lưu snapshot địa chỉ + tên + SĐT người nhận | `Models.kt`, `OrderRepository.kt` |
| **Thông báo lỗi tiếng Việt** | Dịch lỗi Firestore sang tiếng Việt | `FirebaseExt.kt` |
| **`.gitignore` + file mẫu config** | Không lộ API key khi push repo | `.gitignore`, `app/google-services.json.example` |

---

## 7. Công cụ test

Đã tạo `scripts/` để test nhanh hơn:

| Lệnh | Việc |
|---|---|
| `./scripts/dev.sh` | Build + cài + mở app |
| `./scripts/dev.sh --clean` | Xoá app cũ (xoá luôn dữ liệu đăng nhập) rồi cài lại |
| `./scripts/shot.sh [tên]` | Chụp màn hình → `/tmp/<tên>.png` |
| `./scripts/tap.sh "text"` | Bấm theo text trên màn hình (tự tìm toạ độ) |
| `./scripts/tap.sh --field 2` | Bấm ô nhập thứ 2 (dùng cho ô mật khẩu — không tìm được bằng text) |
| `./scripts/type.sh "chu"` | Nhập chu |
| `./scripts/type.sh clear` | Xoá ô đang focus |
| `./scripts/ui.sh` | Xem text + toạ độ đang hiện |
| `./scripts/log.sh errors` | Xem log lỗi/crash |
| `./scripts/flow.sh <file>` | Chạy cả kịch bản nhiều bước |

Chi tiết trong `CLAUDE.md`.
