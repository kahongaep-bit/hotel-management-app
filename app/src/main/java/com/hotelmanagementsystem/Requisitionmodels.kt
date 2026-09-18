package com.hotelmanagementsystem

// Data Models za Requisitions & Stock Management
data class RequisitionItem(
    val id: String?,
    val item_name: String?,
    val quantity: Double?,
    val unit: String?,
    val requested_by: String?,
    val department: String?,
    val status: String?,
    val rejection_comment: String?,
    val supplier: String?,
    val estimated_cost: Double?,
    val ratio_per_unit: Double?,
    val total_portions: Double?,
    val created_at: String? // Transferred from DB for displaying tracking dates
)

data class StockItem(
    val id: Int? = null,
    val name: String? = null,
    val item_name: String? = null,
    val quantity: Double,
    val unit: String
)

data class GenericResponse(
    val message: String,
    val status: Boolean
)