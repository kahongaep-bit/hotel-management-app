package com.hotelmanagementsystem

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.*
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

class ProductionManagerActivity : AppCompatActivity() {

    private lateinit var rvPendingApprovals: RecyclerView
    private lateinit var btnRefresh: Button
    private lateinit var btnMainStore: Button
    private lateinit var btnSubStore: Button
    private lateinit var tvDailySales: TextView
    private lateinit var tvBankDeposit: TextView
    private lateinit var btnResetPassword: Button
    private lateinit var btnViewFinancialReports: Button

    private var pendingRequisitionsDialog: AlertDialog? = null

    private val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://hotel-backend-production-617c.up.railway.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_production_manager)

        rvPendingApprovals = findViewById(R.id.rvPendingApprovals)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnMainStore = findViewById(R.id.btnMainStore)
        btnSubStore = findViewById(R.id.btnSubStore)
        tvDailySales = findViewById(R.id.tvDailySales)
        tvBankDeposit = findViewById(R.id.tvBankDeposit)
        btnResetPassword = findViewById(R.id.btnResetPassword)
        btnViewFinancialReports = findViewById(R.id.btnViewFinancialReports)

        rvPendingApprovals.layoutManager = LinearLayoutManager(this)

        btnRefresh.setOnClickListener {
            fetchPendingApprovals()
            fetchDashboardSalesData()
        }

        btnResetPassword.setOnClickListener { showResetPasswordDialog() }
        btnMainStore.setOnClickListener { showStockItemsDialog("Main Store", "/api/stock/main") }
        btnSubStore.setOnClickListener { showStockItemsDialog("Sub-Store (Hoteli)", "/api/stock/sub") }

        btnViewFinancialReports.setOnClickListener { showFinancialReportDialog() }
        tvDailySales.setOnClickListener { showFinancialReportDialog() }
        tvBankDeposit.setOnClickListener { showFinancialReportDialog() }

