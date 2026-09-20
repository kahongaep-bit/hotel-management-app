package com.hotelmanagementsystem

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CashierActivity : AppCompatActivity() {

    private lateinit var tvCashSales: TextView
    private lateinit var tvLipaNambaSales: TextView
    private lateinit var tvGrossTotalSales: TextView
    private lateinit var tvTotalDeposited: TextView
    private lateinit var tvBalance: TextView
    private lateinit var btnCashierBankDeposit: Button

    private lateinit var btnFilterAll: Button
    private lateinit var btnFilterFood: Button
    private lateinit var btnFilterDrinks: Button
    private lateinit var btnFilterRooms: Button
    private lateinit var btnFilterHalls: Button

    private lateinit var rvMenuItems: RecyclerView
    private lateinit var menuAdapter: MenuAdapter

    private lateinit var rvCartItems: RecyclerView
    private lateinit var tvCartTotal: TextView
    private lateinit var etCustomerName: EditText
    private lateinit var btnPlaceOrder: Button

    private lateinit var btnRejectedOrders: Button
    private lateinit var btnSpecialOrder: Button
    private lateinit var btnCashierMchanganuo: Button

    private val fullMenuList = mutableListOf<MenuItem>()
    private val displayedMenuList = mutableListOf<MenuItem>()

    private val cartList = mutableListOf<CartItem>()
    private lateinit var cartAdapter: CartAdapter

    private var activeCategoryFilter = "ZOTE"
    private var activeResubmittingOrderId: String? = null
    private var originalRejectedAmount: Double = 0.0

    private val BASE_URL = ("https://hotel-backend-production-617c.up.railway.app/")

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cashier_pos)

        tvCashSales = findViewById(R.id.tvCashSales)
        tvLipaNambaSales = findViewById(R.id.tvLipaNambaSales)
        tvGrossTotalSales = findViewById(R.id.tvGrossTotalSales)

        tvTotalDeposited = findViewById(R.id.tvBankDepositedSales)
        tvBalance = findViewById(R.id.tvNetBalanceSales)

        btnCashierBankDeposit = findViewById(R.id.btnCashierBankDeposit)
        rvMenuItems = findViewById(R.id.rvMenuItems)

        rvCartItems = findViewById(R.id.rvCartItems)
        tvCartTotal = findViewById(R.id.tvCartTotal)
        etCustomerName = findViewById(R.id.etCustomerName)
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder)

        btnFilterAll = findViewById(R.id.btnFilterAll)
        btnFilterFood = findViewById(R.id.btnFilterFood)
        btnFilterDrinks = findViewById(R.id.btnFilterDrinks)
        btnFilterRooms = findViewById(R.id.btnFilterRooms)
        btnFilterHalls = findViewById(R.id.btnFilterHalls)

        btnRejectedOrders = findViewById(R.id.btnRejectedOrders)
        btnSpecialOrder = findViewById(R.id.btnSpecialOrder)
        btnCashierMchanganuo = findViewById(R.id.btnCategoryBreakdown)

        rvMenuItems.layoutManager = GridLayoutManager(this, 2)
        menuAdapter = MenuAdapter(displayedMenuList) { selectedItem -> addToCart(selectedItem) }
        rvMenuItems.adapter = menuAdapter

        rvCartItems.layoutManager = LinearLayoutManager(this)
        cartAdapter = CartAdapter(cartList) { calculateCartTotal() }
        rvCartItems.adapter = cartAdapter

        btnCashierBankDeposit.setOnClickListener { showCategorizedDepositDialog() }

        // MAREKEBISHO HAPA: Kitendo cha kubonyeza kitufe cha kutuma au kubadilisha oda
        btnPlaceOrder.setOnClickListener {
            showPaymentMethodDialog()
        }

        btnRejectedOrders.setOnClickListener { showRejectedOrdersDialog() }
        btnSpecialOrder.setOnClickListener { showSpecialOrderDialog() }
        btnCashierMchanganuo?.setOnClickListener { showCategoryBreakdownDialog() }

        btnFilterAll.setOnClickListener { filterCategory("ZOTE") }
        btnFilterFood.setOnClickListener { filterCategory("Vyakula") }
        btnFilterDrinks.setOnClickListener { filterCategory("Vinywaji") }
        btnFilterRooms.setOnClickListener { filterCategory("Chumba") }
        btnFilterHalls.setOnClickListener { filterCategory("Ukumbi") }

        loadAllCashierData()
    }

    override fun onResume() {
        super.onResume()
        loadAllCashierData()
    }

    private fun loadAllCashierData() {
        fetchCashierSummary()
        fetchMenuItemsFromDatabase()
    }

    private fun showCategoryBreakdownDialog() {
        val apiService = getRetrofit().create(ApiService::class.java)
        apiService.getCategoryBreakdown().enqueue(object : Callback<CategoryBreakdownResponse> {
            override fun onResponse(call: Call<CategoryBreakdownResponse>, response: Response<CategoryBreakdownResponse>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val msg = """
                        📊 MCHANGANUO WA MAUZO YA LEO:
                        
                        • Vyakula: TSH ${String.format("%,.0f", data.food_total)}
                        • Vinywaji (Bar): TSH ${String.format("%,.0f", data.drinks_total)}
                        • Vyumba: TSH ${String.format("%,.0f", data.rooms_total)}
                        • Kumbi: TSH ${String.format("%,.0f", data.halls_total)}
                    """.trimIndent()

                    AlertDialog.Builder(this@CashierActivity)
                        .setTitle("📋 Mchanganuo wa Mauzo")
                        .setMessage(msg)
                        .setPositiveButton("SAWA", null)
                        .show()
                }
            }
            override fun onFailure(call: Call<CategoryBreakdownResponse>, t: Throwable) {}
        })
    }

    private fun addToCart(item: MenuItem) {
        val existingItem = cartList.find { it.name == item.name }
        if (existingItem != null) {
            existingItem.quantity += 1
        } else {
            cartList.add(CartItem(name = item.name, price = item.price, quantity = 1, category = item.category ?: "Chakula"))
        }
        cartAdapter.notifyDataSetChanged()
        calculateCartTotal()
    }

    private fun calculateCartTotal() {
        val total = cartList.sumOf { it.price * it.quantity }
        tvCartTotal.text = "TSH ${String.format("%,.0f", total)}"
    }

    private fun showSpecialOrderDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("➕ Special Order (Oda Maalum)")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val etItemName = EditText(this).apply { hint = "Jina la Chakula/Kinywaji Maalum" }
        val etItemPrice = EditText(this).apply {
            hint = "Gharama/Bei (TZS)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        layout.addView(etItemName)
        layout.addView(etItemPrice)
        builder.setView(layout)

        builder.setPositiveButton("WEKA KWENYE CART") { dialog, _ ->
            val name = etItemName.text.toString().trim()
            val price = etItemPrice.text.toString().toDoubleOrNull() ?: 0.0

            if (name.isNotEmpty() && price > 0) {
                cartList.add(CartItem(name = "[Special] $name", price = price, quantity = 1, category = "Special"))
                cartAdapter.notifyDataSetChanged()
                calculateCartTotal()
            } else {
                Toast.makeText(this, "Ingiza jina na bei sahihi!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("GHAIRI", null)
        builder.show()
    }

    private fun showPaymentMethodDialog() {
        if (cartList.isEmpty()) {
            Toast.makeText(this, "Chagua angalau bidhaa moja!", Toast.LENGTH_SHORT).show()
            return
        }

        val totalAmount = cartList.sumOf { it.price * it.quantity }

        if (activeResubmittingOrderId != null && totalAmount < originalRejectedAmount) {
            Toast.makeText(
                this,
                "Huwezi kupunguza gharama! Oda ya awali ilikuwa TSH ${String.format("%,.0f", originalRejectedAmount)}. Ongeza huduma nyingine.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val options = arrayOf("💵 Cash (Pesa Taslimu)", "📱 LipaNamba / M-Pesa")
        val builder = AlertDialog.Builder(this)
        builder.setTitle(if (activeResubmittingOrderId != null) "Badilisha na Chagua Njia ya Malipo" else "Chagua Njia ya Malipo")
        builder.setItems(options) { _, which ->
            val selectedMethod = if (which == 0) "Cash" else "LipaNamba"
            processAndSendOrder(selectedMethod)
        }
        builder.show()
    }

    private fun processAndSendOrder(paymentMethod: String) {
        val customer = etCustomerName.text.toString().trim().ifEmpty { "Mteja" }
        val totalAmount = cartList.sumOf { it.price * it.quantity }

        val orderItemRequests = cartList.map {
            OrderItemRequest(name = it.name, quantity = it.quantity, price = it.price, category = it.category)
        }

        val orderRequest = OrderRequest(
            customer_name = customer,
            items = orderItemRequests,
            total_amount = totalAmount,
            payment_method = paymentMethod
        )

        val apiService = getRetrofit().create(ApiService::class.java)

        if (activeResubmittingOrderId != null) {
            apiService.resubmitOrder(activeResubmittingOrderId!!, orderRequest).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (isFinishing || isDestroyed) return
                    if (response.isSuccessful) {
                        Toast.makeText(this@CashierActivity, "Oda imebadilishwa na kutumwa tena!", Toast.LENGTH_LONG).show()
                        resetCartAndState()
                    } else {
                        Toast.makeText(this@CashierActivity, "Imeshindikana kusasisha oda.", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    if (isFinishing || isDestroyed) return
                    Toast.makeText(this@CashierActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            apiService.sendOrder(orderRequest).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (isFinishing || isDestroyed) return
                    if (response.isSuccessful) {
                        Toast.makeText(this@CashierActivity, "Oda imelipwa ($paymentMethod) na kutumwa!", Toast.LENGTH_LONG).show()
                        resetCartAndState()
                    }
                }
                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    if (isFinishing || isDestroyed) return
                    Toast.makeText(this@CashierActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun resetCartAndState() {
        cartList.clear()
        cartAdapter.notifyDataSetChanged()
        calculateCartTotal()
        etCustomerName.text.clear()
        activeResubmittingOrderId = null
        originalRejectedAmount = 0.0
        btnPlaceOrder.text = "LIPA NA ZALISHA"
        fetchCashierSummary()
    }

    private fun showRejectedOrdersDialog() {
        val apiService = getRetrofit().create(ApiService::class.java)
        apiService.getOrders().enqueue(object : Callback<List<OrderResponseItem>> {
            override fun onResponse(call: Call<List<OrderResponseItem>>, response: Response<List<OrderResponseItem>>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful && response.body() != null) {
                    val rejectedOrders = response.body()!!.filter {
                        val st = (it.status ?: "").lowercase()
                        st.contains("reject")
                    }

                    val builder = AlertDialog.Builder(this@CashierActivity)
                    builder.setTitle("❌ Oda Zilizorudishwa")

                    if (rejectedOrders.isEmpty()) {
                        builder.setMessage("Hakuna oda iliyorudishwa.")
                        builder.setPositiveButton("OK", null)
                    } else {
                        val itemsText = rejectedOrders.map {
                            "• Token #${it.token_number ?: ""} - ${it.customerName} (TSH ${String.format("%,.0f", it.total_amount)})\n  Sababu: ${it.status ?: ""}"
                        }.toTypedArray()

                        builder.setItems(itemsText) { _, index ->
                            val selectedOrder = rejectedOrders[index]
                            loadRejectedOrderToCart(selectedOrder)
                        }
                    }
                    builder.show()
                }
            }
            override fun onFailure(call: Call<List<OrderResponseItem>>, t: Throwable) {}
        })
    }

    private fun loadRejectedOrderToCart(order: OrderResponseItem) {
        cartList.clear()
        order.items.forEach {
            cartList.add(CartItem(name = it.name ?: "", price = it.price ?: 0.0, quantity = it.quantity ?: 1, category = it.category ?: "Chakula"))
        }
        cartAdapter.notifyDataSetChanged()
        calculateCartTotal()
        etCustomerName.setText(order.customerName)
        activeResubmittingOrderId = order.id
        originalRejectedAmount = order.total_amount

        btnPlaceOrder.text = "BADILISHA HUDUMA NA TUMA TENA"
        Toast.makeText(this, "Oda imeingizwa. Fanya mabadiliko kisha bonyeza kitufe cha kijani chini.", Toast.LENGTH_LONG).show()
    }

    private fun fetchCashierSummary() {
        val apiService = getRetrofit().create(ApiService::class.java)

        apiService.getDailySales().enqueue(object : Callback<DailySalesResponse> {
            override fun onResponse(call: Call<DailySalesResponse>, response: Response<DailySalesResponse>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful && response.body() != null) {
                    val sales = response.body()!!
                    tvCashSales.text = "TSH ${String.format("%,.0f", sales.cash_sales)}"
                    tvLipaNambaSales.text = "TSH ${String.format("%,.0f", sales.lipanamba_sales)}"
                    tvGrossTotalSales.text = "TSH ${String.format("%,.0f", sales.gross_total)}"

                    tvTotalDeposited.text = "TSH ${String.format("%,.0f", sales.total_deposited)}"
                    tvBalance.text = "TSH ${String.format("%,.0f", sales.total)}"
                } else {
                    tvCashSales.text = "TSH 0"
                    tvLipaNambaSales.text = "TSH 0"
                    tvGrossTotalSales.text = "TSH 0"
                    tvTotalDeposited.text = "TSH 0"
                    tvBalance.text = "TSH 0"
                }
            }

            override fun onFailure(call: Call<DailySalesResponse>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                tvCashSales.text = "TSH 0"
                tvLipaNambaSales.text = "TSH 0"
                tvGrossTotalSales.text = "TSH 0"
                tvTotalDeposited.text = "TSH 0"
                tvBalance.text = "TSH 0"
            }
        })
    }

    private fun fetchMenuItemsFromDatabase() {
        val apiService = getRetrofit().create(ApiService::class.java)
        apiService.getMenuItems().enqueue(object : Callback<List<MenuItem>> {
            override fun onResponse(call: Call<List<MenuItem>>, response: Response<List<MenuItem>>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful && response.body() != null) {
                    fullMenuList.clear()
                    fullMenuList.addAll(response.body()!!)
                    filterCategory(activeCategoryFilter)
                }
            }
            override fun onFailure(call: Call<List<MenuItem>>, t: Throwable) {}
        })
    }

    private fun filterCategory(category: String) {
        activeCategoryFilter = category
        displayedMenuList.clear()

        if (category.equals("ZOTE", ignoreCase = true)) {
            displayedMenuList.addAll(fullMenuList)
        } else {
            val filtered = fullMenuList.filter { item ->
                val cat = item.category ?: ""
                when (category.uppercase()) {
                    "VYAKULA" -> cat.contains("Vyakula", ignoreCase = true) || cat.contains("Chakula", ignoreCase = true) || cat.isEmpty()
                    "VINYWAJI" -> cat.contains("Vinywaji", ignoreCase = true) || cat.contains("Kinywaji", ignoreCase = true) || cat.contains("Bar", ignoreCase = true)
                    "CHUMBA", "VYUMBA" -> cat.contains("Chumba", ignoreCase = true) || cat.contains("Vyumba", ignoreCase = true) || cat.contains("Room", ignoreCase = true)
                    "UKUMBI", "KUMBI" -> cat.contains("Ukumbi", ignoreCase = true) || cat.contains("Kumbi", ignoreCase = true) || cat.contains("Hall", ignoreCase = true)
                    else -> cat.equals(category, ignoreCase = true)
                }
            }
            displayedMenuList.addAll(filtered)
        }
        menuAdapter.notifyDataSetChanged()
    }

    private fun showCategorizedDepositDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Mchanganuo wa Pesa za Kuweka Benki")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val etBreakfast = EditText(this).apply { hint = "Breakfast Sales (TZS)"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val etLunch = EditText(this).apply { hint = "Lunch Sales (TZS)"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val etDinner = EditText(this).apply { hint = "Dinner Sales (TZS)"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val etDrinks = EditText(this).apply { hint = "Drinks/Bar Sales (TZS)"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val etRooms = EditText(this).apply { hint = "Rooms Sales (TZS)"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }

        layout.addView(etBreakfast)
        layout.addView(etLunch)
        layout.addView(etDinner)
        layout.addView(etDrinks)
        layout.addView(etRooms)

        builder.setView(layout)

        builder.setPositiveButton("Weka Benki") { dialog, _ ->
            val breakfast = etBreakfast.text.toString().toDoubleOrNull() ?: 0.0
            val lunch = etLunch.text.toString().toDoubleOrNull() ?: 0.0
            val dinner = etDinner.text.toString().toDoubleOrNull() ?: 0.0
            val drinks = etDrinks.text.toString().toDoubleOrNull() ?: 0.0
            val rooms = etRooms.text.toString().toDoubleOrNull() ?: 0.0
            val totalAmount = breakfast + lunch + dinner + drinks + rooms

            if (totalAmount > 0) {
                sendCategorizedBankDeposit(breakfast, lunch, dinner, drinks, rooms, totalAmount)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Ghairi", null)
        builder.show()
    }

    private fun sendCategorizedBankDeposit(b: Double, l: Double, d: Double, dr: Double, r: Double, total: Double) {
        val apiService = getRetrofit().create(ApiService::class.java)
        val depositData = hashMapOf<String, Any>(
            "breakfast_amount" to b,
            "lunch_amount" to l,
            "dinner_amount" to d,
            "drinks_amount" to dr,
            "rooms_amount" to r,
            "total_amount" to total,
            "deposited_by" to "Cashier"
        )

        apiService.recordBankDeposit(depositData).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful) {
                    Toast.makeText(this@CashierActivity, "Pesa zimeingia benki kikamilifu!", Toast.LENGTH_LONG).show()
                    fetchCashierSummary()
                } else {
                    Toast.makeText(this@CashierActivity, "Hitilafu kwenye Bank Deposit", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                Toast.makeText(this@CashierActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}