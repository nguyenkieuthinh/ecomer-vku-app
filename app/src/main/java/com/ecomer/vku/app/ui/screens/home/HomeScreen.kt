package com.ecomer.vku.app.ui.screens.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecomer.vku.app.data.FirebaseExt
import com.ecomer.vku.app.data.model.Product
import com.ecomer.vku.app.ui.components.CategoryChip
import com.ecomer.vku.app.ui.components.EmptyState
import com.ecomer.vku.app.ui.components.ErrorBanner
import com.ecomer.vku.app.ui.components.ProductImage
import com.ecomer.vku.app.ui.components.SearchField
import com.ecomer.vku.app.ui.theme.EcomerBackground
import com.ecomer.vku.app.ui.theme.EcomerBorder
import com.ecomer.vku.app.ui.theme.EcomerTextMuted
import com.ecomer.vku.app.ui.theme.EcomerTextPrimary

/**
 * Man hinh trang chu: tim kiem, loc danh muc, luoi san pham 2 cot.
 */
@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onProductClick: (Product) -> Unit,
    viewModel: ShopViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val firestoreError = FirebaseExt.lastError

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcomerBackground)
            .padding(contentPadding),
    ) {
        Text(
            text = "Sản phẩm",
            fontFamily = FontFamily.Serif,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = EcomerTextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

        // Thong bao tu ViewModel (vi du them san pham thanh cong).
        if (state.message != null) {
            ErrorBanner(
                message = state.message!!,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        // Bao loi Firestore (vi du chua tao database) de nguoi dung biet can lam gi.
        if (firestoreError != null) {
            ErrorBanner(
                message = firestoreError,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        SearchField(
            value = state.searchQuery,
            onValueChange = viewModel::search,
            placeholder = "Tìm sản phẩm",
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Chip danh muc
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            lazyItems(state.categories) { category ->
                CategoryChip(
                    text = category,
                    selected = state.selectedCategory == category,
                    onClick = { viewModel.selectCategory(category) },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.visibleProducts.isEmpty()) {
            EmptyState(message = "Không tìm thấy sản phẩm nào")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 4.dp,
                    bottom = 16.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                gridItems(state.visibleProducts, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        imageUrl = state.thumbnail(product),
                        onClick = { onProductClick(product) },
                    )
                }
            }
        }
    }
}

/**
 * The san pham trong luoi 2 cot.
 */
@Composable
fun ProductCard(
    product: Product,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        ProductImage(
            imageUrl = imageUrl,
            contentDescription = product.tenSanPham,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = product.tenSanPham,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = EcomerTextPrimary,
            maxLines = 2,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatPrice(product.gia),
            fontSize = 13.sp,
            color = EcomerTextMuted,
        )
    }
}

/**
 * Dinh dang gia tien theo kieu Viet: 200.000 đ
 *
 * Tien VND thuong lam tron den hang nghin, nen 199.998 se hien la 200.000.
 */
fun formatPrice(price: Long): String {
    val rounded = if (price % 1000 == 0L) price else (price + 500) / 1000 * 1000
    val text = rounded.toString()
    val builder = StringBuilder()
    for ((index, ch) in text.withIndex()) {
        if (index > 0 && (text.length - index) % 3 == 0) builder.append('.')
        builder.append(ch)
    }
    return "$builder đ"
}
