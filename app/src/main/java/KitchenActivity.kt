package com.hotelmanagementsystem

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class KitchenActivity : AppCompatActivity() {

    private lateinit var tvKitchenStaff: TextView
    private lateinit var btnKitchenPassword: Button
    private lateinit var rvKitchenOrders: RecyclerView
    private lateinit var etTokenInput: EditText
    private lateinit var btnVerifyToken: Button
    private lateinit var btnMakabidhiano: Button
    private lateinit var btnKitchenBreakdown: Button
    private lateinit var btnKitchenSalesReport: Button

    private lateinit var kitchenAdapter: KitchenOrderAdapter
    private val kitchenOrdersList = mutableListOf<OrderResponseItem>()
    private val displayedKitchenOrdersList = mutableListOf<OrderResponseItem>()

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed) {
                fetchKitchenOrders(showToastOnFailure = false)
                loadCurrentKitchenStaff()
                refreshHandler.postDelayed(this, 10000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kitchen_display)

        tvKitchenStaff = findViewById(R.id.tvKitchenStaff)
        btnKitchenPassword = findViewById(R.id.btnKitchenPassword)
        rvKitchenOrders = findViewById(R.id.rvKitchenOrders)
        etTokenInput = findViewById(R.id.etSearchKitchenToken)
        btnVerifyToken = findViewById(R.id.btnSearchKitchen)
        btnMakabidhiano = findViewById(R.id.btnKitchenHandover)
        btnKitchenBreakdown = findViewById(R.id.btnKitchenBreakdown)
        btnKitchenSalesReport = findViewById(R.id.btnKitchenSalesReport)

        rvKitchenOrders.layoutManager = LinearLayoutManager(this)

        kitchenAdapter = KitchenOrderAdapter(
            displayedKitchenOrdersList,
            onUpdateStatus = { orderId, status -> updateOrderStatus(orderId, status) },
            onRejectOrder = { orderItem -> showRejectDialog(orderItem) }
        )
        rvKitchenOrders.adapter = kitchenAdapter

        btnVerifyToken.setOnClickListener { filterOrders() }

        btnMakabidhiano.setOnClickListener {
            showHandoverDialog()
        }

        btnKitchenBreakdown.setOnClickListener {
            showKitchenBreakdownDialog()
        }

        btnKitchenSalesReport.setOnClickListener {
            showKitchenSalesReport()
        }

        btnKitchenPassword.setOnClickListener {
            showChangePasswordDialog()
        }

        etTokenInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { filterOrders() }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadCurrentKitchenStaff()
        fetchKitchenOrders(showToastOnFailure = true)
    }

    override fun onResume() {
        super.onResume()
        refreshHandler.post(refreshRunnable)
        loadCurrentKitchenStaff()
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://10.32.78.51:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun loadCurrentKitchenStaff() {
        val apiService = getRetrofit().create(ApiService::class.java)
        apiService.getCurrentShiftInfo("Jikoni").enqueue(object : Callback<ShiftInfoResponse> {
            override fun onResponse(call: Call<ShiftInfoResponse>, response: Response<ShiftInfoResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val staffName = response.body()!!.active_staff.ifEmpty { "Hajathibitishwa" }
                    tvKitchenStaff.text = "Mpishi Zamu Hii: $staffName"
                }
            }
            override fun onFailure(call: Call<ShiftInfoResponse>, t: Throwable) {}
        })
    }

    private fun showKitchenSalesReport() {
        val apiService = getRetrofit().create(ApiService::class.java)
        apiService.getCategoryBreakdown().enqueue(object : Callback<CategoryBreakdownResponse> {
            override fun onResponse(call: Call<CategoryBreakdownResponse>, response: Response<CategoryBreakdownResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val msg = """
                        📊 TAARIFA YA MAUZO YA JIKO:
                        
                        • Jumla ya Vyakula Vilivyouzwa: TSH ${String.format("%,.0f", data.food_total)}
                    """.trimIndent()

                    AlertDialog.Builder(this@KitchenActivity)
                        .setTitle("Taarifa ya Mauzo")
                        .setMessage(msg)
                        .setPositiveButton("SAWA", null)
                        .show()
                }
            }
            override fun onFailure(call: Call<CategoryBreakdownResponse>, t: Throwable) {}
        })
    }

    private fun showChangePasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("🔑 Badilisha Password")
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val etEmail = EditText(this).apply { hint = "Ingiza Email Yako" }
        val etOldPass = EditText(this).apply { hint = "Password ya Zamani"; inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT }
        val etNewPass = EditText(this).apply { hint = "Password Mpya"; inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT }

        layout.addView(etEmail)
        layout.addView(etOldPass)
        layout.addView(etNewPass)
        builder.setView(layout)

        builder.setPositiveButton("BADILISHA") { dialog, _ ->
            val email = etEmail.text.toString().trim()
            val oldPass = etOldPass.text.toString().trim()
            val newPass = etNewPass.text.toString().trim()

            if (email.isNotEmpty() && oldPass.isNotEmpty() && newPass.isNotEmpty()) {
                val apiService = getRetrofit().create(ApiService::class.java)
                val body = hashMapOf<String, Any>(
                    "email" to email,
                    "old_password" to oldPass,
                    "new_password" to newPass
                )
                apiService.changePassword(body).enqueue(object : Callback<GenericResponse> {
                    override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@KitchenActivity, "Password imebadilishwa kikamilifu!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@KitchenActivity, "Imeshindikana. Hakiki taarifa zako.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<GenericResponse>, t: Throwable) {}
                })
            } else {
                Toast.makeText(this, "Jaza sehemu zote!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("GHAIRI", null)
        builder.show()
    }

    private fun filterOrders() {
        val query = etTokenInput.text.toString().trim().lowercase()
        if (query.isEmpty()) {
            displayedKitchenOrdersList.clear()
            displayedKitchenOrdersList.addAll(kitchenOrdersList)
        } else {
            val filtered = kitchenOrdersList.filter { order ->
                val token = order.token_number?.lowercase() ?: ""
                val customer = order.customerName.lowercase()
                token.contains(query) || customer.contains(query)
            }
            displayedKitchenOrdersList.clear()
            displayedKitchenOrdersList.addAll(filtered)
        }
        kitchenAdapter.notifyDataSetChanged()
    }

    private fun isFoodItem(category: String?, itemName: String?): Boolean {
        val cat = (category ?: "").lowercase()
        val name = (itemName ?: "").lowercase()

        val isDrink = cat.contains("bar") || cat.contains("vinywaji") ||
                name.contains("bia") || name.contains("soda") || name.contains("maji")

        return !isDrink
    }

    private fun fetchKitchenOrders(showToastOnFailure: Boolean) {
        if (isFinishing || isDestroyed) return

        val apiService = getRetrofit().create(ApiService::class.java)

        apiService.getOrders().enqueue(object : Callback<List<OrderResponseItem>> {
            override fun onResponse(call: Call<List<OrderResponseItem>>, response: Response<List<OrderResponseItem>>) {
                if (isFinishing || isDestroyed) return

                if (response.isSuccessful && response.body() != null) {
                    val rawOrders = response.body()!!

                    val foodOrders = rawOrders.filter { order ->
                        val st = (order.status ?: "Pending").trim().lowercase()
                        val hasFood = order.items.isEmpty() || order.items.any { isFoodItem(it.category, it.name) }
                        (st == "pending" || st == "cooking" || st.isEmpty()) && hasFood
                    }

                    kitchenOrdersList.clear()
                    kitchenOrdersList.addAll(foodOrders)
                    filterOrders()
                }
            }

            override fun onFailure(call: Call<List<OrderResponseItem>>, t: Throwable) {
                if (showToastOnFailure && !isFinishing && !isDestroyed) {
                    Toast.makeText(this@KitchenActivity, "Hitilafu ya Mtandao", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun updateOrderStatus(orderId: String, newStatus: String) {
        if (isFinishing || isDestroyed || orderId.isEmpty()) return

        val apiService = getRetrofit().create(ApiService::class.java)
        val statusData = hashMapOf<String, Any>("status" to newStatus)

        apiService.updateOrderStatus(orderId, statusData).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (isFinishing || isDestroyed) return

                if (response.isSuccessful) {
                    Toast.makeText(this@KitchenActivity, "Status ya chakula imebadilishwa!", Toast.LENGTH_SHORT).show()
                    fetchKitchenOrders(showToastOnFailure = false)
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {}
        })
    }

    private fun showKitchenBreakdownDialog() {
        val totalPending = kitchenOrdersList.count { (it.status ?: "").lowercase() == "pending" }
        val totalCooking = kitchenOrdersList.count { (it.status ?: "").lowercase() == "cooking" }

        val message = """
            📊 HALI YA JIKONI KWA SASA:
            
            • Jumla ya Oda zinazosubiri: $totalPending
            • Oda zinazopikwa (Cooking): $totalCooking
            • Jumla kuu ya oda mezani: ${kitchenOrdersList.size}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("📋 Mchanganuo wa Jikoni")
            .setMessage(message)
            .setPositiveButton("SAWA", null)
            .show()
    }

    private fun showHandoverDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("📊 Makabidhiano ya Zamu - Jikoni")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val etOutgoing = EditText(this).apply {
            hint = "Jina la Mpishi Anayetoka (Outgoing)"
        }
        val etIncoming = EditText(this).apply {
            hint = "Jina la Mpishi Anayeingia (Incoming)"
        }
        val etItemsDelivered = EditText(this).apply {
            hint = "Jumla ya Vyombo/Vyakula vilivyotoka"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(etOutgoing)
        layout.addView(etIncoming)
        layout.addView(etItemsDelivered)
        builder.setView(layout)

        builder.setPositiveButton("WEKA MAKABIDHIANO") { dialog, _ ->
            val outgoing = etOutgoing.text.toString().trim()
            val incoming = etIncoming.text.toString().trim()
            val delivered = etItemsDelivered.text.toString().trim().toIntOrNull() ?: 0

            if (outgoing.isEmpty() || incoming.isEmpty()) {
                Toast.makeText(this, "Jaza majina ya wapishi wote!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val apiService = getRetrofit().create(ApiService::class.java)
            val handoverData = hashMapOf<String, Any>(
                "outgoing_staff" to outgoing,
                "incoming_staff" to incoming,
                "items_delivered" to delivered,
                "pending_orders" to kitchenOrdersList.size,
                "department" to "Jikoni"
            )

            apiService.submitHandover(handoverData).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@KitchenActivity, "Makabidhiano yamerekodiwa kikamilifu!", Toast.LENGTH_SHORT).show()
                        loadCurrentKitchenStaff()
                    } else {
                        Toast.makeText(this@KitchenActivity, "Imeshindikana kusave makabidhiano", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@KitchenActivity, "Hitilafu ya mtandao", Toast.LENGTH_SHORT).show()
                }
            })
            dialog.dismiss()
        }
        builder.setNegativeButton("GHAIRI", null)
        builder.show()
    }

    private fun showRejectDialog(order: OrderResponseItem) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("❌ Kurudisha Oda #${order.token_number ?: ""} kwa Cashier")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val input = EditText(this).apply {
            hint = "Sababu za kurudisha chakula..."
            setTextColor(Color.parseColor("#0F172A"))
            setHintTextColor(Color.parseColor("#64748B"))
        }
        layout.addView(input)
        builder.setView(layout)

        builder.setPositiveButton("RUDISHA KWA CASHIER") { dialog, _ ->
            val reason = input.text.toString().trim().ifEmpty { "Chakula kimeisha jikoni" }

            val apiService = getRetrofit().create(ApiService::class.java)
            val rejectData = hashMapOf<String, Any>(
                "rejection_reason" to reason,
                "department" to "Jikoni"
            )

            apiService.rejectOrder(order.id, rejectData).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (isFinishing || isDestroyed) return

                    if (response.isSuccessful) {
                        Toast.makeText(this@KitchenActivity, "Oda imerudishwa kwa Cashier!", Toast.LENGTH_SHORT).show()
                        fetchKitchenOrders(showToastOnFailure = false)
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {}
            })
            dialog.dismiss()
        }

        builder.setNegativeButton("GHAIRI", null)
        builder.show()
    }
}