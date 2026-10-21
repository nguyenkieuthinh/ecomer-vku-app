package com.ecomer.vku.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
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
 */
@Composable
fun EcomerField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    heightDp: Int = 48,
    imeAction: ImeAction = ImeAction.Default,
    onNext: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
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
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = keyboardType,
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

/** Chip danh muc, trang thai active co nen xanh. */
@Composable
fun CategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            // widthIn + wrapContentWidth: trong LazyRow item duoc do voi minWidth = 0,
            // Box co height co dinh se bi bop ve 0 chieu rong (chip thanh tron).
            .wrapContentWidth()
            .widthIn(min = 44.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) EcomerPrimary else Color.White)
            .border(
                1.dp,
                if (selected) EcomerPrimary else EcomerBorder,
                RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 14.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else EcomerTextPrimary,
            fontSize = 13.sp,
        )
    }
}

/** O so luong "- 1 +" nhu trong man hinh gio hang. */
@Composable
fun QuantityStepper(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF1F4F8))
                .clickable(onClick = onDecrease),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = "Giảm số lượng",
                tint = EcomerTextPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = quantity.toString(),
            fontSize = 13.sp,
            color = EcomerTextMuted,
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF1F4F8))
                .clickable(onClick = onIncrease),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Tăng số lượng",
                tint = EcomerTextPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** O tim kiem co icon kinh. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    EcomerField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = modifier,
        trailing = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Tìm kiếm",
                tint = EcomerTextMuted,
                modifier = Modifier.size(18.dp),
            )
        },
    )
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

/**
 * Nut dang nhap bang Gmail.
 */
@Composable
fun GoogleSignInButton(
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.5.dp, EcomerBorder, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "G",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4285F4),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = EcomerTextPrimary,
        )
    }
}
