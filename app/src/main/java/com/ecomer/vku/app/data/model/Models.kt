package com.ecomer.vku.app.data.model

/**
 * San pham trong cua hang.
 *
 * Anh duoc luu o collection rieng ("product_images"), moi anh mot document.
 * Ly do: gioi han 1 MiB cua Firestore ap dung cho TUNG document, nen san pham
 * co 2, 3 hay 4 anh deu khong anh huong nhau — moi anh duoc cap rieng 1 MiB.
 * Document san pham chi giu danh sach id anh de lien ket.
 *
 * @param id id cua document, sinh tu dong theo loai: "ao-nam-001".
 * @param name ten san pham.
 * @param category loai san pham ("Áo nam"), dung cho chip loc va sinh id.
 * @param price gia ban hien tai (VND).
 * @param totalStock tong so luong nhap kho.
 * @param availableStock so luong con ban.
 * @param soldCount so luong da ban.
 * @param description mo ta san pham.
 * @param imageIds danh sach id anh cua san pham nay, theo thu tu hien thi.
 * @param coverImageId id cua anh dai dien (thuong la anh dau tien).
 * @param createdAt thoi diem them san pham (milliseconds) — dung de sap xep.
 * @param visible san pham con ban hay da an. San pham da AN khong hien voi
 *   khach, nhung admin van thay va bat lai duoc.
 */
data class Product(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val price: Long = 0,
    val totalStock: Int = 0,
    val availableStock: Int = 0,
    val soldCount: Int = 0,
    val description: String = "",
    val imageIds: List<String> = emptyList(),
    val coverImageId: String = "",
    val createdAt: Long = 0,
    val visible: Boolean = true,
) {
    /** Id cua anh dai dien, fallback ve anh dau tien. */
    val effectiveCoverId: String
        get() = coverImageId.ifEmpty { imageIds.firstOrNull().orEmpty() }
}

/**
 * Mot anh san pham, luu rieng mot document trong collection "product_images".
 *
 * @param id id cua document, sinh tu dong theo loai san pham: "ao-nam-001-img-1".
 * @param productId id cua san pham chua anh nay.
 * @param data duong dan base64 cua anh, kem tien to "data:image/jpeg;base64,".
 * @param order thu tu hien thi (0 la anh dau).
 * @param fileName ten file de hien thi tren UI.
 * @param sizeBytes dung luong cua anh sau khi nen (byte).
 */
data class ProductImage(
    val id: String = "",
    val productId: String = "",
    val data: String = "",
    val order: Int = 0,
    val fileName: String = "",
    val sizeBytes: Long = 0,
) {
    companion object {
        const val COLLECTION = "product_images"
        const val PRODUCT_COLLECTION = "products"
        const val COUNTER_COLLECTION = "counters"

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
