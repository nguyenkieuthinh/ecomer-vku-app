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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecomer.vku.app.ui.components.ErrorBanner
import com.ecomer.vku.app.ui.components.UserAvatar
import com.ecomer.vku.app.ui.screens.home.formatPrice
import com.ecomer.vku.app.ui.theme.EcomerBackground
import com.ecomer.vku.app.ui.theme.EcomerBorder
import com.ecomer.vku.app.ui.theme.EcomerPrimary
import com.ecomer.vku.app.ui.theme.EcomerPrimarySoft
import com.ecomer.vku.app.ui.theme.EcomerTextMuted
import com.ecomer.vku.app.ui.theme.EcomerTextPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Man hinh tong quan cua admin.
 *
 * Hien so lieu kinh doanh + cac muc quan tri: don hang, nguoi dung, san pham.
 */
@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
    onManageProducts: () -> Unit,
    onManageOrders: () -> Unit,
    onManageUsers: () -> Unit,
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
            text = "Quản trị cửa hàng",
            fontFamily = FontFamily.Serif,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = EcomerTextPrimary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.message != null) {
            ErrorBanner(
                message = state.message!!,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // So lieu tong quan
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        label = "Sản phẩm",
                        value = state.products.size.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Đơn hàng",
                        value = state.orders.size.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        label = "Doanh thu",
                        value = formatPrice(state.revenue),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Người dùng",
                        value = state.users.size.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Cac muc quan tri
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Quản lý",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcomerTextMuted,
                )
            }
            item {
                AdminMenuRow(
                    icon = Icons.Filled.Inventory2,
                    title = "Quản lý sản phẩm",
                    subtitle = "${state.products.size} sản phẩm · ${state.products.size} sản phẩm",
                    onClick = onManageProducts,
                )
            }
            item {
                AdminMenuRow(
                    icon = Icons.Filled.Receipt,
                    title = "Đơn hàng",
                    subtitle = if (state.pendingCount > 0) {
                        "${state.pendingCount} đơn chờ xử lý"
                    } else {
                        "Không có đơn chờ xử lý"
                    },
                    onClick = onManageOrders,
                )
            }
            item {
                AdminMenuRow(
                    icon = Icons.Filled.Groups,
                    title = "Người dùng",
                    subtitle = "${state.users.size} tài khoản đã đăng ký",
                    onClick = onManageUsers,
                )
            }


            // Nguoi dang nhap gan nhat
            if (state.users.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Đăng nhập gần đây",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcomerTextMuted,
                    )
                }
                items(state.users.take(5), key = { it.uid }) { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(1.dp, EcomerBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        UserAvatar(
                            photoUrl = user.photoUrl,
                            name = user.displayName.ifBlank { user.email },
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.email,
                                fontSize = 13.sp,
                                color = EcomerTextPrimary,
                                maxLines = 1,
                            )
                            Text(
                                text = formatDateTime(user.lastLoginAt),
                                fontSize = 12.sp,
                                color = EcomerTextMuted,
                            )
                        }
                        if (user.isAdmin) {
                            Text(
                                text = "admin",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EcomerPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(EcomerPrimarySoft)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The so lieu nho.
 */
@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, EcomerBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = EcomerTextMuted,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = EcomerTextPrimary,
            maxLines = 1,
        )
    }
}

/**
 * Dong menu trong trang quan tri.
 */
@Composable
fun AdminMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, EcomerBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = EcomerPrimary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcomerTextPrimary,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = EcomerTextMuted,
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = EcomerTextMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Dinh ngay gio. */
internal fun formatDateTime(timestamp: Long): String {
    if (timestamp == 0L) return "Chưa đăng nhập"
    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return format.format(Date(timestamp))
}
