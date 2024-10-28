package com.berkahjayashop.main.item

data class ProductsItem(
    var id: String? = null,
    val category: String? = null,
    val createdAt: String? = null,
    val description: String? = null,
    val image: String? = null,
    val name: String? = null,
    val purchase_price: Long? = null,
    val selling_price: Long? = null,
    val stock: Long? = null,
    var averageRating: Double = 0.0
)
