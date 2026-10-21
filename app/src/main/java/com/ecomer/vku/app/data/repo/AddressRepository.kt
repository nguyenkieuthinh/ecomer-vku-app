package com.ecomer.vku.app.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.ecomer.vku.app.data.FirebaseExt
import com.ecomer.vku.app.data.model.Address
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository cho dia chi giao hang.
 *
 * Luu trong subcollection `users/{uid}/addresses` — moi nguoi chi thay duoc
 * dia chi cua chinh minh (xem `firestore.rules`).
 */
class AddressRepository(
    private val firestore: FirebaseFirestore? = FirebaseExt.firestore,
) {

    /** Theo doi danh sach dia chi cua nguoi dung. */
    fun observeAddresses(userId: String): Flow<List<Address>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            return@callbackFlow
        }
        val registration = db.collection("users")
            .document(userId)
            .collection(Address.COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Rules chua cho phep doc dia chi (chua publish) — bao loi
                    // de nguoi dung biet can lam gi, thay vi thay bang danh sach rong.
                    FirebaseExt.reportFailure(error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { it.toAddress(userId) } ?: emptyList()
                // Dia chi mac dinh luon len dau.
                trySend(list.sortedByDescending { it.isDefault })
            }
        awaitClose { registration.remove() }
    }

    /** Them hoac sua dia chi. Neu id rong thi tao moi. */
    suspend fun saveAddress(userId: String, address: Address): Result<String> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }
        val ref = db.collection("users").document(userId).collection(Address.COLLECTION)
            .document(address.id.ifBlank { newId() })

        ref.set(
            mapOf(
                "userId" to userId,
                "fullName" to address.fullName.trim(),
                "phone" to address.phone.trim(),
                "street" to address.street.trim(),
                "ward" to address.ward.trim(),
                "district" to address.district.trim(),
                "city" to address.city.trim(),
                "isDefault" to address.isDefault,
            )
        ).await()

        // Chi co mot dia chi mac dinh: bo co o cac dia chi khac.
        if (address.isDefault) {
            setDefault(userId, ref.id)
        }
        ref.id
    }

    /** Dat dia chi mac dinh. */
    suspend fun setDefault(userId: String, addressId: String): Result<Unit> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }
        val col = db.collection("users").document(userId).collection(Address.COLLECTION)
        val snapshot = col.get().await()
        snapshot.documents.forEach { doc ->
            val shouldBeDefault = doc.id == addressId
            if (doc.getBoolean("isDefault") != shouldBeDefault) {
                doc.reference.update(mapOf("isDefault" to shouldBeDefault)).await()
            }
        }
    }

    /** Xoa dia chi. */
    suspend fun deleteAddress(userId: String, addressId: String): Result<Unit> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }
        db.collection("users").document(userId)
            .collection(Address.COLLECTION)
            .document(addressId)
            .delete()
            .await()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toAddress(userId: String) = Address(
        id = id,
        userId = userId,
        fullName = getString("fullName").orEmpty(),
        phone = getString("phone").orEmpty(),
        street = getString("street").orEmpty(),
        ward = getString("ward").orEmpty(),
        district = getString("district").orEmpty(),
        city = getString("city").orEmpty(),
        isDefault = getBoolean("isDefault") ?: false,
    )

    /** Sinh id ngau nhien cho dia chi moi. */
    private fun newId(): String = java.util.UUID.randomUUID().toString()
}
