package com.ecomer.vku.app.data.repo

import com.ecomer.vku.app.data.model.Product

/**
 * Du lieu mau dung khi Firebase chua duoc cau hinh.
 * Gia tri lay tu ban thiet ke UI/UX.
 */
object SampleData {

    const val CATEGORY_ALL = "Tất cả"

    val products = listOf(
        Product(
            id = "ao-nam-001",
            name = "Quần zin nam",
            category = "Quần nam",
            price = 200_000,
            totalStock = 20,
            availableStock = 20,
            soldCount = 0,
        ),
        Product(
            id = "ao-nam-002",
            name = "Áo phông nam",
            category = "Thời trang nữ",
            price = 300_000,
            totalStock = 25,
            availableStock = 25,
            soldCount = 0,
        ),
        Product(
            id = "thoi-trang-nu-001",
            name = "Váy hoa",
            category = "Thời trang nữ",
            price = 350_000,
            totalStock = 15,
            availableStock = 15,
            soldCount = 0,
        ),
        Product(
            id = "ao-nam-003",
            name = "Áo khoác",
            category = "Áo nam",
            price = 450_000,
            totalStock = 10,
            availableStock = 10,
            soldCount = 0,
        ),
    )

    /** Danh muc lay tu danh sach san pham, "Tất cả" luon o dau. */
    val categories = listOf(CATEGORY_ALL, "Quần nam", "Áo nam", "Thời trang nữ")
}
