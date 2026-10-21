package com.ecomer.vku.app.data.model

/**
 * Dia chi giao hang cua nguoi dung.
 *
 * Luu trong subcollection `users/{uid}/addresses` nen moi nguoi chi quan ly
 * duoc dia chi cua minh.
 *
 * @param id id cua document dia chi.
 * @param userId uid cua chu so dia chi.
 * @param fullName nguoi nhan hang.
 * @param phone so dien thoai lien lac.
 * @param street so nha, duong pho.
 * @param ward phuong/xa.
 * @param district quan/huyen.
 * @param city tinh/thanh pho.
 * @param isDefault dia chi mac dinh, dung khi thanh toan.
 */
data class Address(
    val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val phone: String = "",
    val street: String = "",
    val ward: String = "",
    val district: String = "",
    val city: String = "",
    val isDefault: Boolean = false,
) {
    /** Dia chi gop thanh mot dong de hien thi. */
    val fullAddress: String
        get() = listOf(street, ward, district, city)
            .filter { it.isNotBlank() }
            .joinToString(", ")

    /** Hien thi ngan gon tren nut. */
    val shortAddress: String
        get() = listOf(street, ward, city)
            .filter { it.isNotBlank() }
            .joinToString(", ")

    /** Du lieu hop le de co the dat hang. */
    val isComplete: Boolean
        get() = fullName.isNotBlank() &&
                phone.isNotBlank() &&
                street.isNotBlank() &&
                city.isNotBlank()

    companion object {
        const val COLLECTION = "addresses"
    }
}
