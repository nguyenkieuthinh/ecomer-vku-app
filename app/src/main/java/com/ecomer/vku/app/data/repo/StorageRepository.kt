package com.ecomer.vku.app.data.repo

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.ecomer.vku.app.data.FirebaseExt
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository upload anh san pham len Firebase Storage.
 *
 * Ten file duoc sinh ngau nhien de tranh trung ten khi hai nguoi dung
 * cung tai mot luc.
 */
class StorageRepository(
    private val storage: FirebaseStorage? = FirebaseExt.storage,
) {

    /** Tai lenh tu thu vien len Storage va tra ve URL download. */
    suspend fun uploadImage(uri: Uri, fileName: String): Result<String> = runCatching {
        val st = requireNotNull(storage) { "Firebase chưa được cấu hình" }
        val ref = st.reference.child("products/${UUID.randomUUID()}_$fileName")
        ref.putFile(uri).await()
        ref.downloadUrl.await().toString()
    }

    /** Xoa file trong Storage. */
    suspend fun deleteImage(url: String): Result<Unit> = runCatching {
        val st = requireNotNull(storage) { "Firebase chưa được cấu hình" }
        st.getReferenceFromUrl(url).delete().await()
    }
}
