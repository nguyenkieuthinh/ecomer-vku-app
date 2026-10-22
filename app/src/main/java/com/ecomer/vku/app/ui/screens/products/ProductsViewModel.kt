package com.ecomer.vku.app.ui.screens.products

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomer.vku.app.data.EncodedImage
import com.ecomer.vku.app.data.FirebaseExt
import com.ecomer.vku.app.data.ImageEncoder
import com.ecomer.vku.app.data.model.Product
import com.ecomer.vku.app.data.model.User
import com.ecomer.vku.app.data.repo.AuthRepository
import com.ecomer.vku.app.data.repo.ProductRepository
import com.ecomer.vku.app.data.repo.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Mot anh trong form nhap lieu.
 *
 * @param id id cua document anh (rong neu chua luu).
 * @param data du lieu base64.
 * @param isNew true neu vua chon va chua luu vao Firestore.
 */
data class ProductImageDraft(
    val id: String = "",
    val data: String = "",
    val tenFile: String = "",
    val sizeBytes: Long = 0,
    val isNew: Boolean = false,
)

/**
 * Trang thai man hinh du lieu san pham.
 *
 * @param editing san pham dang sua (null neu dang them moi).
 * @param images danh sach anh cua san pham (da luu + moi chon). Anh dau tien
 *   la anh dai dien.
 * @param pendingDelete san pham dang cho xac nhan xoa.
 */
data class ProductsUiState(
    val products: List<Product> = emptyList(),
    val name: String = "",
    val category: String = "",
    val price: String = "",
    val images: List<ProductImageDraft> = emptyList(),
    val editing: Product? = null,
    val loading: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
    val thumbnails: Map<String, String> = emptyMap(),
    val userEmail: String = "",
    val isAdmin: Boolean = false,
    val accountMenuOpen: Boolean = false,
    val pendingDelete: Product? = null,
) {
    /** So anh da chon. */
    val imageCount: Int get() = images.size

    /** Anh dai dien cua mot san pham trong danh sach. */
    fun thumbnail(product: Product): String = thumbnails[product.image].orEmpty()
}

/**
 * ViewModel cho man hinh du lieu san pham.
 *
 * Quyen:
 * - ai dang nhap cung thay tat ca san pham;
 * - user thuong chi sua/xoa san pham do chinh minh nhap;
 * - admin sua/xoa tat ca san pham.
 *
 * Quyen that su duoc Firestore Rules kiem tra o server.
 */
