package com.ecomer.vku.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Anh da duoc nen xong, san sang luu vao Firestore.
 *
 * @param data duong dan base64 kem tien to "data:image/jpeg;base64,".
 * @param fileName ten file hien thi tren UI.
 * @param sizeBytes dung luong sau khi nen (byte).
 */
data class EncodedImage(
    val data: String = "",
    val fileName: String = "",
    val sizeBytes: Long = 0,
)

/**
 * Nen anh va chuyen thanh base64 de luu trong Firestore.
 *
 * Gioi han 1 MiB cua Firestore ap dung cho TUNG document. Vi moi anh duoc luu
 * rieng mot document nen moi anh co tran 1 MiB rieng — san pham bao nhieu anh
 * cung khong vuot gioi han. Nhung base64 phinh them ~33% so voi anh goc, nen
 * anh nen phai nho hon ~700 KB de chac chan vua 1 MiB sau khi ma hoa.
 *
 * Anh can duoc resize ve canh dai toi da 1080 px truoc, roi giam chat luong
 * JPEG cho den khi vua tran.
 */
object ImageEncoder {

    private const val TAG = "ImageEncoder"

    /** Canh dai toi da cua anh sau khi resize. */
    const val MAX_DIMENSION = 1080

    /** Chat luong JPEG ban dau. */
    private const val INITIAL_QUALITY = 85

    /** Chat luong JPEG thap nhat. */
    private const val MIN_QUALITY = 40

    /** Dung luong toi da cho moi anh (byte) — con 1 MiB sau khi ma hoa. */
    const val TARGET_BYTES = 700L * 1024

    /**
     * Doc anh tu [uri], resize va nen lai roi tra ve base64.
     *
     * Ham nay nen chay tren IO thread — viec giai ma bitmap va nen JPEG
     * ton thoi gian, lam tren main thread se lam app treo.
     */
    suspend fun encode(context: Context, uri: Uri): Result<EncodedImage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = decodeScaled(context, uri)
                    ?: error("Không đọc được ảnh")

                val compressed = compressToTarget(bitmap)
                bitmap.recycle()

                EncodedImage(
                    data = "data:image/jpeg;base64," + android.util.Base64.encodeToString(
                        compressed,
                        android.util.Base64.NO_WRAP,
                    ),
                    fileName = displayName(uri),
                    sizeBytes = compressed.size.toLong(),
                )
            }.onFailure { Log.e(TAG, "Không mã hóa được ảnh", it) }
        }

    /**
     * Doc anh va thu nho ve [MAX_DIMENSION] de tiet kiem dung luong.
     */
    private fun decodeScaled(context: Context, uri: Uri): Bitmap? {
        // Doc kich thuoc goc de chon muc thu nho.
        //
        // Luu y: `decodeStream` voi `inJustDecodeBounds = true` LUON tra ve null
        // (chi dien kich thuoc, khong tao bitmap). Nen phai tach phan mo
        // stream ra khoi ket qua `use` — neu gop lai, ket qua `use` se la null
        // va anh bi bo qua.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = context.contentResolver.openInputStream(uri) ?: return null
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }

        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null

        var sample = 1
        while (longest / sample > MAX_DIMENSION) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val imageStream = context.contentResolver.openInputStream(uri) ?: return null
        return imageStream.use { BitmapFactory.decodeStream(it, null, options) }
    }

    /**
     * Nen JPEG bang cach giam dan chat luong cho den khi du dung luong.
     *
     * Neu van qua tran, thu nho kich thuoc them mot lan nua.
     */
    private fun compressToTarget(bitmap: Bitmap): ByteArray {
        var quality = INITIAL_QUALITY
        var result = compress(bitmap, quality)

        while (result.size > TARGET_BYTES && quality > MIN_QUALITY) {
            quality -= 10
            result = compress(bitmap, quality)
        }

        if (result.size > TARGET_BYTES) {
            val smaller = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * 0.75f).roundToInt().coerceAtLeast(1),
                (bitmap.height * 0.75f).roundToInt().coerceAtLeast(1),
                true,
            )
            if (smaller != bitmap) bitmap.recycle()
            quality = MIN_QUALITY
            result = compress(smaller, quality)
            smaller.recycle()
        }

        return result
    }

    private fun compress(bitmap: Bitmap, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    /** Lay ten file de hien thi, bo phan duong va duoi thuoc. */
    private fun displayName(uri: Uri): String =
        uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
            ?.takeIf { it.isNotBlank() }
            ?: "anh"
}
