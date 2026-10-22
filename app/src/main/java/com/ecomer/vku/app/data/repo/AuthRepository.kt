package com.ecomer.vku.app.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.ecomer.vku.app.data.FirebaseExt
import kotlinx.coroutines.tasks.await

/**
 * Repository xu ly dang nhap bang email + mat khau.
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

    /** Dang xuat. */
    fun signOut() {
        auth?.signOut()
    }
}
