package com.ecomer.vku.app.ui.nav

/**
 * Cac man hinh cua app ECOMER.
 *
 * Bottom nav co 4 tab: Trang chu, Gio hang, Don hang, Tai khoan.
 * Man hinh chi tiet san pham va quan ly san pham (admin) nam ngoai bottom nav.
 */
object Screen {
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"

    const val HOME = "home"
    const val PRODUCT_DETAIL = "product_detail/{productId}"
    const val CART = "cart"
    const val ORDERS = "orders"
    const val ACCOUNT = "account"
    const val CHANGE_PASSWORD = "change_password"

    // Trang quan tri cua admin.
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val ADMIN_PRODUCTS = "admin_products"
    const val ADMIN_ORDERS = "admin_orders"
    const val ADMIN_USERS = "admin_users"

    // Dia chi giao hang va thanh toan.
    const val ADDRESS = "address"
    const val CHECKOUT = "checkout"

    // Man xem anh phong to. Anh la chuoi base64 nen duoc truyen qua
    // ViewModel chu khong dua vao duong dan dieu huong (qua dai).
    const val IMAGE_VIEWER = "image_viewer"

    /** Tao duong dan chi tiet san pham tu id. */
    fun productDetail(productId: String) = "product_detail/$productId"
}
