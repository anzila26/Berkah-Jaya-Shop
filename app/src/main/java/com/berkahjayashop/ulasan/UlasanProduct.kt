package com.berkahjayashop.ulasan

import java.sql.Timestamp

data class UlasanProduct(
    val productId: String? = null,
    val image: String? = null,
    val name: String? = null,
    val quantity: String? = null,
    val price: String? = null,
    var rating: Float = 0f,
    var review: String = "",

)
