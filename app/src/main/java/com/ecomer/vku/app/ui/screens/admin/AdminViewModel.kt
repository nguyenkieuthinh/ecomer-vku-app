package com.ecomer.vku.app.ui.screens.admin

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomer.vku.app.data.FirebaseExt
import com.ecomer.vku.app.data.EncodedImage
import com.ecomer.vku.app.data.ImageEncoder
import com.ecomer.vku.app.data.model.Product
import com.ecomer.vku.app.data.repo.ProductRepository
import com.ecomer.vku.app.data.repo.SampleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Mot anh trong form quan ly san pham.
 *
 * @param id id cua document anh (rong neu chua luu).
 * @param data du lieu base64.
 * @param fileName ten file hien thi.
 * @param sizeBytes dung luong sau khi nen.
 * @param isNew true neu vua chon va chua luu vao Firestore.
 */
data class AdminImage(
    val id: String = "",
    val data: String = "",
    val tenFile: String = "",
    val sizeBytes: Long = 0,
    val isNew: Boolean = false,
)

/**
 * Trang thai man hinh quan ly san pham.
 *
 * @param editing san pham dang sua (null neu dang them moi).
 * @param images danh sach anh cua san pham (da luu + moi chon). Anh dau tien
 *   la anh dai dien.
 */
data class AdminUiState(
    val products: List<Product> = emptyList(),
    val categories: List<String> = emptyList(),
    val name: String = "",
    val category: String = "",
    val price: String = "",
    val images: List<AdminImage> = emptyList(),
    val editing: Product? = null,
    val loading: Boolean = false,
    val message: String? = null,
    /**
     * Anh dai dien cua san pham theo id: imageId -> du lieu base64.
     * Dung de hien thumbnail trong danh sach san pham.
     */
    val thumbnails: Map<String, String> = emptyMap(),
) {
    /** Tong dung luong cua tat ca anh (byte). */
    val totalSize: Long get() = images.sumOf { it.sizeBytes }

    /** Anh dang la dai dien — luon la anh dau tinh danh sach. */
    val coverImage: AdminImage?
        get() = images.firstOrNull()

    /** So anh da chon. */
    val imageCount: Int get() = images.size

    /** Anh dai dien cua mot san pham trong danh sach. */
    fun thumbnail(product: Product): String = thumbnails[product.image].orEmpty()
}

/**
 * ViewModel cho man hinh quan ly san pham (chi admin).
 */
