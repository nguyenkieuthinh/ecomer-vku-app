package com.ecomer.vku.app.data.model

/**
 * San pham trong he thong nhap lieu.
 *
 * Theo de thi, document san pham CHI co 4 truong: `tenSanPham`, `loaiSanPham`,
 * `gia` va `image` (id cua document anh dai dien). Khong duoc them truong khac.
 *
 * @param id id cua document trong Firestore (collection "products").
 * @param tenSanPham ten san pham.
 * @param loaiSanPham loai san pham.
 * @param gia gia ban (VND).
 * @param image id cua document anh dai dien trong collection "product_images".
 * @param ownerId uid cua nguoi nhap san pham. Truong nay CHI ton tai trong RAM —
 *   KHONG duoc ghi xuong Firestore. Chu so huu duoc luu o collection rieng
 *   "product_owners" de document san pham van chi co 4 truong nhu de thi.
 */
data class Product(
    val id: String = "",
    val tenSanPham: String = "",
    val loaiSanPham: String = "",
    val gia: Long = 0,
    val image: String = "",
    val ownerId: String = "",
)

/**
 * Mot anh san pham, luu rieng mot document trong collection `product_images`.
 *
 * Anh duoc luu rieng nen gioi han 1 MiB cua Firestore ap dung cho TUNG anh —
 * san pham bao nhieu anh cung khong vuot.
 *
 * @param id id cua document anh.
 * @param productId id cua san pham chua anh nay.
 * @param tieuDe tieu de cua anh (ten san pham) — hinh anh duoc luu va hien thi
 *   theo tung tieu de.
 * @param data du lieu base64 cua anh, kem tien to "data:image/jpeg;base64,".
 * @param tenFile ten file goc cua anh.
 * @param order thu tu hien thi (0 la anh dai dien).
 * @param sizeBytes dung luong cua anh sau khi nen (byte).
 */
data class ProductImage(
    val id: String = "",
    val productId: String = "",
    val tieuDe: String = "",
    val data: String = "",
    val tenFile: String = "",
    val order: Int = 0,
    val sizeBytes: Long = 0,
) {
    companion object {
        const val COLLECTION = "product_images"
        const val PRODUCT_COLLECTION = "products"
        const val COUNTER_COLLECTION = "counters"

        /**
         * Chu so huu san pham — luu o collection rieng de KHONG them truong
         * `userId` vao document san pham (de thi yeu cau chi 4 truong).
         */
        const val OWNER_COLLECTION = "product_owners"

        /** Chuyen "Áo nam" thanh "ao-nam" de dung lam id. */
        fun slug(category: String): String =
            category.trim()
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifEmpty { "san-pham" }
    }
}
