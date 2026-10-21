package com.ecomer.vku.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomer.vku.app.ui.theme.EcomerBorder
import com.ecomer.vku.app.ui.theme.EcomerPrimary
import com.ecomer.vku.app.ui.theme.EcomerTextMuted

/**
 * Thanh dieu huong duoi cung man hinh.
 *
 * Khach thay 4 tab nhu ban thiet ke. ADMIN chi thay 2 tab (Shop, Tai khoan) —
 * admin khong mua hang, va xem don hang qua trang "Quan tri" trong Tai khoan.
 *
 * @param isAdmin nguoi dung hien tai co quyen admin khong.
 */
@Composable
fun BottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    isAdmin: Boolean = false,
) {
    val items = if (isAdmin) {
        listOf(
            NavItem("Shop", "home", Icons.Filled.Home),
            NavItem("Tài khoản", "account", Icons.Filled.AccountCircle),
        )
    } else {
        listOf(
            NavItem("Shop", "home", Icons.Filled.Home),
            NavItem("Giỏ hàng", "cart", Icons.Filled.ShoppingCart),
            NavItem("Đơn hàng", "orders", Icons.Filled.Receipt),
            NavItem("Tài khoản", "account", Icons.Filled.AccountCircle),
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color.White)
            // Padding navigation bar dat TRUOC height de noi dung 64dp khong bi
            // cat, con nen trang van phu duoi thanh dieu huong he thong.
            .navigationBarsPadding()
            .height(64.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigate(item.route) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (selected) EcomerPrimary else EcomerTextMuted,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = item.label,
                    fontSize = 12.sp,
                    color = if (selected) EcomerPrimary else EcomerTextMuted,
                    fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal,
                )
            }
        }
    }
}

private data class NavItem(
    val label: String,
    val route: String,
    val icon: ImageVector,
)