        fetchPendingApprovals()
        fetchDashboardSalesData()
    }

    override fun onResume() {
        super.onResume()
        fetchPendingApprovals()
        fetchDashboardSalesData()
    }

    private fun fetchDashboardSalesData() {
        apiService.getDailySales().enqueue(object : Callback<DailySalesResponse> {
            override fun onResponse(call: Call<DailySalesResponse>, response: Response<DailySalesResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val sales = response.body()!!
                    tvDailySales.text = "TSH ${String.format("%,.0f", sales.gross_total)}"
                    tvBankDeposit.text = "TSH ${String.format("%,.0f", sales.total_deposited)}"
                }
            }
            override fun onFailure(call: Call<DailySalesResponse>, t: Throwable) {}
        })
    }

    private fun showResetPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("🔑 Badili Password yako")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val etEmail = EditText(this).apply { hint = "Email yako" }
        val etOldPass = EditText(this).apply {
            hint = "Password ya Sasa (Zamani)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val etNewPass = EditText(this).apply {
            hint = "Password Mpya"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(etEmail)
        layout.addView(etOldPass)
        layout.addView(etNewPass)
        builder.setView(layout)

        builder.setPositiveButton("BADILI") { dialog, _ ->
            val email = etEmail.text.toString().trim()
            val oldPass = etOldPass.text.toString().trim()
            val newPass = etNewPass.text.toString().trim()

            if (email.isNotEmpty() && oldPass.isNotEmpty() && newPass.isNotEmpty()) {
                val body = hashMapOf<String, Any>(
                    "email" to email,
                    "old_password" to oldPass,
                    "new_password" to newPass
                )

                apiService.changePassword(body).enqueue(object : Callback<GenericResponse> {
                    override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@ProductionManagerActivity, "Password imebadilishwa kikamilifu!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@ProductionManagerActivity, "Taarifa sio sahihi!", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                        Toast.makeText(this@ProductionManagerActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this, "Jaza sehemu zote!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("GHAIRI", null)
        builder.show()
    }

    private fun showFinancialReportDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("📊 Ripoti za Kihasibu na Mchanganuo wa Benki")

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 30)
            setBackgroundColor(Color.WHITE)
        }

        var selectedStartDate: String? = null
        var selectedEndDate: String? = null

        val btnStartDate = Button(this).apply {
            text = "Tarehe ya Kuanzia: Chagua"
            setBackgroundColor(Color.parseColor("#E2E8F0"))
            setTextColor(Color.parseColor("#1A202C"))
            textSize = 13f
        }
        layout.addView(btnStartDate)

        val btnEndDate = Button(this).apply {
            text = "Tarehe ya Mwisho: Chagua"
            setBackgroundColor(Color.parseColor("#E2E8F0"))
            setTextColor(Color.parseColor("#1A202C"))
            textSize = 13f
        }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 10, 0, 10) }
        btnEndDate.layoutParams = params
        layout.addView(btnEndDate)

        val btnFetch = Button(this).apply {
            text = "🔍 TAFUTA RIPOTI"
            setBackgroundColor(Color.parseColor("#3182CE"))
            setTextColor(Color.WHITE)
            textSize = 14f
        }
        val fetchParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 6, 0, 16) }
        btnFetch.layoutParams = fetchParams
        layout.addView(btnFetch)

        val tvReport = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.BLACK)
            text = "Inapakua taarifa..."
            setLineSpacing(8f, 1f)
            setPadding(8, 8, 8, 8)
        }
        layout.addView(tvReport)

        scrollView.addView(layout)
        builder.setView(scrollView)
        builder.setNegativeButton("FUNGA", null)

        val dialog = builder.create()
        dialog.show()

        fun loadReportData(start: String?, end: String?) {
            tvReport.setTextColor(Color.parseColor("#2B6CB0"))
            tvReport.text = "⏳ Inapakua taarifa za kihasibu..."

            val cleanStart = if (!start.isNullOrBlank()) start.trim() else null
            val cleanEnd = if (!end.isNullOrBlank()) end.trim() else null

            apiService.getFinanceReports(cleanStart, cleanEnd).enqueue(object : Callback<FinanceReportResponse> {
                override fun onResponse(call: Call<FinanceReportResponse>, response: Response<FinanceReportResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val report = response.body()!!
                        val rangeInfo = if (cleanStart != null && cleanEnd != null) " ($cleanStart hadi $cleanEnd)" else " (Jumla Yote)"

                        tvReport.setTextColor(Color.BLACK)
                        tvReport.text = """
                            📌 RIPOTI YA KIHASIBU NA MCHANGANUO$rangeInfo:
                            
                            📈 Jumla ya Mauzo: TSH ${String.format("%,.0f", report.total_sales)}
                            💵 Cash Total: TSH ${String.format("%,.0f", report.cash_sales)}
                            📱 Lipa Namba Total: TSH ${String.format("%,.0f", report.lipanamba_sales)}
                            
                            🏦 Jumla Iliyokwenda Benki: TSH ${String.format("%,.0f", report.total_deposits)}
                            💰 Balance (Haijapelekwa Benki): TSH ${String.format("%,.0f", report.balance)}
                            
                            ──────────────────────────
                            📊 MCHANGANUO WA BENKI KWA IDARA:
                            - 🍳 Breakfast: TSH ${String.format("%,.0f", report.breakfast_deposits)}
                            - 🍲 Lunch: TSH ${String.format("%,.0f", report.lunch_deposits)}
                            - 🍝 Dinner: TSH ${String.format("%,.0f", report.dinner_deposits)}
                            - 🥤 Vinywaji/Bar: TSH ${String.format("%,.0f", report.drinks_deposits)}
                            - 🏨 Vyumba na Kumbi: TSH ${String.format("%,.0f", report.rooms_deposits)}
                        """.trimIndent()
                    } else {
                        tvReport.setTextColor(Color.BLACK)
                        tvReport.text = "📌 RIPOTI YA KIHASIBU:\n\n• Taarifa hazijapatikana."
                    }
                }

                override fun onFailure(call: Call<FinanceReportResponse>, t: Throwable) {
                    tvReport.setTextColor(Color.BLACK)
                    tvReport.text = "📌 RIPOTI YA KIHASIBU:\n\n• Hitilafu ya mtandao."
                }
            })
        }

        btnStartDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedStartDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                btnStartDate.text = "KUANZIA: $selectedStartDate"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnEndDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedEndDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                btnEndDate.text = "HADI: $selectedEndDate"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        loadReportData(null, null)
        btnFetch.setOnClickListener { loadReportData(selectedStartDate, selectedEndDate) }
    }

    private fun fetchPendingApprovals() {
        pendingRequisitionsDialog?.dismiss()

        apiService.getRequisitions().enqueue(object : Callback<List<RequisitionItem>> {
            override fun onResponse(call: Call<List<RequisitionItem>>, response: Response<List<RequisitionItem>>) {
                if (response.isSuccessful && response.body() != null) {
                    val allList = response.body()!!

                    val pendingProduction = allList.filter {
                        val st = it.status ?: ""
                        st.equals("Pending", ignoreCase = true) ||
                                st.equals("Pending_Production", ignoreCase = true) ||
                                st.equals("Returned_To_Production", ignoreCase = true)
                    }

                    val adapter = ProcurementRequisitionAdapter(pendingProduction) { selectedReq ->
                        showApprovalActionDialog(selectedReq)
                    }
                    rvPendingApprovals.adapter = adapter
                }
            }

            override fun onFailure(call: Call<List<RequisitionItem>>, t: Throwable) {
                Toast.makeText(this@ProductionManagerActivity, "Hitilafu ya mtandao: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showApprovalActionDialog(requisition: RequisitionItem) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("📋 Hakiki Ombi la Uzalishaji")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 20)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        val rawDate = requisition.created_at ?: ""
        val formattedDate = if (rawDate.length >= 10) rawDate.substring(0, 10) else rawDate

        val ratioInfo = if (requisition.ratio_per_unit != null && requisition.ratio_per_unit!! > 0 && (requisition.department ?: "").contains("Jikoni", ignoreCase = true)) {
            "\n• Uwiano wa Ratio: 1 ${requisition.unit ?: ""} = Oda ${requisition.ratio_per_unit?.toInt()}\n• Kadirio la Sahani/Oda: ${requisition.total_portions?.toInt()} Oda"
        } else ""

        val commentInfo = if (!requisition.rejection_comment.isNullOrBlank()) {
            "\n\n💬 Sababu/Comment:\n\"${requisition.rejection_comment}\""
        } else ""

        val detailsText = "• Bidhaa: ${requisition.item_name}\n" +
                "• Kiasi: ${requisition.quantity} ${requisition.unit ?: ""}\n" +
                "• Idara: ${requisition.department ?: "Jikoni"}\n" +
                "• Tarehe ya Ombi: $formattedDate\n" +
                "• Aliyeomba: ${requisition.requested_by ?: "Hotel Manager"}" + ratioInfo + commentInfo

        val tvDetails = TextView(this).apply {
            text = detailsText
            setTextColor(Color.WHITE)
            textSize = 14f
            setLineSpacing(6f, 1f)
        }
        layout.addView(tvDetails)

        builder.setView(layout)

        builder.setPositiveButton("✅ IDHINISHA (KWEA PROC)") { dialog, _ ->
            updateStatus(requisition.id ?: "", "Approved_Production", null)
            dialog.dismiss()
        }

        builder.setNegativeButton("❌ KATAA / RUDISHA") { dialog, _ ->
            dialog.dismiss()
            showRejectDialog(requisition)
        }

        builder.setNeutralButton("GHAIRI", null)

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#48BB78"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#F56565"))
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(Color.WHITE)
    }

    private fun showRejectDialog(requisition: RequisitionItem) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("❌ Kataa Ombi")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        val etComment = EditText(this).apply {
            hint = "Andika sababu ya kukataa..."
            setHintTextColor(Color.parseColor("#CBD5E1"))
            setTextColor(Color.WHITE)
        }
        layout.addView(etComment)
        builder.setView(layout)

        builder.setPositiveButton("THIBITISHA KUKATAA") { dialog, _ ->
            val comment = etComment.text.toString().trim()
            if (comment.isEmpty()) {
                Toast.makeText(this, "Weka sababu ya kukataa!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            updateStatus(requisition.id ?: "", "Rejected_Production", comment)
            dialog.dismiss()
        }

        builder.setNegativeButton("GHAIRI", null)
        builder.show()
    }

    private fun updateStatus(id: String, status: String, comment: String?) {
        val updateData = hashMapOf<String, Any>(
            "status" to status
        )
        if (comment != null) updateData["rejection_comment"] = comment

        apiService.updateRequisitionApproval(id, updateData).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ProductionManagerActivity, "Status imebadilishwa kikamilifu!", Toast.LENGTH_SHORT).show()
                    fetchPendingApprovals()
                } else {
                    Toast.makeText(this@ProductionManagerActivity, "Imeshindwa kusasisha status!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@ProductionManagerActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showStockItemsDialog(storeName: String, endpoint: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("📦 Orodha ya $storeName")

        val tvLoading = TextView(this).apply {
            text = "⏳ Inapakua bidhaa za $storeName..."
            setPadding(40, 30, 40, 30)
        }
        builder.setView(tvLoading)
        builder.setPositiveButton("Funga", null)

        val dialog = builder.create()
        dialog.show()

        val fullUrl = "https://hotel-backend-production-617c.up.railway.app/$endpoint"
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder().url(fullUrl).build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val responseData = response.body()?.string()

                if (response.isSuccessful && responseData != null) {
                    val jsonArray = org.json.JSONArray(responseData)
                    val itemList = ArrayList<String>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val name = obj.optString("item_name", "N/A")
                        val qty = obj.optDouble("quantity", 0.0)
                        val unit = obj.optString("unit", "")
                        itemList.add("• $name: $qty $unit")
                    }

                    runOnUiThread {
                        tvLoading.text = if (itemList.isEmpty()) "Hakuna bidhaa kwenye $storeName." else itemList.joinToString("\n")
                    }
                } else {
                    runOnUiThread { tvLoading.text = "❌ Imeshindwa kupakua bidhaa za $storeName." }
                }
            } catch (e: Exception) {
                runOnUiThread { tvLoading.text = "❌ Hitilafu ya mtandao: ${e.message}" }
            }
        }.start()
    }
}