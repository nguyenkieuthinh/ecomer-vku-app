package com.ecomer.vku.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomer.vku.app.ui.theme.EcomerBorder
import com.ecomer.vku.app.ui.theme.EcomerBorderFocus
import com.ecomer.vku.app.ui.theme.EcomerPrimary
import com.ecomer.vku.app.ui.theme.EcomerTextMuted
import com.ecomer.vku.app.ui.theme.EcomerTextPrimary

/**
 * O nhap lieu co vien vien, dung chung cho man hinh dang nhap, them san pham, tim kiem.
 *
 * @param imeAction phim Enter se gan nhu nao: Next de chuyen o, Done de ket thuc.
 * @param onNext goi khi bam Enter (voi [imeAction] la Next hoac Done).
 * @param isPassword true = day la O MAT KHAU. O nay luon duoc bao cho ban phim
 *   biet la mat khau (xem [passwordVisible]).
 * @param passwordVisible true = hien mat khau (nut con mat). Chi quyet dinh
 *   MAN HINH, khong quyet dinh ban phim — xem ghi chu ben duoi.
 */
@Composable
fun EcomerField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    heightDp: Int = 48,
    imeAction: ImeAction = ImeAction.Default,
    onNext: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    // Chi khi o mat khau CHE mat moi che chuoi.
    val masked = isPassword && !passwordVisible

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.5.dp, EcomerBorderFocus, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = EcomerTextMuted,
                    fontSize = 15.sp,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = LocalTextStyle.current.copy(
                    color = EcomerTextPrimary,
                    fontSize = 15.sp,
                ),
                cursorBrush = SolidColor(EcomerPrimary),
                visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    // O MAT KHAU LUON dung KeyboardType.Password va tat
                    // autoCorrect — nguoi goi khong can truyen.
                    //
                    // Ly do: chi dung PasswordVisualTransformation thi CHI CHE
                    // HIEN THI tren man hinh. Ban phim van nhan duoc chuoi mat
                    // khau THAT -> no hoc, tu dong hoc va luu vao dai "de xuat"
                    // cua may (dong hoai, dung chung).
                    //
                    // KeyboardType.Password bien doi thanh
                    // EditorInfo.inputType = TYPE_TEXT_VARIATION_PASSWORD
                    // (129) -> IME biet day la o mat khau va KHONG hoc,
                    // KHONG hien thi de xuat. autoCorrect = false bo them
                    // TYPE_TEXT_FLAG_NO_SUGGESTIONS (0x8000).
                    //
                    // QUAN TRONG: `passwordVisible` chi doi hien thi, KHONG doi
                    // keyboardType. Neu hien mat khau ma doi luon keyboardType
                    // thi IME se lai hoc mat khau — mo lai lo hong.
                    keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                    autoCorrectEnabled = !isPassword,
                    imeAction = imeAction,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { onNext?.invoke() },
                    onNext = { onNext?.invoke() },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    // O mat khau co password="true" nen trinh doc man hinh
                    // KHONG doc duoc placeholder. Gan contentDescription de
                    // screen reader va cong cu tu dong hoat dong doc duoc nhan.
                    .then(
                        if (isPassword) {
                            Modifier.semantics {
                                contentDescription = placeholder
                                // Bao cho he thong autofill biet day la o
                                // mat khau → dung dung duong dan de luu/nap
                                // mat khau, khong nham sang truong khac.
                                //
                                // `password()` la API public cua Compose
                                // (SemanticsPropertiesKt). `ContentType` thi
                                // internal trong Compose 1.7 nen khong dung
                                // duoc — nhung `password()` la thu Compose tu
                                // dung internally de danh dau o mat khau cho
                                // autofill.
                                password()
                            }
                        } else {
                            Modifier
                        }
                    ),
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

/** Nut chinh (nen xanh, chu trang) dung cho "ĐĂNG NHẬP", "THÊM VÀO GIỎ"... */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    heightDp: Int = 48,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) EcomerPrimary else EcomerBorder)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Nut phu (nen trang, vien xam) dung cho "Quay lại", "Đăng xuất"... */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = EcomerTextPrimary,
    borderColor: Color = EcomerBorder,
    heightDp: Int = 44,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) Color.White else EcomerBorder.copy(alpha = 0.4f))
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
            fontSize = 14.sp,
        )
    }
}

/** The "admin" xanh nho tren ho so. */
@Composable
fun RoleBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFDCEBFB))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(
            text = text,
            color = Color(0xFF144E92),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
