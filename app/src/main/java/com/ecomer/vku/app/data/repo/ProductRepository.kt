package com.ecomer.vku.app.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.ecomer.vku.app.data.EncodedImage
import com.ecomer.vku.app.data.FirebaseExt
import com.ecomer.vku.app.data.model.Product
import com.ecomer.vku.app.data.model.ProductImage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository cho san pham va anh san pham.
 *
 * Cau truc Firestore (theo de thi):
 * - `products/{id}`: CHI co 4 truong — `tenSanPham`, `loaiSanPham`, `gia`,
 *   `image` (id cua document anh dai dien).
 * - `product_images/{id}`: mot anh, luu base64. Tach rieng nen gioi han 1 MiB
 *   ap dung cho TUNG anh, san pham bao nhieu anh cung khong vuot.
 * - `hidden_products/{productId}`: san pham dang an. Luu o collection rieng
 *   de KHONG them truong "visible" vao document san pham.
 * - `counters/{typeSlug}`: bo dem sinh id theo loai san pham.
 */
class ProductRepository(
    private val firestore: FirebaseFirestore? = FirebaseExt.firestore,
) {

    /**
     * Theo doi danh sach san pham — dung LIVE LISTENER de danh sach tu cap nhat
     * khi admin them / sua / xoa.
     *
     * @param onlyVisible true = chi lay san pham dang ban (dung cho khach).
     *   false = lay tat ca, giu san pham da an (dung cho admin).
     */
    fun observeProducts(onlyVisible: Boolean = true): Flow<List<Product>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(SampleData.products)
            return@callbackFlow
        }

        var hiddenIds: Set<String> = emptySet()
        var products: List<Product> = emptyList()

        val hiddenRegistration = db.collection(ProductImage.HIDDEN_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    FirebaseExt.reportFailure(error)
                    return@addSnapshotListener
                }
                hiddenIds = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                emitFiltered(products, hiddenIds, onlyVisible)
            }

        val productRegistration = db.collection(ProductImage.PRODUCT_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    FirebaseExt.reportFailure(error)
                    trySend(SampleData.products)
                    return@addSnapshotListener
                }
                products = snapshot?.documents?.map { it.toProduct() } ?: emptyList()
                emitFiltered(products, hiddenIds, onlyVisible)
            }

        awaitClose {
            hiddenRegistration.remove()
            productRegistration.remove()
        }
    }

    /**
     * Loc va gui danh sach san pham theo trang thai an/hien.
     *
     * `visible` duoc gan o day (chi trong RAM) — document san pham tren
     * Firestore van chi co 4 truong.
     */
    private fun kotlinx.coroutines.channels.SendChannel<List<Product>>.emitFiltered(
        products: List<Product>,
        hiddenIds: Set<String>,
        onlyVisible: Boolean,
    ) {
        val marked = products.map { it.copy(visible = it.id !in hiddenIds) }
        val visible = marked.filter { !onlyVisible || it.visible }
        trySend(visible.ifEmpty { SampleData.products })
    }

    /**
     * Doc anh cua nhieu san pham mot luc theo id document.
     *
     * Dung `whereIn` theo id (chia lo 30) de gom het len mot lan round-trip
     * thay vi goi tung anh mot lan.
     */
    suspend fun loadImages(imageIds: List<String>): List<ProductImage> {
        val db = firestore ?: return emptyList()
        if (imageIds.isEmpty()) return emptyList()

        val loaded = runCatching {
            imageIds.distinct().chunked(MAX_WHERE_IN).flatMap { chunk ->
                db.collection(ProductImage.COLLECTION)
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                    .get()
                    .await()
                    .documents
                    .map { it.toProductImage() }
            }
        }.getOrElse { emptyList() }

        val byId = loaded.associateBy { it.id }
        return imageIds.mapNotNull { byId[it] }
    }

    /**
     * Doc TAT CA anh cua mot san pham (theo thu tu hien thi).
     *
     * Dung o man chi tiet san pham va khi sua san pham trong admin.
     */
    suspend fun loadImagesOf(productId: String): List<ProductImage> {
        val db = firestore ?: return emptyList()
        if (productId.isBlank()) return emptyList()
        return runCatching {
            db.collection(ProductImage.COLLECTION)
                .whereEqualTo("productId", productId)
                .get()
                .await()
                .documents
                .map { it.toProductImage() }
                .sortedBy { it.order }
        }.getOrElse { emptyList() }
    }

    /**
     * Theo doi TAT CA anh cua mot san pham, theo thu tu hien thi.
     * Dung o man chi tiet san pham de xem / vuot qua cac anh.
     */
    fun observeImagesOf(productId: String): Flow<List<ProductImage>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            return@callbackFlow
        }
        val registration = db.collection(ProductImage.COLLECTION)
            .whereEqualTo("productId", productId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { it.toProductImage() }
                    ?.sortedBy { it.order }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
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
     * Document san pham chi ghi 4 truong: tenSanPham, loaiSanPham, gia, image.
     *
     * @param product thong tin san pham.
     * @param images du lieu base64 cua cac anh, theo thu tu hien thi.
     * @return id cua san pham vua tao.
     */
    suspend fun addProduct(product: Product, images: List<EncodedImage>): Result<String> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }
        val productId = nextProductId(db, product.loaiSanPham)

        val imageIds = images.mapIndexed { index, image ->
            val imageId = "$productId-${index + 1}"
            db.collection(ProductImage.COLLECTION).document(imageId).set(
                mapOf(
                    "productId" to productId,
                    "tieuDe" to product.tenSanPham.trim(),
                    "data" to image.data,
                    "tenFile" to image.tenFile,
                    "sizeBytes" to image.sizeBytes,
                    "order" to index,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                )
            ).await()
            imageId
        }

        db.collection(ProductImage.PRODUCT_COLLECTION).document(productId).set(
            mapOf(
                "tenSanPham" to product.tenSanPham.trim(),
                "loaiSanPham" to product.loaiSanPham.trim(),
                "gia" to product.gia,
                "image" to imageIds.firstOrNull().orEmpty(),
            )
        ).await()
        productId
    }

    /**
     * Cap nhat san pham.
     *
     * Document san pham van chi ghi 4 truong — `image` tro toi anh moi nhat
     * (hoac anh cu neu khong co anh moi).
     */
    suspend fun updateProduct(
        product: Product,
        keptImageIds: List<String>,
        newImages: List<EncodedImage>,
        removedImageIds: List<String> = emptyList(),
    ): Result<Unit> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }

        // Xoa anh bi lo bo.
        removedImageIds.forEach { imageId ->
            runCatching {
                db.collection(ProductImage.COLLECTION).document(imageId).delete().await()
            }
        }

        // Cap nhat thu tu cua anh con lai.
        keptImageIds.forEachIndexed { index, imageId ->
            runCatching {
                db.collection(ProductImage.COLLECTION).document(imageId)
                    .update(mapOf("order" to index))
                    .await()
            }
        }

        // Them anh moi vao cuoi danh sach.
        val addedIds = newImages.mapIndexed { index, image ->
            val imageId = "${product.id}-${keptImageIds.size + index + 1}"
            db.collection(ProductImage.COLLECTION).document(imageId).set(
                mapOf(
                    "productId" to product.id,
                    "tieuDe" to product.tenSanPham.trim(),
                    "data" to image.data,
                    "tenFile" to image.tenFile,
                    "order" to keptImageIds.size + index,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                )
            ).await()
            imageId
        }

        val finalIds = keptImageIds + addedIds
        // `update` chu khong phai `set` — `set` se ghi de toan bo document.
        db.collection(ProductImage.PRODUCT_COLLECTION).document(product.id).update(
            mapOf(
                "tenSanPham" to product.tenSanPham.trim(),
                "loaiSanPham" to product.loaiSanPham.trim(),
                "gia" to product.gia,
                "image" to finalIds.firstOrNull().orEmpty(),
            )
        ).await()
    }

    /** Xoa san pham kem toan bo anh cua no. */
    suspend fun deleteProduct(id: String): Result<Unit> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }
        val productRef = db.collection(ProductImage.PRODUCT_COLLECTION).document(id)
        val snapshot = productRef.get().await()
        val imageId = snapshot.getString("image").orEmpty()

        // Xoa TAT CA anh cua san pham, khong chi anh dai dien.
        runCatching {
            db.collection(ProductImage.COLLECTION)
                .whereEqualTo("productId", id)
                .get()
                .await()
                .documents
                .forEach { it.reference.delete().await() }
        }
        if (imageId.isNotBlank()) {
            runCatching {
                db.collection(ProductImage.COLLECTION).document(imageId).delete().await()
            }
        }
        runCatching {
            db.collection(ProductImage.HIDDEN_COLLECTION).document(id).delete().await()
        }
        productRef.delete().await()
    }

    /**
     * Bat hoac tat hien san pham.
     *
     * Luu o collection `hidden_products` rieng — KHONG them truong vao document
     * san pham (de thi yeu cau chi 4 truong).
     */
    suspend fun setVisibility(productId: String, visible: Boolean): Result<Unit> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }
        val ref = db.collection(ProductImage.HIDDEN_COLLECTION).document(productId)
        if (visible) {
            ref.delete().await()
        } else {
            ref.set(mapOf("hiddenAt" to com.google.firebase.Timestamp.now())).await()
        }
    }

    /** Loc san pham theo danh muc; "Tất cả" hien thi toan bo. */
    fun filterByCategory(products: List<Product>, category: String): List<Product> =
        if (category == SampleData.CATEGORY_ALL) products
        else products.filter { it.loaiSanPham == category }

    private fun com.google.firebase.firestore.DocumentSnapshot.toProduct() = Product(
        id = id,
        tenSanPham = getString("tenSanPham").orEmpty(),
        loaiSanPham = getString("loaiSanPham").orEmpty(),
        gia = when (val v = get("gia")) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: 0L
            else -> 0L
        },
        image = getString("image").orEmpty(),
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toProductImage() = ProductImage(
        id = id,
        productId = getString("productId").orEmpty(),
        tieuDe = getString("tieuDe").orEmpty(),
        data = getString("data").orEmpty(),
        tenFile = getString("tenFile").orEmpty(),
        order = (get("order") as? Number)?.toInt() ?: 0,
        sizeBytes = (get("sizeBytes") as? Number)?.toLong() ?: 0L,
    )

    companion object {
        const val COLLECTION = ProductImage.PRODUCT_COLLECTION

        /** Gioi han so gia tri cho moi `whereIn` cua Firestore. */
        private const val MAX_WHERE_IN = 30
    }
}
