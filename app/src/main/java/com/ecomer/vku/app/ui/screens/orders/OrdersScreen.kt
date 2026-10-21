package com.ecomer.vku.app.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.ecomer.vku.app.data.model.Order
import com.ecomer.vku.app.ui.components.ErrorBanner
import com.ecomer.vku.app.ui.components.EmptyState
import com.ecomer.vku.app.ui.components.ProductImage
import com.ecomer.vku.app.ui.screens.home.ShopViewModel
import com.ecomer.vku.app.ui.screens.home.formatPrice
import com.ecomer.vku.app.ui.theme.EcomerBackground
import com.ecomer.vku.app.ui.theme.EcomerBorder
import com.ecomer.vku.app.ui.theme.EcomerTextMuted
import com.ecomer.vku.app.ui.theme.EcomerTextPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Man hinh "Don hang cua toi": lich su cac don da dat.
 */
@Composable
fun OrdersScreen(
    contentPadding: PaddingValues,
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
            text = "Đơn hàng",
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

        if (state.orders.isEmpty()) {
            EmptyState(message = "Bạn chưa có đơn hàng nào")
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.orders, key = { it.id }) { order ->
                OrderCard(order = order)
            }
        }
    }
}

/**
 * The hien thi mot don hang.
 */
@Composable
fun OrderCard(order: Order, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, EcomerBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Đơn #${order.id.take(6).uppercase()}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcomerTextPrimary,
            )
            StatusBadge(order.status)
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatDate(order.createdAt),
            fontSize = 12.sp,
            color = EcomerTextMuted,
        )

        Spacer(modifier = Modifier.height(10.dp))

        order.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProductImage(
                    imageUrl = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontSize = 13.sp,
                        color = EcomerTextPrimary,
                        maxLines = 1,
                    )
                    Text(
                        text = "x${item.quantity}",
                        fontSize = 12.sp,
                        color = EcomerTextMuted,
                    )
                }
                Text(
                    text = formatPrice(item.subtotal),
                    fontSize = 13.sp,
                    color = EcomerTextMuted,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Tổng cộng",
                fontSize = 14.sp,
                color = EcomerTextPrimary,
            )
            Text(
                text = formatPrice(order.total),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = EcomerTextPrimary,
            )
        }
    }
}

/**
 * The trang thai don hang.
 */
@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (status) {
        Order.STATUS_DONE -> Triple("Hoàn thành", Color(0xFFE3F4E5), Color(0xFF1E6B33))
        Order.STATUS_SHIPPING -> Triple("Đang giao", Color(0xFFDCEBFB), Color(0xFF144E92))
        Order.STATUS_CANCELLED -> Triple("Đã hủy", Color(0xFFFDECEA), Color(0xFFB3262D))
        else -> Triple("Chờ xử lý", Color(0xFFFFF4E0), Color(0xFF8A6A00))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Dinh dang ngay dat hang. */
private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return format.format(Date(timestamp))
}
