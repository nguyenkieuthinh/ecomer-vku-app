package com.ecomer.vku.app.ui.screens.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomer.vku.app.data.FirebaseExt
import com.ecomer.vku.app.data.model.Address
import com.ecomer.vku.app.data.model.CartItem
import com.ecomer.vku.app.data.repo.AddressRepository
import com.ecomer.vku.app.data.repo.CartRepository
import com.ecomer.vku.app.data.repo.OrderRepository
import com.ecomer.vku.app.data.repo.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Trang thai man hinh thanh toan.
 */
data class CheckoutUiState(
    val items: List<CartItem> = emptyList(),
    val addresses: List<Address> = emptyList(),
    val selectedAddress: Address? = null,
    val loading: Boolean = false,
    val message: String? = null,
    val placed: Boolean = false,
) {
    val total: Long get() = items.sumOf { it.subtotal }
    val hasAddress: Boolean get() = selectedAddress != null
}

/**
 * ViewModel cho man hinh thanh toan.
 *
 * Dat hang BAT BUOC phai co dia chi (co du la nguoi dung chua them dia chi nao).
 */
class CheckoutViewModel(
    private val addressRepository: AddressRepository = AddressRepository(),
    private val cartRepository: CartRepository = CartRepository(),
    private val orderRepository: OrderRepository = OrderRepository(),
    private val productRepository: ProductRepository = ProductRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    /**
     * uid cua nguoi dung hien tai.
     *
     * Doc dong (khong cache) vi ViewModel co the song lai qua nhieu lan
     * dang nhap — cache se lam hien du lieu cua tai khoan truoc.
     */
    private var userId: String? = FirebaseExt.currentUserId

    init {
        loadAddresses()
        observeAuthChanges()
    }

    /** Tai lai du lieu khi nguoi dung doi tai khoan. */
    private var authListener: com.google.firebase.auth.FirebaseAuth.AuthStateListener? = null

    private fun observeAuthChanges() {
        authListener = FirebaseExt.observeAuth { newUid ->
            if (newUid != userId) {
                userId = newUid
                _uiState.update {
                    it.copy(addresses = emptyList(), items = emptyList(), selectedAddress = null)
                }
                loadAddresses()
            }
        }
    }

    override fun onCleared() {
        authListener?.let { FirebaseExt.auth?.removeAuthStateListener(it) }
        super.onCleared()
    }

    /** Nap dia chi va gio hang. */
    fun loadAddresses() {
        val uid = userId ?: return
        viewModelScope.launch {
            addressRepository.observeAddresses(uid).collect { list ->
                _uiState.update { state ->
                    val selected = state.selectedAddress?.let { sel ->
                        list.firstOrNull { it.id == sel.id }
                    } ?: list.firstOrNull { it.isDefault } ?: list.firstOrNull()
                    state.copy(addresses = list, selectedAddress = selected)
                }
            }
        }
        viewModelScope.launch {
            cartRepository.observeCart(uid).collect { items ->
                _uiState.update { it.copy(items = items) }
            }
        }
    }

    /** Chon dia chi giao hang. */
    fun selectAddress(address: Address) {
        _uiState.update { it.copy(selectedAddress = address) }
    }

    /**
     * Xac nhan dat hang.
     *
     * @param onDone goi khi thanh cong (dieu huong sang man don hang).
     */
    fun placeOrder(onDone: () -> Unit = {}) {
        val uid = userId
        val state = _uiState.value
        val address = state.selectedAddress

        if (uid == null) {
            _uiState.update { it.copy(message = "Chưa đăng nhập") }
            return
        }
        if (state.items.isEmpty()) {
            _uiState.update { it.copy(message = "Giỏ hàng đang trống") }
            return
        }
        // BAT BUOC: phai co dia chi giao hang moi dat duoc hang.
        if (address == null || !address.isComplete) {
            _uiState.update { it.copy(message = "Vui lòng thêm địa chỉ giao hàng trước khi thanh toán") }
            return
        }

        _uiState.update { it.copy(loading = true, message = null) }
        viewModelScope.launch {
            orderRepository.placeOrder(uid, state.items, address)
                .onSuccess {
                    cartRepository.clearCart(uid)
                    _uiState.update {
                        it.copy(loading = false, placed = true, message = "Đặt hàng thành công")
                    }
                    onDone()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(loading = false, message = FirebaseExt.friendlyMessage(e))
                    }
                }
        }
    }

    /** Xoa thong bao. */
    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
