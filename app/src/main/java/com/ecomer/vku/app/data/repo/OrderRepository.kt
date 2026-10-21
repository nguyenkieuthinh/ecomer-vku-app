package com.ecomer.vku.app.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.ecomer.vku.app.data.FirebaseExt
import com.ecomer.vku.app.data.model.Address
import com.ecomer.vku.app.data.model.CartItem
import com.ecomer.vku.app.data.model.Order
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Repository cho don hang.
 *
 * Moi don la mot document trong collection "orders", co truong userId de chay
 * "Đơn hàng của tôi" cho dung nguoi dat.
 */
class OrderRepository(
    private val firestore: FirebaseFirestore? = FirebaseExt.firestore,
) {

    /** Dat don hang moi. */
    /**
     * Dat don hang moi.
     *
     * @param address dia chi giao hang — bat buoc khi thanh toan.
     */
    suspend fun placeOrder(
        userId: String,
        items: List<CartItem>,
        address: Address = Address(),
    ): Result<String> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }
        val doc = db.collection(COLLECTION).document()
        doc.set(
            mapOf(
                "userId" to userId,
                "items" to items.map {
                    mapOf(
                        "productId" to it.productId,
                        "name" to it.name,
                        "price" to it.price,
                        "imageUrl" to it.imageUrl,
                        "quantity" to it.quantity,
                    )
                },
                "total" to items.sumOf { it.subtotal },
                "createdAt" to com.google.firebase.Timestamp.now(),
                "status" to Order.STATUS_PENDING,
                "address" to address.fullAddress,
                "receiverName" to address.fullName,
                "receiverPhone" to address.phone,
            )
        ).await()
        doc.id
    }

    /** Danh sach don hang cua nguoi dung, moi nhat luon o dau. Loi Firestore duoc bat lai. */
    fun observeOrders(userId: String): Flow<List<Order>> = flow {
        val db = firestore
        if (db == null) {
            emit(emptyList())
            return@flow
        }
        val orders = runCatching {
            db.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .map { doc ->
                    val items = (doc.get("items") as? List<Map<String, Any>> ?: emptyList()).map {
                        CartItem(
                            productId = it["productId"] as? String ?: "",
                            name = it["name"] as? String ?: "",
                            price = (it["price"] as? Number)?.toLong() ?: 0L,
                            imageUrl = it["imageUrl"] as? String ?: "",
                            quantity = (it["quantity"] as? Number)?.toInt() ?: 1,
                        )
                    }
                    Order(
                        id = doc.id,
                        userId = userId,
                        items = items,
                        total = (doc.get("total") as? Number)?.toLong() ?: items.sumOf { it.subtotal },
                        createdAt = (doc.get("createdAt") as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L,
                        status = doc.getString("status") ?: Order.STATUS_PENDING,
                        address = doc.getString("address").orEmpty(),
                        receiverName = doc.getString("receiverName").orEmpty(),
                        receiverPhone = doc.getString("receiverPhone").orEmpty(),
                    )
                }
                .sortedByDescending { it.createdAt }
        }.getOrElse { emptyList() }
        emit(orders)
    }

    /** Doi trang thai don (admin dung khi xu ly don). */
    suspend fun updateStatus(orderId: String, status: String): Result<Unit> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }
        // `update` chu khong phai `set` — `set` se xoa mat items, total, userId cua don.
        db.collection(COLLECTION).document(orderId)
            .update(mapOf("status" to status))
            .await()
    }

    /**
     * Tat ca don hang cua tat ca nguoi dung (chi admin doc duoc theo Rules).
     * Moi nhat luon o dau.
     */
    fun observeAllOrders(): Flow<List<Order>> = callbackFlow {
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
                val orders = snapshot?.documents?.map { doc ->
                    val items = (doc.get("items") as? List<Map<String, Any>> ?: emptyList()).map {
                        CartItem(
                            productId = it["productId"] as? String ?: "",
                            name = it["name"] as? String ?: "",
                            price = (it["price"] as? Number)?.toLong() ?: 0L,
                            imageUrl = it["imageUrl"] as? String ?: "",
                            quantity = (it["quantity"] as? Number)?.toInt() ?: 1,
                        )
                    }
                    Order(
                        id = doc.id,
                        userId = doc.getString("userId").orEmpty(),
                        items = items,
                        total = (doc.get("total") as? Number)?.toLong() ?: items.sumOf { it.subtotal },
                        createdAt = (doc.get("createdAt") as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L,
                        status = doc.getString("status") ?: Order.STATUS_PENDING,
                    )
                } ?: emptyList()
                trySend(orders.sortedByDescending { it.createdAt })
            }
        awaitClose { registration.remove() }
    }

    companion object {
        const val COLLECTION = "orders"
    }
}
