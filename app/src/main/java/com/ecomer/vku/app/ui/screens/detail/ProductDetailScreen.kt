package com.ecomer.vku.app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecomer.vku.app.ui.components.ErrorBanner
import com.ecomer.vku.app.ui.components.OutlineButton
import com.ecomer.vku.app.ui.components.PrimaryButton
import com.ecomer.vku.app.ui.components.ProductImage
import com.ecomer.vku.app.ui.screens.home.ShopViewModel
import com.ecomer.vku.app.ui.screens.home.formatPrice
import com.ecomer.vku.app.ui.theme.EcomerBackground
import com.ecomer.vku.app.ui.theme.EcomerBorder
import com.ecomer.vku.app.ui.theme.EcomerDanger
import com.ecomer.vku.app.ui.theme.EcomerTextMuted
import com.ecomer.vku.app.ui.theme.EcomerTextPrimary

/**
 * Man hinh chi tiet san pham: anh lon (co the xem nhieu anh), ten, loai, gia,
 * so luong con ban va nut them vao gio.
 */
@Composable
fun ProductDetailScreen(
    productId: String,
    onBack: () -> Unit,
    onAddedToCart: () -> Unit,
    onBuyNow: () -> Unit = {},
    onViewImages: (List<String>, Int) -> Unit = { _, _ -> },
    viewModel: ShopViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val product = state.products.find { it.id == productId }
        ?: state.products.firstOrNull()

    val images = product?.let { state.imagesOf(it) } ?: emptyList()
    var selectedImage by remember(productId) { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcomerBackground)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(20.dp),
    ) {
        OutlineButton(
            text = "Quay lại",
            onClick = onBack,
            modifier = Modifier.width(96.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (product == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Không tìm thấy sản phẩm",
                    color = EcomerTextMuted,
                    fontSize = 14.sp,
                )
            }
            return@Column
        }

        // Thong bao loi / thanh cong tu ViewModel.
        if (state.message != null) {
            ErrorBanner(
                message = state.message!!,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        // Anh lon — bam vao de xem phong to.
        ProductImage(
            imageUrl = images.getOrNull(selectedImage).orEmpty(),
            contentDescription = product.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clickable {
                    if (images.isNotEmpty()) {
                        onViewImages(images, selectedImage)
                    }
                },
            placeholderText = "Ảnh sản phẩm",
        )

        // Danh sách anh nho
        if (images.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(images) { image ->
                    ProductImage(
                        imageUrl = image,
                        contentDescription = product.name,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                selectedImage = images.indexOf(image)
                            },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = product.name,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = EcomerTextPrimary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Loại sp: ${product.category}",
            fontSize = 14.sp,
            color = EcomerTextMuted,
            lineHeight = 22.sp,
        )
        Text(
            text = "Giá sp: ${formatPrice(product.price)}",
            fontSize = 14.sp,
            color = EcomerTextMuted,
            lineHeight = 22.sp,
        )
        Text(
            text = "Còn lại: ${product.availableStock} sản phẩm",
            fontSize = 14.sp,
            color = if (product.availableStock > 0) EcomerTextMuted else EcomerDanger,
            lineHeight = 22.sp,
        )
        if (product.description.isNotBlank()) {
            Text(
                text = product.description,
                fontSize = 13.sp,
                color = EcomerTextMuted,
                lineHeight = 20.sp,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hai hanh dong rieng: them vao gio, hoac mua ngay (thang qua man thanh toan).
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlineButton(
                text = if (product.availableStock > 0) "THÊM VÀO GIỎ" else "HẾT HÀNG",
                onClick = {
                    viewModel.addToCart(product)
                    onAddedToCart()
                },
                enabled = product.availableStock > 0,
                modifier = Modifier.weight(1f),
                heightDp = 48,
            )
            PrimaryButton(
                text = "MUA NGAY",
                onClick = {
                    viewModel.addToCart(product)
                    onBuyNow()
                },
                enabled = product.availableStock > 0,
                modifier = Modifier.weight(1f),
                heightDp = 48,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
