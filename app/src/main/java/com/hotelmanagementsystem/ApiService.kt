package com.hotelmanagementsystem

import retrofit2.Call
import retrofit2.http.*

data class AddProductRequest(
    val name: String,
    val price: Double,
    val category: String
)

data class TodayDepositResponse(
    val total: Double = 0.0
)

data class FinanceReportResponse(
    val total_sales: Double = 0.0,
    val cash_sales: Double = 0.0,
    val lipanamba_sales: Double = 0.0,
    val total_orders: Int = 0,
    val total_deposits: Double = 0.0,
    val balance: Double = 0.0,
    val breakfast_deposits: Double = 0.0,
    val lunch_deposits: Double = 0.0,
    val dinner_deposits: Double = 0.0,
    val drinks_deposits: Double = 0.0,
    val rooms_deposits: Double = 0.0
)

data class DailySalesResponse(
    val total: Double = 0.0,
    val gross_total: Double = 0.0,
    val cash_sales: Double = 0.0,
    val lipanamba_sales: Double = 0.0,
    val total_deposited: Double = 0.0
)

data class CategoryBreakdownResponse(
    val food_total: Double = 0.0,
    val drinks_total: Double = 0.0,
    val rooms_total: Double = 0.0,
    val halls_total: Double = 0.0
)

data class BarSalesResponse(
    val completed_orders: Int = 0,
    val drinks_total: Double = 0.0
)

data class OrderResponseItem(
    val id: String = "",
    val token_number: String? = "",
    val customer_name: String? = "Mteja",
    val items: List<OrderItemDetail> = emptyList(),
    val total_amount: Double = 0.0,
    val status: String? = "Pending",
    val created_at: String? = ""
) {
    val tokenNumber: String? get() = token_number
    val customerName: String get() = customer_name ?: "Mteja"
}

data class OrderItemDetail(
    val name: String = "",
    val quantity: Int = 1,
    val price: Double = 0.0,
    val category: String? = ""
)

data class ShiftInfoResponse(
    val active_staff: String = "Hajathibitishwa",
    val history: List<ShiftHandoverRecord> = emptyList()
)

data class ShiftHandoverRecord(
    val id: Int = 0,
    val department: String = "Bar",
    val outgoing_staff: String = "",
    val incoming_staff: String = "",
    val items_delivered: Int = 0,
    val pending_orders: Int = 0,
    val handed_over_at: String = ""
)

interface ApiService {

    // AUTHENTICATION & USERS
    @POST("api/login")
    fun loginUser(@Body request: LoginRequest): Call<LoginResponse>

    @PUT("api/users/change-password")
    fun changePassword(@Body body: HashMap<String, Any>): Call<GenericResponse>

    @POST("api/users/reset-password")
    fun resetPassword(@Body body: HashMap<String, Any>): Call<GenericResponse>

    // SHIFT HANDOVER (BAR & OTHER DEPARTMENTS)
    @POST("api/bar/handover")
    fun recordBarShiftHandover(@Body data: HashMap<String, Any>): Call<GenericResponse>

    @GET("api/bar/current-shift")
    fun getCurrentShiftInfo(@Query("department") department: String = "Bar"): Call<ShiftInfoResponse>

    // PRODUCTS / MENU ITEMS
    @GET("api/products")
    fun getProducts(): Call<List<MenuItem>>

    @POST("api/products")
    fun addProduct(@Body request: AddProductRequest): Call<GenericResponse>

    @PUT("api/products/{id}")
    fun updateProduct(
        @Path("id") id: String,
        @Body request: AddProductRequest
    ): Call<GenericResponse>

    @DELETE("api/products/{id}")
    fun deleteProduct(@Path("id") id: String): Call<GenericResponse>

    @GET("api/menu")
    fun getMenuItems(): Call<List<MenuItem>>

    @POST("api/menu")
    fun addMenuItem(@Body data: HashMap<String, Any>): Call<GenericResponse>

    @PUT("api/menu/{id}")
    fun updateMenuItem(
        @Path("id") id: String,
        @Body data: HashMap<String, Any>
    ): Call<GenericResponse>

