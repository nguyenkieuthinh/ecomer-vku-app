package com.ecomer.vku.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Wrapper quanh Firebase de app khong bi crash khi chua co google-services.json.
 *
 * [EcomerApp] (Application) thu khoi tao Firebase luc app bat. Neu file config
 * bi thieu hoac sai, co [available] se la false va app chay o che do du lieu mau.
 */
object FirebaseExt {

    /** True neu Firebase da khoi tao thanh cong. */
    var available: Boolean = false

    val auth: FirebaseAuth?
        get() = if (available) runCatching { FirebaseAuth.getInstance() }.getOrNull() else null

    val firestore: FirebaseFirestore?
        get() = if (available) runCatching { FirebaseFirestore.getInstance() }.getOrNull() else null

    val storage: FirebaseStorage?
        get() = if (available) runCatching { FirebaseStorage.getInstance() }.getOrNull() else null

    /** uid cua nguoi dang dang nhap, null neu chua dang nhap. */
    val currentUserId: String?
        get() = auth?.currentUser?.uid

    /**
     * Thong bao loi Firestore gan nhat, de UI hien thi cho nguoi dung biet
     * vi sao du lieu khong co (vi du database chua duoc tao trong Console).
     *
     * Dung Compose [mutableStateOf] de UI tu dong ve lai khi loi xuat hien.
     */
    var lastError: String? by mutableStateOf(null)

    /**
     * Ghi nhan loi Firestore va tra ve thong bao thich hop de hien thi.
     * Loi "database chua ton tai" duoc ghi lai rieng vi day la cau hinh
     * can sua tren Firebase Console chu khong phai loi code.
     */
    fun reportFailure(error: Throwable): List<Nothing> {
        val raw = error.message.orEmpty()
        lastError = when {
            raw.contains("does not exist for project", ignoreCase = true) ->
                "Chưa tạo Firestore database cho project. Mở Firebase Console → Firestore Database → Create database."
            raw.contains("PERMISSION_DENIED", ignoreCase = true) ||
                raw.contains("Missing or insufficient permissions", ignoreCase = true) ->
                "Bị Firestore từ chối truy cập. Kiểm tra lại Security Rules."
            raw.contains("client is offline", ignoreCase = true) ||
                raw.contains("UNAVAILABLE", ignoreCase = true) ->
                "Không kết nối được Firestore. Kiểm tra mạng."
            else -> "Lỗi Firestore: $raw"
        }
        return emptyList()
    }

    /** Xoa loi da ghi nhan. */
    fun clearError() {
        lastError = null
    }

    /**
     * Theo doi thay doi dang nhap.
     *
     * Firebase Auth chi co API listener (khong co Flow) nen dung
     * [FirebaseAuth.addAuthStateListener]. Listener duoc tra ve de goi
     * `removeAuthStateListener` trong `onCleared` cua ViewModel.
     *
     * @param onUserChanged goi moi khi nguoi dung dang nhap / dang xuat,
     *   nhan uid moi (null neu da dang xuat).
     */
    fun observeAuth(onUserChanged: (String?) -> Unit): FirebaseAuth.AuthStateListener? {
        val fb = auth ?: return null
        val listener = FirebaseAuth.AuthStateListener { user ->
            onUserChanged(user?.uid)
        }
        fb.addAuthStateListener(listener)
        return listener
    }

    /**
     * Chuyen loi Firebase thanh thong bao tieng Viet de hien thi cho nguoi dung.
     *
     * Loi `PERMISSION_DENIED` thuong nghia la Security Rules tren Firebase
     * chua duoc publish (hoac chua cho phep thao tac nay) — thong bao do la
     * thong tin can cho nguoi dung, khong phai loi code.
     */
    fun friendlyMessage(error: Throwable): String {
        val raw = error.message.orEmpty()
        return when {
            raw.contains("PERMISSION_DENIED", ignoreCase = true) ||
                raw.contains("Missing or insufficient permissions", ignoreCase = true) ->
                "Không có quyền thực hiện. Kiểm tra lại Security Rules trên Firebase Console (nhớ bấm Publish)."
            raw.contains("does not exist for project", ignoreCase = true) ->
                "Chưa tạo Firestore database cho project. Mở Firebase Console → Firestore Database → Create database."
            raw.contains("UNAVAILABLE", ignoreCase = true) ||
                raw.contains("client is offline", ignoreCase = true) ->
                "Không kết nối được Firestore. Kiểm tra mạng rồi thử lại."
            raw.contains("not authorized", ignoreCase = true) ->
                "Tài khoản không có quyền thực hiện thao tác này."
            raw.isBlank() -> "Đã xảy ra lỗi, vui lòng thử lại"
            else -> raw
        }
    }
}
