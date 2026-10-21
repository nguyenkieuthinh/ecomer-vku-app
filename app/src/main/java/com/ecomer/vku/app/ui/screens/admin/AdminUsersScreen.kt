package com.ecomer.vku.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecomer.vku.app.data.model.User
import com.ecomer.vku.app.ui.components.ErrorBanner
import com.ecomer.vku.app.ui.components.EmptyState
import com.ecomer.vku.app.ui.components.UserAvatar
import com.ecomer.vku.app.ui.theme.EcomerBackground
import com.ecomer.vku.app.ui.theme.EcomerBorder
import com.ecomer.vku.app.ui.theme.EcomerPrimary
import com.ecomer.vku.app.ui.theme.EcomerPrimarySoft
import com.ecomer.vku.app.ui.theme.EcomerTextMuted
import com.ecomer.vku.app.ui.theme.EcomerTextPrimary

/**
 * Man hinh danh sach nguoi dung cua admin.
 *
 * Cho admin biet ai da dang ky, dang nhap lan cuoi, va da mua bao nhieu don.
 */
@Composable
fun AdminUsersScreen(
    onBack: () -> Unit,
    viewModel: AdminDashboardViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Dem so don cua tung nguoi dung de hien thi cột "Số đơn".
    val orderCountByUser = state.orders.groupingBy { it.userId }.eachCount()

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

        // Thong bao loi / thanh cong tu ViewModel — tranh bi "nuot" im lang.
        if (state.message != null) {
            ErrorBanner(
                message = state.message!!,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Text(
            text = "Người dùng",
            fontFamily = FontFamily.Serif,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = EcomerTextPrimary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.users.isEmpty()) {
            EmptyState(message = "Chưa có người dùng nào đăng ký")
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.users, key = { it.uid }) { user ->
                AdminUserRow(
                    user = user,
                    orderCount = orderCountByUser[user.uid] ?: 0,
                )
            }
        }
    }
}

/**
 * Mot nguoi dung trong danh sach.
 */
@Composable
private fun AdminUserRow(
    user: User,
    orderCount: Int,
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
        UserAvatar(
            photoUrl = user.photoUrl,
            name = user.displayName.ifBlank { user.email },
            modifier = Modifier.size(44.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.email,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcomerTextPrimary,
                maxLines = 1,
            )
            Text(
                text = "Đăng nhập: ${formatDateTime(user.lastLoginAt)}",
                fontSize = 12.sp,
                color = EcomerTextMuted,
            )
            Text(
                text = "Số đơn: $orderCount",
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
