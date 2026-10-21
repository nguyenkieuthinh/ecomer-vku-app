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
            id = "quan-nam-001",
            tenSanPham = "Quần zin nam",
            loaiSanPham = "Quần nam",
            gia = 200_000,
        ),
        Product(
            id = "ao-nam-002",
            tenSanPham = "Áo phông nam",
            loaiSanPham = "Thời trang nữ",
            gia = 300_000,
        ),
        Product(
            id = "thoi-trang-nu-001",
            tenSanPham = "Váy hoa",
            loaiSanPham = "Thời trang nữ",
            gia = 350_000,
        ),
        Product(
            id = "ao-nam-003",
            tenSanPham = "Áo khoác",
            loaiSanPham = "Áo nam",
            gia = 450_000,
        ),
    )

    /** Danh muc lay tu danh sach san pham, "Tất cả" luon o dau. */
    val categories = listOf(CATEGORY_ALL, "Quần nam", "Áo nam", "Thời trang nữ")
}
