package com.ecotrack.android.data.api

import com.ecotrack.android.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object EcoTrackApiClient {
    // 10.0.2.2 points to host localhost in Android Emulator.
    // When testing on USB physical device with `adb reverse tcp:8080 tcp:8080`, http://localhost:8080 works directly.
    var baseUrl: String = "http://localhost:8080"
    var authToken: String? = null
    var currentUser: UserDto? = null

    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = false
            })
        }
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> = runCatching {
        val response = client.post("$baseUrl/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            throw Exception("Login failed: HTTP ${response.status.value}")
        }
        val auth: AuthResponse = response.body()
        authToken = auth.token
        currentUser = auth.user
        auth
    }

    suspend fun register(request: RegisterRequest): Result<AuthResponse> = runCatching {
        val response = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            throw Exception("Registration failed: HTTP ${response.status.value}")
        }
        val auth: AuthResponse = response.body()
        authToken = auth.token
        currentUser = auth.user
        auth
    }

    suspend fun identifyWaste(request: IdentifyWasteRequest): Result<WasteClassificationResult> = runCatching {
        val response = client.post("$baseUrl/api/v1/waste/identify") {
            contentType(ContentType.Application.Json)
            setBody(request)
            authToken?.let { bearer ->
                header(HttpHeaders.Authorization, "Bearer $bearer")
            }
        }
        if (!response.status.isSuccess()) {
            throw Exception("AI Scan failed: HTTP ${response.status.value}")
        }
        response.body()
    }

    suspend fun getCategories(): Result<List<WasteCategoryInfo>> = runCatching {
        val response = client.get("$baseUrl/api/v1/waste/categories")
        if (!response.status.isSuccess()) {
            throw Exception("Categories fetch failed: HTTP ${response.status.value}")
        }
        response.body()
    }

    suspend fun saveWasteReport(request: CreateWasteReportRequest): Result<WasteReportDto> = runCatching {
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        val response = client.post("$baseUrl/api/v1/waste/reports") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            throw Exception("Save report failed: HTTP ${response.status.value}")
        }
        response.body()
    }

    suspend fun getWasteReports(page: Int = 1, category: String? = null): Result<WasteReportsListResponse> = runCatching {
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        val response = client.get("$baseUrl/api/v1/waste/reports") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("page", page)
            parameter("pageSize", 20)
            category?.let { parameter("category", it) }
        }
        if (!response.status.isSuccess()) {
            throw Exception("Fetch reports failed: HTTP ${response.status.value}")
        }
        response.body()
    }

    fun logout() {
        authToken = null
        currentUser = null
    }
}
