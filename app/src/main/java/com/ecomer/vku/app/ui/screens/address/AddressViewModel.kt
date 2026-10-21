package com.ecomer.vku.app.ui.screens.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomer.vku.app.data.FirebaseExt
import com.ecomer.vku.app.data.model.Address
import com.ecomer.vku.app.data.repo.AddressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Trang thai man hinh dia chi giao hang.
 */
data class AddressUiState(
    val addresses: List<Address> = emptyList(),
    val editing: Address? = null,
    val loading: Boolean = false,
    val message: String? = null,
    /** True khi nguoi dung bam "Thêm địa chỉ" tu man thanh toan. */
    val requiredForCheckout: Boolean = false,
) {
    /** Dia chi mac dinh, hoac dia chi dau tien neu co. */
    val defaultAddress: Address? get() = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
}

/**
 * ViewModel cho man hinh dia chi giao hang.
 */
class AddressViewModel(
    private val addressRepository: AddressRepository = AddressRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddressUiState())
    val uiState: StateFlow<AddressUiState> = _uiState.asStateFlow()

    /**
     * uid cua nguoi dung hien tai.
     *
     * Doc dong (khong cache) vi ViewModel co the song lai qua nhieu lan
     * dang nhap — cache se lam hien dia chi cua tai khoan truoc.
     */
    private var userId: String? = FirebaseExt.currentUserId

    init {
        loadAddresses()
        observeAuthChanges()
    }

    /** Tai lai danh sach dia chi khi nguoi dung doi tai khoan. */
    private var authListener: com.google.firebase.auth.FirebaseAuth.AuthStateListener? = null

    private fun observeAuthChanges() {
        authListener = FirebaseExt.observeAuth { newUid ->
            if (newUid != userId) {
                userId = newUid
                _uiState.update { it.copy(addresses = emptyList(), editing = null) }
                loadAddresses()
            }
        }
    }

    override fun onCleared() {
        authListener?.let { FirebaseExt.auth?.removeAuthStateListener(it) }
        super.onCleared()
    }

    /** Nap danh sach dia chi. */
    fun loadAddresses() {
        val uid = userId ?: return
        viewModelScope.launch {
            addressRepository.observeAddresses(uid).collect { list ->
                _uiState.update { it.copy(addresses = list) }
            }
        }
    }

    /** Bat dau them dia chi moi. */
    fun startAdd(requiredForCheckout: Boolean = false) {
        _uiState.update { it.copy(editing = Address(), requiredForCheckout = requiredForCheckout) }
    }

    /** Bat dau sua mot dia chi. */
    fun startEdit(address: Address) {
        _uiState.update { it.copy(editing = address) }
    }

    /** Huy them / sua. */
    fun cancelEdit() {
        _uiState.update { it.copy(editing = null) }
    }

    /** Luu dia chi. */
    fun save(address: Address) {
        val uid = userId ?: return
        if (address.fullName.isBlank()) {
            _uiState.update { it.copy(message = "Vui lòng nhập tên người nhận") }
            return
        }
        if (address.phone.isBlank()) {
            _uiState.update { it.copy(message = "Vui lòng nhập số điện thoại") }
            return
        }
        if (address.street.isBlank()) {
            _uiState.update { it.copy(message = "Vui lòng nhập số nhà, đường") }
            return
        }
        if (address.city.isBlank()) {
            _uiState.update { it.copy(message = "Vui lòng nhập tỉnh/thành phố") }
            return
        }

        _uiState.update { it.copy(loading = true, message = null) }
        viewModelScope.launch {
            addressRepository.saveAddress(uid, address)
                .onSuccess {
                    _uiState.update {
                        it.copy(loading = false, editing = null, message = "Đã lưu địa chỉ")
                    }
                    loadAddresses()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(loading = false, message = FirebaseExt.friendlyMessage(e))
                    }
                }
        }
    }

    /** Xoa dia chi. */
    fun delete(address: Address) {
        val uid = userId ?: return
        viewModelScope.launch {
            addressRepository.deleteAddress(uid, address.id)
                .onSuccess {
                    _uiState.update { it.copy(message = "Đã xóa địa chỉ") }
                    loadAddresses()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(message = FirebaseExt.friendlyMessage(e)) }
                }
        }
    }

    /** Dat dia chi mac dinh. */
    fun setDefault(address: Address) {
        val uid = userId ?: return
        viewModelScope.launch {
            addressRepository.setDefault(uid, address.id)
            loadAddresses()
        }
    }

    /** Xoa thong bao. */
    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
