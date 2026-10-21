package com.ecomer.vku.app.data.model

/**
 * Thong tin nguoi dung luu trong Firestore (collection "users").
 *
 * @param uid id cua tai khoan Firebase Auth.
 * @param email email dang ky.
 * @param displayName ten hien thi (lay tu ho so Google neu dang nhap bang Gmail).
 * @param photoUrl anh dai dien.
 * @param role vai tro trong Firestore: "user" hoac "admin".
 * @param lastLoginAt thoi diem dang nhap gan nhat (milliseconds).
 */
data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val role: String = ROLE_USER,
    val lastLoginAt: Long = 0,
) {
    /**
     * Co phai admin khong.
     *
     * Nguon su that la danh sach email trong [ADMIN_EMAILS] — Firebase Rules
     * cung kiem tra chinh danh sach nay bang `request.auth.token.email`, nen
     * nguoi dung khong the tu sua role trong Firestore de len admin.
     * Truong `role` chi de hien thi.
     */
    val isAdmin: Boolean
        get() = role == ROLE_ADMIN || email.lowercase() in ADMIN_EMAILS

    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ADMIN = "admin"

        /**
         * Email duoc co quyen admin.
         *
         * Danh sach nay phai trung khop voi `firestore.rules` (ham `isAdmin`).
         * Doi o ca hai noi thi moi co quyen.
         */
        val ADMIN_EMAILS = setOf(
            "admin@ecomer.vn",
        )
    }
}
