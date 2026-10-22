package com.ecomer.vku.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomer.vku.app.data.FirebaseExt
import com.ecomer.vku.app.data.model.User
import com.ecomer.vku.app.data.repo.AuthRepository
import com.ecomer.vku.app.data.repo.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Trang thai cua man hinh dang nhap / dang ky.
 *
 * @param loading dang trong luc goi Firebase.
 * @param error thong bao loi can hien thi.
 * @param success true khi vua dang nhap thanh cong (dung de dieu huong).
 */
data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

/**
 * ViewModel cho man hinh dang nhap va dang ky.
 */
class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Ho so nguoi dung sau khi dang nhap (de biet co phai admin khong). */
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    /** Dang nhap bang email + mat khau. */
    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Vui lòng nhập email và mật khẩu") }
            return
        }
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            authRepository.signInWithEmail(email, password)
                .onSuccess { loadUser() }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(loading = false, error = e.friendlyMessage(), success = false)
                    }
                }
        }
    }

    /** Tao tai khoan moi. */
    fun signUp(email: String, password: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Vui lòng nhập email và mật khẩu") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(error = "Mật khẩu phải có ít nhất 6 ký tự") }
            return
        }
        if (password != confirmPassword) {
            _uiState.update { it.copy(error = "Hai mật khẩu không khớp") }
            return
        }
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            authRepository.signUpWithEmail(email, password)
                .onSuccess { loadUser() }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(loading = false, error = e.friendlyMessage(), success = false)
                    }
                }
        }
    }

    /** Gui email dat lai mat khau. */
    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Vui lòng nhập email") }
            return
        }
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            authRepository.sendPasswordReset(email)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = null,
                            success = true,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(loading = false, error = e.friendlyMessage()) }
                }
        }
    }

    /** Xoa loi cua len. */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /** Hien thi loi len UI. */
    fun showError(message: String) {
        _uiState.update { it.copy(loading = false, error = message) }
    }

    /** Doc ho so nguoi dung sau khi dang nhap. */
    private suspend fun loadUser() {
        val firebaseUser = FirebaseExt.auth?.currentUser
        if (firebaseUser == null) {
            _uiState.update { it.copy(loading = false, success = true) }
            return
        }
        // Email lay tu Firebase Auth — cung la nguon ma Firestore Rules dung
        // de kiem tra quyen admin.
        val authEmail = firebaseUser.email.orEmpty()
        val isAdminEmail = authEmail.lowercase() in User.ADMIN_EMAILS

        userRepository.ensureUser(firebaseUser)
            .onSuccess { loaded ->
                _user.value = loaded.copy(
                    email = authEmail,
                    role = if (isAdminEmail) User.ROLE_ADMIN else loaded.role,
                )
            }
            .onFailure {
                // Firestore loi (chua tao database, bi tu choi) — van dung thong
                // tin tu Auth de app van chay binh thuong.
                _user.value = User(
                    uid = firebaseUser.uid,
                    email = authEmail,
                    displayName = firebaseUser.displayName.orEmpty(),
                    photoUrl = firebaseUser.photoUrl?.toString().orEmpty(),
                    role = if (isAdminEmail) User.ROLE_ADMIN else User.ROLE_USER,
                )
            }
        _uiState.update { it.copy(loading = false, success = true) }
    }
}

/**
 * Chuyen thong bao loi cua Firebase sang tieng Viet de nguoi dung doc hieu.
 */
fun Throwable.friendlyMessage(): String {
    val raw = message.orEmpty()
    return when {
        raw.contains("email address is already in use", ignoreCase = true) ->
            "Email này đã được đăng ký"
        raw.contains("password is invalid", ignoreCase = true) ||
            raw.contains("wrong password", ignoreCase = true) ->
            "Mật khẩu không đúng"
        raw.contains("no user record", ignoreCase = true) ||
            raw.contains("no account exists", ignoreCase = true) ->
            "Không tìm thấy tài khoản này"
        raw.contains("network", ignoreCase = true) ->
            "Lỗi mạng, kiểm tra kết nối rồi thử lại"
        raw.contains("Firebase chưa được cấu hình", ignoreCase = true) ->
            "Chưa cấu hình Firebase, app đang chạy dữ liệu mẫu"
        raw.isBlank() -> "Đã xảy ra lỗi, vui lòng thử lại"
        else -> raw
    }
}
