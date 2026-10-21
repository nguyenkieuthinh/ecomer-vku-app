package com.ecomer.vku.app.ui.screens.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecomer.vku.app.data.ImageEncoder
import com.ecomer.vku.app.data.model.Product
import com.ecomer.vku.app.ui.components.EcomerField
import com.ecomer.vku.app.ui.components.ErrorBanner
import com.ecomer.vku.app.ui.components.OutlineButton
import com.ecomer.vku.app.ui.components.PrimaryButton
import com.ecomer.vku.app.ui.components.ProductImage
import com.ecomer.vku.app.ui.theme.EcomerBackground
import com.ecomer.vku.app.ui.theme.EcomerBorder
import com.ecomer.vku.app.ui.theme.EcomerBorderFocus
import com.ecomer.vku.app.ui.theme.EcomerDanger
import com.ecomer.vku.app.ui.theme.EcomerPrimary
import com.ecomer.vku.app.ui.theme.EcomerTextMuted
import com.ecomer.vku.app.ui.theme.EcomerTextPrimary
import com.ecomer.vku.app.ui.theme.EcomerWarning

/**
 * Man hinh quan ly san pham (chi admin).
 *
 * Form them / sua san pham, chon nhieu anh tu thu vien (anh duoc resize ve
 * 1080 px roi nen xuong duoi 700 KB), chon anh dai dien, va danh sach san pham
 * voi nut Sua / Xoa.
 */