class AdminViewModel(
    private val productRepository: ProductRepository = ProductRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    /** Nap danh sach san pham kem anh de danh sach hien thumbnail. */
    fun loadProducts() {
        viewModelScope.launch {
            // Admin thay TAT CA san pham, ke ca san pham da an.
            productRepository.observeProducts(onlyVisible = false).collect { list ->
                _uiState.update { state ->
                    state.copy(
                        products = list,
                        categories = listOf(SampleData.CATEGORY_ALL) +
                                list.map { it.loaiSanPham }.distinct().filter { it.isNotBlank() },
                    )
                }
                loadThumbnails(list)
            }
        }
    }

    /**
     * Tai anh dai dien cua cac san pham de danh sach hien thumbnail.
     *
     * Anh duoc luu o collection rieng nen phai tai theo id — dung mot lan
     * cho tat ca san pham thay vi goi tung anh mot lan.
     */
    private suspend fun loadThumbnails(products: List<Product>) {
        val imageIds = products.map { it.image }.filter { it.isNotBlank() }.distinct()
        if (imageIds.isEmpty()) return
        val loaded = productRepository.loadImages(imageIds)
        _uiState.update { state ->
            state.copy(thumbnails = loaded.associate { it.id to it.data })
        }
    }

    /** Cap nhat ten san pham trong form. */
    fun setName(value: String) = _uiState.update { it.copy(name = value) }

    /** Cap nhat loai san pham trong form. */
    fun setCategory(value: String) = _uiState.update { it.copy(category = value) }

    /** Cap nhat gia san pham trong form. */
    fun setPrice(value: String) = _uiState.update { it.copy(price = value.filter { it.isDigit() }) }



    /**
     * Chon nhieu anh tu thu vien.
     *
     * Anh duoc resize ve canh dai toi da 1080 px roi nen xuong duoi 700 KB
     * ngay tai day, nen moi lan chon la du dung luong de luu trong Firestore.
     */
    fun addImages(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        _uiState.update { it.copy(loading = true, message = null) }
        viewModelScope.launch {
            var failed = 0
            val encoded = uris.mapNotNull { uri ->
                ImageEncoder.encode(context, uri).getOrElse {
                    failed++
                    null
                }
            }
            _uiState.update { state ->
                val added = encoded.map {
                    AdminImage(
                        data = it.data,
                        tenFile = it.tenFile,
                        isNew = true,
                    )
                }
                state.copy(
                    loading = false,
                    images = state.images + added,
                    // Bao cho nguoi dung biet so anh that bai, thay vi im lang.
                    message = if (failed > 0) {
                        "Không đọc được $failed ảnh. Ảnh còn lại đã thêm."
                    } else {
                        null
                    },
                )
            }
        }
    }

    /**
     * Xoa anh khoi danh sach.
     *
     * Anh da luu trong Firestore se bi xoa luon khi bam "Cập nhật".
     */
    fun removeImage(index: Int) {
        _uiState.update { state ->
            val removed = state.images.getOrNull(index) ?: return@update state
            val remaining = state.images.filterIndexed { i, _ -> i != index }
            state.copy(images = remaining)
        }
    }

    /**
     * Chon mot anh lam anh dai dien.
     *
     * Anh dai dien luon la anh DUNG DAU danh sach, nen thao tac nay chi can
     * chuyen anh duoc chon len truoc. Cach nay tranh phai luu them mot truong
     * id rieng — quan trong vi anh moi chua co id trong Firestore.
     */
    fun setCover(index: Int) {
        _uiState.update { state ->
            val image = state.images.getOrNull(index) ?: return@update state
            val reordered = listOf(image) + state.images.filterIndexed { i, _ -> i != index }
            state.copy(images = reordered)
        }
    }

    /** Bat dau sua mot san pham kem tai anh cu cua no. */
    fun startEdit(product: Product) {
        viewModelScope.launch {
            val loaded = productRepository.loadImages(listOf(product.image))
            val images = loaded.map {
                AdminImage(
                    id = it.id,
                    data = it.data,
                    tenFile = it.tenFile,
                    isNew = false,
                )
            }
            _uiState.update { state ->
                state.copy(
                    editing = product,
                    name = product.tenSanPham,
                    category = product.loaiSanPham,
                    price = product.gia.toString(),
                    images = images,
                )
            }
        }
    }

    /** Huy chinh sua, quay ve che do them moi. */
    fun cancelEdit() {
        _uiState.update {
            it.copy(
                editing = null,
                name = "",
                category = "",
                price = "",
                images = emptyList(),
            )
        }
    }

    /** Luu san pham (them moi hoac cap nhat). */
    fun save(onDone: () -> Unit = {}) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(message = "Vui lòng nhập tên sản phẩm") }
            return
        }
        if (state.category.isBlank()) {
            _uiState.update { it.copy(message = "Vui lòng nhập loại sản phẩm") }
            return
        }
        val price = state.price.toLongOrNull()
        if (price == null || price <= 0) {
            _uiState.update { it.copy(message = "Giá sản phẩm không hợp lệ") }
            return
        }

        _uiState.update { it.copy(loading = true, message = null) }
        viewModelScope.launch {
            val editing = state.editing
            val product = Product(
                id = editing?.id.orEmpty(),
                tenSanPham = state.name,
                loaiSanPham = state.category,
                gia = price,
                image = editing?.image.orEmpty(),
            )

            // Anh moi chua luu -> them. Anh cu bi lo -> xoa.
            val newImages = state.images.filter { it.isNew }.map {
                EncodedImage(it.data, it.tenFile, it.sizeBytes)
            }
            // Anh cu con lai, theo thu tu hien thi (dau tien la anh dai dien).
            val keptIds = state.images.filterNot { it.isNew }.map { it.id }
            // Anh cu cua san pham dang sua ma KHONG con trong danh sach
            // (da bi xoa o form) -> se bi xoa khoi Firestore.
            val removedIds = if (editing == null) {
                emptyList()
            } else {
                val currentIds = productRepository.loadImages(listOf(editing.image)).map { it.id }
                currentIds.filter { id -> id !in keptIds }
            }

            val result = if (editing == null) {
                productRepository.addProduct(product, newImages)
            } else {
                productRepository.updateProduct(product, keptIds, newImages, removedIds)
            }

            result
                .onSuccess {
                    // Luu xong thi xoa form de nhap san pham tiep theo ngay.
                    // Neu dang sua, giu nguyen hinh thuc "sua" de khong vong lap.
                    val wasEditing = editing != null
                    if (wasEditing) {
                        cancelEdit()
                    } else {
                        _uiState.update {
                            it.copy(
                                name = "",
                                category = "",
                                price = "",
                                images = emptyList(),
                                            )
                        }
                    }
                    _uiState.update { it.copy(loading = false, message = "Lưu sản phẩm thành công") }
                    loadProducts()
                    onDone()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(loading = false, message = FirebaseExt.friendlyMessage(e))
                    }
                }
        }
    }

    /**
     * Bat hoac tat hien san pham.
     *
     * San pham da an khong hien voi khach, admin van thay va bat lai duoc.
     */
    fun setVisibility(product: Product, visible: Boolean) {
        viewModelScope.launch {
            productRepository.setVisibility(product.id, visible)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            message = if (visible) {
                                "Đã hiện sản phẩm"
                            } else {
                                "Đã ẩn sản phẩm khỏi cửa hàng"
                            }
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(message = FirebaseExt.friendlyMessage(e)) }
                }
        }
    }

    /** Xoa san pham. */
    fun delete(product: Product) {
        viewModelScope.launch {
            productRepository.deleteProduct(product.id)
                .onSuccess {
                    _uiState.update { it.copy(message = "Đã xóa sản phẩm") }
                    loadProducts()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(message = FirebaseExt.friendlyMessage(e)) }
                }
        }
    }

    /** Xoa thong bao. */
    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
