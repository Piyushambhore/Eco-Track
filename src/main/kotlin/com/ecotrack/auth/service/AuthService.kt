package com.ecotrack.auth.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ecotrack.auth.api.*
import com.ecotrack.auth.repository.UserEntity
import com.ecotrack.auth.repository.UserRepository
import com.ecotrack.common.*
import com.ecotrack.config.JwtConfig
import java.util.*

class AuthService(
    private val userRepository: UserRepository,
    private val jwtConfig: JwtConfig
) {
    fun register(request: RegisterRequest): AuthResponse {
        validateRegistration(request)

        val existing = userRepository.findByEmail(request.email)
        if (existing != null) {
            throw ConflictException("An account with this email already exists")
        }

        val passwordHash = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())
        val userId = UUID.randomUUID()

        val user = userRepository.create(
            id = userId,
            email = request.email,
            passwordHash = passwordHash,
            displayName = request.displayName,
            role = request.role.ifBlank { "CITIZEN" }
        )

        val token = generateToken(user)
        return AuthResponse(
            token = token,
            user = user.toResponse()
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        if (request.email.isBlank() || request.password.isBlank()) {
            throw ValidationException("Email and password are required")
        }

        val user = userRepository.findByEmail(request.email)
            ?: throw UnauthorizedException("Invalid email or password")

        if (user.status != "active") {
            throw ForbiddenException("Account is not active")
        }

        val result = BCrypt.verifyer().verify(request.password.toCharArray(), user.passwordHash)
        if (!result.verified) {
            throw UnauthorizedException("Invalid email or password")
        }

        val token = generateToken(user)
        return AuthResponse(
            token = token,
            user = user.toResponse()
        )
    }

    fun getProfile(userId: UUID): UserResponse {
        val user = userRepository.findById(userId)
            ?: throw NotFoundException("User not found")
        return user.toResponse()
    }

    fun updateProfile(userId: UUID, request: UpdateProfileRequest): UserResponse {
        if (request.displayName == null && request.email == null) {
            throw ValidationException("At least one field must be provided for update")
        }

        request.displayName?.let {
            if (it.isBlank() || it.length > 100) {
                throw ValidationException("Display name must be between 1 and 100 characters")
            }
        }

        request.email?.let {
            if (!it.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
                throw ValidationException("Invalid email format")
            }
            val existing = userRepository.findByEmail(it)
            if (existing != null && existing.id != userId) {
                throw ConflictException("Email is already in use")
            }
        }

        val updated = userRepository.updateProfile(userId, request.displayName, request.email)
            ?: throw NotFoundException("User not found")
        return updated.toResponse()
    }

    fun getUserStats(): Map<String, Long> {
        return mapOf("totalUsers" to userRepository.countUsers())
    }

    private fun generateToken(user: UserEntity): String {
        return JWT.create()
            .withAudience(jwtConfig.audience)
            .withIssuer(jwtConfig.issuer)
            .withClaim("userId", user.id.toString())
            .withClaim("email", user.email)
            .withClaim("displayName", user.displayName)
            .withClaim("role", user.role)
            .withExpiresAt(Date(System.currentTimeMillis() + jwtConfig.expirationSeconds * 1000))
            .sign(Algorithm.HMAC256(jwtConfig.secret))
    }

    private fun validateRegistration(request: RegisterRequest) {
        val errors = mutableListOf<FieldError>()

        if (request.email.isBlank()) {
            errors.add(FieldError("email", "must not be blank"))
        } else if (!request.email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
            errors.add(FieldError("email", "must be a valid email address"))
        }

        if (request.password.isBlank()) {
            errors.add(FieldError("password", "must not be blank"))
        } else if (request.password.length < 8) {
            errors.add(FieldError("password", "must be at least 8 characters"))
        }

        if (request.displayName.isBlank()) {
            errors.add(FieldError("displayName", "must not be blank"))
        } else if (request.displayName.length > 100) {
            errors.add(FieldError("displayName", "must not exceed 100 characters"))
        }

        if (errors.isNotEmpty()) {
            throw ValidationException("The request contains invalid fields.", errors)
        }
    }

    private fun UserEntity.toResponse() = UserResponse(
        id = id.toString(),
        email = email,
        displayName = displayName,
        status = status,
        role = role,
        createdAt = createdAt.toString()
    )
}
