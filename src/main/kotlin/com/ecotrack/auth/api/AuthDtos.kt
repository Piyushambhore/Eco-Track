package com.ecotrack.auth.api

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
    val role: String = "CITIZEN"
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserResponse
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val displayName: String,
    val status: String,
    val role: String = "CITIZEN",
    val createdAt: String
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val email: String? = null
)
