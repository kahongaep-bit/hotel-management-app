package com.hotelmanagementsystem

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
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

class FinancePriceActivity : AppCompatActivity() {

    private lateinit var tvFinanceDailySales: TextView
    private lateinit var tvFinanceBankDeposit: TextView
    private lateinit var btnResetPassword: Button

    private lateinit var etProductName: EditText
    private lateinit var etProductPrice: EditText
    private lateinit var spCategory: Spinner
    private lateinit var btnAddProduct: Button
    private lateinit var etSearchProduct: EditText
    private lateinit var rvFinanceProducts: RecyclerView
    private lateinit var btnFinanceRequisitions: Button
    private lateinit var btnViewFinancialReports: Button

    private lateinit var btnFilterAll: Button
    private lateinit var btnFilterFood: Button
    private lateinit var btnFilterDrinks: Button
    private lateinit var btnFilterRooms: Button
    private lateinit var btnFilterHalls: Button

    private val fullProductList = mutableListOf<MenuItem>()
    private val displayedProductList = mutableListOf<MenuItem>()
    private lateinit var adapter: FinanceProductAdapter

    private var currentCategoryFilter = "ZOTE"
    private var pendingRequisitionsDialog: AlertDialog? = null

    private val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://hotel-backend-production-d71e.up.railway.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finance_price_management)

        tvFinanceDailySales = findViewById(R.id.tvFinanceDailySales)
        tvFinanceBankDeposit = findViewById(R.id.tvFinanceBankDeposit)
        btnResetPassword = findViewById(R.id.btnResetPassword)

        etProductName = findViewById(R.id.etProductName)
        etProductPrice = findViewById(R.id.etProductPrice)
        spCategory = findViewById(R.id.spCategory)
        btnAddProduct = findViewById(R.id.btnAddProduct)
        etSearchProduct = findViewById(R.id.etSearchProduct)
        rvFinanceProducts = findViewById(R.id.rvFinanceProducts)
        btnFinanceRequisitions = findViewById(R.id.btnFinanceRequisitions)
        btnViewFinancialReports = findViewById(R.id.btnViewFinancialReports)

        btnFilterAll = findViewById(R.id.btnFilterAll)
        btnFilterFood = findViewById(R.id.btnFilterFood)
        btnFilterDrinks = findViewById(R.id.btnFilterDrinks)
        btnFilterRooms = findViewById(R.id.btnFilterRooms)
        btnFilterHalls = findViewById(R.id.btnFilterHalls)

        btnResetPassword.setOnClickListener { showResetPasswordDialog() }

        btnViewFinancialReports.setOnClickListener { showFinancialReportDialog() }
        tvFinanceDailySales.setOnClickListener { showFinancialReportDialog() }
        tvFinanceBankDeposit.setOnClickListener { showFinancialReportDialog() }

        btnFinanceRequisitions.setOnClickListener { showPendingApprovalsDialog() }

