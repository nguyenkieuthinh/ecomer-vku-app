package com.ecomer.vku.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomer.vku.app.data.FirebaseExt
import com.ecomer.vku.app.data.model.CartItem
import com.ecomer.vku.app.data.model.Order
import com.ecomer.vku.app.data.model.Product
import com.ecomer.vku.app.data.repo.CartRepository
import com.ecomer.vku.app.data.repo.OrderRepository
import com.ecomer.vku.app.data.repo.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Trang thai chung cua phan shop: san pham, gio hang, don hang.
 */
data class ShopUiState(
    val products: List<Product> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "",
    val searchQuery: String = "",
    val cart: List<CartItem> = emptyList(),
    val orders: List<Order> = emptyList(),
    val message: String? = null,
    /**
     * Anh san pham theo id: imageId -> du lieu base64.
     * Anh duoc tach rieng thanh document trong Firestore nen phai tai theo id.
     */
    val images: Map<String, String> = emptyMap(),
) {
    /** San pham sau khi loc theo danh muc va tim kiem. */
    val visibleProducts: List<Product>
        get() = products.filter { product ->
            val inCategory = selectedCategory.isEmpty() ||
                    product.loaiSanPham == selectedCategory
            val inSearch = searchQuery.isBlank() ||
                    product.tenSanPham.contains(searchQuery, ignoreCase = true) ||
                    product.loaiSanPham.contains(searchQuery, ignoreCase = true)
            inCategory && inSearch
        }

    val cartTotal: Long get() = cart.sumOf { it.subtotal }
    val cartCount: Int get() = cart.sumOf { it.quantity }

    /** Anh dai dien cua san pham. */
    fun thumbnail(product: Product): String =
        images[product.image].orEmpty()

    /** Tat ca anh cua san pham, theo thu tu. */
    fun imagesOf(product: Product): List<String> =
        images.values.toList()
}

/**
 * ViewModel dung chung cho cac man hinh trong shop.
 */
