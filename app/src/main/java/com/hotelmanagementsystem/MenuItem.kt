package com.hotelmanagementsystem

import com.google.gson.annotations.SerializedName

data class MenuItem(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("name")
    val name: String,

    @SerializedName("price")
    val price: Double,

    @SerializedName("category")
    val category: String? = "Chakula"
)