package com.ecomer.vku.app.data

import android.util.Base64

/**
 * Giai ma chuoi base64 thanh byte de hien thi anh.
 *
 * Coil 2 KHONG ho tro data URI ("data:image/jpeg;base64,...") — khong co
 * `DataUriFetcher` trong thu vien, nen truyen chuoi do cho [coil.compose.AsyncImage]
 * se khong tai duoc gi va anh khong hien.
 *
 * Giai ma thanh byte thu cong roi truyen cho Coil la cach dung cho trong truong
 * hop nay (Coil co san `ByteArrayMapper` va `ByteBufferFetcher`).
 */
object Base64Image {

    /** Tien to cua du lieu anh trong Firestore. */
    private const val PREFIX = "data:image/jpeg;base64,"

    /**
     * Giai ma [data] thanh byte, tra ve null neu khong phai base64 hoac loi.
     */
    fun decode(data: String): ByteArray? {
        if (data.isBlank()) return null

        val payload = if (data.startsWith(PREFIX)) {
            data.removePrefix(PREFIX)
        } else if (data.startsWith("data:")) {
            // Data URI dang khac (vi du image/png) — cat phan sau dau phay.
            data.substringAfter("base64,", "")
        } else {
            // La base64 thuan.
            data
        }
        if (payload.isBlank()) return null

        return runCatching { Base64.decode(payload, Base64.DEFAULT) }.getOrNull()
    }
}
