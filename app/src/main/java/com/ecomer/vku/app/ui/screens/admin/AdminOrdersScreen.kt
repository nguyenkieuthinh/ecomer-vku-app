package com.ecomer.vku.app.ui.screens.admin

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
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.ecomer.vku.app.ui.components.EmptyState
import com.ecomer.vku.app.ui.components.ErrorBanner
import com.ecomer.vku.app.ui.screens.orders.StatusBadge
import com.ecomer.vku.app.ui.screens.home.formatPrice
import com.ecomer.vku.app.ui.theme.EcomerBackground
import com.ecomer.vku.app.ui.theme.EcomerBorder
import com.ecomer.vku.app.ui.theme.EcomerDanger
import com.ecomer.vku.app.ui.theme.EcomerPrimary
import com.ecomer.vku.app.ui.theme.EcomerTextMuted
import com.ecomer.vku.app.ui.theme.EcomerTextPrimary

/**
 * Man hinh quan ly don hang cua admin.
 *
 * Admin xem tat ca don cua moi khach va doi trang thai de khach theo doi.
 */
@Composable
fun AdminOrdersScreen(
    onBack: () -> Unit,
    viewModel: AdminDashboardViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcomerBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "← Quay lại",
            fontSize = 14.sp,
            color = EcomerTextMuted,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .clickable(onClick = onBack),
        )

        Text(
            text = "Đơn hàng",
            fontFamily = FontFamily.Serif,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = EcomerTextPrimary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.message != null) {
            ErrorBanner(
                message = state.message!!,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        if (state.orders.isEmpty()) {
            EmptyState(message = "Chưa có đơn hàng nào")
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.orders, key = { it.id }) { order ->
                AdminOrderCard(
                    order = order,
                    onStatusChange = { status ->
                        viewModel.updateOrderStatus(order.id, status)
                    },
                )
            }
        }
    }
}

/**
 * The don hang cua admin, kem cac nut doi trang thai.
 */
@Composable
private fun AdminOrderCard(
    order: Order,
    onStatusChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            text = formatDateTime(order.createdAt),
            fontSize = 12.sp,
            color = EcomerTextMuted,
        )

        Spacer(modifier = Modifier.height(8.dp))

        order.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
            ) {
                Text(
                    text = "${item.name} x${item.quantity}",
                    fontSize = 13.sp,
                    color = EcomerTextPrimary,
                    modifier = Modifier.weight(1f),
                )
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

        Spacer(modifier = Modifier.height(10.dp))

        // Nut doi trang thai — gom ca trang thai "Huy".
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusActionButton(
                text = "Chờ xử lý",
                selected = order.status == Order.STATUS_PENDING,
                onClick = { onStatusChange(Order.STATUS_PENDING) },
            )
            StatusActionButton(
                text = "Đang giao",
                selected = order.status == Order.STATUS_SHIPPING,
                onClick = { onStatusChange(Order.STATUS_SHIPPING) },
            )
            StatusActionButton(
                text = "Hoàn thành",
                selected = order.status == Order.STATUS_DONE,
                onClick = { onStatusChange(Order.STATUS_DONE) },
            )
            StatusActionButton(
                text = "Hủy",
                selected = order.status == Order.STATUS_CANCELLED,
                tint = EcomerDanger,
                onClick = { onStatusChange(Order.STATUS_CANCELLED) },
            )
        }
    }
}

/**
 * Nut chon trang thai don hang.
 */
@Composable
private fun StatusActionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = EcomerPrimary,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) tint else Color.White)
            .border(
                1.dp,
                if (selected) tint else EcomerBorder,
                RoundedCornerShape(6.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (selected) Color.White else EcomerTextPrimary,
        )
    }
}