    @DELETE("api/menu/{id}")
    fun deleteMenuItem(@Path("id") id: String): Call<GenericResponse>

    // ORDERS MANAGEMENT
    @POST("api/orders")
    fun sendOrder(@Body request: OrderRequest): Call<GenericResponse>

    @PUT("api/orders/{id}/resubmit")
    fun resubmitOrder(
        @Path("id") id: String,
        @Body request: OrderRequest
    ): Call<GenericResponse>

    @GET("api/orders")
    fun getOrders(): Call<List<OrderResponseItem>>

    @PUT("api/orders/{id}/status")
    fun updateOrderStatus(
        @Path("id") id: String,
        @Body statusData: HashMap<String, Any>
    ): Call<GenericResponse>

    @PUT("api/orders/{id}/reject")
    fun rejectOrder(
        @Path("id") id: String,
        @Body rejectData: HashMap<String, Any>
    ): Call<GenericResponse>

    // DASHBOARD & REPORTS
    // Endpoint ya daily sales iweze kupokea tarehe kama inahitajika au kuhakikisha server inachuja ya leo
    @GET("api/finance/daily-sales")
    fun getDailySales(
        @Query("date") date: String? = null
    ): Call<DailySalesResponse>

    @GET("api/finance/bar-sales")
    fun getBarSales(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Call<BarSalesResponse>

    @GET("api/finance/category-breakdown")
    fun getCategoryBreakdown(): Call<CategoryBreakdownResponse>

    @GET("api/finance/today-deposits")
    fun getTodayDeposits(): Call<TodayDepositResponse>

    @GET("api/finance/reports")
    fun getFinanceReports(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Call<FinanceReportResponse>

    @GET("api/reports/stock-issues")
    fun getStockIssueReports(
        @Query("startDate") startDate: String?,
        @Query("endDate") endDate: String?,
        @Query("department") department: String?
    ): Call<List<StockIssueReportItem>>

    @GET("api/reports/procured-items")
    fun getProcuredItemsReport(
        @Query("startDate") startDate: String?,
        @Query("endDate") endDate: String?
    ): Call<List<RequisitionItem>>

    // REQUISITIONS
    @GET("api/requisitions")
    fun getRequisitions(): Call<List<RequisitionItem>>

    @GET("api/requisitions")
    fun getAllRequisitions(@Query("all") all: Boolean = true): Call<List<RequisitionItem>>

    @GET("api/requisitions")
    fun getRequisitionsByStatus(@Query("status") status: String): Call<List<RequisitionItem>>

    @POST("api/requisitions")
    fun createRequisition(@Body data: HashMap<String, Any>): Call<GenericResponse>

    @DELETE("api/requisitions/{id}")
    fun deleteRequisition(@Path("id") id: String): Call<GenericResponse>

    @PUT("api/requisitions/{id}")
    fun updateAndResubmitRequisition(
        @Path("id") id: String,
        @Body data: HashMap<String, Any>
    ): Call<GenericResponse>

    @PUT("api/requisitions/{id}/approval")
    fun updateRequisitionApproval(
        @Path("id") id: String,
        @Body data: HashMap<String, Any>
    ): Call<GenericResponse>

    // STOCKS MANAGEMENT
    @GET("api/stock/main")
    fun getMainStockItems(): Call<List<StockItem>>

    @POST("api/stock/main")
    fun addMainStockItem(@Body stockData: HashMap<String, Any>): Call<GenericResponse>

    @GET("api/stock/sub")
    fun getSubStockItems(@Query("department") department: String? = null): Call<List<StockItem>>

    @POST("api/stock/issue-substore")
    fun issueStockToSubStore(@Body issueData: HashMap<String, Any>): Call<GenericResponse>

    // BANK DEPOSITS
    @POST("api/deposits")
    fun addDeposit(@Body depositData: HashMap<String, Any>): Call<GenericResponse>

    @POST("api/finance/bank-deposit")
    fun recordBankDeposit(@Body data: HashMap<String, Any>): Call<GenericResponse>

    @POST("api/bar/handover")
    fun submitHandover(@Body handoverData: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

}