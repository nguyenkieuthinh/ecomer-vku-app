package com.ecomer.vku.app.ui.screens.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
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
import com.ecomer.vku.app.data.model.CartItem
import com.ecomer.vku.app.ui.components.ErrorBanner
import com.ecomer.vku.app.ui.components.EmptyState
import com.ecomer.vku.app.ui.components.PrimaryButton
import com.ecomer.vku.app.ui.components.ProductImage
import com.ecomer.vku.app.ui.components.QuantityStepper
import com.ecomer.vku.app.ui.screens.home.ShopViewModel
import com.ecomer.vku.app.ui.screens.home.formatPrice
import com.ecomer.vku.app.ui.theme.EcomerBackground
import com.ecomer.vku.app.ui.theme.EcomerBorder
import com.ecomer.vku.app.ui.theme.EcomerTextMuted
import com.ecomer.vku.app.ui.theme.EcomerTextPrimary

/**
 * Man hinh gio hang: danh sach mon, tang/giam so luong, tong cong va nut dat hang.
 */
@Composable
fun CartScreen(
    contentPadding: PaddingValues,
    onPlacedOrder: () -> Unit,
    viewModel: ShopViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcomerBackground)
            .padding(contentPadding),
    ) {
        Text(
            text = "Giỏ hàng",
            fontFamily = FontFamily.Serif,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = EcomerTextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

        // Thong bao loi / thanh cong tu ViewModel — tranh bi "nuot" im lang.
        if (state.message != null) {
            ErrorBanner(
                message = state.message!!,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (state.cart.isEmpty()) {
            EmptyState(message = "Giỏ hàng đang trống")
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(state.cart, key = { it.productId }) { item ->
                CartRow(
                    item = item,
                    onIncrease = { viewModel.increaseQuantity(item.productId) },
                    onDecrease = { viewModel.decreaseQuantity(item.productId) },
                    onRemove = { viewModel.removeFromCart(item.productId) },
                )
            }
        }

        // Tong cong
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Tổng cộng",
                fontSize = 16.sp,
                color = EcomerTextPrimary,
            )
            Text(
                text = formatPrice(state.cartTotal),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = EcomerTextPrimary,
            )
        }

        PrimaryButton(
            text = "ĐẶT HÀNG",
            onClick = { viewModel.placeOrder(onPlacedOrder) },
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Mot mon trong gio hang.
 */
@Composable
fun CartRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, EcomerBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductImage(
            imageUrl = item.imageUrl,
            contentDescription = item.name,
            modifier = Modifier.size(64.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcomerTextPrimary,
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatPrice(item.price),
                fontSize = 13.sp,
                color = EcomerTextMuted,
            )
            Spacer(modifier = Modifier.height(6.dp))
            QuantityStepper(
                quantity = item.quantity,
                onDecrease = onDecrease,
                onIncrease = onIncrease,
            )
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Xóa sản phẩm",
                tint = EcomerTextMuted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
