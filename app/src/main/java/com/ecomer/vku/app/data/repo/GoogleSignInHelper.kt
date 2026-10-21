package com.ecomer.vku.app.data.repo

import android.content.Context
import android.content.Intent
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ecomer.vku.app.R
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Lay ID token cua Google.
 *
 * Uu tien luong cua Play Services (Google Sign-In classic): bang chon tai khoan
 * do Play Services ve nen khong phu thuoc man hinh chon tai khoan cua ROM.
 * Tren MIUI/HyperOS man hinh do co the khong mo duoc, kien Credential Manager
 * treo rat lau — nen luon dat thoi han, va khi that bai moi thu Credential Manager.
 */
object GoogleSignInHelper {

    private const val CREDENTIAL_TIMEOUT_MS = 20_000L

    /**
     * Server client id cho Google Identity.
     *
     * Lay tu resource `default_web_client_id` do plugin google-services sinh tu
     * client type 3 (Web application) trong google-services.json. Do vay doi
     * project Firebase khong can sua code.
     */
    private fun serverClientId(context: Context): String {
        val id = context.getString(R.string.default_web_client_id)
        require(id.isNotBlank() && !id.startsWith("DAN_")) {
            "Chưa cấu hình default_web_client_id (thiếu OAuth client type 3 trong google-services.json)"
        }
        return id
    }

    /** Lay ID token qua Credential Manager. */
    suspend fun getIdToken(context: Context): String {
        val credentialManager = CredentialManager.create(context)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId(context))
                    .setAutoSelectEnabled(false)
                    .build(),
            )
            .build()

        val response = withTimeout(CREDENTIAL_TIMEOUT_MS) {
            credentialManager.getCredential(context, request)
        }
        val credential = response.credential

        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            error("Không nhận được Google ID token")
        }

        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    /**
     * Intent dang nhap Google cua Play Services. Day la luong du phong: bang chon
     * tai khoan do Play Services ve nen khong phu thuoc vao Credential Manager cua ROM.
     *
     * Dang xuat khoi Google truoc khi lay intent de xoa tai khoan da ghi nho;
     * neu khong, lan dang nhap sau se leng le dung lai dung tai khoan cu va
     * nguoi dung khong co cach chon tai khoan khac.
     */
    suspend fun legacySignInIntent(context: Context): Intent {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(serverClientId(context))
            .requestEmail()
            .build()
        val client: GoogleSignInClient = GoogleSignIn.getClient(context, options)
        runCatching { client.signOut().await() }
        return client.signInIntent
    }

    /** Doc ID token tu ket qua tra ve cua [legacySignInIntent]. */
    fun idTokenFromLegacyResult(data: Intent?): String {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data)
            .getResult(com.google.android.gms.common.api.ApiException::class.java)
        return account.idToken ?: error("Không nhận được Google ID token")
    }
}
