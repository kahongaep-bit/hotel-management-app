package com.hotelmanagementsystem

data class CartItem(
    var name: String,
    var price: Double,
    var quantity: Int = 1,
    var category: String = "Chakula"
)