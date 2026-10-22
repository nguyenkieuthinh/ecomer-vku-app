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
 * Repository cho san pham, anh san pham va chu so huu san pham.
 *
 * Cau truc Firestore (theo de thi):
 * - `products/{id}`: CHI co 4 truong — `tenSanPham`, `loaiSanPham`, `gia`,
 *   `image` (id cua document anh dai dien).
 * - `product_images/{id}`: mot anh, luu base64. Tach rieng nen gioi han 1 MiB
 *   ap dung cho TUNG anh.
 * - `product_owners/{productId}`: uid nguoi nhap san pham. Luu o collection
 *   rieng de KHONG them truong `userId` vao document san pham.
 * - `counters/{typeSlug}`: bo dem sinh id theo loai san pham.
 */
class ProductRepository(
    private val firestore: FirebaseFirestore? = FirebaseExt.firestore,
) {

    /**
     * Theo doi TAT CA san pham.
     *
     * Moi tai khoan da dang nhap deu thay san pham cua nguoi khac; quyen
     * sua/xoa moi la thu bi gioi han (xem `product_owners`). `ownerId` duoc
     * gan vao RAM de biet dong nao sua/xoa duoc — khong ghi xuong Firestore.
     */
    fun observeProducts(): Flow<List<Product>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            return@callbackFlow
        }

        var owners: Map<String, String> = emptyMap()
        var products: List<Product> = emptyList()

        fun sendProducts() {
            trySend(products.map { it.copy(ownerId = owners[it.id].orEmpty()) })
        }

        val ownerRegistration = db.collection(ProductImage.OWNER_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    FirebaseExt.reportFailure(error)
                    return@addSnapshotListener
                }
                owners = snapshot?.documents?.associate {
                    it.id to it.getString("userId").orEmpty()
                } ?: emptyMap()
                sendProducts()
            }

        val productRegistration = db.collection(ProductImage.PRODUCT_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    FirebaseExt.reportFailure(error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                products = snapshot?.documents?.map { it.toProduct() } ?: emptyList()
                sendProducts()
            }

        awaitClose {
            ownerRegistration.remove()
            productRegistration.remove()
        }
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
     * Dung khi sua san pham.
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
     * Chu so huu ghi vao `product_owners/{productId}`.
     *
     * @param product thong tin san pham.
     * @param images du lieu anh, theo thu tu hien thi.
     * @param ownerId uid nguoi nhap san pham.
     * @param ownerEmail email nguoi nhap (de doi chieu khi can).
     * @return id cua san pham vua tao.
     */
    suspend fun addProduct(
        product: Product,
        images: List<EncodedImage>,
        ownerId: String,
        ownerEmail: String,
    ): Result<String> = runCatching {
        val db = requireNotNull(firestore) { "Firebase chưa được cấu hình" }
        val productId = nextProductId(db, product.loaiSanPham)

        // Ghi chu so huu TRUOC: Rules kiem tra `product_owners` khi tao anh.
        db.collection(ProductImage.OWNER_COLLECTION).document(productId).set(
            mapOf(
                "userId" to ownerId,
                "userEmail" to ownerEmail,
                "createdAt" to com.google.firebase.Timestamp.now(),
            )
        ).await()

        try {
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
        } catch (error: Throwable) {
            // Don dep neu ghi do: khong de lai san pham "mo coi".
            runCatching {
                db.collection(ProductImage.COLLECTION)
                    .whereEqualTo("productId", productId)
                    .get()
                    .await()
                    .documents
                    .forEach { it.reference.delete().await() }
                // Xoa san pham truoc, chu so huu sau (Rules dua vao chu so huu).
                db.collection(ProductImage.PRODUCT_COLLECTION).document(productId).delete().await()
                db.collection(ProductImage.OWNER_COLLECTION).document(productId).delete().await()
            }
            throw error
        }

        productId
    }

    /**
     * Cap nhat san pham.
     *
     * Document san pham van chi ghi 4 truong — `image` tro toi anh dai dien
     * (anh DUNG DAU danh sach).
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

        // Cap nhat thu tu + tieu de cua anh con lai.
        keptImageIds.forEachIndexed { index, imageId ->
            runCatching {
                db.collection(ProductImage.COLLECTION).document(imageId)
                    .update(
                        mapOf(
                            "order" to index,
                            "tieuDe" to product.tenSanPham.trim(),
                        )
                    )
                    .await()
            }
        }

        // Them anh moi vao cuoi danh sach.
        //
        // Id anh moi phai LON HON moi id dang ton tai — khong duoc sinh tu
        // `keptImageIds.size`, vi danh sach co the da xoa o giua.
        val usedNumbers = (keptImageIds + removedImageIds)
            .mapNotNull { it.substringAfterLast('-', "").toIntOrNull() }
        var nextNumber = (usedNumbers.maxOrNull() ?: 0) + 1

        val addedIds = newImages.mapIndexed { index, image ->
            val imageId = "${product.id}-${nextNumber++}"
            db.collection(ProductImage.COLLECTION).document(imageId).set(
                mapOf(
                    "productId" to product.id,
                    "tieuDe" to product.tenSanPham.trim(),
                    "data" to image.data,
                    "tenFile" to image.tenFile,
                    "sizeBytes" to image.sizeBytes,
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

    /** Xoa san pham kem toan bo anh va chu so huu cua no. */
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

        // Xoa san pham TRUOC chu so huu: Rules cho xoa san pham dua vao
        // `product_owners/{id}` con ton tai. Xoa chu so huu truoc thi chinh
        // nguoi tao cung mat quyen xoa san pham cua minh.
        productRef.delete().await()

        runCatching {
            db.collection(ProductImage.OWNER_COLLECTION).document(id).delete().await()
        }
    }

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
