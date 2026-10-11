package com.ecotrack.common

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val error: ErrorBody
)

@Serializable
data class ErrorBody(
    val code: String,
    val message: String,
    val requestId: String? = null,
    val details: List<FieldError>? = null
)

@Serializable
data class FieldError(
    val field: String,
    val reason: String
)

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Long,
    val totalPages: Int
)

// Domain exceptions
open class AppException(val code: String, override val message: String, val statusCode: Int) : RuntimeException(message)

class ValidationException(message: String, val errors: List<FieldError> = emptyList()) :
    AppException("VALIDATION_ERROR", message, 400)

class UnauthorizedException(message: String = "Authentication required") :
    AppException("UNAUTHORIZED", message, 401)

class ForbiddenException(message: String = "Access denied") :
    AppException("FORBIDDEN", message, 403)

class NotFoundException(message: String = "Resource not found") :
    AppException("NOT_FOUND", message, 404)

class ConflictException(message: String = "Resource conflict") :
    AppException("CONFLICT", message, 409)
