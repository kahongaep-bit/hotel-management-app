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

class PrincipalDashboardActivity : AppCompatActivity() {

    private lateinit var tvTodayRevenue: TextView
    private lateinit var tvTodayDeposits: TextView
    private lateinit var rvPrincipalApprovals: RecyclerView
    private lateinit var btnRefresh: Button
    private lateinit var btnViewMainStore: Button
    private lateinit var btnViewSubStore: Button // Imeunganishwa kuwa moja kama Procurement
    private lateinit var btnFilterFinancialReport: Button
    private lateinit var btnTrackAllRequisitions: Button
    private lateinit var btnProcuredReport: Button
    private lateinit var btnIssuedReport: Button
    private lateinit var btnChangePassword: Button

    private var currentUserId: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal_dashboard)

        currentUserId = intent.getIntExtra("USER_ID", 1)

        tvTodayRevenue = findViewById(R.id.tvTodayRevenue)
        tvTodayDeposits = findViewById(R.id.tvTodayDeposits)
        rvPrincipalApprovals = findViewById(R.id.rvPrincipalApprovals)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnViewMainStore = findViewById(R.id.btnViewMainStore)
        btnViewSubStore = findViewById(R.id.btnViewSubStore) // Imeunganishwa ID mpya ya XML
        btnFilterFinancialReport = findViewById(R.id.btnFilterFinancialReport)
        btnTrackAllRequisitions = findViewById(R.id.btnTrackAllRequisitions)
        btnProcuredReport = findViewById(R.id.btnProcuredReport)
        btnIssuedReport = findViewById(R.id.btnIssuedReport)
        btnChangePassword = findViewById(R.id.btnChangePassword)

        rvPrincipalApprovals.layoutManager = LinearLayoutManager(this)

        btnRefresh.setOnClickListener {
            fetchExecutiveReports()
            fetchPendingFinalApprovals()
        }

        btnViewMainStore.setOnClickListener {
            showStockDialog("Stoo Kuu ya Chuo (Main Store)", isMainStore = true)
        }

        // Muonekano mmoja wa SubStore unaofungua chaguo la Jikoni na Bar (Sawa na Procurement)
        btnViewSubStore.setOnClickListener {
            showUnifiedSubStoreChoiceDialog()
        }

        btnFilterFinancialReport.setOnClickListener {
            showFilteredFinancialReportDialog()
        }

        btnTrackAllRequisitions.setOnClickListener {
            showAllRequisitionsTrackingDialog()
        }

        btnProcuredReport.setOnClickListener {
            showProcuredReportDialog()
        }

        btnIssuedReport.setOnClickListener {
            showStockIssueReportDialog()
        }

        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        fetchExecutiveReports()
        fetchPendingFinalApprovals()
    }

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://10.32.78.51:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun fetchExecutiveReports() {
        val apiService = getRetrofit().create(ApiService::class.java)

        apiService.getDailySales().enqueue(object : Callback<DailySalesResponse> {
            override fun onResponse(call: Call<DailySalesResponse>, response: Response<DailySalesResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val sales = response.body()!!
                    tvTodayRevenue.text = "TZS ${String.format("%,.0f", sales.gross_total)}"
                    tvTodayDeposits.text = "TZS ${String.format("%,.0f", sales.total_deposited)}"
                } else {
                    tvTodayRevenue.text = "TZS 0"
                    tvTodayDeposits.text = "TZS 0"
                }
            }

            override fun onFailure(call: Call<DailySalesResponse>, t: Throwable) {
                tvTodayRevenue.text = "TZS 0"
                tvTodayDeposits.text = "TZS 0"
            }
        })
    }

    // Kurekebisha Password ya Principal iweze kufanya kazi sawa na akaunti zingine
    private fun showChangePasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("🔑 Badilisha Nenosiri (Password)")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 20)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        val etEmail = EditText(this).apply {
            hint = "Weka Barua Pepe (Email) Yako"
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        val etCurrentPassword = EditText(this).apply {
            hint = "Password ya Sasa"
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val etNewPassword = EditText(this).apply {
            hint = "Password Mpya"
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(etEmail)
        layout.addView(etCurrentPassword)
        layout.addView(etNewPassword)
        builder.setView(layout)

        builder.setPositiveButton("BADILISHA") { dialog, _ ->
            val email = etEmail.text.toString().trim()
            val oldPass = etCurrentPassword.text.toString().trim()
            val newPass = etNewPassword.text.toString().trim()

            if (email.isEmpty() || oldPass.isEmpty() || newPass.isEmpty()) {
                Toast.makeText(this, "Tafadhali jaza sehemu zote!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            updatePasswordInServer(email, oldPass, newPass)
            dialog.dismiss()
        }

        builder.setNegativeButton("GHAIRI", null)
        builder.show()
    }

    private fun updatePasswordInServer(email: String, oldPass: String, newPass: String) {
        val apiService = getRetrofit().create(ApiService::class.java)
        // Kutumia muundo sahihi wa kupokea email na password za zamani/mpya kama ilivyo kwenye system
        val requestData = hashMapOf<String, Any>(
            "email" to email,
            "old_password" to oldPass,
            "new_password" to newPass
        )

        apiService.changePassword(requestData).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@PrincipalDashboardActivity, "Password imebadilishwa kikamilifu!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@PrincipalDashboardActivity, "Imeshindikana! Hakiki Email au Password ya sasa.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@PrincipalDashboardActivity, "Hitilafu ya mtandao: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchPendingFinalApprovals() {
        val apiService = getRetrofit().create(ApiService::class.java)

        apiService.getRequisitionsByStatus("Approved_Finance").enqueue(object : Callback<List<RequisitionItem>> {
            override fun onResponse(call: Call<List<RequisitionItem>>, response: Response<List<RequisitionItem>>) {
                if (response.isSuccessful && response.body() != null) {
                    val allList = response.body()!!
                    val pendingList = allList.filter {
                        it.status.equals("Approved_Finance", ignoreCase = true)
                    }
                    setupRequisitionAdapter(pendingList)
                } else {
                    setupRequisitionAdapter(emptyList())
                }
            }

            override fun onFailure(call: Call<List<RequisitionItem>>, t: Throwable) {
                setupRequisitionAdapter(emptyList())
            }
        })
    }

    private fun setupRequisitionAdapter(requisitions: List<RequisitionItem>) {
        val adapter = ProcurementRequisitionAdapter(requisitions) { selectedReq ->
            showFinalApprovalDialog(selectedReq)
        }
        rvPrincipalApprovals.adapter = adapter
    }

    private fun showFinalApprovalDialog(requisition: RequisitionItem) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("📋 Hakiki Ombi la Ununuzi (Principal)")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 20)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        val ratioVal = requisition.ratio_per_unit ?: 0.0
        val ratioInfo = if (ratioVal > 0 && (requisition.department ?: "").contains("Jikoni", ignoreCase = true)) {
            val portionsVal = requisition.total_portions ?: 0.0
            "\n• Uwiano wa Ratio: 1 ${requisition.unit ?: ""} = Oda ${ratioVal.toInt()}\n• Kadirio la Sahani/Oda: ${portionsVal.toInt()} Oda"
        } else ""

        val commentInfo = if (!requisition.rejection_comment.isNullOrBlank()) {
            "\n\n💬 Sababu/Comment ya Zamani:\n\"${requisition.rejection_comment}\""
        } else ""

        val supplierInfo = requisition.supplier ?: "Haijaainishwa"
        val costInfo = if (requisition.estimated_cost != null && requisition.estimated_cost!! > 0) "TZS ${String.format("%,.0f", requisition.estimated_cost)}" else "Haijaainishwa"

        val detailsText = "• Idara: ${requisition.department ?: "Jikoni"}\n" +
                "• Bidhaa: ${requisition.item_name}\n" +
                "• Kiasi: ${requisition.quantity} ${requisition.unit ?: ""}\n" +
                "• Mzabuni/Supplier: $supplierInfo\n" +
                "• Gharama ya Sokoni: $costInfo" + ratioInfo + commentInfo

        val tvDetails = TextView(this).apply {
            text = detailsText
            setTextColor(Color.WHITE)
            textSize = 14f
            setLineSpacing(6f, 1f)
        }
        layout.addView(tvDetails)
        builder.setView(layout)

        builder.setPositiveButton("✅ FINAL APPROVE") { dialog, _ ->
            approveFinalRequisition(requisition.id ?: "")
            dialog.dismiss()
        }

        builder.setNegativeButton("❌ RUDISHA KWA MHASIBU") { dialog, _ ->
            dialog.dismiss()
            showReturnToFinanceDialog(requisition)
        }

        builder.setNeutralButton("GHAIRI", null)

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#48BB78"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#F56565"))
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(Color.WHITE)
    }

    private fun showReturnToFinanceDialog(requisition: RequisitionItem) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("↩️ Rudisha Ombi kwa Mhasibu")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        val etComment = EditText(this).apply {
            hint = "Andika sababu ya kurudisha kwa Mhasibu..."
            setHintTextColor(Color.parseColor("#CBD5E1"))
            setTextColor(Color.WHITE)
        }
        layout.addView(etComment)
        builder.setView(layout)

        builder.setPositiveButton("THIBITISHA KURUDISHA") { dialog, _ ->
            val comment = etComment.text.toString().trim()
            if (comment.isEmpty()) {
                Toast.makeText(this, "Weka sababu ya kurudisha!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val updateData = hashMapOf<String, Any>(
                "status" to "Rejected_Principal",
                "rejection_comment" to comment
            )

            val apiService = getRetrofit().create(ApiService::class.java)
            apiService.updateRequisitionApproval(requisition.id ?: "", updateData).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@PrincipalDashboardActivity, "Ombi limerudishwa kwa Mhasibu!", Toast.LENGTH_SHORT).show()
                        fetchPendingFinalApprovals()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@PrincipalDashboardActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
            dialog.dismiss()
        }

        builder.setNegativeButton("GHAIRI", null)
        val dialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#F56565"))
    }

    private fun approveFinalRequisition(reqId: String) {
        val apiService = getRetrofit().create(ApiService::class.java)
        val updateData = hashMapOf<String, Any>("status" to "Approved_Principal")

        apiService.updateRequisitionApproval(reqId, updateData).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@PrincipalDashboardActivity, "Ombi limeidhinishwa kikamilifu!", Toast.LENGTH_LONG).show()
                    fetchPendingFinalApprovals()
                } else {
                    Toast.makeText(this@PrincipalDashboardActivity, "Imeshindwa kutoa idhini!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@PrincipalDashboardActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAllRequisitionsTrackingDialog() {
        val apiService = getRetrofit().create(ApiService::class.java)

        apiService.getAllRequisitions(true).enqueue(object : Callback<List<RequisitionItem>> {
            override fun onResponse(call: Call<List<RequisitionItem>>, response: Response<List<RequisitionItem>>) {
                if (response.isSuccessful && response.body() != null) {
                    val allList = response.body()!!

                    val builder = AlertDialog.Builder(this@PrincipalDashboardActivity)
                    builder.setTitle("🔍 Status ya Maombi Yote")

                    val scrollView = ScrollView(this@PrincipalDashboardActivity)
                    val layout = LinearLayout(this@PrincipalDashboardActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(40, 30, 40, 20)
                        setBackgroundColor(Color.parseColor("#1E293B"))
                    }

                    if (allList.isEmpty()) {
                        val tvEmpty = TextView(this@PrincipalDashboardActivity).apply {
                            text = "Hakuna maombi yoyote kwenye mfumo."
                            setTextColor(Color.WHITE)
                        }
                        layout.addView(tvEmpty)
                    } else {
                        for (item in allList) {
                            val tvItem = TextView(this@PrincipalDashboardActivity).apply {
                                val statusText = when (item.status) {
                                    "Pending" -> "⏳ Ipo kwa Production Coordinator"
                                    "Approved_Production" -> "🛍️ Ipo kwa Afisa Ununuzi (Procurement)"
                                    "Approved_Procurement" -> "💰 Ipo kwa Mhasibu (Finance)"
                                    "Approved_Finance" -> "👔 Inasubiri Idhini Yako (Principal)"
                                    "Approved_Principal" -> "✅ Inasubiri Kukamilisha Ununuzi"
                                    "Procured" -> "📦 Imeshununuliwa"
                                    else -> item.status ?: "Inashughulikiwa"
                                }

                                text = "• ${item.item_name} (${item.quantity} ${item.unit ?: ""})\n" +
                                        "  Idara: ${item.department ?: "Jikoni"} | Aliyeomba: ${item.requested_by ?: "Manager"}\n" +
                                        "  Status: $statusText\n"
                                textSize = 13f
                                setTextColor(Color.WHITE)
                                setPadding(0, 10, 0, 10)
                            }
                            layout.addView(tvItem)
                        }
                    }

                    scrollView.addView(layout)
                    builder.setView(scrollView)
                    builder.setPositiveButton("FUNGA", null)
                    builder.show()
                }
            }

            override fun onFailure(call: Call<List<RequisitionItem>>, t: Throwable) {}
        })
    }

    private fun showFilteredFinancialReportDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("📊 Ripoti ya Fedha na Mchanganuo wa Benki")

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
            textSize = 14f
        }
        layout.addView(btnStartDate)

        val btnEndDate = Button(this).apply {
            text = "Tarehe ya Mwisho: Chagua"
            setBackgroundColor(Color.parseColor("#E2E8F0"))
            setTextColor(Color.parseColor("#1A202C"))
            textSize = 14f
        }
        val endParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 10, 0, 10) }
        btnEndDate.layoutParams = endParams
        layout.addView(btnEndDate)

        val btnFetch = Button(this).apply {
            text = "🔍 TAFUTA RIPOTI YA FEDHA"
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

        val tvReportDetails = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.BLACK)
            text = "⏳ Inapakua ripoti ya fedha..."
            setLineSpacing(6f, 1f)
            setPadding(10, 10, 10, 10)
        }
        layout.addView(tvReportDetails)

        scrollView.addView(layout)
        builder.setView(scrollView)
        builder.setPositiveButton("FUNGA", null)

        val dialog = builder.create()
        dialog.show()

        fun loadFinancialData(start: String?, end: String?) {
            tvReportDetails.setTextColor(Color.parseColor("#2B6CB0"))
            tvReportDetails.text = "⏳ Inapakua ripoti ya fedha..."

            val cleanStart = if (!start.isNullOrBlank()) start.trim() else null
            val cleanEnd = if (!end.isNullOrBlank()) end.trim() else null

            val apiService = getRetrofit().create(ApiService::class.java)
            apiService.getFinanceReports(cleanStart, cleanEnd).enqueue(object : Callback<FinanceReportResponse> {
                override fun onResponse(call: Call<FinanceReportResponse>, response: Response<FinanceReportResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val report = response.body()!!
                        val rangeInfo = if (cleanStart != null && cleanEnd != null) " ($cleanStart hadi $cleanEnd)" else " (Kipindi Yote)"

                        tvReportDetails.setTextColor(Color.BLACK)
                        tvReportDetails.text = """
                            📌 RIPOTI YA KIHASIBU$rangeInfo:
                            
                            • Jumla ya Mauzo: TSH ${String.format("%,.0f", report.total_sales)}
                            • 💵 Cash Total: TSH ${String.format("%,.0f", report.cash_sales)}
                            • 📱 Lipa Namba: TSH ${String.format("%,.0f", report.lipanamba_sales)}
                            • Idadi ya Oda: ${report.total_orders}
                            • 🏦 Pesa Iliyokwenda Benki: TSH ${String.format("%,.0f", report.total_deposits)}
                            • 💰 Balance (Iliyopo): TSH ${String.format("%,.0f", report.balance)}
                        """.trimIndent()
                    } else {
                        tvReportDetails.text = "Imeshindwa kupakua ripoti ya fedha."
                    }
                }

                override fun onFailure(call: Call<FinanceReportResponse>, t: Throwable) {
                    tvReportDetails.text = "Hitilafu: ${t.message}"
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

        loadFinancialData(null, null)
        btnFetch.setOnClickListener { loadFinancialData(selectedStartDate, selectedEndDate) }
    }

    private fun showProcuredReportDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("📦 Ripoti ya Bidhaa Zilizonunuliwa")

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 30)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        var selectedStartDate: String? = null
        var selectedEndDate: String? = null

        val btnStartDate = Button(this).apply {
            text = "Tarehe ya Kuanzia: Chagua"
            setBackgroundColor(Color.parseColor("#2D3748"))
            setTextColor(Color.WHITE)
            textSize = 13f
        }
        layout.addView(btnStartDate)

        val btnEndDate = Button(this).apply {
            text = "Tarehe ya Mwisho: Chagua"
            setBackgroundColor(Color.parseColor("#2D3748"))
            setTextColor(Color.WHITE)
            textSize = 13f
        }
        val endParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 10, 0, 10) }
        btnEndDate.layoutParams = endParams
        layout.addView(btnEndDate)

        val btnFetch = Button(this).apply {
            text = "🔍 FILTER PROCURED ITEMS"
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

        val tvDetails = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.WHITE)
            text = "⏳ Inapakua taarifa..."
            setLineSpacing(6f, 1f)
            setPadding(10, 10, 10, 10)
        }
        layout.addView(tvDetails)

        scrollView.addView(layout)
        builder.setView(scrollView)
        builder.setPositiveButton("FUNGA", null)

        val dialog = builder.create()
        dialog.show()

        fun loadProcuredData(start: String?, end: String?) {
            tvDetails.text = "⏳ Inapakua ripoti..."
            val cleanStart = if (!start.isNullOrBlank()) start.trim() else null
            val cleanEnd = if (!end.isNullOrBlank()) end.trim() else null

            val apiService = getRetrofit().create(ApiService::class.java)
            apiService.getProcuredItemsReport(cleanStart, cleanEnd).enqueue(object : Callback<List<RequisitionItem>> {
                override fun onResponse(call: Call<List<RequisitionItem>>, response: Response<List<RequisitionItem>>) {
                    if (response.isSuccessful && response.body() != null) {
                        val reportList = response.body()!!
                        if (reportList.isEmpty()) {
                            tvDetails.text = "Hakuna kumbukumbu."
                            return
                        }
                        val sb = StringBuilder("📌 RIPOTI YA UNUNUZI:\n\n")
                        var grandTotal = 0.0
                        for (item in reportList) {
                            val cost = item.estimated_cost ?: 0.0
                            grandTotal += cost
                            sb.append("• ${item.item_name}: ${item.quantity} ${item.unit ?: ""}\n")
                            sb.append("  - Mzabuni: ${item.supplier ?: "N/A"} | TZS ${String.format("%,.0f", cost)}\n\n")
                        }
                        sb.append("----------------------------------\n")
                        sb.append("💰 JUMLA: TZS ${String.format("%,.0f", grandTotal)}")
                        tvDetails.text = sb.toString()
                    }
                }
                override fun onFailure(call: Call<List<RequisitionItem>>, t: Throwable) {
                    tvDetails.text = "Hitilafu: ${t.message}"
                }
            })
        }

        btnStartDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                selectedStartDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                btnStartDate.text = "KUANZIA: $selectedStartDate"
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnEndDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                selectedEndDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                btnEndDate.text = "HADI: $selectedEndDate"
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        loadProcuredData(null, null)
        btnFetch.setOnClickListener { loadProcuredData(selectedStartDate, selectedEndDate) }
    }

    private fun showStockIssueReportDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("📊 Ripoti ya Bidhaa Zilizotolewa Stoo")

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 30)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        var selectedStartDate: String? = null
        var selectedEndDate: String? = null

        val btnStartDate = Button(this).apply {
            text = "Tarehe ya Kuanzia: Chagua"
            setBackgroundColor(Color.parseColor("#2D3748"))
            setTextColor(Color.WHITE)
            textSize = 13f
        }
        layout.addView(btnStartDate)

        val btnEndDate = Button(this).apply {
            text = "Tarehe ya Mwisho: Chagua"
            setBackgroundColor(Color.parseColor("#2D3748"))
            setTextColor(Color.WHITE)
            textSize = 13f
        }
        val endParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 10, 0, 10) }
        btnEndDate.layoutParams = endParams
        layout.addView(btnEndDate)

        val btnFetch = Button(this).apply {
            text = "🔍 FILTER STOCK ISSUES"
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

        val tvDetails = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.WHITE)
            text = "⏳ Inapakua taarifa..."
            setPadding(10, 10, 10, 10)
        }
        layout.addView(tvDetails)

        scrollView.addView(layout)
        builder.setView(scrollView)
        builder.setPositiveButton("FUNGA", null)

        val dialog = builder.create()
        dialog.show()

        fun loadIssueData(start: String?, end: String?) {
            tvDetails.text = "⏳ Inapakua ripoti..."
            val cleanStart = if (!start.isNullOrBlank()) start.trim() else null
            val cleanEnd = if (!end.isNullOrBlank()) end.trim() else null

            val apiService = getRetrofit().create(ApiService::class.java)
            apiService.getStockIssueReports(cleanStart, cleanEnd, "All").enqueue(object : Callback<List<StockIssueReportItem>> {
                override fun onResponse(call: Call<List<StockIssueReportItem>>, response: Response<List<StockIssueReportItem>>) {
                    if (response.isSuccessful && response.body() != null) {
                        val reportList = response.body()!!
                        if (reportList.isEmpty()) {
                            tvDetails.text = "Hakuna kumbukumbu."
                            return
                        }
                        val sb = StringBuilder("📌 RIPOTI YA BIDHAA ZILIZOTOLEWA:\n\n")
                        var grandTotal = 0.0
                        for (item in reportList) {
                            val totalVal = item.total_value ?: 0.0
                            grandTotal += totalVal
                            sb.append("• ${item.item_name}: ${item.quantity} ${item.unit} [${item.department}]\n")
                            sb.append("  - Thamani: TZS ${String.format("%,.0f", totalVal)}\n\n")
                        }
                        sb.append("----------------------------------\n")
                        sb.append("💰 JUMLA: TZS ${String.format("%,.0f", grandTotal)}")
                        tvDetails.text = sb.toString()
                    }
                }
                override fun onFailure(call: Call<List<StockIssueReportItem>>, t: Throwable) {
                    tvDetails.text = "Hitilafu: ${t.message}"
                }
            })
        }

        btnStartDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                selectedStartDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                btnStartDate.text = "KUANZIA: $selectedStartDate"
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnEndDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                selectedEndDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                btnEndDate.text = "HADI: $selectedEndDate"
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        loadIssueData(null, null)
        btnFetch.setOnClickListener { loadIssueData(selectedStartDate, selectedEndDate) }
    }

    // Njia ya Sub-Store Moja ambayo ikibonyezwa inafungua uchaguzi wa Jikoni au Bar (Inafanana na ya Procurement)
    private fun showUnifiedSubStoreChoiceDialog() {
        val options = arrayOf("🍲 Sub-Store Jikoni (Vyakula)", "🥤 Sub-Store Bar (Vinywaji)")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("🏢 Chagua Sub-Store ya Kutazama")
        builder.setItems(options) { _, which ->
            if (which == 0) {
                showStockDialog("Sub-Store Jikoni (Vyakula)", isMainStore = false, department = "Jikoni")
            } else {
                showStockDialog("Sub-Store Bar (Vinywaji)", isMainStore = false, department = "Bar")
            }
        }
        builder.show()
    }

    private fun showStockDialog(title: String, isMainStore: Boolean, department: String? = null) {
        val apiService = getRetrofit().create(ApiService::class.java)
        val call = if (isMainStore) apiService.getMainStockItems() else apiService.getSubStockItems(department)

        call.enqueue(object : Callback<List<StockItem>> {
            override fun onResponse(call: Call<List<StockItem>>, response: Response<List<StockItem>>) {
                if (response.isSuccessful && response.body() != null) {
                    var stockList = response.body()!!

                    // CHUJA HAPA ILI KUZUIA BIA KUTOKEA JIKONI NA VYAKULA KUTOKEA BAR
                    if (!isMainStore && department != null) {
                        stockList = stockList.filter { item ->
                            val itemName = (item.name ?: item.item_name ?: "").lowercase()
                            val isDrink = itemName.contains("bia") || itemName.contains("soda") || itemName.contains("maji") || itemName.contains("wine")

                            if (department.equals("Jikoni", ignoreCase = true)) {
                                !isDrink // Jikoni ruhusu vyakula tu, ondoa vinywaji
                            } else {
                                isDrink  // Bar ruhusu vinywaji tu
                            }
                        }
                    }

                    val builder = AlertDialog.Builder(this@PrincipalDashboardActivity)
                    builder.setTitle("📦 $title")

                    val layout = LinearLayout(this@PrincipalDashboardActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(40, 30, 40, 10)
                        setBackgroundColor(Color.parseColor("#1E293B")) // Background ya kisasa ya giza kama ya Procurement
                    }

                    if (stockList.isEmpty()) {
                        val tvEmpty = TextView(this@PrincipalDashboardActivity).apply {
                            text = "Hakuna bidhaa zilizosajiliwa hapa kwa sasa."
                            setTextColor(Color.WHITE)
                        }
                        layout.addView(tvEmpty)
                    } else {
                        for (item in stockList) {
                            val tvItem = TextView(this@PrincipalDashboardActivity).apply {
                                text = "• ${item.name ?: item.item_name}: ${item.quantity} ${item.unit}"
                                textSize = 15f
                                setTextColor(Color.WHITE) // Maandishi yawe meupe wazi
                                setPadding(0, 8, 0, 8)
                            }
                            layout.addView(tvItem)
                        }
                    }

                    builder.setView(layout)
                    builder.setPositiveButton("Funga", null)

                    val dialog = builder.create()
                    dialog.show()
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#38BDF8"))
                } else {
                    Toast.makeText(this@PrincipalDashboardActivity, "Imeshindwa kupakua stoki!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<StockItem>>, t: Throwable) {
                Toast.makeText(this@PrincipalDashboardActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}