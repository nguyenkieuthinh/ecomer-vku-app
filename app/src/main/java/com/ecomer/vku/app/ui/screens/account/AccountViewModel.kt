package com.ecomer.vku.app.ui.screens.account

import android.util.Log
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
 * Trang thai man hinh tai khoan.
 */
data class AccountUiState(
    val user: User? = null,
    val loading: Boolean = false,
    val message: String? = null,
    val passwordChanged: Boolean = false,
)

/**
 * ViewModel cho man hinh tai khoan va doi mat khau.
 */
private const val TAG = "AccountViewModel"

class AccountViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    /**
     * Nap ho so nguoi dung.
     *
     * Firebase Auth la nguon CHINH XAC cho email va quyen — cung dung voi
     * cach Firestore Rules kiem tra (`request.auth.token.email`). Firestore
     * chi bo sung thong tin phu (anh dai dien, so don) va KHONG bao gio ghi
     * de email, vi doc trong Firestore co the cu (thieu field) do du lieu cũ.
     */
    fun loadUser() {
        val uid = FirebaseExt.currentUserId ?: return
        val authUser = FirebaseExt.auth?.currentUser ?: return
        val authEmail = authUser.email.orEmpty()
        val isAdminEmail = authEmail.lowercase() in User.ADMIN_EMAILS

        Log.d(TAG, "loadUser email=$authEmail isAdmin=$isAdminEmail")

        // Hien thi ngay thong tin tu Auth, khong cho doi den Firestore.
        _uiState.update { state ->
            state.copy(
                user = state.user?.copy(
                    email = authEmail,
                    displayName = authUser.displayName.orEmpty()
                        .ifBlank { state.user?.displayName.orEmpty() },
                    photoUrl = authUser.photoUrl?.toString().orEmpty()
                        .ifBlank { state.user?.photoUrl.orEmpty() },
                    role = if (isAdminEmail) User.ROLE_ADMIN else state.user?.role.orEmpty(),
                ) ?: User(
                    uid = authUser.uid,
                    email = authEmail,
                    displayName = authUser.displayName.orEmpty(),
                    photoUrl = authUser.photoUrl?.toString().orEmpty(),
                    role = if (isAdminEmail) User.ROLE_ADMIN else User.ROLE_USER,
                )
            )
        }

        viewModelScope.launch {
            runCatching {
                userRepository.observeUser(uid).collect { loaded ->
                    if (loaded == null) return@collect
                    // Email lay tu Auth, khong lay tu Firestore.
                    // Role: admin neu email trong danh sach, ngay ca khi Firestore
                    // luu role "user" (du lieu tao tu truoc khi co quyen admin).
                    val merged = loaded.copy(
                        email = authEmail,
                        displayName = loaded.displayName.ifBlank {
                            authUser.displayName.orEmpty()
                        },
                        photoUrl = loaded.photoUrl.ifBlank {
                            authUser.photoUrl?.toString().orEmpty()
                        },
                        role = if (isAdminEmail) User.ROLE_ADMIN else loaded.role,
                    )
                    _uiState.update { it.copy(user = merged) }
                }
            }
        }
    }

    /** Doi mat khau. */
    fun changePassword(currentPassword: String, newPassword: String) {
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            _uiState.update { it.copy(message = "Vui lòng nhập đầy đủ mật khẩu") }
            return
        }
        if (newPassword.length < 6) {
            _uiState.update { it.copy(message = "Mật khẩu mới phải có ít nhất 6 ký tự") }
            return
        }
        _uiState.update { it.copy(loading = true, message = null) }
        viewModelScope.launch {
            authRepository.changePassword(currentPassword, newPassword)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            message = "Đổi mật khẩu thành công",
                            passwordChanged = true,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(loading = false, message = e.message ?: "Không đổi được mật khẩu")
                    }
                }
        }
    }

    /** Dang xuat. */
    fun signOut(onDone: () -> Unit) {
        authRepository.signOut()
        onDone()
    }

    /** Xoa thong bao. */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
