package com.ecomer.vku.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ecomer.vku.app.data.Base64Image
import com.ecomer.vku.app.ui.theme.EcomerBorder
import com.ecomer.vku.app.ui.theme.EcomerPlaceholder
import com.ecomer.vku.app.ui.theme.EcomerPrimary
import com.ecomer.vku.app.ui.theme.EcomerTextMuted

/**
 * Anh san pham co fallback ve o mau xam khi khong co URL hoac tai that bai.
 *
 * @param imageUrl duong dan anh: URL tu Storage hoac chuoi base64
 *   "data:image/jpeg;base64,...".
 */
@Composable
fun ProductImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholderText: String = "Ảnh",
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(EcomerPlaceholder),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNotBlank()) {
            // Coil 2 khong ho tro data URI, nen tu giai ma base64 thanh byte.
            // `remember` giu ket qua de khong giai ma lai moi lan recompose.
            val bytes = remember(imageUrl) { Base64Image.decode(imageUrl) }
            if (bytes != null) {
                AsyncImage(
                    model = bytes,
                    contentDescription = contentDescription,
                    // Crop de anh lap day khung nhu ban thiet ke, khong bi letterbox.
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = placeholderText,
                    fontSize = 11.sp,
                    color = Color(0xFF3D4B5C),
                )
            }
        } else {
            Text(
                text = placeholderText,
                fontSize = 11.sp,
                color = Color(0xFF3D4B5C),
            )
        }
    }
}

/**
 * Avatar vong tron, hien anh tu Google neu co, khong thi hien chu cai dau tien.
 */
@Composable
fun UserAvatar(
    photoUrl: String,
    name: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(EcomerPlaceholder),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = name.trim().take(2).uppercase().ifBlank { "?" },
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3D4B5C),
            )
        }
    }
}

/** Hien thi thong bao loi chung. */
@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFDECEA))
            .border(1.dp, Color(0xFFF2B8B5), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = message,
            color = Color(0xFFB3262D),
            fontSize = 13.sp,
        )
    }
}

/** Man hinh trong khi dang tai du lieu. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = EcomerPrimary)
    }
}

/** Chu thong bao khi danh sach rong. */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = EcomerTextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** Hang thong bao nho phia duoi man hinh. */
@Composable
fun ToastMessage(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF14202E))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = message,
            color = Color.White,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
