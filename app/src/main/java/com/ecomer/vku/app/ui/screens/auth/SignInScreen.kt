package com.ecomer.vku.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecomer.vku.app.ui.components.EcomerField
import com.ecomer.vku.app.ui.components.ErrorBanner
import com.ecomer.vku.app.ui.components.PrimaryButton
import com.ecomer.vku.app.ui.theme.EcomerBackground
import com.ecomer.vku.app.ui.theme.EcomerBorder
import com.ecomer.vku.app.ui.theme.EcomerPrimary
import com.ecomer.vku.app.ui.theme.EcomerTextMuted

/**
 * Man hinh dang nhap.
 *
 * Luong: nhap email + mat khau -> FirebaseAuth -> sang man hinh chinh.
 * Hoac bam "Dang nhap voi Gmail" de dung Google Identity.
 */
@Composable
fun SignInScreen(
    onSignedIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.success) {
        if (state.success) onSignedIn()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcomerBackground)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Logo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Hệ thống nhập liệu sản phẩm",
                fontSize = 13.sp,
                color = EcomerPrimary,
            )
            Text(
                text = "ECOMER",
                fontFamily = FontFamily.Serif,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF14202E),
            )
        }


        EcomerField(
            value = email,
            onValueChange = { email = it },
            placeholder = "email",
            keyboardType = KeyboardType.Email,
        )
        Spacer(modifier = Modifier.height(12.dp))
        EcomerField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Mật khẩu",
            isPassword = true,
            passwordVisible = showPassword,
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

        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Quên mật khẩu?",
                fontSize = 13.sp,
                color = EcomerTextMuted,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable { viewModel.sendPasswordReset(email) }
                    .padding(vertical = 12.dp),
            )
        }

        // Banner loi dat DUOI form: dat o tren se day toan bo form xuong,
        // khi ban phim mo thi nội dung bi tran ra ngoai man hinh.
        if (state.error != null) {
            ErrorBanner(
                message = state.error!!,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        PrimaryButton(
            text = if (state.loading) "ĐANG XỬ LÝ..." else "ĐĂNG NHẬP",
            onClick = { viewModel.signIn(email, password) },
            enabled = !state.loading,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Chưa có tài khoản? Đăng ký",
            fontSize = 13.sp,
            color = EcomerTextMuted,
            modifier = Modifier.clickable(onClick = onNavigateToSignUp),
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}
