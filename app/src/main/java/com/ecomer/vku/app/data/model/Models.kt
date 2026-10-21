package com.ecomer.vku.app.data.model

/**
 * San pham trong cua hang.
 *
 * Theo de thi, document san pham CHI co 4 truong: `tenSanPham`, `loaiSanPham`,
 * `gia` va `image` (id cua document anh). Khong duoc them truong khac.
 *
 * @param id id cua document trong Firestore (collection "products").
 * @param tenSanPham ten san pham.
 * @param loaiSanPham loai san pham, dung cho chip loc o man hinh Shop.
 * @param gia gia ban (VND).
 * @param image id cua document anh dai dien trong collection "product_images".
 * @param visible san pham dang hien voi khach hay da bi admin an.
 *   Truong nay CHI ton tai trong RAM — KHONG duoc ghi xuong Firestore.
 *   Trang thai an duoc luu o collection rieng "hidden_products" de document
 *   san pham van chi co 4 truong nhu de thi yeu cau.
 */
data class Product(
    val id: String = "",
    val tenSanPham: String = "",
    val loaiSanPham: String = "",
    val gia: Long = 0,
    val image: String = "",
    val visible: Boolean = true,
)

/**
 * Mot anh san pham, luu rieng mot document trong collection "product_images".
 *
 * Anh duoc luu rieng nen gioi han 1 MiB cua Firestore ap dung cho TUNG anh —
 * san pham bao nhieu anh cung khong vuot gioi han.
 *
 * @param id id cua document anh.
 * @param productId id cua san pham chua anh nay.
 * @param tieuDe tieu de cua anh (trung ten san pham) — hinh anh duoc luu
 *   va hien thi theo tung tieu de.
 * @param data duong dan base64 cua anh, kem tien to "data:image/jpeg;base64,".
 * @param tenFile ten file goc cua anh.
 * @param order thu tu hien thi (0 la anh dau).
 */
data class ProductImage(
    val id: String = "",
    val productId: String = "",
    val tieuDe: String = "",
    val data: String = "",
    val tenFile: String = "",
    val order: Int = 0,
) {
    companion object {
        const val COLLECTION = "product_images"
        const val PRODUCT_COLLECTION = "products"
        const val COUNTER_COLLECTION = "counters"

        /**
         * San pham dang an — luu o collection rieng de KHONG them truong
         * "visible" vao document san pham (de thi yeu cau chi 4 truong).
         */
        const val HIDDEN_COLLECTION = "hidden_products"

        /**
         * Chuyen "Áo nam" thanh "ao-nam" de dung lam id.
         */
        fun slug(category: String): String =
            category.trim()
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifEmpty { "san-pham" }
    }
}

/**
 * Mot mon trong gio hang cua nguoi dung.
 *
 * @param productId id san pham trong collection "products".
 * @param quantity so luong.
 */
data class CartItem(
    val productId: String = "",
    val name: String = "",
    val price: Long = 0,
    val imageUrl: String = "",
    val quantity: Int = 1,
) {
    val subtotal: Long get() = price * quantity
}

/**
 * Mot don hang da dat.
 *
 * @param id id cua document trong collection "orders".
 * @param userId uid cua nguoi dat, dung de loc don hang cua rieng minh.
 * @param items cac mon trong don (snapshot luc dat hang).
 * @param total tong tien don hang.
 * @param createdAt thoi diem dat hang (milliseconds).
 * @param status trang thai don: "pending" | "shipping" | "done" | "cancelled".
 * @param address dia chi giao hang tai thoi diem dat (snapshot).
 * @param receiverName nguoi nhan hang.
 * @param receiverPhone so dien thoai lien lac.
 */
data class Order(
    val id: String = "",
    val userId: String = "",
    val items: List<CartItem> = emptyList(),
    val total: Long = 0,
    val createdAt: Long = 0,
    val status: String = STATUS_PENDING,
    val address: String = "",
    val receiverName: String = "",
    val receiverPhone: String = "",
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_SHIPPING = "shipping"
        const val STATUS_DONE = "done"
        const val STATUS_CANCELLED = "cancelled"
    }
}
