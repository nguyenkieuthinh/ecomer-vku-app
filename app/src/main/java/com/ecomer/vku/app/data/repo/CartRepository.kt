package com.ecomer.vku.app.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.ecomer.vku.app.data.FirebaseExt
import com.ecomer.vku.app.data.model.CartItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Repository cho gio hang.
 *
 * Gio hang luu theo uid trong collection "carts", moi nguoi dung mot document.
 * Khi chua co Firebase, gio hang chi nam trong bo nho (khong ton tai qua app restart).
 */
class CartRepository(
    private val firestore: FirebaseFirestore? = FirebaseExt.firestore,
) {

    /** Doc gio hang cua nguoi dung. Loi Firestore duoc bat lai, tra ve gio hang rong. */
    fun observeCart(userId: String): Flow<List<CartItem>> = flow {
        val db = firestore
        if (db == null) {
            emit(emptyList())
            return@flow
        }
        val items = runCatching {
            val snapshot = db.collection(COLLECTION).document(userId).get().await()
            (snapshot.get("items") as? List<Map<String, Any>> ?: emptyList())
                .map { it.toCartItem() }
        }.getOrElse { emptyList() }
        emit(items)
    }

    /** Ghi lai toan bo gio hang. */
    suspend fun saveCart(userId: String, items: List<CartItem>): Result<Unit> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }
        db.collection(COLLECTION).document(userId)
            .set(
                mapOf(
                    "items" to items.map {
                        mapOf(
                            "productId" to it.productId,
                            "name" to it.name,
                            "price" to it.price,
                            "imageUrl" to it.imageUrl,
                            "quantity" to it.quantity,
                        )
                    },
                    "updatedAt" to com.google.firebase.Timestamp.now(),
                )
            )
            .await()
    }

    /** Xoa gio hang sau khi dat hang. */
    suspend fun clearCart(userId: String): Result<Unit> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }
        db.collection(COLLECTION).document(userId).delete().await()
    }

    private fun Map<String, Any>.toCartItem() = CartItem(
        productId = this["productId"] as? String ?: "",
        name = this["name"] as? String ?: "",
        price = (this["price"] as? Number)?.toLong() ?: 0L,
        imageUrl = this["imageUrl"] as? String ?: "",
        quantity = (this["quantity"] as? Number)?.toInt() ?: 1,
    )

    companion object {
        const val COLLECTION = "carts"
    }
}
