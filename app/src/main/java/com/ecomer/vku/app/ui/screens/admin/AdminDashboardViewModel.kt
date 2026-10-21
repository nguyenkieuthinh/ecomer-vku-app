package com.ecomer.vku.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomer.vku.app.data.model.Order
import com.ecomer.vku.app.data.model.Product
import com.ecomer.vku.app.data.model.User
import com.ecomer.vku.app.data.repo.OrderRepository
import com.ecomer.vku.app.data.repo.ProductRepository
import com.ecomer.vku.app.data.repo.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Trang thai man hinh quan tri cua admin.
 */
data class AdminDashboardUiState(
    val products: List<Product> = emptyList(),
    val orders: List<Order> = emptyList(),
    val users: List<User> = emptyList(),
    val loading: Boolean = false,
    val message: String? = null,
) {
    /** Doanh thu tu don chua bi huy. */
    val revenue: Long
        get() = orders.filter { it.status != Order.STATUS_CANCELLED }.sumOf { it.total }

    /** So don dang cho xu ly. */
    val pendingCount: Int
        get() = orders.count { it.status == Order.STATUS_PENDING }

    /** So san pham dang an (khong hien voi khach). */
    val hiddenCount: Int get() = products.count { !it.visible }
}

/**
 * ViewModel cho trang quan tri: tong quan, don hang, nguoi dung.
 */
class AdminDashboardViewModel(
    private val orderRepository: OrderRepository = OrderRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val productRepository: ProductRepository = ProductRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    /** Nap tat ca du lieu cho trang quan tri. */
    fun loadAll() {
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch {
            launch {
                orderRepository.observeAllOrders().collect { list ->
                    _uiState.update { it.copy(orders = list) }
                }
            }
            launch {
                userRepository.observeAllUsers().collect { list ->
                    _uiState.update { it.copy(users = list) }
                }
            }
            launch {
                productRepository.observeProducts().collect { list ->
                    _uiState.update { it.copy(products = list) }
                }
            }
        }
    }

    /** Doi trang thai don hang. */
    fun updateOrderStatus(orderId: String, status: String) {
        viewModelScope.launch {
            orderRepository.updateStatus(orderId, status)
                .onSuccess {
                    _uiState.update { it.copy(message = "Đã cập nhật đơn hàng") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(message = e.message ?: "Không cập nhật được đơn hàng") }
                }
        }
    }

    /** Xoa thong bao. */
    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
