package com.berkahjayashop.transaction


data class Transaction(
    val address: String = "",
    val estimationDate: String = "",
    val orderDate: String = "",
    val orderID: String = "",
    val shipping: String = "",
    val statusPayment: String = "",
    val statusShipping: String = "",
    val totalPrice: Double = 0.0,
    var products: List<ProductOrder> = listOf()
)

data class ProductOrder(
    val productId: String,
    val image: String,
    val name: String,
    val quantity: String,
    val price: String
)