@Composable
fun AdminProductsScreen(
    onBack: () -> Unit,
    onViewProduct: (Product) -> Unit = {},
    viewModel: AdminViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Enter chuyen o giua cac truong, giup nhap lien tuc kh dung chuot.
    val nameFocus = remember { FocusRequester() }
    val categoryFocus = remember { FocusRequester() }
    val priceFocus = remember { FocusRequester() }
    val stockFocus = remember { FocusRequester() }
    val descriptionFocus = remember { FocusRequester() }

    // Chon nhieu anh mot luc tu thu vien (khong can quyen doc bo nho).
    //
    // Luu y: `PickMultipleVisualMedia` tra ve LIST RONG neu nguoi dung chon
    // nhieu hon `maxItems`. Nen dat maxItems rat lon va tu cat lai phia duoi
    // de nguoi dung chon bao nhieu cung duoc.
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK)
    ) { uris ->
        viewModel.addImages(context, uris.take(MAX_IMAGES))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcomerBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "← Quay lại",
                fontSize = 14.sp,
                color = EcomerTextMuted,
                modifier = Modifier.clickable(onClick = onBack),
            )
        }

        Text(
            text = if (state.editing != null) "Sửa sản phẩm" else "Dữ liệu sản phẩm",
            fontFamily = FontFamily.Serif,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = EcomerTextPrimary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.message != null) {
            ErrorBanner(
                message = state.message!!,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        // Form them / sua
        EcomerField(
            value = state.name,
            onValueChange = viewModel::setName,
            placeholder = "Tên sản phẩm",
            heightDp = 42,
            modifier = Modifier.focusRequester(nameFocus),
            imeAction = ImeAction.Next,
            onNext = { categoryFocus.requestFocus() },
        )
        Spacer(modifier = Modifier.height(8.dp))
        EcomerField(
            value = state.category,
            onValueChange = viewModel::setCategory,
            placeholder = "Loại sản phẩm",
            heightDp = 42,
            modifier = Modifier.focusRequester(categoryFocus),
            imeAction = ImeAction.Next,
            onNext = { priceFocus.requestFocus() },
        )
        Spacer(modifier = Modifier.height(8.dp))
        EcomerField(
            value = state.price,
            onValueChange = viewModel::setPrice,
            placeholder = "Giá sản phẩm",
            heightDp = 42,
            keyboardType = KeyboardType.Number,
            modifier = Modifier.focusRequester(priceFocus),
            imeAction = ImeAction.Next,
            onNext = { stockFocus.requestFocus() },
        )
        Spacer(modifier = Modifier.height(8.dp))
        EcomerField(
            value = state.stock,
            onValueChange = viewModel::setStock,
            placeholder = "Số lượng tồn kho",
            heightDp = 42,
            keyboardType = KeyboardType.Number,
            modifier = Modifier.focusRequester(stockFocus),
            imeAction = ImeAction.Next,
            onNext = { descriptionFocus.requestFocus() },
        )
        Spacer(modifier = Modifier.height(8.dp))
        EcomerField(
            value = state.description,
            onValueChange = viewModel::setDescription,
            placeholder = "Mô tả sản phẩm",
            heightDp = 42,
            modifier = Modifier.focusRequester(descriptionFocus),
            imeAction = ImeAction.Done,
            onNext = { viewModel.save() },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tieu de danh sach anh + tong dung luong
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Hình ảnh (${state.imageCount} ảnh)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcomerTextPrimary,
            )
            if (state.imageCount > 0) {
                Text(
                    text = "Tổng ${formatSize(state.totalSize)}",
                    fontSize = 12.sp,
                    color = EcomerTextMuted,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Danh sach anh da chon
        if (state.images.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(state.images) { index, image ->
                    // Anh dau tien trong danh sach la anh dai dien.
                    val isCover = index == 0
                    AdminImageThumb(
                        image = image,
                        isCover = isCover,
                        onClick = { viewModel.setCover(index) },
                        onRemove = { viewModel.removeImage(index) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ảnh được nén về cạnh dài tối đa ${ImageEncoder.MAX_DIMENSION} px, " +
                    "mỗi ảnh dưới ${ImageEncoder.TARGET_BYTES / 1024} KB. " +
                    "Bấm vào ảnh để chọn làm ảnh đại diện.",
            fontSize = 12.sp,
            color = EcomerTextMuted,
            lineHeight = 18.sp,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Nut chon anh
        OutlineButton(
            text = "Chọn từ thư viện ảnh",
            onClick = {
                imagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier.padding(bottom = 8.dp),
            heightDp = 44,
        )

        Spacer(modifier = Modifier.height(12.dp))

        PrimaryButton(
            text = when {
                state.loading -> "ĐANG LƯU..."
                state.editing != null -> "CẬP NHẬT"
                else -> "THÊM SẢN PHẨM"
            },
            onClick = { viewModel.save() },
            enabled = !state.loading,
            heightDp = 44,
        )

        if (state.editing != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hủy sửa",
                fontSize = 13.sp,
                color = EcomerTextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.cancelEdit() }
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Danh sách sản phẩm:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = EcomerTextPrimary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(state.products, key = { it.id }) { product ->
                AdminProductRow(
                    product = product,
                    imageUrl = state.thumbnail(product),
                    onEdit = { viewModel.startEdit(product) },
                    onDelete = { viewModel.delete(product) },
                    onView = { onViewProduct(product) },
                )
            }
        }
    }
}

/**
 * Anh nho trong form, co badge "Đại diện" va nut xoa.
 */
@Composable
private fun AdminImageThumb(
    image: AdminImage,
    isCover: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(96.dp)) {
        ProductImage(
            imageUrl = image.data,
            contentDescription = image.fileName,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onClick),
        )

        // Badge "Đại diện"
        if (isCover) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(4.dp))
                    .background(EcomerPrimary)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
                Text(
                    text = "Đại diện",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Nut xoa
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Xóa ảnh",
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }

        // Ten file + dung luong
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = image.fileName.ifBlank { "ảnh" },
                color = Color.White,
                fontSize = 9.sp,
                maxLines = 1,
            )
            Text(
                text = formatSize(image.sizeBytes),
                color = Color.White,
                fontSize = 9.sp,
            )
        }
    }
}

/**
 * Mot san pham trong danh sach quan ly, kem nut Sua / Xoa.
 *
 * Bam vao san pham se mo man hinh chi tiet — khong can nut rieng.
 */
@Composable
fun AdminProductRow(
    product: Product,
    imageUrl: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onView: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.5.dp, EcomerBorderFocus, RoundedCornerShape(10.dp))
            .clickable(onClick = onView)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductImage(
            imageUrl = imageUrl,
            contentDescription = product.name,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Tên sp: ${product.name}",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = EcomerTextPrimary,
            )
            Text(
                text = "Giá sp: ${product.price}",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = EcomerTextPrimary,
            )
            Text(
                text = "Loại sp: ${product.category}",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = EcomerTextPrimary,
            )
            Text(
                text = "Còn: ${product.availableStock}/${product.totalStock} · Đã bán: ${product.soldCount}",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = EcomerTextMuted,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallActionButton(
                text = "Sửa",
                textColor = EcomerWarning,
                borderColor = EcomerWarning,
                onClick = onEdit,
            )
            SmallActionButton(
                text = "Xóa",
                textColor = EcomerDanger,
                borderColor = EcomerDanger,
                onClick = onDelete,
            )
        }
    }
}

/**
 * Nut nho cua man hinh quan ly san pham.
 */
@Composable
private fun SmallActionButton(
    text: String,
    textColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(52.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .border(1.5.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = textColor,
        )
    }
}

/** Dinh dung luong: 612 KB, 1,3 MB. */
private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}

/**
 * So anh toi da cho moi san pham (gioi han lưu trong Firestore).
 */
private const val MAX_IMAGES = 10

/**
 * So anh ma bo chon cho phep — phai LON hon [MAX_IMAGES], vi
 * `PickMultipleVisualMedia` tra ve list rong khi nguoi dung chon vuot qua
 * gioi han nay.
 */
private const val MAX_PICK = 50
