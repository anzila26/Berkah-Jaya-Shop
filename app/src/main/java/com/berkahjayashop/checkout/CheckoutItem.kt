package com.berkahjayashop.checkout

data class CheckoutItem(
    var productId: String? = null,
    var createAt: Long = 0,
    var name: String? = null,
    var price: String? = null,
    var quantity: Int = 0,
    var image: String? = null
)
