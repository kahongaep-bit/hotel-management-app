package com.hotelmanagementsystem

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
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

class AdminActivity : AppCompatActivity() {

    private lateinit var rvUsers: RecyclerView
    private lateinit var btnAddUser: Button
    private lateinit var btnRefreshUsers: Button
    private lateinit var tvAdminHeader: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Tunatengeneza UI kwa Code moja kwa moja ili kuepusha kukosekana kwa XML layout
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
            setBackgroundColor(Color.parseColor("#0F172A"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        tvAdminHeader = TextView(this).apply {
            text = "🛡️ Usimamizi wa Watumiaji (Admin Panel)"
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(0, 0, 0, 20)
        }
        mainLayout.addView(tvAdminHeader)

        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 20)
        }

        btnAddUser = Button(this).apply {
            text = "➕ Ongeza Mtumiaji"
            setBackgroundColor(Color.parseColor("#3182CE"))
            setTextColor(Color.WHITE)
            setOnClickListener { showAddUserDialog() }
        }

        btnRefreshUsers = Button(this).apply {
            text = "🔄 Sasisha"
            setBackgroundColor(Color.parseColor("#4A5568"))
            setTextColor(Color.WHITE)
            setOnClickListener { fetchUsersList() }
        }

        buttonLayout.addView(btnAddUser)
        buttonLayout.addView(btnRefreshUsers)
        mainLayout.addView(buttonLayout)

        rvUsers = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        mainLayout.addView(rvUsers)

        setContentView(mainLayout)
        fetchUsersList()
    }

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://hotel-backend-production-617c.up.railway.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun fetchUsersList() {
        val apiService = getRetrofit().create(ApiService::class.java)
        apiService.getAllUsers().enqueue(object : Callback<List<UserData>> {
            override fun onResponse(call: Call<List<UserData>>, response: Response<List<UserData>>) {
                if (response.isSuccessful && response.body() != null) {
                    val usersList = response.body()!!
                    setupUsersAdapter(usersList)
                } else {
                    Toast.makeText(this@AdminActivity, "Imeshindwa kupata watumiaji!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<UserData>>, t: Throwable) {
                Toast.makeText(this@AdminActivity, "Hitilafu ya mtandao: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupUsersAdapter(users: List<UserData>) {
        val adapter = UserAdapter(users,
            onEditRoleClick = { user -> showChangeRoleDialog(user) },
            onDeleteClick = { user -> showDeleteUserConfirmation(user) }
        )
        rvUsers.adapter = adapter
    }

    private fun showAddUserDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("➕ Ongeza Mtumiaji Mpya")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        val etName = EditText(this).apply {
            hint = "Jina Kamili (Full Name)"
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
        }
        val etEmail = EditText(this).apply {
            hint = "Barua Pepe (Email)"
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val etPassword = EditText(this).apply {
            hint = "Nenosiri (Password)"
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val roles = arrayOf("Manager", "Cashier", "Principal", "Kitchen", "Bartender", "Production", "Procurement", "Finance", "admin")
        val spinnerRole = Spinner(this)
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        spinnerRole.adapter = spinnerAdapter

        layout.addView(etName)
        layout.addView(etEmail)
        layout.addView(etPassword)
        layout.addView(TextView(this).apply { text = "Chagua Role:"; setTextColor(Color.WHITE); setPadding(0, 10, 0, 0) })
        layout.addView(spinnerRole)

        builder.setView(layout)

        builder.setPositiveButton("ONGEZA") { dialog, _ ->
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val selectedRole = spinnerRole.selectedItem.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Tafadhali jaza sehemu zote!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            createNewUser(name, email, password, selectedRole)
            dialog.dismiss()
        }
        builder.setNegativeButton("GHAIRI", null)
        builder.show()
    }

    private fun createNewUser(name: String, email: String, pass: String, role: String) {
        val apiService = getRetrofit().create(ApiService::class.java)
        val request = AddUserRequest(name, email, pass, role)

        apiService.addNewUser(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminActivity, "Mtumiaji ameongezwa mafanikio!", Toast.LENGTH_SHORT).show()
                    fetchUsersList()
                } else {
                    Toast.makeText(this@AdminActivity, "Imeshindikana kuongeza mtumiaji!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@AdminActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showChangeRoleDialog(user: UserData) {
        val roles = arrayOf("Manager", "Cashier", "Principal", "Kitchen", "Bartender", "Production", "Procurement", "Finance", "admin")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("🔄 Badilisha Role ya ${user.full_name}")

        val spinnerRole = Spinner(this)
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        spinnerRole.adapter = spinnerAdapter

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
            addView(spinnerRole)
        }
        builder.setView(layout)

        builder.setPositiveButton("BADILISHA") { dialog, _ ->
            val newRole = spinnerRole.selectedItem.toString()
            updateUserRoleOnServer(user.id, newRole)
            dialog.dismiss()
        }
        builder.setNegativeButton("GHAIRI", null)
        builder.show()
    }

    private fun updateUserRoleOnServer(userId: Int, newRole: String) {
        val apiService = getRetrofit().create(ApiService::class.java)
        val data = hashMapOf("role" to newRole)

        apiService.updateUserRole(userId, data).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminActivity, "Role imebadilishwa kikamilifu!", Toast.LENGTH_SHORT).show()
                    fetchUsersList()
                } else {
                    Toast.makeText(this@AdminActivity, "Imeshindikana kubadilisha role!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@AdminActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDeleteUserConfirmation(user: UserData) {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Futa Mtumiaji")
            .setMessage("Una hakika unataka kumfuta ${user.full_name}?")
            .setPositiveButton("NDIYO, FUTA") { _, _ ->
                deleteUserFromServer(user.id)
            }
            .setNegativeButton("GHAIRI", null)
            .show()
    }

    private fun deleteUserFromServer(userId: Int) {
        val apiService = getRetrofit().create(ApiService::class.java)
        apiService.deleteUser(userId).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminActivity, "Mtumiaji amefutwa!", Toast.LENGTH_SHORT).show()
                    fetchUsersList()
                } else {
                    Toast.makeText(this@AdminActivity, "Imeshindikana kufuta!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@AdminActivity, "Hitilafu: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

// ---- ADAPTER YA KUONYESHA WATUMIAJI KENYE LIST ----
class UserAdapter(
    private val users: List<UserData>,
    private val onEditRoleClick: (UserData) -> Unit,
    private val onDeleteClick: (UserData) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(val layout: LinearLayout, val tvInfo: TextView, val btnRole: Button, val btnDelete: Button) : RecyclerView.ViewHolder(layout)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val context = parent.context
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.parseColor("#1E293B"))
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 10) }
        }

        val tvInfo = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnRole = Button(context).apply {
            text = "Role"
            textSize = 11f
            setBackgroundColor(Color.parseColor("#D69E2E"))
            setTextColor(Color.WHITE)
        }

        val btnDelete = Button(context).apply {
            text = "Futa"
            textSize = 11f
            setBackgroundColor(Color.parseColor("#E53E3E"))
            setTextColor(Color.WHITE)
        }

        layout.addView(tvInfo)
        layout.addView(btnRole)
        layout.addView(btnDelete)

        return UserViewHolder(layout, tvInfo, btnRole, btnDelete)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.tvInfo.text = "👤 ${user.full_name}\n📧 ${user.email}\n🔑 Role: ${user.role}"

        holder.btnRole.setOnClickListener { onEditRoleClick(user) }
        holder.btnDelete.setOnClickListener { onDeleteClick(user) }
    }

    override fun getItemCount(): Int {
        return users.size
    }
}