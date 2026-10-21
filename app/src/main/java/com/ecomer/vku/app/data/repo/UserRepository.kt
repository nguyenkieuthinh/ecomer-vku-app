package com.ecomer.vku.app.data.repo

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.ecomer.vku.app.data.FirebaseExt
import com.ecomer.vku.app.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Repository cho ho so nguoi dung (collection "users").
 *
 * Với mỗi tài khoản mới, tạo document trong "users" với role = "user".
 * Admin được nâng quyền bằng cách email nằm trong [User.ADMIN_EMAILS] —
 * cùng danh sách đó được Firestore Rules kiểm tra nên người dùng không thể
 * tự sửa role.
 */
class UserRepository(
    private val firestore: FirebaseFirestore? = FirebaseExt.firestore,
) {

    /** Dam bao co document nguoi dung, tao neu chua co. */
    suspend fun ensureUser(firebaseUser: FirebaseUser): Result<User> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }
        val ref = db.collection(COLLECTION).document(firebaseUser.uid)
        val snapshot = ref.get().await()

        val email = firebaseUser.email.orEmpty()
        val displayName = firebaseUser.displayName.orEmpty()
        val photoUrl = firebaseUser.photoUrl?.toString().orEmpty()
        val lastLoginAt = firebaseUser.metadata?.lastSignInTimestamp ?: 0L
        val isAdminEmail = email.lowercase() in User.ADMIN_EMAILS

        if (!snapshot.exists()) {
            // Tao moi: Rules yeu cau role la "user" luc create.
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                role = User.ROLE_USER,
                lastLoginAt = lastLoginAt,
            )
            ref.set(user.toMap()).await()
        }

        // Ghi lai thong tin cho khop voi Firebase Auth.
        //
        // Dung `update` chu khong phai `set` — `set` ghi de toan bo document,
        // lam mat field "role" va vi pham Security Rules.
        //
        // Với admin, them ca `role` vao de thay cho du lieu cu bi cu (do
        // ban `set` cu hon xoa mat field). Rules cho phep vi `isAdmin()` kiem
        // tra email trong token.
        val update = mutableMapOf<String, Any>(
            "email" to email,
            "displayName" to displayName,
            "photoUrl" to photoUrl,
            "lastLoginAt" to lastLoginAt,
        )
        if (isAdminEmail) {
            update["role"] = User.ROLE_ADMIN
        }
        runCatching { ref.update(update).await() }

        User(
            uid = firebaseUser.uid,
            email = email,
            displayName = displayName.ifBlank { snapshot.getString("displayName").orEmpty() },
            photoUrl = photoUrl.ifBlank { snapshot.getString("photoUrl").orEmpty() },
            role = if (isAdminEmail) User.ROLE_ADMIN else User.ROLE_USER,
            lastLoginAt = lastLoginAt,
        )
    }

    /** Doc ho so cua mot nguoi dung. */
    fun observeUser(uid: String): Flow<User?> = flow {
        val db = firestore
        if (db == null) {
            emit(null)
            return@flow
        }
        val loaded = runCatching {
            val snapshot = db.collection(COLLECTION).document(uid).get().await()
            if (!snapshot.exists()) null else snapshot.toUser()
        }.getOrNull()
        emit(loaded)
    }

    /**
     * Danh sach tat ca nguoi dung (chi admin doc duoc theo Rules).
     * Sap xep: dang nhap gan nhat truoc.
     */
    fun observeAllUsers(): Flow<List<User>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            return@callbackFlow
        }
        val registration = db.collection(COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.map { it.toUser() } ?: emptyList()
                trySend(users.sortedByDescending { it.lastLoginAt })
            }
        awaitClose { registration.remove() }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toUser() = User(
        uid = id,
        email = getString("email").orEmpty(),
        displayName = getString("displayName").orEmpty(),
        photoUrl = getString("photoUrl").orEmpty(),
        role = getString("role") ?: User.ROLE_USER,
        lastLoginAt = (get("lastLoginAt") as? com.google.firebase.Timestamp)?.toDate()?.time
            ?: (get("lastLoginAt") as? Number)?.toLong() ?: 0L,
    )

    private fun User.toMap() = mapOf(
        "uid" to uid,
        "email" to email,
        "displayName" to displayName,
        "photoUrl" to photoUrl,
        "role" to role,
        "lastLoginAt" to lastLoginAt,
    )

    companion object {
        const val COLLECTION = "users"
    }
}
