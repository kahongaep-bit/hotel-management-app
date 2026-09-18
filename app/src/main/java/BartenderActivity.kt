package com.hotelmanagementsystem

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import java.util.Calendar
import java.util.Locale

class BartenderActivity : AppCompatActivity() {

    private var rvBarOrders: RecyclerView? = null
    private var etBarReceiptSearch: EditText? = null
    private var btnSearchDrinkOrder: Button? = null
    private var btnBarMakabidhiano: Button? = null
    private var btnBarSalesReport: Button? = null
    private var btnResetPasswordBar: Button? = null
    private var tvActiveBartender: TextView? = null

    private lateinit var adapter: BarOrderAdapter
    private val allBarOrdersList = mutableListOf<OrderResponseItem>()
    private val displayedBarOrdersList = mutableListOf<OrderResponseItem>()

    private var completedBarOrdersCount = 0
    private var totalBarSalesAmount = 0.0

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed) {
                fetchBarOrders()
                refreshHandler.postDelayed(this, 10000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_bartender)

            // Kuunganisha ID sahihi kutoka kwenye activity_bartender.xml
            rvBarOrders = findViewById(R.id.rvDrinkItems)
            etBarReceiptSearch = findViewById(R.id.etBarReceiptSearch)
            btnSearchDrinkOrder = findViewById(R.id.btnSearchDrinkOrder)
            btnBarMakabidhiano = findViewById(R.id.btnMakabidhiano)
            btnBarSalesReport = findViewById(R.id.btnBarSalesReport)
            btnResetPasswordBar = findViewById(R.id.btnResetPasswordBar)
            tvActiveBartender = findViewById(R.id.tvActiveBartender)

            rvBarOrders?.layoutManager = LinearLayoutManager(this)

            adapter = BarOrderAdapter(
                displayedBarOrdersList,
                onUpdateStatus = { orderId, newStatus -> updateOrderStatus(orderId, newStatus) },
                onRejectOrder = { order -> showRejectOrderDialog(order) }
            )
            rvBarOrders?.adapter = adapter

            btnSearchDrinkOrder?.setOnClickListener { filterOrders() }
            btnBarMakabidhiano?.setOnClickListener { showDetailedMakabidhianoDialog() }
            btnBarSalesReport?.setOnClickListener { showSalesReportWithDateFilterDialog() }
            btnResetPasswordBar?.setOnClickListener { showResetPasswordDialog() }

            etBarReceiptSearch?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { filterOrders() }
                override fun afterTextChanged(s: Editable?) {}
            })

            fetchBarOrders()
        } catch (e: Exception) {
            Log.e("BartenderCrash", "Error on onCreate: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        refreshHandler.post(refreshRunnable)
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

    private fun filterOrders() {
        val query = etBarReceiptSearch?.text?.toString()?.trim()?.lowercase() ?: ""

        if (query.isEmpty()) {
            displayedBarOrdersList.clear()
            displayedBarOrdersList.addAll(allBarOrdersList)
        } else {
            val filtered = allBarOrdersList.filter { order ->
                val token = order.token_number?.lowercase() ?: ""
                val customer = order.customerName.lowercase()
                token.contains(query) || customer.contains(query)
            }
            displayedBarOrdersList.clear()
            displayedBarOrdersList.addAll(filtered)
        }
        adapter.notifyDataSetChanged()
    }

    private fun isDrinkItem(itemsList: List<OrderItemDetail>): Boolean {
        if (itemsList.isEmpty()) return true

        return itemsList.any { item ->
            val cat = (item.category ?: "").lowercase()
            val name = (item.name ?: "").lowercase()

            cat.contains("bar") || cat.contains("vinywaji") || cat.contains("drink") ||
                    name.contains("bia") || name.contains("soda") || name.contains("juic") ||
                    name.contains("maji") || name.contains("wine") || name.contains("whisky")
        }
    }

    private fun fetchBarOrders() {
        if (isFinishing || isDestroyed) return

        try {
            val apiService = getRetrofit().create(ApiService::class.java)

            apiService.getOrders().enqueue(object : Callback<List<OrderResponseItem>> {
                override fun onResponse(call: Call<List<OrderResponseItem>>, response: Response<List<OrderResponseItem>>) {
                    if (isFinishing || isDestroyed) return

                    if (response.isSuccessful && response.body() != null) {
                        try {
                            val rawOrders = response.body()!!

                            completedBarOrdersCount = 0
                            totalBarSalesAmount = 0.0

                            rawOrders.forEach { order ->
                                val st = (order.status ?: "").lowercase()
                                val hasDrinks = isDrinkItem(order.items)
                                if ((st == "ready" || st == "completed" || st == "accepted_by_bar") && hasDrinks) {
                                    completedBarOrdersCount++
                                    totalBarSalesAmount += order.total_amount
                                }
                            }

                            val drinkOrders = rawOrders.filter { order ->
                                val st = (order.status ?: "Pending").trim().lowercase()
                                val hasDrinks = isDrinkItem(order.items)
                                val isNotRejected = !st.contains("reject")
                                val isNotReady = st != "ready" && st != "completed"

                                (st == "pending" || st == "preparing" || st.isEmpty()) && hasDrinks && isNotRejected && isNotReady
                            }

                            allBarOrdersList.clear()
                            allBarOrdersList.addAll(drinkOrders)
                            filterOrders()
                        } catch (e: Exception) {
                            Log.e("BartenderFetchError", "Error parsing list: ${e.message}")
                        }
                    }
                }

                override fun onFailure(call: Call<List<OrderResponseItem>>, t: Throwable) {
                    Log.e("BartenderNetworkError", "Failure: ${t.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("BartenderFetchCrash", "Error: ${e.message}")
        }
    }

    private fun updateOrderStatus(orderId: String, newStatus: String) {
        if (isFinishing || isDestroyed || orderId.isEmpty()) return

        val apiService = getRetrofit().create(ApiService::class.java)
        val statusData = hashMapOf<String, Any>("status" to newStatus)

        apiService.updateOrderStatus(orderId, statusData).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (isFinishing || isDestroyed) return

                if (response.isSuccessful) {
                    Toast.makeText(this@BartenderActivity, "Kinywaji Kiko Tayari!", Toast.LENGTH_SHORT).show()
                    fetchBarOrders()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {}
        })
    }

    private fun showRejectOrderDialog(order: OrderResponseItem) {
        val orderId = order.id
        if (orderId.isNullOrEmpty()) {
            Toast.makeText(this, "ID ya oda haikupatikana!", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("❌ Kurudisha Oda #${order.token_number ?: ""} kwa Cashier")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        val input = EditText(this).apply {
            hint = "Ingiza sababu ya kurudisha..."
            setTextColor(Color.parseColor("#FFFFFF"))
            setHintTextColor(Color.parseColor("#94A3B8"))
            textSize = 16f
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.parseColor("#334155"))
        }
        layout.addView(input)
        builder.setView(layout)

        builder.setPositiveButton("RUDISHA KWA CASHIER") { dialog, _ ->
            val reason = input.text.toString().trim().ifEmpty { "Kinywaji kimeisha bar" }

            val apiService = getRetrofit().create(ApiService::class.java)
            val rejectData = hashMapOf<String, Any>(
                "rejection_reason" to reason,
                "department" to "Bar"
            )

            apiService.rejectOrder(orderId.trim(), rejectData).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (isFinishing || isDestroyed) return

                    if (response.isSuccessful) {
                        Toast.makeText(this@BartenderActivity, "Oda imerudishwa kwa Cashier!", Toast.LENGTH_SHORT).show()
                        fetchBarOrders()
                    } else {
                        Toast.makeText(this@BartenderActivity, "Haikuweza kurudisha oda!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@BartenderActivity, "Hitilafu ya Mtandao!", Toast.LENGTH_SHORT).show()
                }
            })
            dialog.dismiss()
        }

        builder.setNegativeButton("GHAIRI", null)
        builder.show()
    }

    private fun showDetailedMakabidhianoDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("📋 Makabidhiano ya Zamu (Bar)")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 20)
        }

        val etOutgoingStaff = EditText(this).apply {
            hint = "Jina la Aliyekuwepo Zamu"
            setTextColor(Color.parseColor("#0F172A"))
            setHintTextColor(Color.parseColor("#64748B"))
        }
        val etIncomingStaff = EditText(this).apply {
            hint = "Jina la Anayekabidhiwa Zamu"
            setTextColor(Color.parseColor("#0F172A"))
            setHintTextColor(Color.parseColor("#64748B"))
        }

        val tvStats = TextView(this).apply {
            text = "\n📊 MUHTASARI WA ZAMU:\n• Oda Zilizotoka: $completedBarOrdersCount\n• Oda Zinazosubiri: ${allBarOrdersList.size}\n"
            setTextColor(Color.parseColor("#C2410C"))
            textSize = 14f
        }

        layout.addView(etOutgoingStaff)
        layout.addView(etIncomingStaff)
        layout.addView(tvStats)
        builder.setView(layout)

        builder.setPositiveButton("THIBITISHA") { dialog, _ ->
            val outgoing = etOutgoingStaff.text.toString().trim()
            val incoming = etIncomingStaff.text.toString().trim()

            if (outgoing.isEmpty() || incoming.isEmpty()) {
                Toast.makeText(this, "Jaza majina ya watumishi wote!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            tvActiveBartender?.text = "Bartender Zamu Hii: $incoming"
            Toast.makeText(this, "Makabidhiano yamethibitishwa!", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }
        builder.setNegativeButton("FUNGA", null)
        builder.show()
    }

    private fun showSalesReportWithDateFilterDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("📊 Taarifa ya Mauzo ya Bar")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val calendar = Calendar.getInstance()
        val todayStr = String.format(Locale.US, "%d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))

        var selectedStartDate = todayStr
        var selectedEndDate = todayStr

        val btnStartDate = Button(this).apply { text = "Tarehe ya Anzia: $selectedStartDate" }
        val btnEndDate = Button(this).apply { text = "Tarehe ya Mwisho: $selectedEndDate" }

        val tvResult = TextView(this).apply {
            text = "\n🔄 Chagua tarehe kisha bonyeza 'Tafuta Taarifa'"
            setTextColor(Color.parseColor("#0F172A"))
            textSize = 15f
        }

        btnStartDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedStartDate = String.format(Locale.US, "%d-%02d-%02d", year, month + 1, dayOfMonth)
                btnStartDate.text = "Tarehe ya Anzia: $selectedStartDate"
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnEndDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedEndDate = String.format(Locale.US, "%d-%02d-%02d", year, month + 1, dayOfMonth)
                btnEndDate.text = "Tarehe ya Mwisho: $selectedEndDate"
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        layout.addView(btnStartDate)
        layout.addView(btnEndDate)
        layout.addView(tvResult)
        builder.setView(layout)

        builder.setPositiveButton("TAFUTA TAARIFA") { dialog, _ ->
            val apiService = getRetrofit().create(ApiService::class.java)

            apiService.getBarSales(selectedStartDate, selectedEndDate).enqueue(object : Callback<BarSalesResponse> {
                override fun onResponse(call: Call<BarSalesResponse>, response: Response<BarSalesResponse>) {
                    if (isFinishing || isDestroyed) return
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!

                        val summaryText = """
                            📊 MUHTASARI WA MAUZO:
                            📅 Vipindi: $selectedStartDate hadi $selectedEndDate
                            -----------------------------------
                            • Jumla ya Oda Zilizokamilika: ${data.completed_orders}
                            • Thamani ya Mauzo (Bar): TSH ${String.format("%,.0f", data.drinks_total)}
                        """.trimIndent()

                        AlertDialog.Builder(this@BartenderActivity)
                            .setTitle("📈 Matokeo ya Mauzo")
                            .setMessage(summaryText)
                            .setPositiveButton("SAWA", null)
                            .show()
                    } else {
                        Toast.makeText(this@BartenderActivity, "Imeshindikana kupata takwimu", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<BarSalesResponse>, t: Throwable) {
                    if (isFinishing || isDestroyed) return
                    Toast.makeText(this@BartenderActivity, "Hitilafu ya mtandao!", Toast.LENGTH_SHORT).show()
                }
            })
            dialog.dismiss()
        }
        builder.setNegativeButton("FUNGA", null)
        builder.show()
    }

    private fun showResetPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("🔑 Badili / Reset Password")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val etEmail = EditText(this).apply { hint = "Ingiza Barua Pepe (Email)" }
        val etOldPass = EditText(this).apply { hint = "Password ya Zamani" }
        val etNewPass = EditText(this).apply { hint = "Password Mpya" }

        layout.addView(etEmail)
        layout.addView(etOldPass)
        layout.addView(etNewPass)
        builder.setView(layout)

        builder.setPositiveButton("BADILISHA") { dialog, _ ->
            val email = etEmail.text.toString().trim()
            val oldPass = etOldPass.text.toString().trim()
            val newPass = etNewPass.text.toString().trim()

            if (email.isEmpty() || oldPass.isEmpty() || newPass.isEmpty()) {
                Toast.makeText(this, "Jaza taarifa zote!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val apiService = getRetrofit().create(ApiService::class.java)
            val passData = hashMapOf<String, Any>(
                "email" to email,
                "old_password" to oldPass,
                "new_password" to newPass
            )

            apiService.changePassword(passData).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@BartenderActivity, "Password imebadilishwa kikamilifu!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@BartenderActivity, "Imeshindikana!", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@BartenderActivity, "Hitilafu ya mtandao!", Toast.LENGTH_SHORT).show()
                }
            })
            dialog.dismiss()
        }
        builder.setNegativeButton("GHAIRI", null)
        builder.show()
    }
}