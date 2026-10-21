package com.ecomer.vku.app.ui.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecomer.vku.app.ui.components.EcomerField
import com.ecomer.vku.app.ui.components.ErrorBanner
import com.ecomer.vku.app.ui.components.PrimaryButton
import com.ecomer.vku.app.ui.theme.EcomerBackground
import com.ecomer.vku.app.ui.theme.EcomerTextMuted
import com.ecomer.vku.app.ui.theme.EcomerTextPrimary

/**
 * Man hinh doi mat khau.
 */
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcomerBackground)
            .verticalScroll(rememberScrollState())
            .imePadding()
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
            text = "Đổi mật khẩu",
            fontFamily = FontFamily.Serif,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = EcomerTextPrimary,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        if (state.message != null) {
            ErrorBanner(
                message = state.message!!,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        EcomerField(
            value = currentPassword,
            onValueChange = { currentPassword = it },
            placeholder = "Mật khẩu hiện tại",
            isPassword = !showPassword,
        )
        Spacer(modifier = Modifier.height(12.dp))
        EcomerField(
            value = newPassword,
            onValueChange = { newPassword = it },
            placeholder = "Mật khẩu mới",
            isPassword = !showPassword,
            trailing = {
                Icon(
                    imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (showPassword) "Ẩn mật khẩu" else "Hiện mật khẩu",
                    tint = EcomerTextMuted,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { showPassword = !showPassword },
                )
            },
        )
        Spacer(modifier = Modifier.height(12.dp))
        EcomerField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder = "Xác nhận mật khẩu mới",
            isPassword = !showPassword,
        )

        Spacer(modifier = Modifier.height(20.dp))

        PrimaryButton(
            text = if (state.loading) "ĐANG XỬ LÝ..." else "ĐỔI MẬT KHẨU",
            onClick = {
                if (newPassword != confirmPassword) {
                    viewModel.clearMessage()
                } else {
                    viewModel.changePassword(currentPassword, newPassword)
                }
            },
            enabled = !state.loading,
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
