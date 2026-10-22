package com.ecomer.vku.app.ui.screens.products

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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecomer.vku.app.data.model.Product
import com.ecomer.vku.app.ui.components.EcomerField
import com.ecomer.vku.app.ui.components.EmptyState
import com.ecomer.vku.app.ui.components.ErrorBanner
import com.ecomer.vku.app.ui.components.OutlineButton
import com.ecomer.vku.app.ui.components.PrimaryButton
import com.ecomer.vku.app.ui.components.ProductImage
import com.ecomer.vku.app.ui.components.RoleBadge
import com.ecomer.vku.app.ui.theme.EcomerBackground
import com.ecomer.vku.app.ui.theme.EcomerBorderFocus
import com.ecomer.vku.app.ui.theme.EcomerDanger
import com.ecomer.vku.app.ui.theme.EcomerPrimary
import com.ecomer.vku.app.ui.theme.EcomerTextMuted
import com.ecomer.vku.app.ui.theme.EcomerTextPrimary
import com.ecomer.vku.app.ui.theme.EcomerWarning

/**
 * Man hinh du lieu san pham — man hinh chinh cua app sau khi dang nhap.
 *
 * - Ai dang nhap cung THAY tat ca san pham (cua minh va cua nguoi khac).
 * - User thuong: chi SUA / XOA san pham do chinh minh nhap.
 * - Admin: sua / xoa tat ca san pham.
 *
 * Quyen duoc Firestore Rules kiem tra o server.
 */
@Composable
fun ProductsScreen(
    onSignOut: () -> Unit,
    viewModel: ProductsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Enter chuyen o giua cac truong, giup nhap lien tuc khong can chuot.
    val nameFocus = remember { FocusRequester() }
    val categoryFocus = remember { FocusRequester() }
    val priceFocus = remember { FocusRequester() }

    // Chon nhieu anh mot luc tu thu vien.
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
        // Thanh tieu de + tai khoan
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (state.editing != null) "Sửa sản phẩm" else "Dữ liệu sản phẩm",
                fontFamily = FontFamily.Serif,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = EcomerTextPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )

            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { viewModel.toggleAccountMenu() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.userEmail.ifBlank { "Tài khoản" },
                        fontSize = 11.sp,
                        color = EcomerTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(110.dp),
                        textAlign = TextAlign.End,
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Tài khoản",
                        tint = EcomerTextMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }

                DropdownMenu(
                    expanded = state.accountMenuOpen,
                    onDismissRequest = viewModel::closeAccountMenu,
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = state.userEmail,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EcomerTextPrimary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        RoleBadge(text = if (state.isAdmin) "admin" else "khách")
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Đăng xuất", color = EcomerDanger) },
                        onClick = { viewModel.signOut(onSignOut) },
                    )
                }
            }
        }

        if (state.message != null) {
            if (state.messageIsError) {
                ErrorBanner(
                    message = state.message!!,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            } else {
                SuccessBanner(
                    message = state.message!!,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
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
            imeAction = ImeAction.Done,
            onNext = { viewModel.save() },
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Hình ảnh (${state.imageCount} ảnh)",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = EcomerTextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (state.images.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(state.images) { index, image ->
                    ProductImageThumb(
                        image = image,
                        isCover = index == 0,
                        onClick = { viewModel.setCover(index) },
                        onRemove = { viewModel.removeImage(index) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlineButton(
            text = "Chọn từ thư viện ảnh",
            onClick = {
                imagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            heightDp = 44,
        )

        Spacer(modifier = Modifier.height(10.dp))

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
            Text(
                text = "Huỷ sửa",
                fontSize = 13.sp,
                color = EcomerTextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.cancelEdit() }
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Danh sách sản phẩm:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = EcomerTextPrimary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.products.isEmpty()) {
            EmptyState(message = "Chưa có sản phẩm nào")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(state.products, key = { it.id }) { product ->
                    ProductRow(
                        product = product,
                        imageUrl = state.thumbnail(product),
                        canModify = viewModel.canModify(product),
                        onEdit = { viewModel.startEdit(product) },
                        onDelete = { viewModel.requestDelete(product) },
                    )
                }
            }
        }
    }

    // Hop thoai xac nhan xoa
    val pendingDelete = state.pendingDelete
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Xoá sản phẩm?") },
            text = {
                Text(
                    "\"${pendingDelete.tenSanPham}\" sẽ bị xoá vĩnh viễn, " +
                            "kèm toàn bộ ảnh của nó."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Xoá", color = EcomerDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) {
                    Text("Huỷ")
                }
            },
        )
    }
}

/**
 * Anh nho trong form, co badge "Đại diện" va nut xoa.
 */
@Composable
private fun ProductImageThumb(
    image: ProductImageDraft,
    isCover: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(88.dp)) {
        ProductImage(
            imageUrl = image.data,
            contentDescription = image.tenFile,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onClick),
        )

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
    }
}

/**
 * Mot san pham trong danh sach: anh + 3 thong tin chinh + nut Sua / Xoa.
 */
@Composable
private fun ProductRow(
    product: Product,
    imageUrl: String,
    canModify: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.5.dp, EcomerBorderFocus, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductImage(
            imageUrl = imageUrl,
            contentDescription = product.tenSanPham,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Tên sp: ${product.tenSanPham}",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = EcomerTextPrimary,
            )
            Text(
                text = "Giá sp: ${product.gia}",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = EcomerTextPrimary,
            )
            Text(
                text = "Loại sp: ${product.loaiSanPham}",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = EcomerTextPrimary,
            )
        }
        // San pham cua nguoi khac: chi xem, khong hien nut sua / xoa.
        if (canModify) {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Sửa sản phẩm",
                    tint = EcomerWarning,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Xoá sản phẩm",
                    tint = EcomerDanger,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/** Thong bao thanh cong (nen xanh la). */
@Composable
private fun SuccessBanner(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE7F6EC))
            .border(1.dp, Color(0xFF9FD8B0), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text = message, color = Color(0xFF1E6B34), fontSize = 13.sp)
    }
}

/**
 * So anh toi da cho moi san pham.
 */
private const val MAX_IMAGES = 10

/**
 * So anh ma bo chon cho phep — phai LON hon [MAX_IMAGES], vi
 * `PickMultipleVisualMedia` tra ve list rong khi nguoi dung chon vuot qua
 * gioi han nay.
 */
private const val MAX_PICK = 50
