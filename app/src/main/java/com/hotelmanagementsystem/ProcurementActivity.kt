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

class ProcurementActivity : AppCompatActivity() {

    private lateinit var rvPendingRequisitions: RecyclerView
    private lateinit var btnPendingRequisitions: Button
    private lateinit var btnStockIn: Button
    private lateinit var btnViewStockBalance: Button
    private lateinit var btnViewSubStore: Button
    private lateinit var btnStockIssueReport: Button
    private lateinit var btnResetPassword: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_procurement)

        rvPendingRequisitions = findViewById(R.id.rvPendingRequisitions)
        btnPendingRequisitions = findViewById(R.id.btnPendingRequisitions)
        btnStockIn = findViewById(R.id.btnStockIn)
        btnViewStockBalance = findViewById(R.id.btnViewStockBalance)
        btnViewSubStore = findViewById(R.id.btnViewSubStore)
        btnStockIssueReport = findViewById(R.id.btnStockIssueReport)
        btnResetPassword = findViewById(R.id.btnResetPassword)

        rvPendingRequisitions.layoutManager = LinearLayoutManager(this)

        btnResetPassword.setOnClickListener { showResetPasswordDialog() }
        btnPendingRequisitions.setOnClickListener { fetchRequisitionsForProcurement() }
        btnStockIn.setOnClickListener { showAddStockDialog(null) }
        btnViewStockBalance.setOnClickListener { showMainStockBalanceDialog() }
        btnViewSubStore.setOnClickListener { showDepartmentalSubStoreDialog() }
        btnStockIssueReport.setOnClickListener { showStockIssueReportDialog() }

        fetchRequisitionsForProcurement()
    }

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://10.32.78.51:5000/")
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
                    override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@ProcurementActivity, "Password imebadilishwa kikamilifu!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@ProcurementActivity, "Taarifa sio sahihi!", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                        Toast.makeText(this@ProcurementActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
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

    private fun fetchRequisitionsForProcurement() {
        val apiService = getRetrofit().create(ApiService::class.java)

        apiService.getRequisitions().enqueue(object : Callback<List<RequisitionItem>> {
            override fun onResponse(
                call: Call<List<RequisitionItem>>,
                response: Response<List<RequisitionItem>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val allList = response.body()!!

                    val procurementList = allList.filter {
                        val st = it.status ?: ""
                        st.equals("Approved_Production", ignoreCase = true) ||
                                st.equals("Approved_Principal", ignoreCase = true) ||
                                st.equals("Rejected_Finance", ignoreCase = true)
                    }

                    setupProcurementAdapter(procurementList)
                }
            }

            override fun onFailure(call: Call<List<RequisitionItem>>, t: Throwable) {
                Toast.makeText(this@ProcurementActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupProcurementAdapter(requisitions: List<RequisitionItem>) {
        val adapter = ProcurementRequisitionAdapter(requisitions) { selectedReq ->
            showProcurementActionDialog(selectedReq)
        }
        rvPendingRequisitions.adapter = adapter
    }

    private fun showProcurementActionDialog(requisition: RequisitionItem) {
        val builder = AlertDialog.Builder(this)

        val isPrincipalApproved = requisition.status.equals("Approved_Principal", ignoreCase = true)
        val isFromFinance = requisition.status.equals("Rejected_Finance", ignoreCase = true)

        val titleText = when {
            isPrincipalApproved -> "✅ Ombi Lililoidhinishwa na Principal"
            isFromFinance -> "❌ Ombi Lililorudishwa na Mhasibu"
            else -> "📋 Hakiki Ombi la Manunuzi"
        }
        builder.setTitle(titleText)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 20)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        val ratioInfo = if (requisition.ratio_per_unit != null && requisition.ratio_per_unit!! > 0 && (requisition.department ?: "").contains("Jikoni", ignoreCase = true)) {
            "\n• Uwiano wa Ratio: 1 ${requisition.unit ?: ""} = Oda ${requisition.ratio_per_unit?.toInt()}\n• Kadirio la Sahani/Oda: ${requisition.total_portions?.toInt()} Oda"
        } else ""

        val priceInfo = if (requisition.estimated_cost != null && requisition.estimated_cost!! > 0) {
            "\n• Mzabuni: ${requisition.supplier ?: "Haijawekwa"}\n• Bei ya Sokoni: TZS ${String.format("%,.0f", requisition.estimated_cost)}"
        } else ""

        val commentInfo = if (isFromFinance && !requisition.rejection_comment.isNullOrBlank() && requisition.rejection_comment.trim().isNotEmpty()) {
            "\n\n💬 Sababu ya Kurudishwa na Mhasibu:\n\"${requisition.rejection_comment}\""
        } else ""

        val detailsText = "• Bidhaa: ${requisition.item_name}\n" +
                "• Kiasi Kilichoombwa: ${requisition.quantity} ${requisition.unit ?: ""}\n" +
                "• Aliyeomba: ${requisition.requested_by ?: "Hotel Manager"}" + ratioInfo + priceInfo + commentInfo

        val tvDetails = TextView(this).apply {
            text = detailsText
            setTextColor(Color.WHITE)
            textSize = 14f
            setLineSpacing(6f, 1f)
        }
        layout.addView(tvDetails)

        builder.setView(layout)

        if (isPrincipalApproved) {
            builder.setPositiveButton("THIBITISHA KUNUNUA (PROCURED)") { dialog, _ ->
                markAsProcured(requisition)
                dialog.dismiss()
            }
        } else {
            builder.setPositiveButton("✏️ REKEBISHA & TUMA KWA MHASIBU") { dialog, _ ->
                dialog.dismiss()
                showEditAndResubmitToFinanceDialog(requisition)
            }

            builder.setNegativeButton("↩️ RUDISHA KWA PRODUCTION") { dialog, _ ->
                dialog.dismiss()
                showReturnToProductionDialog(requisition)
            }
        }

        builder.setNeutralButton("GHAIRI", null)

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#63B3ED"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#F6AD55"))
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(Color.WHITE)
    }

    private fun showEditAndResubmitToFinanceDialog(requisition: RequisitionItem) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("✏️ Weka Bei ya Unit Moja kwenda kwa Mhasibu")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        val etSupplier = EditText(this).apply {
            hint = "Jina la Mzabuni/Duka"
            setHintTextColor(Color.parseColor("#CBD5E1"))
            setTextColor(Color.WHITE)
            setText(requisition.supplier ?: "")
        }

        // Badala ya gharama ya jumla moja kwa moja, hapa tunaweka bei ya kitengo kimoja (Unit Price)
        val etUnitPrice = EditText(this).apply {
            hint = "Bei ya Unit Moja (kwa Kipindi/Kilo/Lita) TZS"
            setHintTextColor(Color.parseColor("#CBD5E1"))
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            val currentQty = requisition.quantity ?: 1.0
            val existingUnitPr = if (requisition.estimated_cost != null && currentQty > 0) requisition.estimated_cost!! / currentQty else 0.0
            setText(if (existingUnitPr > 0) existingUnitPr.toString() else "")
        }

        layout.addView(etSupplier)
        layout.addView(etUnitPrice)
        builder.setView(layout)

        builder.setPositiveButton("THIBITISHA & TUMA") { dialog, _ ->
            val newSupplier = etSupplier.text.toString().trim()
            val unitPriceVal = etUnitPrice.text.toString().toDoubleOrNull() ?: 0.0
            val reqQty = requisition.quantity ?: 1.0

            if (newSupplier.isEmpty() || unitPriceVal <= 0) {
                Toast.makeText(this, "Jaza mzabuni na bei sahihi ya kitengo!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            // Hesabu ya jumla = Unit Price * Quantity
            val totalCalculatedCost = unitPriceVal * reqQty

            forwardToFinanceWithUnitPrice(requisition.id ?: "", newSupplier, unitPriceVal, totalCalculatedCost)
            dialog.dismiss()
        }

        builder.setNegativeButton("GHAIRI", null)

        val dialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#48BB78"))
    }

    private fun forwardToFinanceWithUnitPrice(reqId: String, supplier: String, unitPrice: Double, estimatedCost: Double) {
        val apiService = getRetrofit().create(ApiService::class.java)

        val updateData = hashMapOf<String, Any>(
            "status" to "Approved_Procurement",
            "supplier" to supplier,
            "unit_price" to unitPrice,
            "estimated_cost" to estimatedCost,
            "rejection_comment" to ""
        )

        apiService.updateAndResubmitRequisition(reqId, updateData).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ProcurementActivity, "Ombi limetumwa kwa Mhasibu na jumla imehesabiwa kikamilifu!", Toast.LENGTH_LONG).show()
                    fetchRequisitionsForProcurement()
                } else {
                    Toast.makeText(this@ProcurementActivity, "Imeshindwa kutuma kwa Mhasibu!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@ProcurementActivity, "Hitilafu ya Mtandao: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun forwardToFinance(reqId: String, supplier: String, estimatedCost: Double) {
        val apiService = getRetrofit().create(ApiService::class.java)

        val updateData = hashMapOf<String, Any>(
            "status" to "Approved_Procurement",
            "supplier" to supplier,
            "estimated_cost" to estimatedCost,
            "rejection_comment" to ""
        )

        apiService.updateAndResubmitRequisition(reqId, updateData).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ProcurementActivity, "Ombi limetumwa kwa Mhasibu kikamilifu!", Toast.LENGTH_LONG).show()
                    fetchRequisitionsForProcurement()
                } else {
                    Toast.makeText(this@ProcurementActivity, "Imeshindwa kutuma kwa Mhasibu!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@ProcurementActivity, "Hitilafu ya Mtandao: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showReturnToProductionDialog(requisition: RequisitionItem) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("↩️ Rudisha Ombi kwa Production Coordinator")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        val etComment = EditText(this).apply {
            hint = "Andika sababu ya kurudisha kwa Production..."
            setHintTextColor(Color.parseColor("#CBD5E1"))
            setTextColor(Color.WHITE)
        }
        layout.addView(etComment)
        builder.setView(layout)

        builder.setPositiveButton("RUDISHA KWA PRODUCTION") { dialog, _ ->
            val comment = etComment.text.toString().trim()
            if (comment.isEmpty()) {
                Toast.makeText(this, "Weka sababu ya kurudisha!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val updateData = hashMapOf<String, Any>(
                "status" to "Returned_To_Production",
                "rejection_comment" to comment
            )

            val apiService = getRetrofit().create(ApiService::class.java)
            apiService.updateRequisitionApproval(requisition.id ?: "", updateData).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ProcurementActivity, "Ombi limerudishwa kwa Production Coordinator!", Toast.LENGTH_SHORT).show()
                        fetchRequisitionsForProcurement()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@ProcurementActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
            dialog.dismiss()
        }

        builder.setNegativeButton("GHAIRI", null)

        val dialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#F56565"))
    }

    private fun markAsProcured(requisition: RequisitionItem) {
        val apiService = getRetrofit().create(ApiService::class.java)

        val updateData = hashMapOf<String, Any>(
            "status" to "Procured"
        )

        apiService.updateRequisitionApproval(requisition.id ?: "", updateData).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    fetchRequisitionsForProcurement()
                    showAddStockDialog(requisition)
                } else {
                    Toast.makeText(this@ProcurementActivity, "Imeshindwa kusasisha taarifa za ununuzi!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@ProcurementActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddStockDialog(prefillRequisition: RequisitionItem? = null) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Sajili Stoki Mpya Stoo Kuu (GRN)")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 20)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        val inputName = EditText(this).apply {
            hint = "Jina la Bidhaa"
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
            if (prefillRequisition != null) {
                setText(prefillRequisition.item_name)
                isEnabled = false
            }
        }
        layout.addView(inputName)

        val inputQuantity = EditText(this).apply {
            hint = "Idadi / Kiasi"
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            if (prefillRequisition != null) {
                setText(prefillRequisition.quantity?.toString() ?: "")
            }
        }
        layout.addView(inputQuantity)

        val inputUnit = EditText(this).apply {
            hint = "Kipimo (Kilo, Lita, Chupa, Katoni)"
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
            if (prefillRequisition != null) {
                setText(prefillRequisition.unit ?: "")
            }
        }
        layout.addView(inputUnit)

        val inputUnitPrice = EditText(this).apply {
            hint = "Ingiza Bei ya Unit Moja (TZS)"
            setHintTextColor(Color.parseColor("#63B3ED"))
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            requestFocus()
        }
        layout.addView(inputUnitPrice)

        val inputSupplier = EditText(this).apply {
            hint = "Mzabuni / Supplier"
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
            if (prefillRequisition != null && !prefillRequisition.supplier.isNullOrBlank()) {
                setText(prefillRequisition.supplier)
            }
        }
        layout.addView(inputSupplier)

        builder.setView(layout)

        builder.setPositiveButton("HIFADHI STOO KUU") { dialog, _ ->
            val name = inputName.text.toString().trim()
            val qtyStr = inputQuantity.text.toString().trim()
            val unit = inputUnit.text.toString().trim()
            val unitPriceStr = inputUnitPrice.text.toString().trim()
            val supplier = inputSupplier.text.toString().trim()

            if (name.isNotEmpty() && qtyStr.isNotEmpty() && unitPriceStr.isNotEmpty()) {
                val qty = qtyStr.toDoubleOrNull() ?: 0.0
                val unitPrice = unitPriceStr.toDoubleOrNull() ?: 0.0
                val totalCost = qty * unitPrice

                saveStockToMainStore(name, qty, unit, unitPrice, totalCost, supplier)
            } else {
                Toast.makeText(this, "Tafadhali ingiza bei ya unit na kiasi sahihi!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("GHAIRI", null)
        builder.show()
    }

    private fun saveStockToMainStore(name: String, quantity: Double, unit: String, unitPrice: Double, totalCost: Double, supplier: String) {
        val apiService = getRetrofit().create(ApiService::class.java)

        val stockData = hashMapOf<String, Any>(
            "item_name" to name,
            "quantity" to quantity,
            "unit" to unit,
            "unit_price" to unitPrice,
            "total_cost" to totalCost,
            "supplier" to supplier
        )

        apiService.addMainStockItem(stockData).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ProcurementActivity, "Mzigo wa '$name' umeingizwa Stoo Kuu kikamilifu!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@ProcurementActivity, "Imeshindwa kusajili stoki Stoo Kuu!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@ProcurementActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
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
                    !itemName.contains("soda") && !itemName.contains("bia") && !itemName.contains("maji") && !itemName.contains("juice") && !itemName.contains("wine") && !itemName.contains("drink")
                }

                val barItems = stockList.filter { item ->
                    val itemName = (item.item_name ?: item.name ?: "").lowercase()
                    itemName.contains("soda") || itemName.contains("bia") || itemName.contains("maji") || itemName.contains("juice") || itemName.contains("wine") || itemName.contains("drink")
                }

                val builder = AlertDialog.Builder(this@ProcurementActivity)
                builder.setTitle("📦 Stoo ya Hoteli (Sub-Store)")

                val scrollView = ScrollView(this@ProcurementActivity)
                val layout = LinearLayout(this@ProcurementActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(40, 30, 40, 30)
                    setBackgroundColor(Color.parseColor("#1E293B"))
                }

                val tvKitchenHeader = TextView(this@ProcurementActivity).apply {
                    text = "🍳 SUB-STORE VYAKULA (KITCHEN):"
                    setTextColor(Color.parseColor("#63B3ED"))
                    textSize = 14f
                    setPadding(0, 0, 0, 10)
                }
                layout.addView(tvKitchenHeader)

                if (kitchenItems.isEmpty()) {
                    val tvEmpty = TextView(this@ProcurementActivity).apply {
                        text = "Hakuna bidhaa za vyakula Sub-Store.\n"
                        setTextColor(Color.WHITE)
                    }
                    layout.addView(tvEmpty)
                } else {
                    for (item in kitchenItems) {
                        val tvItem = TextView(this@ProcurementActivity).apply {
                            val name = item.item_name ?: item.name ?: "Bidhaa"
                            text = "• $name: ${item.quantity} ${item.unit}"
                            setTextColor(Color.WHITE)
                            setPadding(0, 4, 0, 4)
                        }
                        layout.addView(tvItem)
                    }
                }

                val tvBarHeader = TextView(this@ProcurementActivity).apply {
                    text = "\n🥤 SUB-STORE VINYWAJI (BAR):"
                    setTextColor(Color.parseColor("#68D391"))
                    textSize = 14f
                    setPadding(0, 10, 0, 10)
                }
                layout.addView(tvBarHeader)

                if (barItems.isEmpty()) {
                    val tvEmpty = TextView(this@ProcurementActivity).apply {
                        text = "Hakuna vinywaji Sub-Store.\n"
                        setTextColor(Color.WHITE)
                    }
                    layout.addView(tvEmpty)
                } else {
                    for (item in barItems) {
                        val tvItem = TextView(this@ProcurementActivity).apply {
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
                Toast.makeText(this@ProcurementActivity, "Hitilafu ya mtandao: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showMainStockBalanceDialog() {
        val apiService = getRetrofit().create(ApiService::class.java)

        apiService.getMainStockItems().enqueue(object : Callback<List<StockItem>> {
            override fun onResponse(call: Call<List<StockItem>>, response: Response<List<StockItem>>) {
                if (response.isSuccessful && response.body() != null) {
                    val stockList = response.body()!!

                    val builder = AlertDialog.Builder(this@ProcurementActivity)
                    builder.setTitle("📦 Main Store - Kutoa Mzigo Kwenda Sub-Store")

                    val scrollView = ScrollView(this@ProcurementActivity)
                    val layout = LinearLayout(this@ProcurementActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(40, 30, 40, 20)
                        setBackgroundColor(Color.parseColor("#1E293B"))
                    }

                    val parentDialog = builder.create()

                    if (stockList.isEmpty()) {
                        val tvEmpty = TextView(this@ProcurementActivity).apply {
                            text = "Hakuna bidhaa zozote Stoo Kuu."
                            setTextColor(Color.WHITE)
                        }
                        layout.addView(tvEmpty)
                    } else {
                        for (item in stockList) {
                            val row = LinearLayout(this@ProcurementActivity).apply {
                                orientation = LinearLayout.HORIZONTAL
                                setPadding(0, 10, 0, 10)
                            }

                            val tvItem = TextView(this@ProcurementActivity).apply {
                                val name = item.item_name ?: item.name ?: "Bidhaa"
                                text = "• $name\n  Stoki: ${item.quantity} ${item.unit}"
                                textSize = 14f
                                setTextColor(Color.WHITE)
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                            }

                            val btnIssue = Button(this@ProcurementActivity).apply {
                                text = "📤 TOA"
                                textSize = 10f
                                setBackgroundColor(Color.parseColor("#D69E2E"))
                                setTextColor(Color.WHITE)
                                setOnClickListener {
                                    val name = item.item_name ?: item.name ?: ""
                                    showIssueFormDialog(name, item.unit ?: "", parentDialog)
                                }
                            }

                            row.addView(tvItem)
                            row.addView(btnIssue)
                            layout.addView(row)
                        }
                    }

                    scrollView.addView(layout)
                    parentDialog.setView(scrollView)
                    parentDialog.setButton(AlertDialog.BUTTON_POSITIVE, "FUNGA") { dialog, _ -> dialog.dismiss() }
                    parentDialog.show()
                } else {
                    Toast.makeText(this@ProcurementActivity, "Imeshindwa kupakua Stoo Kuu!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<StockItem>>, t: Throwable) {
                Toast.makeText(this@ProcurementActivity, "Hitilafu ya mtandao: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showIssueFormDialog(itemName: String, unit: String, mainStoreDialog: AlertDialog?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Kutoa '$itemName' Kwenda Sub-Store")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 20)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        val tvInfo = TextView(this).apply {
            text = "Bidhaa: $itemName ($unit)"
            setTextColor(Color.WHITE)
            textSize = 14f
        }
        layout.addView(tvInfo)

        val inputQuantity = EditText(this).apply {
            hint = "Kiasi cha Kutoa"
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        layout.addView(inputQuantity)

        val tvLabel = TextView(this).apply {
            text = "\nPeleka Sub-Store Gani?"
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 12f
        }
        layout.addView(tvLabel)

        val spTargetDept = Spinner(this)
        val depts = arrayOf("Jikoni (Kitchen)", "Bar (Bartender)")
        val spAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, depts)
        spTargetDept.adapter = spAdapter
        layout.addView(spTargetDept)

        builder.setView(layout)

        builder.setPositiveButton("THIBITISHA KUTOA") { dialog, _ ->
            val qtyStr = inputQuantity.text.toString().trim()
            val targetDept = spTargetDept.selectedItem.toString()

            if (qtyStr.isNotEmpty()) {
                val qty = qtyStr.toDouble()
                issueStockToSubStore(itemName, qty, targetDept, mainStoreDialog)
            } else {
                Toast.makeText(this, "Tafadhali jaza kiasi cha kutoa!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("GHAIRI", null)
        builder.show()
    }

    private fun issueStockToSubStore(itemName: String, quantity: Double, department: String, parentDialog: AlertDialog?) {
        val apiService = getRetrofit().create(ApiService::class.java)

        val cleanDept = if (department.contains("Bar", ignoreCase = true)) "Bar" else "Jikoni"

        val issueData = hashMapOf<String, Any>(
            "item_name" to itemName,
            "quantity" to quantity,
            "department" to cleanDept
        )

        apiService.issueStockToSubStore(issueData).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ProcurementActivity, "Mzigo wa '$itemName' umetolewa kwenda $cleanDept kikamilifu!", Toast.LENGTH_LONG).show()

                    // Funga dialog ya zamani na ufungue upya ili takwimu zijirekebishe papo hapo kwenye screen
                    parentDialog?.dismiss()
                    showMainStockBalanceDialog()
                } else {
                    Toast.makeText(this@ProcurementActivity, "Imeshindwa kutoa: Hakikisha stoki inatosha Stoo Kuu!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@ProcurementActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showStockIssueReportDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("📊 Ripoti ya Bidhaa Zilizotolewa")

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
            text = "🔍 FILTER RIPOTI YA ISSUES"
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

        fun loadIssueData(start: String?, end: String?) {
            tvDetails.setTextColor(Color.parseColor("#63B3ED"))
            tvDetails.text = "⏳ Inapakua ripoti ya bidhaa zilizotolewa..."

            val cleanStart = if (!start.isNullOrBlank()) start.trim() else null
            val cleanEnd = if (!end.isNullOrBlank()) end.trim() else null

            val apiService = getRetrofit().create(ApiService::class.java)

            apiService.getStockIssueReports(cleanStart, cleanEnd, null).enqueue(object : Callback<List<StockIssueReportItem>> {
                override fun onResponse(call: Call<List<StockIssueReportItem>>, response: Response<List<StockIssueReportItem>>) {
                    if (response.isSuccessful && response.body() != null) {
                        val reportList = response.body()!!
                        if (reportList.isEmpty()) {
                            tvDetails.setTextColor(Color.WHITE)
                            tvDetails.text = "Hakuna kumbukumbu za bidhaa zilizotolewa kwasasa."
                            return
                        }

                        val rangeInfo = if (cleanStart != null && cleanEnd != null) " ($cleanStart hadi $cleanEnd)" else " (Kipindi Yote)"
                        val sb = StringBuilder()
                        sb.append("📌 RIPOTI YA BIDHAA ZILIZOTOLEWA$rangeInfo:\n\n")

                        var grandTotal = 0.0
                        for (item in reportList) {
                            val totalVal = item.total_value ?: 0.0
                            grandTotal += totalVal
                            val itemName = item.item_name ?: "Bidhaa"
                            val qty = item.quantity ?: 0.0
                            val unit = item.unit ?: ""
                            val dept = item.department ?: "Sub-Store"

                            sb.append("• $itemName: $qty $unit [$dept]\n")
                            sb.append("  - Thamani: TZS ${String.format("%,.0f", totalVal)}\n\n")
                        }
                        sb.append("----------------------------------\n")
                        sb.append("💰 JUMLA THAMANI ILIYOTOLEWA: TZS ${String.format("%,.0f", grandTotal)}")

                        tvDetails.setTextColor(Color.WHITE)
                        tvDetails.text = sb.toString()
                    } else {
                        tvDetails.setTextColor(Color.WHITE)
                        tvDetails.text = "Imeshindwa kupakua ripoti za stoki."
                    }
                }

                override fun onFailure(call: Call<List<StockIssueReportItem>>, t: Throwable) {
                    tvDetails.setTextColor(Color.WHITE)
                    tvDetails.text = "Hitilafu ya mtandao: ${t.message}"
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

        loadIssueData(null, null)

        btnFetch.setOnClickListener {
            loadIssueData(selectedStartDate, selectedEndDate)
        }
    }
}