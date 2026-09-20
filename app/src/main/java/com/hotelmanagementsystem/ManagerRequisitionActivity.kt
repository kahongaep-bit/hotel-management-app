package com.hotelmanagementsystem

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
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

data class DraftRequisitionItem(
    var itemName: String,
    var quantity: Double,
    var unit: String,
    var department: String,
    var ratioPerUnit: Double,
    var totalPortions: Double
)

class ManagerRequisitionActivity : AppCompatActivity() {

    private lateinit var btnResetPassword: Button
    private lateinit var btnCheckMainStore: Button
    private lateinit var btnCheckSubStore: Button
    private lateinit var btnViewLiveSales: Button

    private lateinit var etReqItemName: EditText
    private lateinit var etReqQuantity: EditText
    private lateinit var etReqUnit: EditText
    private lateinit var spReqDepartment: Spinner
    private lateinit var etReqRatioPerOrder: EditText
    private lateinit var btnAddReqToDraft: Button

    private lateinit var llDraftItemsList: LinearLayout
    private lateinit var btnSendAllRequisitions: Button
    private lateinit var rvRequisitionsHistory: RecyclerView

    private val draftList = mutableListOf<DraftRequisitionItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager_requisition)

        // Bind Views
        btnResetPassword = findViewById(R.id.btnResetPassword)
        btnCheckMainStore = findViewById(R.id.btnCheckMainStore)
        btnCheckSubStore = findViewById(R.id.btnCheckSubStore)
        btnViewLiveSales = findViewById(R.id.btnViewLiveSales)

        etReqItemName = findViewById(R.id.etReqItemName)
        etReqQuantity = findViewById(R.id.etReqQuantity)
        etReqUnit = findViewById(R.id.etReqUnit)
        spReqDepartment = findViewById(R.id.spReqDepartment)
        etReqRatioPerOrder = findViewById(R.id.etReqRatioPerOrder)
        btnAddReqToDraft = findViewById(R.id.btnAddReqToDraft)

        llDraftItemsList = findViewById(R.id.llDraftItemsList)
        btnSendAllRequisitions = findViewById(R.id.btnSendAllRequisitions)
        rvRequisitionsHistory = findViewById(R.id.rvRequisitionsHistory)

        rvRequisitionsHistory.layoutManager = LinearLayoutManager(this)

        btnResetPassword.setOnClickListener { showResetPasswordDialog() }

        val departments = arrayOf("Jikoni (Kitchen)", "Bar (Bartender)", "Vyumba & Kumbi")
        val customSpinnerAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            departments
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(Color.parseColor("#1A202C"))
                view.textSize = 14f
                view.setPadding(10, 10, 10, 10)
                return view
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(Color.parseColor("#1A202C"))
                view.setBackgroundColor(Color.parseColor("#FFFFFF"))
                view.textSize = 14f
                view.setPadding(20, 20, 20, 20)
                return view
            }
        }

        spReqDepartment.adapter = customSpinnerAdapter

        spReqDepartment.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position == 0) { // Jikoni
                    etReqRatioPerOrder.visibility = View.VISIBLE
                    etReqRatioPerOrder.hint = "Kipimo 1 kinatoa Oda ngapi? (mfano: 4)"
                } else {
                    etReqRatioPerOrder.visibility = View.GONE
                    etReqRatioPerOrder.text.clear()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnAddReqToDraft.setOnClickListener { addToDraftList() }
        btnSendAllRequisitions.setOnClickListener { sendAllDraftRequisitions() }
        btnCheckMainStore.setOnClickListener { showMainStockDialog() }
        btnCheckSubStore.setOnClickListener { showDepartmentalSubStoreDialog() }
        btnViewLiveSales.setOnClickListener { showLiveSalesSummaryDialog() }

        fetchRequisitionsHistory()
        updateDraftListUI()
    }

    override fun onResume() {
        super.onResume()
        fetchRequisitionsHistory()
    }

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://hotel-backend-production-d71e.up.railway.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
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

                val apiService = getRetrofit().create(ApiService::class.java)
                apiService.changePassword(body).enqueue(object : Callback<GenericResponse> {
                    override fun onResponse(
                        call: Call<GenericResponse>,
                        response: Response<GenericResponse>
                    ) {
                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@ManagerRequisitionActivity,
                                "Password imebadilishwa kikamilifu!",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this@ManagerRequisitionActivity,
                                "Taarifa sio sahihi!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                        Toast.makeText(
                            this@ManagerRequisitionActivity,
                            "Hitilafu: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
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

    private fun addToDraftList() {
        val name = etReqItemName.text.toString().trim()
        val qtyStr = etReqQuantity.text.toString().trim()
        val unit = etReqUnit.text.toString().trim()
        val dept = spReqDepartment.selectedItem.toString()
        val ratioStr = etReqRatioPerOrder.text.toString().trim()

        if (name.isEmpty() || qtyStr.isEmpty() || unit.isEmpty()) {
            Toast.makeText(this, "Tafadhali jaza jina, idadi na kipimo!", Toast.LENGTH_SHORT).show()
            return
        }

        val qty = qtyStr.toDoubleOrNull() ?: 0.0
        // Hakikisha ratio haiwi 0 au null ili kuepusha kugawanya kwa sifira (Division by zero)
        val ratio = if (ratioStr.toDoubleOrNull() != null && ratioStr.toDoubleOrNull()!! > 0.0) {
            ratioStr.toDoubleOrNull()!!
        } else {
            1.0
        }
        val portions = qty * ratio

        draftList.add(DraftRequisitionItem(name, qty, unit, dept, ratio, portions))

        etReqItemName.text.clear()
        etReqQuantity.text.clear()
        etReqUnit.text.clear()
        etReqRatioPerOrder.text.clear()

        updateDraftListUI()
    }

    private fun updateDraftListUI() {
        llDraftItemsList.removeAllViews()

        if (draftList.isEmpty()) {
            btnSendAllRequisitions.visibility = View.GONE
            val tvEmpty = TextView(this)
            tvEmpty.text = "Hakuna vitu kwenye rasimu bado."
            tvEmpty.setTextColor(Color.parseColor("#718096"))
            llDraftItemsList.addView(tvEmpty)
        } else {
            btnSendAllRequisitions.visibility = View.VISIBLE
            for ((index, item) in draftList.withIndex()) {
                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                row.setPadding(0, 8, 0, 8)

                val tvInfo = TextView(this)
                tvInfo.layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                val ratioDetails = if (item.ratioPerUnit > 0 && item.department.contains(
                        "Jikoni",
                        ignoreCase = true
                    )
                ) {
                    " (1 ${item.unit} = Oda ${item.ratioPerUnit} | Jumla Oda: ${item.totalPortions})"
                } else ""

                tvInfo.text =
                    "${index + 1}. ${item.itemName} - ${item.quantity} ${item.unit} [${item.department}]$ratioDetails"
                tvInfo.setTextColor(Color.parseColor("#1A202C"))
                tvInfo.textSize = 12f

                val btnDelete = Button(this)
                btnDelete.text = "❌"
                btnDelete.textSize = 10f
                btnDelete.setOnClickListener {
                    draftList.removeAt(index)
                    updateDraftListUI()
                }

                row.addView(tvInfo)
                row.addView(btnDelete)
                llDraftItemsList.addView(row)
            }
        }
    }

    private fun sendAllDraftRequisitions() {
        if (draftList.isEmpty()) return

        val apiService = getRetrofit().create(ApiService::class.java)
        var successCount = 0
        val totalItems = draftList.size

        for (item in draftList) {
            val reqData = hashMapOf<String, Any>(
                "item_name" to item.itemName,
                "quantity" to item.quantity,
                "unit" to item.unit,
                "requested_by" to "Hotel Manager",
                "department" to item.department,
                "ratio_per_unit" to item.ratioPerUnit,
                "total_portions" to item.totalPortions,
                "status" to "Pending"
            )

            apiService.createRequisition(reqData).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(
                    call: Call<GenericResponse>,
                    response: Response<GenericResponse>
                ) {
                    if (response.isSuccessful) {
                        successCount++
                        if (successCount == totalItems) {
                            Toast.makeText(
                                this@ManagerRequisitionActivity,
                                "Maombi yote $totalItems yametumwa kwa Production Coordinator!",
                                Toast.LENGTH_LONG
                            ).show()
                            draftList.clear()
                            updateDraftListUI()
                            fetchRequisitionsHistory()
                        }
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {}
            })
        }
    }

    private fun fetchRequisitionsHistory() {
        val apiService = getRetrofit().create(ApiService::class.java)

        apiService.getRequisitions().enqueue(object : Callback<List<RequisitionItem>> {
            override fun onResponse(
                call: Call<List<RequisitionItem>>,
                response: Response<List<RequisitionItem>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val requisitions = response.body()!!
                    setupAdapter(requisitions)
                }
            }

            override fun onFailure(call: Call<List<RequisitionItem>>, t: Throwable) {}
        })
    }

    private fun setupAdapter(requisitions: List<RequisitionItem>) {
        val adapter = ManagerHistoryAdapter(
            requisitions = requisitions,
            onEditClick = { item -> showEditRequisitionDialog(item) },
            onDeleteClick = { item -> showDeleteConfirmationDialog(item) }
        )
        rvRequisitionsHistory.adapter = adapter
    }

    private fun showEditRequisitionDialog(item: RequisitionItem) {
        val reqId = item.id ?: ""
        if (reqId.isEmpty()) {
            Toast.makeText(this, "ID ya ombi haipatikani!", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("✏️ Rekebisha Ombi: ${item.item_name}")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val etItemName = EditText(this).apply {
            hint = "Jina la Bidhaa"
            setText(item.item_name)
        }
        val etQty = EditText(this).apply {
            hint = "Kiasi"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(item.quantity?.toString() ?: "0")
        }
        val etUnit = EditText(this).apply {
            hint = "Kipimo (Kilo, Lita, Chupa)"
            setText(item.unit)
        }

        layout.addView(etItemName)
        layout.addView(etQty)
        layout.addView(etUnit)

        builder.setView(layout)

        builder.setPositiveButton("TUMA KWA PRODUCTION") { dialog, _ ->
            val newName = etItemName.text.toString().trim()
            val newQty = etQty.text.toString().toDoubleOrNull() ?: 0.0
            val newUnit = etUnit.text.toString().trim()

            if (newName.isEmpty() || newQty <= 0 || newUnit.isEmpty()) {
                Toast.makeText(this, "Jaza taarifa zote kwa usahihi!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val updateData = hashMapOf<String, Any>(
                "item_name" to newName,
                "quantity" to newQty,
                "unit" to newUnit,
                "department" to (item.department ?: "Jikoni"),
                "status" to "Pending"
            )

            val apiService = getRetrofit().create(ApiService::class.java)
            apiService.updateAndResubmitRequisition(reqId, updateData)
                .enqueue(object : Callback<GenericResponse> {
                    override fun onResponse(
                        call: Call<GenericResponse>,
                        response: Response<GenericResponse>
                    ) {
                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@ManagerRequisitionActivity,
                                "Ombi limerekebishwa na kutumwa kwa Production Coordinator!",
                                Toast.LENGTH_SHORT
                            ).show()
                            fetchRequisitionsHistory()
                        } else {
                            Toast.makeText(
                                this@ManagerRequisitionActivity,
                                "Imeshindwa kutuma mabadiliko!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                        Toast.makeText(
                            this@ManagerRequisitionActivity,
                            "Hitilafu: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            dialog.dismiss()
        }

        builder.setNegativeButton("GHAIRI", null)
        builder.show()
    }

    private fun showDeleteConfirmationDialog(item: RequisitionItem) {
        val reqId = item.id ?: ""
        if (reqId.isEmpty()) {
            Toast.makeText(this, "ID ya ombi haipatikani!", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("🗑️ Kuthibitisha Kufuta")
            .setMessage("Je, una uhakika unataka kufuta ombi la '${item.item_name}'?")
            .setPositiveButton("NDIO, FUTA") { dialog, _ ->
                val apiService = getRetrofit().create(ApiService::class.java)
                apiService.deleteRequisition(reqId).enqueue(object : Callback<GenericResponse> {
                    override fun onResponse(
                        call: Call<GenericResponse>,
                        response: Response<GenericResponse>
                    ) {
                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@ManagerRequisitionActivity,
                                "Ombi limefutwa kikamilifu!",
                                Toast.LENGTH_SHORT
                            ).show()
                            fetchRequisitionsHistory()
                        } else {
                            Toast.makeText(
                                this@ManagerRequisitionActivity,
                                "Imeshindwa kufuta ombi!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                        Toast.makeText(
                            this@ManagerRequisitionActivity,
                            "Hitilafu: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
                dialog.dismiss()
            }
            .setNegativeButton("HAPANA", null)
            .show()
    }

    private fun showMainStockDialog() {
        val apiService = getRetrofit().create(ApiService::class.java)

        apiService.getMainStockItems().enqueue(object : Callback<List<StockItem>> {
            override fun onResponse(
                call: Call<List<StockItem>>,
                response: Response<List<StockItem>>
            ) {
                val stockList = response.body() ?: emptyList()

                val builder = AlertDialog.Builder(this@ManagerRequisitionActivity)
                builder.setTitle("🏪 Main Store (Stoo Kuu)")

                val layout = LinearLayout(this@ManagerRequisitionActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(40, 30, 40, 30)
                    setBackgroundColor(Color.parseColor("#1A202C"))
                }

                if (stockList.isEmpty()) {
                    val tvEmpty = TextView(this@ManagerRequisitionActivity).apply {
                        text = "Hakuna bidhaa kwenye Stoo Kuu kwasasa."
                        setTextColor(Color.WHITE)
                        textSize = 14f
                    }
                    layout.addView(tvEmpty)
                } else {
                    for (item in stockList) {
                        val tvItem = TextView(this@ManagerRequisitionActivity).apply {
                            val name = item.item_name ?: item.name ?: "Bidhaa"
                            text = "• $name: ${item.quantity} ${item.unit}"
                            textSize = 14f
                            setTextColor(Color.WHITE)
                            setPadding(0, 6, 0, 6)
                        }
                        layout.addView(tvItem)
                    }
                }

                builder.setView(layout)
                builder.setPositiveButton("FUNGA", null)
                builder.show()
            }

            override fun onFailure(call: Call<List<StockItem>>, t: Throwable) {
                Toast.makeText(
                    this@ManagerRequisitionActivity,
                    "Hitilafu: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showDepartmentalSubStoreDialog() {
        val apiService = getRetrofit().create(ApiService::class.java)

        apiService.getSubStockItems().enqueue(object : Callback<List<StockItem>> {
            override fun onResponse(
                call: Call<List<StockItem>>,
                response: Response<List<StockItem>>
            ) {
                val stockList = response.body() ?: emptyList()

                val kitchenItems = stockList.filter { item ->
                    val itemName = (item.item_name ?: item.name ?: "").lowercase()
                    !itemName.contains("soda") && !itemName.contains("bia") && !itemName.contains("maji") && !itemName.contains(
                        "juice"
                    ) && !itemName.contains("wine") && !itemName.contains("drink")
                }

                val barItems = stockList.filter { item ->
                    val itemName = (item.item_name ?: item.name ?: "").lowercase()
                    itemName.contains("soda") || itemName.contains("bia") || itemName.contains("maji") || itemName.contains(
                        "juice"
                    ) || itemName.contains("wine") || itemName.contains("drink")
                }

                val builder = AlertDialog.Builder(this@ManagerRequisitionActivity)
                builder.setTitle("📦 Stoo ya Hoteli (Sub-Store)")

                val scrollView = ScrollView(this@ManagerRequisitionActivity)
                val layout = LinearLayout(this@ManagerRequisitionActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(40, 30, 40, 30)
                    setBackgroundColor(Color.parseColor("#1A202C"))
                }

                val tvKitchenHeader = TextView(this@ManagerRequisitionActivity).apply {
                    text = "🍳 SUB-STORE JIKONI (KITCHEN):"
                    setTextColor(Color.parseColor("#63B3ED"))
                    textSize = 14f
                    setPadding(0, 0, 0, 10)
                }
                layout.addView(tvKitchenHeader)

                if (kitchenItems.isEmpty()) {
                    val tvEmpty = TextView(this@ManagerRequisitionActivity).apply {
                        text = "Hakuna bidhaa Jikoni.\n"
                        setTextColor(Color.WHITE)
                    }
                    layout.addView(tvEmpty)
                } else {
                    for (item in kitchenItems) {
                        val tvItem = TextView(this@ManagerRequisitionActivity).apply {
                            val name = item.item_name ?: item.name ?: "Bidhaa"
                            text = "• $name: ${item.quantity} ${item.unit}"
                            setTextColor(Color.WHITE)
                            setPadding(0, 4, 0, 4)
                        }
                        layout.addView(tvItem)
                    }
                }

                val tvBarHeader = TextView(this@ManagerRequisitionActivity).apply {
                    text = "\n🥤 SUB-STORE BAR (VINYWAJI):"
                    setTextColor(Color.parseColor("#68D391"))
                    textSize = 14f
                    setPadding(0, 10, 0, 10)
                }
                layout.addView(tvBarHeader)

                if (barItems.isEmpty()) {
                    val tvEmpty = TextView(this@ManagerRequisitionActivity).apply {
                        text = "Hakuna bidhaa Bar.\n"
                        setTextColor(Color.WHITE)
                    }
                    layout.addView(tvEmpty)
                } else {
                    for (item in barItems) {
                        val tvItem = TextView(this@ManagerRequisitionActivity).apply {
                            val name = item.item_name ?: item.name ?: "Bidhaa"
                            text = "• $name: ${item.quantity} ${item.unit}"
                            setTextColor(Color.WHITE)
                            setPadding(0, 4, 0, 4)
                        }
                        layout.addView(tvItem)
                    }
                }

                scrollView.addView(layout)
                builder.setView(scrollView)
                builder.setPositiveButton("FUNGA", null)
                builder.show()
            }

            override fun onFailure(call: Call<List<StockItem>>, t: Throwable) {
                Toast.makeText(
                    this@ManagerRequisitionActivity,
                    "Hitilafu ya mtandao: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showLiveSalesSummaryDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("📊 Taarifa za Mauzo na Benki")

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 30)
            setBackgroundColor(Color.parseColor("#1A202C"))
        }

        var selectedStartDate: String? = null
        var selectedEndDate: String? = null

        val btnStartDate = Button(this).apply {
            text = "TAREHE YA KUANZIA: CHAGUA"
            setBackgroundColor(Color.parseColor("#2D3748"))
            setTextColor(Color.WHITE)
            textSize = 13f
        }
        layout.addView(btnStartDate)

        val btnEndDate = Button(this).apply {
            text = "TAREHE YA MWISHO: CHAGUA"
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
            text = "🔍 TAFUTA MAUZO"
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
            setLineSpacing(8f, 1f)
            setPadding(10, 10, 10, 10)
        }
        layout.addView(tvDetails)

        scrollView.addView(layout)
        builder.setView(scrollView)
        builder.setPositiveButton("FUNGA", null)

        val dialog = builder.create()
        dialog.show()

        fun loadSalesData(start: String?, end: String?) {
            tvDetails.setTextColor(Color.parseColor("#63B3ED"))
            tvDetails.text = "⏳ Inapakua taarifa za mauzo na benki..."

            val cleanStart = if (!start.isNullOrBlank()) start.trim() else null
            val cleanEnd = if (!end.isNullOrBlank()) end.trim() else null

            val apiService = getRetrofit().create(ApiService::class.java)
            apiService.getFinanceReports(cleanStart, cleanEnd)
                .enqueue(object : Callback<FinanceReportResponse> {
                    override fun onResponse(
                        call: Call<FinanceReportResponse>,
                        response: Response<FinanceReportResponse>
                    ) {
                        if (response.isSuccessful && response.body() != null) {
                            val rep = response.body()!!
                            val rangeInfo =
                                if (cleanStart != null && cleanEnd != null) " ($cleanStart hadi $cleanEnd)" else " (Jumla Yote)"

                            tvDetails.setTextColor(Color.WHITE)
                            tvDetails.text = """
                            📌 TAARIFA YA MAUZO$rangeInfo:
                            
                            • Jumla ya Mauzo: TZS ${String.format("%,.0f", rep.total_sales)}
                            • 💵 Cash Total: TZS ${String.format("%,.0f", rep.cash_sales)}
                            • 📱 Lipa Namba Total: TZS ${String.format("%,.0f", rep.lipanamba_sales)}
                            • Idadi ya Oda Zilizouzwa: ${rep.total_orders}
                            
                            • 🏦 Jumla Iliyopo Benki: TZS ${
                                String.format(
                                    "%,.0f",
                                    rep.total_deposits
                                )
                            }
                            • 💰 Balance (Haijapelekwa Benki): TZS ${
                                String.format(
                                    "%,.0f",
                                    rep.balance
                                )
                            }
                            
                            ──────────────────────────
                            📊 MCHANGANUO WA BENKI KWA IDARA:
                            - 🍳 Breakfast: TZS ${String.format("%,.0f", rep.breakfast_deposits)}
                            - 🍲 Lunch: TZS ${String.format("%,.0f", rep.lunch_deposits)}
                            - 🍝 Dinner: TZS ${String.format("%,.0f", rep.dinner_deposits)}
                            - 🥤 Vinywaji/Bar: TZS ${String.format("%,.0f", rep.drinks_deposits)}
                            - 🏨 Vyumba/Kumbi: TZS ${String.format("%,.0f", rep.rooms_deposits)}
                        """.trimIndent()
                        } else {
                            tvDetails.setTextColor(Color.WHITE)
                            tvDetails.text = "• Taarifa hazijapatikana."
                        }
                    }

                    override fun onFailure(call: Call<FinanceReportResponse>, t: Throwable) {
                        tvDetails.setTextColor(Color.WHITE)
                        tvDetails.text = "Hitilafu ya mtandao: ${t.message}"
                    }
                })
        }

        btnStartDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedStartDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    btnStartDate.text = "KUANZIA: $selectedStartDate"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnEndDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedEndDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    btnEndDate.text = "HADI: $selectedEndDate"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        loadSalesData(null, null)

        btnFetch.setOnClickListener {
            loadSalesData(selectedStartDate, selectedEndDate)
        }
    }
}