class ShopViewModel(
    private val productRepository: ProductRepository = ProductRepository(),
    private val cartRepository: CartRepository = CartRepository(),
    private val orderRepository: OrderRepository = OrderRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopUiState())
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    /**
     * uid cua nguoi dung hien tai.
     *
     * KHONG cache uid o khoi tao: ViewModel co the song lai sau khi logout
     * roi dang nhap tai khoan khac. Doc uid dong va tai lai du lieu moi khi
     * nguoi dung doi giup khong con hien du lieu cua tai khoan truoc.
     */
    private var userId: String? = FirebaseExt.currentUserId

    init {
        loadProducts()
        loadCart()
        loadOrders()
        observeAuthChanges()
    }

    /** Tai lai gio hang va don hang moi khi nguoi dung doi tai khoan. */
    private var authListener: com.google.firebase.auth.FirebaseAuth.AuthStateListener? = null

    private fun observeAuthChanges() {
        authListener = FirebaseExt.observeAuth { newUid ->
            if (newUid != userId) {
                userId = newUid
                // Xoa du lieu cu truoc khi tai du lieu moi.
                _uiState.update { it.copy(cart = emptyList(), orders = emptyList()) }
                loadCart()
                loadOrders()
            }
        }
    }

    override fun onCleared() {
        authListener?.let { FirebaseExt.auth?.removeAuthStateListener(it) }
        super.onCleared()
    }

    /** Nap danh sach san pham kem anh cua chung. */
    fun loadProducts() {
        viewModelScope.launch {
            runCatching {
                // Khach chi thay san pham DANG HIEN — san pham da an bi an khoi shop.
                productRepository.observeProducts(onlyVisible = true).collect { list ->
                    _uiState.update { state ->
                        state.copy(
                            products = list,
                            categories = listOf(com.ecomer.vku.app.data.repo.SampleData.CATEGORY_ALL) +
                                    list.map { it.loaiSanPham }.distinct(),
                        )
                    }
                    loadImages(list)
                }
            }
        }
    }

    /**
     * Tai anh cua cac san pham.
     *
     * Gom tat ca id anh lai roi doc mot lan bang [loadImages] de khong phai
     * goi moi anh mot lan (N+1 request).
     */
    private suspend fun loadImages(products: List<Product>) {
        val imageIds = products.map { it.image }.filter { it.isNotBlank() }.distinct()
        if (imageIds.isEmpty()) return
        val loaded = productRepository.loadImages(imageIds)
        _uiState.update { state ->
            state.copy(images = loaded.associate { it.id to it.data })
        }
    }

    /** Nap gio hang cua nguoi dung hien tai. */
    fun loadCart() {
        val uid = userId ?: return
        viewModelScope.launch {
            runCatching {
                cartRepository.observeCart(uid).collect { items ->
                    _uiState.update { it.copy(cart = items) }
                }
            }
        }
    }

    /** Nap don hang cua nguoi dung hien tai. */
    fun loadOrders() {
        val uid = userId ?: return
        viewModelScope.launch {
            runCatching {
                orderRepository.observeOrders(uid).collect { list ->
                    _uiState.update { it.copy(orders = list) }
                }
            }
        }
    }

    /** Chon danh muc de loc. */
    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    /** Tim kiem san pham theo ten hoac loai. */
    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /** Them san pham vao gio hang. */
    fun addToCart(product: Product, quantity: Int = 1) {
        val current = _uiState.value.cart
        val existing = current.find { it.productId == product.id }
        val updated = if (existing != null) {
            current.map {
                if (it.productId == product.id) {
                    it.copy(quantity = it.quantity + quantity)
                } else {
                    it
                }
            }
        } else {
            current + CartItem(
                productId = product.id,
                name = product.tenSanPham,
                price = product.gia,
                imageUrl = _uiState.value.thumbnail(product),
                quantity = quantity,
            )
        }
        _uiState.update { it.copy(cart = updated, message = "Đã thêm vào giỏ hàng") }
        persistCart(updated)
    }

    /** Tang so luong mot mon trong gio hang. */
    fun increaseQuantity(productId: String) {
        updateQuantity(productId) { it + 1 }
    }

    /** Giam so luong mot mon trong gio hang. */
    fun decreaseQuantity(productId: String) {
        updateQuantity(productId) { (it - 1).coerceAtLeast(0) }
    }

    private fun updateQuantity(productId: String, transform: (Int) -> Int) {
        val updated = _uiState.value.cart.mapNotNull { item ->
            if (item.productId != productId) {
                item
            } else {
                val newQty = transform(item.quantity)
                if (newQty <= 0) null else item.copy(quantity = newQty)
            }
        }
        _uiState.update { it.copy(cart = updated) }
        persistCart(updated)
    }

    /** Xoa mot mon khoi gio hang. */
    fun removeFromCart(productId: String) {
        val updated = _uiState.value.cart.filterNot { it.productId == productId }
        _uiState.update { it.copy(cart = updated) }
        persistCart(updated)
    }

    /** Dat hang: tao don trong Firestore roi xoa gio hang. */
    fun placeOrder(onSuccess: () -> Unit = {}) {
        val uid = userId
        val items = _uiState.value.cart
        if (uid == null || items.isEmpty()) {
            _uiState.update { it.copy(message = "Giỏ hàng đang trống") }
            return
        }
        viewModelScope.launch {
            orderRepository.placeOrder(uid, items)
                .onSuccess {
                    cartRepository.clearCart(uid)
                    _uiState.update { state ->
                        state.copy(cart = emptyList(), message = "Đặt hàng thành công")
                    }
                    loadOrders()
                    loadProducts()
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(message = FirebaseExt.friendlyMessage(e))
                    }
                }
        }
    }

    /** Xoa thong bao nho. */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun persistCart(items: List<CartItem>) {
        val uid = userId ?: return
        viewModelScope.launch { cartRepository.saveCart(uid, items) }
    }
}