class ProductsViewModel(
    private val productRepository: ProductRepository = ProductRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductsUiState())
    val uiState: StateFlow<ProductsUiState> = _uiState.asStateFlow()

    private val firebaseUser = FirebaseExt.auth?.currentUser
    private val uid: String = firebaseUser?.uid.orEmpty()
    private val email: String = firebaseUser?.email.orEmpty()

    init {
        val isAdmin = email.lowercase() in User.ADMIN_EMAILS
        _uiState.update { it.copy(userEmail = email, isAdmin = isAdmin) }

        firebaseUser?.let { user ->
            viewModelScope.launch { userRepository.ensureUser(user) }
        }

        if (uid.isNotBlank()) {
            observeProducts()
        }
    }

    /** Theo doi tat ca san pham — ai dang nhap cung thay het. */
    private fun observeProducts() {
        viewModelScope.launch {
            productRepository.observeProducts().collect { list ->
                _uiState.update { it.copy(products = list) }
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
    fun setPrice(value: String) =
        _uiState.update { it.copy(price = value.filter { it.isDigit() }) }

    /**
     * Chon nhieu anh tu thu vien.
     *
     * Anh duoc resize ve canh dai toi da 1080 px roi nen xuong duoi 700 KB
     * ngay tai day.
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
                    ProductImageDraft(
                        data = it.data,
                        tenFile = it.tenFile,
                        sizeBytes = it.sizeBytes,
                        isNew = true,
                    )
                }
                state.copy(
                    loading = false,
                    images = state.images + added,
                    message = if (failed > 0) {
                        "Không đọc được $failed ảnh. Ảnh còn lại đã thêm."
                    } else {
                        null
                    },
                    messageIsError = failed > 0,
                )
            }
        }
    }

    /**
     * Xoa anh khoi danh sach.
     *
     * Anh da luu trong Firestore se bi xoa khi bam "Cập nhật".
     */
    fun removeImage(index: Int) {
        _uiState.update { state ->
            state.images.getOrNull(index) ?: return@update state
            state.copy(images = state.images.filterIndexed { i, _ -> i != index })
        }
    }

    /**
     * Chon mot anh lam anh dai dien.
     *
     * Anh dai dien luon la anh DUNG DAU danh sach, nen thao tac nay chi can
     * chuyen anh duoc chon len truoc.
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
        if (!canModify(product)) return
        viewModelScope.launch {
            val loaded = productRepository.loadImagesOf(product.id)
            val images = loaded.map {
                ProductImageDraft(
                    id = it.id,
                    data = it.data,
                    tenFile = it.tenFile,
                    sizeBytes = it.sizeBytes,
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
                    message = null,
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
    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            showError("Vui lòng nhập tên sản phẩm")
            return
        }
        if (state.category.isBlank()) {
            showError("Vui lòng nhập loại sản phẩm")
            return
        }
        val price = state.price.toLongOrNull()
        if (price == null || price <= 0) {
            showError("Giá sản phẩm không hợp lệ")
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
                ownerId = uid,
            )

            val newImages = state.images.filter { it.isNew }.map {
                EncodedImage(it.data, it.tenFile, it.sizeBytes)
            }
            val keptIds = state.images.filterNot { it.isNew }.map { it.id }
            val removedIds = if (editing == null) {
                emptyList()
            } else {
                val currentIds = productRepository.loadImagesOf(editing.id).map { it.id }
                currentIds.filter { id -> id !in keptIds }
            }

            val result = if (editing == null) {
                productRepository.addProduct(product, newImages, uid, email)
            } else {
                if (!canModify(editing)) {
                    showError("Bạn không có quyền sửa sản phẩm này")
                    return@launch
                }
                productRepository.updateProduct(product, keptIds, newImages, removedIds)
            }

            result
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            editing = null,
                            name = "",
                            category = "",
                            price = "",
                            images = emptyList(),
                            loading = false,
                            message = "Lưu sản phẩm thành công",
                            messageIsError = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            message = FirebaseExt.friendlyMessage(error),
                            messageIsError = true,
                        )
                    }
                }
        }
    }

    /** Bam nut Xoa — hoi xac nhan truoc. */
    fun requestDelete(product: Product) {
        if (!canModify(product)) return
        _uiState.update { it.copy(pendingDelete = product) }
    }

    /** Huy hop thoai xac nhan xoa. */
    fun cancelDelete() = _uiState.update { it.copy(pendingDelete = null) }

    /** Xac nhan xoa san pham. */
    fun confirmDelete() {
        val product = _uiState.value.pendingDelete ?: return
        _uiState.update { it.copy(pendingDelete = null, loading = true) }
        viewModelScope.launch {
            productRepository.deleteProduct(product.id)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            message = "Đã xoá sản phẩm",
                            messageIsError = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            message = FirebaseExt.friendlyMessage(error),
                            messageIsError = true,
                        )
                    }
                }
        }
    }

    /** Mo / dong menu tai khoan. */
    fun toggleAccountMenu() =
        _uiState.update { it.copy(accountMenuOpen = !it.accountMenuOpen) }

    fun closeAccountMenu() = _uiState.update { it.copy(accountMenuOpen = false) }

    /** Dang xuat va ve man hinh dang nhap. */
    fun signOut(onDone: () -> Unit) {
        authRepository.signOut()
        _uiState.update { it.copy(accountMenuOpen = false) }
        onDone()
    }

    /** Xoa thong bao. */
    fun clearMessage() = _uiState.update { it.copy(message = null) }

    private fun showError(text: String) =
        _uiState.update { it.copy(loading = false, message = text, messageIsError = true) }

    /** Chi chu san pham hoac admin moi duoc sua / xoa. */
    fun canModify(product: Product): Boolean =
        _uiState.value.isAdmin || product.ownerId == uid
}
