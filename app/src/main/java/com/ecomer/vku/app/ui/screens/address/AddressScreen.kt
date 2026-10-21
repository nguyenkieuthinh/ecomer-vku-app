package com.ecomer.vku.app.ui.screens.address

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.ecomer.vku.app.ui.components.EmptyState
import com.ecomer.vku.app.ui.components.ErrorBanner
import com.ecomer.vku.app.ui.components.OutlineButton
import com.ecomer.vku.app.ui.components.PrimaryButton
import com.ecomer.vku.app.ui.theme.EcomerBackground
import com.ecomer.vku.app.ui.theme.EcomerBorder
import com.ecomer.vku.app.ui.theme.EcomerDanger
import com.ecomer.vku.app.ui.theme.EcomerPrimary
import com.ecomer.vku.app.ui.theme.EcomerPrimarySoft
import com.ecomer.vku.app.ui.theme.EcomerTextMuted
import com.ecomer.vku.app.ui.theme.EcomerTextPrimary

/**
 * Man hinh dia chi giao hang cua nguoi dung.
 *
 * @param onBack quay lai.
 * @param onCheckoutRequired true khi nguoi dung den day tu man thanh toan —
 *   luu dia chi xong phai quay lai man thanh toan de tiep tuc dat hang.
 */
@Composable
fun AddressScreen(
    onBack: () -> Unit,
    onCheckoutRequired: () -> Unit = {},
    viewModel: AddressViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Man nhap sua dia chi.
    if (state.editing != null) {
        AddressForm(
            address = state.editing!!,
            loading = state.loading,
            onSave = { viewModel.save(it) },
            onCancel = { viewModel.cancelEdit() },
        )
        return
    }

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
            text = "Địa chỉ giao hàng",
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

        if (state.addresses.isEmpty()) {
            EmptyState(
                message = "Chưa có địa chỉ giao hàng.\nThêm địa chỉ để đặt hàng.",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(state.addresses, key = { it.id }) { address ->
                    AddressCard(
                        address = address,
                        onEdit = { viewModel.startEdit(address) },
                        onDelete = { viewModel.delete(address) },
                        onSetDefault = { viewModel.setDefault(address) },
                    )
                }
            }
        }

        PrimaryButton(
            text = "Thêm địa chỉ",
            onClick = {
                if (state.requiredForCheckout) {
                    viewModel.startAdd(requiredForCheckout = true)
                } else {
                    viewModel.startAdd()
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * The dia chi trong danh sach.
 */
@Composable
private fun AddressCard(
    address: Address,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, EcomerBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = address.fullName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcomerTextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (address.isDefault) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(EcomerPrimarySoft)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "Mặc định",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcomerPrimary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = address.phone,
            fontSize = 13.sp,
            color = EcomerTextMuted,
        )
        Text(
            text = address.fullAddress,
            fontSize = 13.sp,
            color = EcomerTextMuted,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AddressAction(
                icon = Icons.Filled.Edit,
                label = "Sửa",
                tint = EcomerTextMuted,
                onClick = onEdit,
            )
            AddressAction(
                icon = Icons.Filled.Delete,
                label = "Xóa",
                tint = EcomerDanger,
                onClick = onDelete,
            )
            if (!address.isDefault) {
                AddressAction(
                    icon = Icons.Filled.Check,
                    label = "Đặt mặc định",
                    tint = EcomerPrimary,
                    onClick = onSetDefault,
                )
            }
        }
    }
}

/**
 * Nut nho co icon trong the dia chi.
 */
@Composable
private fun AddressAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = tint,
        )
    }
}
