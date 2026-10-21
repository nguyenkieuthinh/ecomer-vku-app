package com.ecomer.vku.app.ui.screens.address

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomer.vku.app.data.model.Address
import com.ecomer.vku.app.ui.components.EcomerField
import com.ecomer.vku.app.ui.components.PrimaryButton
import com.ecomer.vku.app.ui.theme.EcomerBackground
import com.ecomer.vku.app.ui.theme.EcomerBorder
import com.ecomer.vku.app.ui.theme.EcomerPrimary
import com.ecomer.vku.app.ui.theme.EcomerTextMuted
import com.ecomer.vku.app.ui.theme.EcomerTextPrimary

/**
 * Form them / sua dia chi giao hang.
 */
@Composable
fun AddressForm(
    address: Address,
    loading: Boolean,
    onSave: (Address) -> Unit,
    onCancel: () -> Unit,
) {
    var fullName by remember(address.id) { mutableStateOf(address.fullName) }
    var phone by remember(address.id) { mutableStateOf(address.phone) }
    var street by remember(address.id) { mutableStateOf(address.street) }
    var ward by remember(address.id) { mutableStateOf(address.ward) }
    var district by remember(address.id) { mutableStateOf(address.district) }
    var city by remember(address.id) { mutableStateOf(address.city) }
    var isDefault by remember(address.id) { mutableStateOf(address.isDefault) }

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
            text = "← Hủy",
            fontSize = 14.sp,
            color = EcomerTextMuted,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .clickable(onClick = onCancel),
        )

        Text(
            text = if (address.id.isBlank()) "Thêm địa chỉ" else "Sửa địa chỉ",
            fontFamily = FontFamily.Serif,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = EcomerTextPrimary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(20.dp))

        EcomerField(
            value = fullName,
            onValueChange = { fullName = it },
            placeholder = "Người nhận hàng",
        )
        Spacer(modifier = Modifier.height(10.dp))
        EcomerField(
            value = phone,
            onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' } },
            placeholder = "Số điện thoại",
        )
        Spacer(modifier = Modifier.height(10.dp))
        EcomerField(
            value = street,
            onValueChange = { street = it },
            placeholder = "Số nhà, đường",
        )
        Spacer(modifier = Modifier.height(10.dp))
        EcomerField(
            value = ward,
            onValueChange = { ward = it },
            placeholder = "Phường / xã",
        )
        Spacer(modifier = Modifier.height(10.dp))
        EcomerField(
            value = district,
            onValueChange = { district = it },
            placeholder = "Quận / huyện",
        )
        Spacer(modifier = Modifier.height(10.dp))
        EcomerField(
            value = city,
            onValueChange = { city = it },
            placeholder = "Tỉnh / thành phố",
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Tuy chon dia chi mac dinh
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .clickable { isDefault = !isDefault }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isDefault) EcomerPrimary else Color.Transparent)
                    .then(
                        if (isDefault) {
                            Modifier
                        } else {
                            Modifier.background(Color.Transparent)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isDefault) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontSize = 13.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = "Đặt làm địa chỉ mặc định",
                fontSize = 14.sp,
                color = EcomerTextPrimary,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        PrimaryButton(
            text = if (loading) "ĐANG LƯU..." else "LƯU ĐỊA CHỈ",
            onClick = {
                onSave(
                    address.copy(
                        fullName = fullName,
                        phone = phone,
                        street = street,
                        ward = ward,
                        district = district,
                        city = city,
                        isDefault = isDefault,
                    )
                )
            },
            enabled = !loading,
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
