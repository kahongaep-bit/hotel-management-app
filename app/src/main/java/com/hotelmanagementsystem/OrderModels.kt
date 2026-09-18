package com.hotelmanagementsystem

data class OrderItemRequest(
    val name: String,
    val quantity: Int,
    val price: Double,
    val category: String?
)

data class OrderRequest(
    val items: List<OrderItemRequest>,
    val total_amount: Double,
    val payment_method: String,
    val customer_name: String? = "Mteja"
)