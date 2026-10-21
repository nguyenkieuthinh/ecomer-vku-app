package com.ecomer.vku.app.ui.screens.checkout

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
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
import com.ecomer.vku.app.data.model.Address
import com.ecomer.vku.app.ui.components.ErrorBanner
import com.ecomer.vku.app.ui.components.OutlineButton
import com.ecomer.vku.app.ui.components.PrimaryButton
import com.ecomer.vku.app.ui.components.ProductImage
import com.ecomer.vku.app.ui.screens.home.formatPrice
import com.ecomer.vku.app.ui.theme.EcomerBackground
import com.ecomer.vku.app.ui.theme.EcomerBorder
import com.ecomer.vku.app.ui.theme.EcomerPrimary
import com.ecomer.vku.app.ui.theme.EcomerPrimarySoft
import com.ecomer.vku.app.ui.theme.EcomerTextMuted
import com.ecomer.vku.app.ui.theme.EcomerTextPrimary

/**
 * Man hinh thanh toan.
 *
 * Bat buoc co dia chi giao hang — neu chua co thi hien empty state kem nut
 * "Thêm địa chỉ" de nguoi dung dien vao.
 */
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onAddAddress: () -> Unit,
    onPlacedOrder: () -> Unit,
    viewModel: CheckoutViewModel = viewModel(),
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
            text = "Thanh toán",
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

        // --- Dia chi giao hang ---
        Text(
            text = "Địa chỉ giao hàng",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = EcomerTextMuted,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (state.addresses.isEmpty()) {
            // Chua co dia chi -> he thong yeu cau them.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, EcomerBorder, RoundedCornerShape(10.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Bạn chưa có địa chỉ giao hàng",
                    fontSize = 14.sp,
                    color = EcomerTextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cần địa chỉ và số điện thoại để đặt hàng",
                    fontSize = 12.sp,
                    color = EcomerTextMuted,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlineButton(
                    text = "Thêm địa chỉ",
                    onClick = onAddAddress,
                    heightDp = 42,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(state.addresses, key = { it.id }) { address ->
                    AddressOption(
                        address = address,
                        selected = address.id == state.selectedAddress?.id,
                        onClick = { viewModel.selectAddress(address) },
                    )
                }
            }
            OutlineButton(
                text = "Thêm địa chỉ khác",
                onClick = onAddAddress,
                heightDp = 42,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // --- San pham trong don ---
        if (state.items.isNotEmpty()) {
            Text(
                text = "Sản phẩm (${state.items.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcomerTextMuted,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(state.items, key = { it.productId }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(1.dp, EcomerBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProductImage(
                            imageUrl = item.imageUrl,
                            contentDescription = item.name,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                fontSize = 13.sp,
                                color = EcomerTextPrimary,
                                maxLines = 1,
                            )
                            Text(
                                text = "x${item.quantity}",
                                fontSize = 12.sp,
                                color = EcomerTextMuted,
                            )
                        }
                        Text(
                            text = formatPrice(item.subtotal),
                            fontSize = 13.sp,
                            color = EcomerTextMuted,
                        )
                    }
                }
            }
        }

        // --- Tong cong + xac nhan ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Tổng cộng",
                fontSize = 16.sp,
                color = EcomerTextPrimary,
            )
            Text(
                text = formatPrice(state.total),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = EcomerTextPrimary,
            )
        }

        PrimaryButton(
            text = if (state.loading) "ĐANG ĐẶT HÀNG..." else "XÁC NHẬN ĐẶT HÀNG",
            onClick = { viewModel.placeOrder(onPlacedOrder) },
            enabled = !state.loading && state.hasAddress && state.items.isNotEmpty(),
        )

        if (!state.hasAddress) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Cần có địa chỉ giao hàng để thanh toán",
                fontSize = 12.sp,
                color = EcomerTextMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Lua chon dia chi trong man thanh toan.
 */
@Composable
private fun AddressOption(
    address: Address,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) EcomerPrimary else EcomerBorder,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) EcomerPrimary else Color.Transparent)
                .border(
                    1.dp,
                    if (selected) EcomerPrimary else EcomerBorder,
                    RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = address.fullName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcomerTextPrimary,
            )
            Text(
                text = address.phone,
                fontSize = 12.sp,
                color = EcomerTextMuted,
            )
            Text(
                text = address.fullAddress,
                fontSize = 12.sp,
                color = EcomerTextMuted,
            )
        }
        if (address.isDefault) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(EcomerPrimarySoft)
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "Mặc định",
                    fontSize = 10.sp,
                    color = EcomerPrimary,
                )
            }
        }
    }
}
