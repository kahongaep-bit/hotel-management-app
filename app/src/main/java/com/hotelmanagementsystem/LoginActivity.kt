package com.hotelmanagementsystem

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBarLogin: ProgressBar

    private var apiService: ApiService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBarLogin = findViewById(R.id.progressBarLogin)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://hotel-backend-production-617c.up.railway.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Jaza Email na Password!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(email, password)
        }
    }

    private fun performLogin(email: String, pass: String) {
        progressBarLogin.visibility = View.VISIBLE
        btnLogin.isEnabled = false

        val request = LoginRequest(email, pass)

        apiService?.loginUser(request)?.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (isFinishing || isDestroyed) return
                progressBarLogin.visibility = View.GONE
                btnLogin.isEnabled = true

                if (response.isSuccessful && response.body() != null) {
                    val loginRes = response.body()!!
                    val role = loginRes.user?.role ?: ""
                    val roleNormalized = role.lowercase()

                    Toast.makeText(this@LoginActivity, "Umeingia kama: $role", Toast.LENGTH_SHORT).show()

                    when {
                        roleNormalized.contains("admin") -> startActivity(Intent(this@LoginActivity, AdminActivity::class.java))
                        roleNormalized.contains("finance") || roleNormalized.contains("mhasibu") -> startActivity(Intent(this@LoginActivity, FinancePriceActivity::class.java))
                        roleNormalized.contains("cashier") || roleNormalized.contains("mweka_hazina") -> startActivity(Intent(this@LoginActivity, CashierActivity::class.java))
                        roleNormalized.contains("manager") -> startActivity(Intent(this@LoginActivity, ManagerRequisitionActivity::class.java))
                        roleNormalized.contains("principal") || roleNormalized.contains("mkuu") -> startActivity(Intent(this@LoginActivity, PrincipalDashboardActivity::class.java))
                        roleNormalized.contains("bar") || roleNormalized.contains("bartender") -> startActivity(Intent(this@LoginActivity, BartenderActivity::class.java))
                        roleNormalized.contains("kitchen") || roleNormalized.contains("jikoni") -> startActivity(Intent(this@LoginActivity, KitchenActivity::class.java))
                        roleNormalized.contains("procurement") -> startActivity(Intent(this@LoginActivity, ProcurementActivity::class.java))
                        roleNormalized.contains("production") -> startActivity(Intent(this@LoginActivity, ProductionManagerActivity::class.java))
                        else -> startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    }
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Email au Password sio sahihi!", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                progressBarLogin.visibility = View.GONE
                btnLogin.isEnabled = true
                Toast.makeText(this@LoginActivity, "Hitilafu ya Mtandao: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}