package com.hotelmanagementsystem

data class StockIssueReportItem(
    val id: Any? = null,
    val item_name: String? = null,
    val quantity: Double? = 0.0,
    val unit: String? = null,
    val unit_price: Double? = 0.0,
    val total_value: Double? = 0.0,
    val department: String? = null,
    val created_at: String? = null,
    val issued_by: String? = null
)