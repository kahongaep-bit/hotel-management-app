package com.hotelmanagementsystem

// Data Models za Login & Authentication PEKEE
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val user: UserData?
)

data class UserData(
    val id: Int,
    val full_name: String,
    val email: String,
    val role: String
)