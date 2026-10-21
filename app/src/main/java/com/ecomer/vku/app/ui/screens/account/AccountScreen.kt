package com.ecomer.vku.app.ui.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.ecomer.vku.app.ui.components.ErrorBanner
import com.ecomer.vku.app.ui.components.OutlineButton
import com.ecomer.vku.app.ui.components.RoleBadge
import com.ecomer.vku.app.ui.components.UserAvatar
import com.ecomer.vku.app.ui.theme.EcomerBackground
import com.ecomer.vku.app.ui.theme.EcomerDanger
import com.ecomer.vku.app.ui.theme.EcomerTextMuted
import com.ecomer.vku.app.ui.theme.EcomerTextPrimary

/**
 * Man hinh tai khoan: ho so, menu (chi admin moi thay "Quan ly san pham"), dang xuat.
 */
@Composable
fun AccountScreen(
    contentPadding: PaddingValues,
    onManageProducts: () -> Unit,
    onMyOrders: () -> Unit,
    onChangePassword: () -> Unit,
    onAddresses: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: AccountViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val user = state.user

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcomerBackground)
            .padding(contentPadding)
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "Tài khoản",
            fontFamily = FontFamily.Serif,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = EcomerTextPrimary,
            modifier = Modifier.padding(vertical = 16.dp),
        )

        // Thong bao loi / thanh cong tu ViewModel — tranh bi "nuot" im lang.
        if (state.message != null) {
            ErrorBanner(
                message = state.message!!,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        // Ho so
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(
                photoUrl = user?.photoUrl.orEmpty(),
                // displayName co the la chuoi RONG (khong phai null) nen phai dung ifBlank.
                name = user?.displayName?.ifBlank { user?.email.orEmpty() }.orEmpty(),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.email.orEmpty(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcomerTextPrimary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                // isAdmin da kiem tra ca role trong Firestore va danh sach email,
                // nen hien thi dung quyen ke ca khi Firestore chua co database.
                RoleBadge(text = if (user?.isAdmin == true) "admin" else "khách")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Menu
        if (user?.isAdmin == true) {
            // Admin vao trang quan tri de xem don hang, nguoi dung, san pham.
            MenuButton(text = "Quản trị cửa hàng", onClick = onManageProducts)
            Spacer(modifier = Modifier.height(8.dp))
        }
        MenuButton(text = "Đơn hàng của tôi", onClick = onMyOrders)
        Spacer(modifier = Modifier.height(8.dp))
        MenuButton(text = "Địa chỉ giao hàng", onClick = onAddresses)
        Spacer(modifier = Modifier.height(8.dp))
        MenuButton(text = "Đổi mật khẩu", onClick = onChangePassword)

        Spacer(modifier = Modifier.weight(1f))

        MenuButton(
            text = "Đăng xuất",
            onClick = { viewModel.signOut(onSignOut) },
            textColor = EcomerDanger,
            borderColor = EcomerDanger,
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Nut menu trong man hinh tai khoan.
 */
@Composable
fun MenuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = EcomerTextPrimary,
    borderColor: Color = com.ecomer.vku.app.ui.theme.EcomerBorder,
) {
    OutlineButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        textColor = textColor,
        borderColor = borderColor,
    )
}
