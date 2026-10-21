package com.ecomer.vku.app.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.ecomer.vku.app.data.repo.GoogleSignInHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** Nguoi dung tu dong bang chon tai khoan Google. */
class GoogleSignInCancelled : Exception("Đã huỷ đăng nhập Google.")

/**
 * Tra ve ham lay Google ID token.
 *
 * Uu tien Google Sign-In cua Play Services: bang chon tai khoan mo nhanh va
 * khong phu thuoc man hinh chon tai khoan cua ROM. Tren MIUI/HyperOS man hinh
 * do co the khong mo duoc, kien Credential Manager treo rat lau.
 * Chi khi luong cu khong dung duoc moi thu Credential Manager.
 */
@Composable
fun rememberGoogleIdTokenProvider(): suspend (Context) -> String {
    val holder = remember { GoogleIdTokenHolder() }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        holder::onResult,
    )
    holder.launch = { intent -> launcher.launch(intent) }

    return holder::getToken
}

private class GoogleIdTokenHolder {

    /**
     * Thoi han cho luong Play Services.
     *
     * Luong nay mo bang chon tai khoan cua Google — nguoi dung can thoi gian
     * de chon, nen thoi han phai rong. Timeout 20s chi ap dung cho Credential
     * Manager (man hinh do ROM ve, co the treo vo han tren MIUI/HyperOS).
     */
    private companion object {
        const val LEGACY_TIMEOUT_MS = 120_000L
    }

    var launch: (Intent) -> Unit = {}

    private var pending: CompletableDeferred<String>? = null

    fun onResult(result: ActivityResult) {
        val deferred = pending ?: return
        pending = null

        val data = result.data
        if (result.resultCode != Activity.RESULT_OK || data == null) {
            deferred.completeExceptionally(GoogleSignInCancelled())
            return
        }

        runCatching { GoogleSignInHelper.idTokenFromLegacyResult(data) }
            .onSuccess { deferred.complete(it) }
            .onFailure { error ->
                val isCancelled = error is ApiException &&
                    error.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED
                deferred.completeExceptionally(
                    if (isCancelled) GoogleSignInCancelled() else error,
                )
            }
    }

    suspend fun getToken(context: Context): String {
        val deferred = CompletableDeferred<String>()
        pending = deferred

        val launched = runCatching {
            withContext(Dispatchers.Main) {
                launch(GoogleSignInHelper.legacySignInIntent(context))
            }
        }
        if (launched.isFailure) {
            pending = null
            return credentialManagerToken(context)
        }

        return try {
            // Luong Play Services co the treo vo han tren MIUI/HyperOS khi man
            // hinh chon tai khoan khong mo duoc. Dat thoi han de app khong bi
            // treo, va chuyen sang Credential Manager neu qua han.
            withTimeout(LEGACY_TIMEOUT_MS) { deferred.await() }
        } catch (cancel: GoogleSignInCancelled) {
            throw cancel
        } catch (legacyError: Throwable) {
            // May khong ho tro luong cu (GMS thieu/cu) hoac man hinh bi treo
            // -> thu Credential Manager.
            pending = null
            credentialManagerToken(context)
        }
    }
}

private suspend fun credentialManagerToken(context: Context): String =
    try {
        GoogleSignInHelper.getIdToken(context)
    } catch (cancel: GetCredentialCancellationException) {
        throw GoogleSignInCancelled()
    }
