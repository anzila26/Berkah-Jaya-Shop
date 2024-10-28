package com.berkahjayashop.main.item

import java.io.Serializable

data class Product(
    val productId: String,
    val name: String,
    val quantity: Int,
    val price: Double
)