        // Mabadiliko ya Spinner ili iwe na muonekano nadhifu, mpana na usiojibana
        val categories = arrayOf("Vyakula", "Vinywaji", "Chumba", "Ukumbi")
        val spinnerAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(Color.parseColor("#1A202C"))
                view.textSize = 14f
                view.setPadding(4, 0, 4, 0)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(Color.parseColor("#1A202C"))
                view.setBackgroundColor(Color.WHITE)
                view.textSize = 14f
                view.setPadding(24, 20, 24, 20) // Nafasi nzuri (padding) isiyobana maneno
                return view
            }
        }
        spCategory.adapter = spinnerAdapter

        rvFinanceProducts.layoutManager = LinearLayoutManager(this)
        adapter = FinanceProductAdapter(
            displayedProductList,
            onDeleteClick = { itemToDelete -> showDeleteConfirmationDialog(itemToDelete) },
            onEditClick = { itemToEdit -> showEditProductDialog(itemToEdit) }
        )
        rvFinanceProducts.adapter = adapter

        btnAddProduct.setOnClickListener { saveProductToDatabase() }

        etSearchProduct.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { applyFilters() }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnFilterAll.setOnClickListener { filterByCategory("ZOTE") }
        btnFilterFood.setOnClickListener { filterByCategory("Vyakula") }
        btnFilterDrinks.setOnClickListener { filterByCategory("Vinywaji") }
        btnFilterRooms.setOnClickListener { filterByCategory("Chumba") }
        btnFilterHalls.setOnClickListener { filterByCategory("Ukumbi") }

        fetchDashboardData()
    }

    override fun onResume() {
        super.onResume()
        fetchProducts()
        fetchDashboardData()
    }

    private fun filterByCategory(category: String) {
        currentCategoryFilter = category
        applyFilters()
    }

    private fun applyFilters() {
        val query = etSearchProduct.text.toString().trim().lowercase()
        val filtered = fullProductList.filter { item ->
            val matchesCategory = if (currentCategoryFilter == "ZOTE") true else item.category?.equals(currentCategoryFilter, ignoreCase = true) == true
            val matchesSearch = if (query.isEmpty()) true else item.name.lowercase().contains(query) || (item.category?.lowercase()?.contains(query) == true)
            matchesCategory && matchesSearch
        }

        displayedProductList.clear()
        displayedProductList.addAll(filtered)
        adapter.notifyDataSetChanged()
    }

    private fun fetchDashboardData() {
        apiService.getDailySales().enqueue(object : Callback<DailySalesResponse> {
            override fun onResponse(call: Call<DailySalesResponse>, response: Response<DailySalesResponse>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful && response.body() != null) {
                    val sales = response.body()!!
                    tvFinanceDailySales.text = "TSH ${String.format("%,.0f", sales.gross_total)}"
                    tvFinanceBankDeposit.text = "TSH ${String.format("%,.0f", sales.total_deposited)}"
                }
            }
            override fun onFailure(call: Call<DailySalesResponse>, t: Throwable) {}
        })
    }

    private fun fetchProducts() {
        apiService.getProducts().enqueue(object : Callback<List<MenuItem>> {
            override fun onResponse(call: Call<List<MenuItem>>, response: Response<List<MenuItem>>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful && response.body() != null && response.body()!!.isNotEmpty()) {
                    fullProductList.clear()
                    fullProductList.addAll(response.body()!!)
                    applyFilters()
                } else {
                    fetchProductsFallback()
                }
            }

            override fun onFailure(call: Call<List<MenuItem>>, t: Throwable) {
                fetchProductsFallback()
            }
        })
    }

    private fun fetchProductsFallback() {
        apiService.getMenuItems().enqueue(object : Callback<List<MenuItem>> {
            override fun onResponse(call: Call<List<MenuItem>>, response: Response<List<MenuItem>>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful && response.body() != null) {
                    fullProductList.clear()
                    fullProductList.addAll(response.body()!!)
                    applyFilters()
                }
            }
            override fun onFailure(call: Call<List<MenuItem>>, t: Throwable) {}
        })
    }

    private fun saveProductToDatabase() {
        val name = etProductName.text.toString().trim()
        val priceStr = etProductPrice.text.toString().trim()
        val category = spCategory.selectedItem.toString()

        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Ingiza jina na bei sahihi!", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.toDoubleOrNull() ?: 0.0
        val addRequest = AddProductRequest(name = name, price = price, category = category)

        apiService.addProduct(addRequest).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful) {
                    Toast.makeText(this@FinancePriceActivity, "'$name' imesajiliwa kikamilifu!", Toast.LENGTH_SHORT).show()
                    etProductName.text.clear()
                    etProductPrice.text.clear()
                    fetchProducts()
                } else {
                    val menuData = hashMapOf<String, Any>("name" to name, "price" to price, "category" to category)
                    apiService.addMenuItem(menuData).enqueue(object : Callback<GenericResponse> {
                        override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                            if (isFinishing || isDestroyed) return
                            Toast.makeText(this@FinancePriceActivity, "'$name' imesajiliwa!", Toast.LENGTH_SHORT).show()
                            etProductName.text.clear()
                            etProductPrice.text.clear()
                            fetchProducts()
                        }
                        override fun onFailure(call: Call<GenericResponse>, t: Throwable) {}
                    })
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                Toast.makeText(this@FinancePriceActivity, "Hitilafu ya mtandao: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showPendingApprovalsDialog() {
        pendingRequisitionsDialog?.dismiss()

        apiService.getRequisitions().enqueue(object : Callback<List<RequisitionItem>> {
            override fun onResponse(call: Call<List<RequisitionItem>>, response: Response<List<RequisitionItem>>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful && response.body() != null) {
                    val allReqs = response.body()!!

                    val pendingFinance = allReqs.filter {
                        val status = it.status ?: ""
                        status.equals("Approved_Procurement", ignoreCase = true) ||
                                status.equals("Procurement_Approved", ignoreCase = true) ||
                                status.equals("Rejected_Principal", ignoreCase = true)
                    }

                    val builder = AlertDialog.Builder(this@FinancePriceActivity)
                    builder.setTitle("📋 Idhini na Maombi Mhasibu")

                    val layout = LinearLayout(this@FinancePriceActivity)
                    layout.orientation = LinearLayout.VERTICAL
                    layout.setPadding(40, 30, 40, 10)

                    if (pendingFinance.isEmpty()) {
                        val tvEmpty = TextView(this@FinancePriceActivity)
                        tvEmpty.text = "Hakuna maombi mapya yanayosubiri idhini au kurekebishwa na Mhasibu."
                        tvEmpty.textSize = 14f
                        tvEmpty.setPadding(0, 20, 0, 20)
                        layout.addView(tvEmpty)
                        builder.setView(layout)
                        builder.setPositiveButton("Funga", null)
                        pendingRequisitionsDialog = builder.show()
                    } else {
                        val adapter = ProcurementRequisitionAdapter(pendingFinance) { selectedReq ->
                            showApproveConfirmationDialog(selectedReq)
                        }
                        val recyclerView = RecyclerView(this@FinancePriceActivity)
                        recyclerView.layoutManager = LinearLayoutManager(this@FinancePriceActivity)
                        recyclerView.adapter = adapter

                        builder.setView(recyclerView)
                        builder.setPositiveButton("Funga", null)
                        pendingRequisitionsDialog = builder.show()
                    }
                } else {
                    Toast.makeText(this@FinancePriceActivity, "Imeshindwa kupakua maombi!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<RequisitionItem>>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                Toast.makeText(this@FinancePriceActivity, "Hitilafu ya mtandao: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showApproveConfirmationDialog(requisition: RequisitionItem) {
        val builder = AlertDialog.Builder(this)

        val isFromPrincipal = requisition.status.equals("Rejected_Principal", ignoreCase = true)
        val titleText = if (isFromPrincipal) "❌ Ombi Lililorudishwa na Principal" else "📋 Maamuzi ya Mhasibu: ${requisition.item_name}"
        builder.setTitle(titleText)

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

        val commentInfo = if (!requisition.rejection_comment.isNullOrBlank() && requisition.rejection_comment.trim().isNotEmpty()) {
            "\n\n💬 Sababu/Comment ya Kurudishwa:\n\"${requisition.rejection_comment}\""
        } else ""

        val detailsText = "• Idara: ${requisition.department ?: "Jikoni"}\n" +
                "• Kiasi: ${requisition.quantity} ${requisition.unit ?: ""}\n" +
                "• Mzabuni: ${requisition.supplier ?: "Haijawekwa"}\n" +
                "• Gharama za Sokoni: TZS ${String.format("%,.0f", requisition.estimated_cost ?: 0.0)}" + ratioInfo + commentInfo

        val tvDetails = TextView(this).apply {
            text = detailsText
            setTextColor(Color.WHITE)
            textSize = 14f
            setLineSpacing(6f, 1f)
        }
        layout.addView(tvDetails)

        builder.setView(layout)

        builder.setPositiveButton("✅ IDHINISHA KWA PRINCIPAL") { dialog, _ ->
            val reqId = requisition.id ?: ""
            if (reqId.isNotEmpty()) {
                approveRequisitionForFinance(reqId)
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("❌ RUDISHA KWA PROCUREMENT") { dialog, _ ->
            dialog.dismiss()
            showReturnToProcurementDialog(requisition)
        }

        builder.setNeutralButton("GHAIRI", null)

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#48BB78"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#F56565"))
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(Color.WHITE)
    }

    private fun showReturnToProcurementDialog(requisition: RequisitionItem) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("↩️ Rudisha Ombi kwa Procurement")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        val etComment = EditText(this).apply {
            hint = "Andika sababu za kurudisha ombi hili..."
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

            val reqId = requisition.id ?: ""
            val updateData = hashMapOf<String, Any>(
                "status" to "Rejected_Finance",
                "rejection_comment" to comment
            )

            apiService.updateRequisitionApproval(reqId, updateData).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (isFinishing || isDestroyed) return
                    if (response.isSuccessful) {
                        Toast.makeText(this@FinancePriceActivity, "Ombi limerudishwa kwa Procurement kikamilifu!", Toast.LENGTH_LONG).show()
                        showPendingApprovalsDialog()
                    }
                }
                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    if (isFinishing || isDestroyed) return
                    Toast.makeText(this@FinancePriceActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
            dialog.dismiss()
        }

        builder.setNegativeButton("GHAIRI", null)
        builder.show()
    }

    private fun approveRequisitionForFinance(reqId: String) {
        val updateData = hashMapOf<String, Any>(
            "status" to "Approved_Finance"
        )

        apiService.updateRequisitionApproval(reqId, updateData).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful) {
                    Toast.makeText(this@FinancePriceActivity, "Ombi limeidhinishwa na kutumwa kwa Mkuu wa Chuo (Principal)!", Toast.LENGTH_LONG).show()
                    showPendingApprovalsDialog()
                }
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                Toast.makeText(this@FinancePriceActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showEditProductDialog(item: MenuItem) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Hariri Taarifa za ${item.name}")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)
        layout.setBackgroundColor(Color.parseColor("#1E293B"))

        val inputName = EditText(this)
        inputName.setText(item.name)
        inputName.setTextColor(Color.WHITE)
        inputName.setHintTextColor(Color.parseColor("#94A3B8"))
        layout.addView(inputName)

        val inputPrice = EditText(this)
        inputPrice.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        inputPrice.setText(item.price.toString())
        inputPrice.setTextColor(Color.WHITE)
        inputPrice.setHintTextColor(Color.parseColor("#94A3B8"))
        layout.addView(inputPrice)

        val spinnerEditCategory = Spinner(this)
        val categories = arrayOf("Vyakula", "Vinywaji", "Chumba", "Ukumbi")

        val spinnerAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(Color.WHITE)
                view.textSize = 16f
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(Color.WHITE)
                view.setBackgroundColor(Color.parseColor("#334155"))
                return view
            }
        }

        spinnerEditCategory.adapter = spinnerAdapter

        val categoryIndex = categories.indexOfFirst { it.equals(item.category, ignoreCase = true) }
        if (categoryIndex >= 0) {
            spinnerEditCategory.setSelection(categoryIndex)
        }
        layout.addView(spinnerEditCategory)

        builder.setView(layout)

        builder.setPositiveButton("Hifadhi Mabadiliko") { dialog, _ ->
            val newName = inputName.text.toString().trim()
            val newPriceStr = inputPrice.text.toString().trim()
            val newCategory = spinnerEditCategory.selectedItem.toString()

            if (newName.isNotEmpty() && newPriceStr.isNotEmpty()) {
                val newPrice = newPriceStr.toDoubleOrNull() ?: 0.0
                item.id?.let { itemId -> updateProductInDatabase(itemId, newName, newPrice, newCategory) }
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Ghairi") { dialog, _ -> dialog.cancel() }

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#38BDF8"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#F87171"))
    }

    private fun updateProductInDatabase(id: String, name: String, price: Double, category: String) {
        val updateData = hashMapOf<String, Any>(
            "name" to name,
            "price" to price,
            "category" to category
        )

        apiService.updateMenuItem(id, updateData).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (isFinishing || isDestroyed) return
                Toast.makeText(this@FinancePriceActivity, "Mabadiliko yamehifadhiwa kikamilifu!", Toast.LENGTH_SHORT).show()
                fetchProducts()
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                apiService.updateProduct(id, AddProductRequest(name, price, category)).enqueue(object : Callback<GenericResponse> {
                    override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                        if (isFinishing || isDestroyed) return
                        Toast.makeText(this@FinancePriceActivity, "Mabadiliko yamehifadhiwa!", Toast.LENGTH_SHORT).show()
                        fetchProducts()
                    }
                    override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                        if (isFinishing || isDestroyed) return
                        Toast.makeText(this@FinancePriceActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        })
    }

    private fun showDeleteConfirmationDialog(item: MenuItem) {
        AlertDialog.Builder(this)
            .setTitle("Futa Bidhaa")
            .setMessage("Je, una uhakika unataka kufuta '${item.name}' kwenye mfumo?")
            .setPositiveButton("Ndiyo, Futa") { _, _ -> deleteProductFromDatabase(item) }
            .setNegativeButton("Hapana", null)
            .show()
    }

    private fun deleteProductFromDatabase(item: MenuItem) {
        val itemId = item.id ?: return

        apiService.deleteMenuItem(itemId).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (isFinishing || isDestroyed) return
                Toast.makeText(this@FinancePriceActivity, "'${item.name}' imefutwa kikamilifu!", Toast.LENGTH_SHORT).show()
                fetchProducts()
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                apiService.deleteProduct(itemId).enqueue(object : Callback<GenericResponse> {
                    override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                        if (isFinishing || isDestroyed) return
                        Toast.makeText(this@FinancePriceActivity, "'${item.name}' imefutwa!", Toast.LENGTH_SHORT).show()
                        fetchProducts()
                    }
                    override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                        if (isFinishing || isDestroyed) return
                        Toast.makeText(this@FinancePriceActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
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
                        if (isFinishing || isDestroyed) return
                        if (response.isSuccessful) {
                            Toast.makeText(this@FinancePriceActivity, "Password imebadilishwa kikamilifu!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@FinancePriceActivity, "Taarifa sio sahihi!", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                        if (isFinishing || isDestroyed) return
                        Toast.makeText(this@FinancePriceActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
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
                    if (isFinishing || isDestroyed) return
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
                    if (isFinishing || isDestroyed) return
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
}