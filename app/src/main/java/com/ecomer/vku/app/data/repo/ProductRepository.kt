package com.ecomer.vku.app.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.ecomer.vku.app.data.EncodedImage
import com.ecomer.vku.app.data.FirebaseExt
import com.ecomer.vku.app.data.model.Product
import com.ecomer.vku.app.data.model.ProductImage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Repository cho san pham va anh san pham.
 *
 * Cau truc Firestore:
 * - `products/{id}`: thong tin san pham + `imageIds` (danh sach id anh).
 * - `product_images/{id}`: mot anh, luu base64. Tach rieng de moi anh co
 *   1 MiB rieng — san pham bao nhieu anh cung khong vuot gioi han.
 * - `counters/{typeSlug}`: bo dem sinh id theo loai san pham.
 */
class ProductRepository(
    private val firestore: FirebaseFirestore? = FirebaseExt.firestore,
) {

    /**
     * Theo doi danh sach san pham — dung LIVE LISTENER.
     *
     * Listener cho phep danh sach cap nhat ngay khi admin them / sua / xoa san
     * pham, khong can goi lai [loadProducts] theo tay. Neu dung `get()` (doc
     * mot lan) thi danh sach se khong bao gio tu doi.
     *
     * Khong dung `orderBy("createdAt")`: Firestore loai bo nhung document
     * KHONG co field do, nen san pham tao truoc khi co `createdAt` se khong
     * bao gio xuat hien. Thay vao do sap xep o phia app.
     *
     * Loi Firestore (database chua duoc tao, khong co mang, bi tu choi) duoc bat
     * lai va dua ve danh sach mau — neu khong, exception se lan ra [flow] va lam
     * app crash ngay khi mo.
     */
    fun observeProducts(onlyVisible: Boolean = true): Flow<List<Product>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(SampleData.products)
            return@callbackFlow
        }

        val registration = db.collection(ProductImage.PRODUCT_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    FirebaseExt.reportFailure(error)
                    trySend(SampleData.products)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.map { it.toProduct() }
                    ?.filter { !onlyVisible || it.visible }
                    ?: emptyList()
                // Moi san pham len dau — san pham moi them la thu nguoi dung
                // dang xem.
                val sorted = list.sortedByDescending { it.createdAt }
                trySend(sorted.ifEmpty { SampleData.products })
            }

        awaitClose { registration.remove() }
    }

    /**
     * Doc anh cua nhieu san pham mot luc.
     *
     * Dung [FirebaseFirestore.getAll] de gom het len mot lan round-trip thay vi
     * goi tung anh — tranh N+1 request khi luoi hien nhieu san pham.
     *
     * @param imageIds danh sach id can doc.
     * @return anh theo dung thu tu truyen vao.
     */
    suspend fun loadImages(imageIds: List<String>): List<ProductImage> {
        val db = firestore ?: return emptyList()
        if (imageIds.isEmpty()) return emptyList()

        val loaded = runCatching {
            // Firestore gioi han 30 gia tri cho moi `whereIn`, nen chia lo.
            imageIds.distinct().chunked(MAX_WHERE_IN).flatMap { chunk ->
                db.collection(ProductImage.COLLECTION)
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                    .get()
                    .await()
                    .documents
                    .map { it.toProductImage() }
            }
        }.getOrElse { emptyList() }

        // Tra ve theo dung thu tu id nguoi goi yeu cau.
        val byId = loaded.associateBy { it.id }
        return imageIds.mapNotNull { byId[it] }
    }

    /**
     * Sinh id moi cho san pham theo loai: "ao-nam-001", "ao-nam-002"...
     *
     * Firestore khong co auto-increment, nen phai tu dem bang transaction tren
     * document `counters/{slug}` de khong bao gio trung id khi hai nguoi them
     * san pham cung luc.
     */
    private suspend fun nextProductId(db: FirebaseFirestore, category: String): String {
        val slug = ProductImage.slug(category)
        val counterRef = db.collection(ProductImage.COUNTER_COLLECTION).document(slug)
        val seq = db.runTransaction { transaction ->
            val snapshot = transaction.get(counterRef)
            val current = (snapshot.get("seq") as? Number)?.toLong() ?: 0L
            val next = current + 1
            transaction.set(counterRef, mapOf("seq" to next))
            next
        }.await()
        return "$slug-${seq.toString().padStart(3, '0')}"
    }

    /**
     * Them san pham moi kem danh sach anh (base64).
     *
     * @param product thong tin san pham.
     * @param images du lieu base64 cua cac anh. Anh DAU TIEN la anh dai dien.
     * @return id cua san pham vua tao.
     */
    suspend fun addProduct(product: Product, images: List<EncodedImage>): Result<String> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }
        val productId = nextProductId(db, product.category)

        val imageIds = images.mapIndexed { index, image ->
            val imageId = "$productId-img-${index + 1}"
            db.collection(ProductImage.COLLECTION).document(imageId).set(
                mapOf(
                    "productId" to productId,
                    "data" to image.data,
                    "fileName" to image.fileName,
                    "sizeBytes" to image.sizeBytes,
                    "order" to index,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                )
            ).await()
            imageId
        }

        val doc = db.collection(ProductImage.PRODUCT_COLLECTION).document(productId)
        doc.set(
            mapOf(
                "name" to product.name.trim(),
                "category" to product.category.trim(),
                "price" to product.price,
                "totalStock" to product.totalStock,
                "availableStock" to product.availableStock,
                "soldCount" to 0,
                "description" to product.description.trim(),
                "imageIds" to imageIds,
                "coverImageId" to imageIds.firstOrNull().orEmpty(),
                "visible" to product.visible,
                "createdAt" to com.google.firebase.Timestamp.now(),
            )
        ).await()
        productId
    }

    /**
     * Cap nhat san pham.
     *
     * - `keptImageIds` la danh sach id anh giu lai, THEO THU TU HIEN THI.
     *   Anh dau tien la anh dai dien.
     * - `newImages` la cac anh moi, them vao cuoi danh sach.
     * - `removedImageIds` la cac anh bi xoa khoi Firestore.
     */
    suspend fun updateProduct(
        product: Product,
        keptImageIds: List<String>,
        newImages: List<EncodedImage>,
        removedImageIds: List<String> = emptyList(),
    ): Result<Unit> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }
        val productRef = db.collection(ProductImage.PRODUCT_COLLECTION).document(product.id)

        // Xoa anh bi lo bo khoi Firestore.
        removedImageIds.forEach { imageId ->
            runCatching {
                db.collection(ProductImage.COLLECTION).document(imageId).delete().await()
            }
        }

        // Cap nhat thu tu cua anh con lai (anh dai dien len dau).
        // `update` chu khong phai `set` — `set` se xoa mat du lieu base64 cua anh.
        keptImageIds.forEachIndexed { index, imageId ->
            runCatching {
                db.collection(ProductImage.COLLECTION).document(imageId)
                    .update(mapOf("order" to index))
                    .await()
            }
        }

        // Them anh moi vao cuoi danh sach.
        val addedIds = newImages.mapIndexed { index, image ->
            val imageId = "${product.id}-img-${keptImageIds.size + index + 1}"
            db.collection(ProductImage.COLLECTION).document(imageId).set(
                mapOf(
                    "productId" to product.id,
                    "data" to image.data,
                    "fileName" to image.fileName,
                    "sizeBytes" to image.sizeBytes,
                    "order" to keptImageIds.size + index,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                )
            ).await()
            imageId
        }

        val finalIds = keptImageIds + addedIds
        // Dung `update` de giu nguyen field "createdAt" va "soldCount" —
        // `set` se ghi de toan bo document va lam mat cac field do.
        productRef.update(
            mapOf(
                "name" to product.name.trim(),
                "category" to product.category.trim(),
                "price" to product.price,
                "totalStock" to product.totalStock,
                "availableStock" to product.availableStock,
                "description" to product.description.trim(),
                "imageIds" to finalIds,
                "coverImageId" to finalIds.firstOrNull().orEmpty(),
                "visible" to product.visible,
            )
        ).await()
    }

    /**
     * Xoa mot anh cua san pham.
     *
     * @param productId id san pham.
     * @param imageId id anh can xoa.
     */
    suspend fun deleteImage(productId: String, imageId: String): Result<Unit> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }
        db.collection(ProductImage.COLLECTION).document(imageId).delete().await()

        val productRef = db.collection(ProductImage.PRODUCT_COLLECTION).document(productId)
        val snapshot = productRef.get().await()
        val ids = snapshot.get("imageIds") as? List<String> ?: emptyList()
        val remaining = ids.filterNot { it == imageId }
        // `update` chu khong phai `set` — `set` se xoa mat ten, gia, anh cua san pham.
        productRef.update(
            mapOf(
                "imageIds" to remaining,
                "coverImageId" to remaining.firstOrNull().orEmpty(),
            )
        ).await()
    }

    /** Xoa san pham kem toan bo anh cua no. */
    suspend fun deleteProduct(id: String): Result<Unit> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }
        val productRef = db.collection(ProductImage.PRODUCT_COLLECTION).document(id)
        val imageIds = productRef.get().await().get("imageIds") as? List<String> ?: emptyList()

        imageIds.forEach { imageId ->
            runCatching {
                db.collection(ProductImage.COLLECTION).document(imageId).delete().await()
            }
        }
        productRef.delete().await()
    }

    /**
     * Tru so luong con ban khi dat hang.
     *
     * Dung transaction de hai nguoi mua cung luc khong bao gio mua het hang.
     *
     * PHAI dung `update` chu khong phai `set` — `set` ghi de toan bo document,
     * se xoa mat ten, gia va anh cua san pham. `update` chi ghi hai field
     * ton kho, dung voi Security Rules (chi cho doi availableStock/soldCount).
     */
    suspend fun reserveStock(productId: String, quantity: Int): Result<Unit> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }
        val ref = db.collection(ProductImage.PRODUCT_COLLECTION).document(productId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val available = (snapshot.get("availableStock") as? Number)?.toInt() ?: 0
            val sold = (snapshot.get("soldCount") as? Number)?.toInt() ?: 0
            check(available >= quantity) { "Sản phẩm không còn đủ hàng" }
            transaction.update(
                ref,
                mapOf(
                    "availableStock" to available - quantity,
                    "soldCount" to sold + quantity,
                )
            )
        }.await()
    }

    /**
     * Bat hoac tat hien san pham.
     *
     * San pham da AN khong hien voi khach, nhung admin van thay va bat lai duoc.
     */
    suspend fun setVisibility(productId: String, visible: Boolean): Result<Unit> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }
        db.collection(ProductImage.PRODUCT_COLLECTION).document(productId)
            .update(mapOf("visible" to visible))
            .await()
    }

    /** Loc san pham theo danh muc; "Tất cả" hien thi toan bo. */
    fun filterByCategory(products: List<Product>, category: String): List<Product> =
        if (category == SampleData.CATEGORY_ALL) products
        else products.filter { it.category == category }

    private fun com.google.firebase.firestore.DocumentSnapshot.toProduct() = Product(
        id = id,
        name = getString("name").orEmpty(),
        category = getString("category").orEmpty(),
        price = when (val v = get("price")) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: 0L
            else -> 0L
        },
        totalStock = (get("totalStock") as? Number)?.toInt() ?: 0,
        availableStock = (get("availableStock") as? Number)?.toInt() ?: 0,
        soldCount = (get("soldCount") as? Number)?.toInt() ?: 0,
        description = getString("description").orEmpty(),
        imageIds = (get("imageIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        coverImageId = getString("coverImageId").orEmpty(),
        createdAt = (get("createdAt") as? com.google.firebase.Timestamp)?.toDate()?.time
            ?: (get("createdAt") as? Number)?.toLong() ?: 0L,
        // San pham cu khong co field "visible" (tao truoc khi co tinh nang an)
        // duoc coi la dang ban.
        visible = get("visible") as? Boolean ?: true,
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toProductImage() = ProductImage(
        id = id,
        productId = getString("productId").orEmpty(),
        data = getString("data").orEmpty(),
        order = (get("order") as? Number)?.toInt() ?: 0,
        fileName = getString("fileName").orEmpty(),
        sizeBytes = (get("sizeBytes") as? Number)?.toLong() ?: 0L,
    )

    companion object {
        const val COLLECTION = ProductImage.PRODUCT_COLLECTION

        /** Gioi han so gia tri cho moi `whereIn` cua Firestore. */
        private const val MAX_WHERE_IN = 30
    }
}
