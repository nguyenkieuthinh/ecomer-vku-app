package com.ecomer.vku.app.ui.nav

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Giu danh sach anh dang xem cho man hinh phong to.
 *
 * Anh la chuoi base64 nen qua la rat dai — dua vao duong dan dieu huong se
 * lam route bi cat ngan. Vay viet sang ViewModel de truyen qua.
 */
class ImageViewerViewModel : ViewModel() {

    private val _images = MutableStateFlow<List<String>>(emptyList())
    val images: StateFlow<List<String>> = _images.asStateFlow()

    private val _initialIndex = MutableStateFlow(0)
    val initialIndex: StateFlow<Int> = _initialIndex.asStateFlow()

    /** Chuan bi anh de xem. */
    fun show(images: List<String>, initialIndex: Int) {
        _images.value = images
        _initialIndex.value = initialIndex
    }
}
