package com.ecomer.vku.app.data.repo

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.ecomer.vku.app.data.FirebaseExt
import kotlinx.coroutines.tasks.await

/**
 * Repository xu ly dang nhap: email/mat khau va Gmail (Google Identity).
 *
 * Gmail dung Credential Manager de lay "Sign in with Google" token.
 * Server client id lay tu resource `default_web_client_id` do plugin google-services
 * sinh tu client type 3 trong google-services.json — nen doi project Firebase
 * khong can sua code.
 */
class AuthRepository(
    private val auth: FirebaseAuth? = FirebaseExt.auth,
) {

    /** Dang nhap bang email + mat khau. */
    suspend fun signInWithEmail(email: String, password: String): Result<Unit> = runCatching {
        val fb = requireNotNull(auth) { "Firebase chưa được cấu hình" }
        fb.signInWithEmailAndPassword(email.trim(), password).await()
    }

    /** Tao tai khoan moi bang email + mat khau. */
    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> = runCatching {
        val fb = requireNotNull(auth) { "Firebase chưa được cấu hình" }
        fb.createUserWithEmailAndPassword(email.trim(), password).await()
    }

    /** Gui email dat lai mat khau. */
    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        val fb = requireNotNull(auth) { "Firebase chưa được cấu hình" }
        fb.sendPasswordResetEmail(email.trim()).await()
    }

    /** Doi mat khau (dung o man hinh "Đổi mật khẩu"). */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = runCatching {
        val fb = requireNotNull(auth) { "Firebase chưa được cấu hình" }
        val user = fb.currentUser ?: error("Chưa đăng nhập")
        // Firebase yeu cau xac thuc lai bang mat khau hien tai truoc khi doi.
        user.reauthenticate(
            EmailAuthProvider.getCredential(user.email.orEmpty(), currentPassword)
        ).await()
        user.updatePassword(newPassword).await()
    }

    /** Dang nhap bang Gmail tu Google ID token. */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> = runCatching {
        val fb = requireNotNull(auth) { "Firebase chưa được cấu hình" }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        fb.signInWithCredential(credential).await()
    }

    /** Dang xuat. */
    fun signOut() {
        auth?.signOut()
    }
}
