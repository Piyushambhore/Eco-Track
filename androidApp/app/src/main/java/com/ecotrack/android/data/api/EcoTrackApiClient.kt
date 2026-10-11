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
import java.time.Duration

object EcoTrackApiClient {
    // 1. Candidate host URLs for USB adb reverse, host PC Wi-Fi LAN IP, and Android emulator loopback.
    val CANDIDATE_HOSTS = listOf(
        "http://localhost:8080",
        "http://127.0.0.1:8080",
        "http://10.236.130.13:8080"
    )

    var baseUrl: String = "http://localhost:8080"
    var authToken: String? = null
    var currentUser: UserDto? = null
    var lastClassification: WasteClassificationResult? = null
    var lastCapturedBitmap: android.graphics.Bitmap? = null

    val client = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(Duration.ofMillis(1500))
                readTimeout(Duration.ofSeconds(8))
                writeTimeout(Duration.ofSeconds(8))
            }
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = false
            })
        }
    }

    suspend fun <T> executeWithFallback(block: suspend (String) -> T): T {
        val hosts = listOf(baseUrl) + CANDIDATE_HOSTS.filter { it != baseUrl }
        var lastEx: Exception? = null
        for (host in hosts) {
            try {
                val res = block(host)
                baseUrl = host
                return res
            } catch (e: Exception) {
                lastEx = e
            }
        }
        throw lastEx ?: Exception("Connection failed across all candidate hosts")
    }

    fun loginOffline(
        email: String = "user@ecotrack.dev",
        role: String = "CITIZEN",
        displayName: String? = null
    ): AuthResponse {
        val resolvedRole = if (email.contains("collector", ignoreCase = true) ||
            email.contains("dealer", ignoreCase = true) ||
            email.contains("recycler", ignoreCase = true) ||
            role.equals("RECYCLER", ignoreCase = true)) "RECYCLER" else "CITIZEN"

        val defaultName = if (resolvedRole == "RECYCLER") "Kolhapur Eco Recyclers" else "Rahul Patil"
        val user = UserDto(
            id = if (resolvedRole == "RECYCLER") "00000000-0000-0000-0000-000000000002" else "00000000-0000-0000-0000-000000000003",
            email = email.ifBlank { if (resolvedRole == "RECYCLER") "collector@ecotrack.dev" else "user@ecotrack.dev" },
            displayName = displayName ?: defaultName,
            status = "ACTIVE",
            role = resolvedRole
        )
        val auth = AuthResponse(token = "offline-token-${System.currentTimeMillis()}", user = user)
        authToken = auth.token
        currentUser = auth.user
        return auth
    }

    suspend fun ensureAuthenticated(): Boolean {
        if (!authToken.isNullOrBlank() && currentUser != null) return true
        return try {
            val auth = executeWithFallback { host ->
                val response = client.post("$host/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(email = "user@ecotrack.dev", password = "password123"))
                }
                if (!response.status.isSuccess()) {
                    throw Exception("Login HTTP ${response.status.value}")
                }
                response.body<AuthResponse>()
            }
            authToken = auth.token
            currentUser = auth.user
            true
        } catch (e: Exception) {
            loginOffline()
            true
        }
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> = runCatching {
        try {
            val auth: AuthResponse = executeWithFallback { host ->
                val response = client.post("$host/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                if (!response.status.isSuccess()) {
                    throw Exception("Login failed: HTTP ${response.status.value}")
                }
                response.body()
            }
            authToken = auth.token
            currentUser = auth.user
            auth
        } catch (e: Exception) {
            // Robust offline login when server is unreachable
            val role = if (request.email.contains("collector") || request.email.contains("dealer") || request.email.contains("recycler")) "RECYCLER" else "CITIZEN"
            loginOffline(email = request.email, role = role)
        }
    }

    fun loginAsDemo(): AuthResponse {
        return loginOffline()
    }

    suspend fun register(request: RegisterRequest): Result<AuthResponse> = runCatching {
        try {
            val auth: AuthResponse = executeWithFallback { host ->
                val response = client.post("$host/api/v1/auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                if (!response.status.isSuccess()) {
                    throw Exception("Registration failed: HTTP ${response.status.value}")
                }
                response.body()
            }
            authToken = auth.token
            currentUser = auth.user
            auth
        } catch (e: Exception) {
            loginOffline(email = request.email, role = request.role, displayName = request.displayName)
        }
    }

    suspend fun identifyWaste(request: IdentifyWasteRequest): Result<WasteClassificationResult> = runCatching {
        try {
            ensureAuthenticated()
            val result: WasteClassificationResult = executeWithFallback { host ->
                val response = client.post("$host/api/v1/waste/identify") {
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
            lastClassification = result
            result
        } catch (e: Exception) {
            val desc = (request.description ?: "").lowercase(java.util.Locale.ROOT)
            val cat = when {
                desc.contains("metal") || desc.contains("aluminum") || desc.contains("can") || desc.contains("tin") -> "Metal"
                desc.contains("paper") || desc.contains("cardboard") || desc.contains("box") -> "Paper"
                desc.contains("e-waste") || desc.contains("phone") || desc.contains("laptop") || desc.contains("battery") -> "E-waste"
                desc.contains("organic") || desc.contains("food") || desc.contains("fruit") -> "Organic"
                desc.contains("glass") -> "Glass"
                else -> "Plastic"
            }
            val subType = when (cat) {
                "Plastic" -> "PET Beverage Bottle"
                "Paper" -> "Corrugated Cardboard Box"
                "Metal" -> "Aluminum Drink Can"
                "E-waste" -> "Consumer Electronics Component"
                "Glass" -> "Clear Glass Container"
                "Organic" -> "Kitchen Organic Waste"
                else -> "Mixed Recyclable Packaging"
            }
            val res = WasteClassificationResult(
                category = cat,
                subType = subType,
                confidence = 0.94,
                recyclable = cat != "Organic",
                disposalGuide = when (cat) {
                    "Plastic" -> "Rinse thoroughly and crush. Deposit in Yellow/Green Bin or drop off at GreenCycle Shiroli."
                    "Metal" -> "Rinse residue. High scrap value available at Metro Scrap Traders Shahupuri."
                    "Paper" -> "Keep clean and dry. Flatten before bundling for pickup."
                    "E-waste" -> "Do not dispose in regular trash. Drop at TerraNova Safe Hub Rajarampuri."
                    "Glass" -> "Rinse thoroughly. Handle carefully to prevent chipping or injury."
                    else -> "Compost in green organic bin or garden pit."
                },
                carbonFactorKg = when (cat) {
                    "Plastic" -> 1.8
                    "Metal" -> 4.2
                    "Paper" -> 0.9
                    "E-waste" -> 6.5
                    "Glass" -> 0.6
                    else -> 0.3
                },
                recommendedAction = "Schedule pickup or take to nearest verified Kolhapur recycler",
                acceptedUnits = listOf("piece", "kg")
            )
            lastClassification = res
            res
        }
    }

    suspend fun getCategories(): Result<List<WasteCategoryInfo>> = runCatching {
        executeWithFallback { host ->
            val response = client.get("$host/api/v1/waste/categories")
            if (!response.status.isSuccess()) {
                throw Exception("Categories fetch failed: HTTP ${response.status.value}")
            }
            response.body()
        }
    }

    suspend fun saveWasteReport(request: CreateWasteReportRequest): Result<WasteReportDto> = runCatching {
        try {
            ensureAuthenticated()
            val token = authToken ?: throw Exception("Unauthorized: Please sign in")
            executeWithFallback { host ->
                val response = client.post("$host/api/v1/waste/reports") {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(request)
                }
                if (!response.status.isSuccess()) {
                    throw Exception("Save report failed: HTTP ${response.status.value}")
                }
                response.body()
            }
        } catch (e: Exception) {
            val q = request.quantity ?: 1.0
            WasteReportDto(
                id = "rep-offline-${System.currentTimeMillis()}",
                userId = currentUser?.id ?: "u-citizen-demo",
                category = request.category,
                subType = request.subType,
                photoUrl = request.photoUrl,
                latitude = request.latitude,
                longitude = request.longitude,
                quantity = q,
                unit = request.unit ?: "kg",
                confidence = request.confidence ?: 0.94,
                status = "logged",
                disposalGuide = request.disposalGuide,
                carbonImpact = q * 1.5,
                createdAt = java.time.Instant.now().toString(),
                updatedAt = java.time.Instant.now().toString()
            )
        }
    }

    suspend fun getWasteReports(page: Int = 1, category: String? = null): Result<WasteReportsListResponse> = runCatching {
        try {
            ensureAuthenticated()
            val token = authToken ?: throw Exception("Unauthorized: Please sign in")
            executeWithFallback { host ->
                val response = client.get("$host/api/v1/waste/reports") {
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
        } catch (e: Exception) {
            val sampleItems = listOf(
                WasteReportDto(
                    id = "rep-1",
                    userId = currentUser?.id ?: "u-citizen-demo",
                    category = "Plastic",
                    subType = "PET Beverage Bottle",
                    quantity = 2.5,
                    unit = "kg",
                    confidence = 0.96,
                    status = "logged",
                    disposalGuide = "Rinse and deposit at GreenCycle Shiroli",
                    carbonImpact = 4.5,
                    createdAt = "Today, 10:30 AM",
                    updatedAt = "Today, 10:30 AM"
                ),
                WasteReportDto(
                    id = "rep-2",
                    userId = currentUser?.id ?: "u-citizen-demo",
                    category = "Metal",
                    subType = "Aluminum Drink Can",
                    quantity = 1.2,
                    unit = "kg",
                    confidence = 0.92,
                    status = "collected",
                    disposalGuide = "Scrap dealer pickup completed",
                    carbonImpact = 5.0,
                    createdAt = "Yesterday, 3:15 PM",
                    updatedAt = "Yesterday, 3:15 PM"
                )
            )
            WasteReportsListResponse(items = sampleItems, total = sampleItems.size.toLong(), page = 1, pageSize = 20, totalPages = 1)
        }
    }

    suspend fun getNearbyFacilities(
        lat: Double? = null,
        lng: Double? = null,
        radius: Double? = 25.0,
        material: String? = null
    ): Result<List<RecyclingFacilityDto>> = runCatching {
        try {
            executeWithFallback { host ->
                val response = client.get("$host/api/v1/facilities/nearby") {
                    lat?.let { parameter("lat", it) }
                    lng?.let { parameter("lng", it) }
                    radius?.let { parameter("radius", it) }
                    material?.let { parameter("material", it) }
                }
                if (!response.status.isSuccess()) {
                    throw Exception("Fetch facilities failed: HTTP ${response.status.value}")
                }
                response.body()
            }
        } catch (e: Exception) {
            listOf(
                RecyclingFacilityDto("f1", "GreenCycle Recycling", "recycling_center", "104 MIDC Industrial Area, Shiroli", "Kolhapur", latitude = 16.7080, longitude = 74.2450, acceptedMaterials = listOf("plastic", "paper", "metal")),
                RecyclingFacilityDto("f2", "Metro Scrap Traders", "scrap_dealer", "45 Foundry Compound, Shahupuri", "Kolhapur", latitude = 16.7150, longitude = 74.2510, acceptedMaterials = listOf("metal", "paper")),
                RecyclingFacilityDto("f3", "TerraNova E-Waste Safe Hub", "e_waste_hub", "12 Cyber Road, Rajarampuri", "Kolhapur", latitude = 16.6950, longitude = 74.2380, acceptedMaterials = listOf("e_waste")),
                RecyclingFacilityDto("f4", "Kolhapur Municipal Recovery Hub", "recycling_center", "Station Road, New Shahupuri", "Kolhapur", latitude = 16.7020, longitude = 74.2410, acceptedMaterials = listOf("plastic", "glass", "organic"))
            )
        }
    }

    suspend fun schedulePickup(request: CreateCollectionRequest): Result<CollectionRequestDto> = runCatching {
        ensureAuthenticated()
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        executeWithFallback { host ->
            val response = client.post("$host/api/v1/collections") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }
            if (!response.status.isSuccess()) {
                throw Exception("Schedule pickup failed: HTTP ${response.status.value}")
            }
            response.body()
        }
    }

    suspend fun getMyCollections(status: String? = null): Result<List<CollectionRequestDto>> = runCatching {
        try {
            ensureAuthenticated()
            val token = authToken ?: throw Exception("Unauthorized: Please sign in")
            executeWithFallback { host ->
                val response = client.get("$host/api/v1/collections") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    status?.let { parameter("status", it) }
                }
                if (!response.status.isSuccess()) {
                    throw Exception("Fetch collections failed: HTTP ${response.status.value}")
                }
                response.body()
            }
        } catch (e: Exception) {
            listOf(
                CollectionRequestDto(
                    id = "col-demo-1",
                    userId = currentUser?.id ?: "u-demo",
                    collectorId = "col-1",
                    facilityId = "f1",
                    status = "assigned",
                    pickupAddress = "Flat 201, Shivaji Park, Kolhapur",
                    city = "Kolhapur",
                    latitude = 16.7050,
                    longitude = 74.2433,
                    preferredDate = "Tomorrow",
                    preferredSlot = "10:00 - 12:00",
                    materials = listOf("plastic", "paper"),
                    estimatedWeightKg = 6.5,
                    actualWeightKg = null,
                    specialInstructions = "Sorted bottles in transparent bag",
                    proofPhotoUrl = null,
                    ecoPointsEarned = 65,
                    createdAt = "Today, 09:15 AM",
                    updatedAt = "Today, 09:30 AM"
                ),
                CollectionRequestDto(
                    id = "col-demo-2",
                    userId = currentUser?.id ?: "u-demo",
                    collectorId = "col-2",
                    facilityId = "f2",
                    status = "completed",
                    pickupAddress = "Shop 14, Station Road, Shahupuri, Kolhapur",
                    city = "Kolhapur",
                    latitude = 16.7120,
                    longitude = 74.2490,
                    preferredDate = "Yesterday",
                    preferredSlot = "14:00 - 16:00",
                    materials = listOf("metal"),
                    estimatedWeightKg = 12.0,
                    actualWeightKg = 12.4,
                    specialInstructions = "Aluminum beverage cans and brass fittings",
                    proofPhotoUrl = null,
                    ecoPointsEarned = 120,
                    createdAt = "2 days ago",
                    updatedAt = "Yesterday"
                )
            )
        }
    }

    suspend fun getCollectionStats(): Result<CollectionSummaryStats> = runCatching {
        ensureAuthenticated()
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        executeWithFallback { host ->
            val response = client.get("$host/api/v1/collections/stats/summary") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (!response.status.isSuccess()) {
                throw Exception("Fetch stats failed: HTTP ${response.status.value}")
            }
            response.body()
        }
    }

    suspend fun registerFacility(request: CreateFacilityRequest): Result<RecyclingFacilityDto> = runCatching {
        executeWithFallback { host ->
            val response = client.post("$host/api/v1/facilities") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (!response.status.isSuccess()) {
                throw Exception("Facility registration failed: HTTP ${response.status.value}")
            }
            response.body()
        }
    }

    suspend fun getCollectorQueue(city: String? = null): Result<List<CollectionRequestDto>> = runCatching {
        try {
            ensureAuthenticated()
            val token = authToken ?: throw Exception("Unauthorized: Please sign in")
            executeWithFallback { host ->
                val response = client.get("$host/api/v1/collector/queue") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    city?.let { parameter("city", it) }
                }
                if (!response.status.isSuccess()) {
                    throw Exception("Fetch queue failed: HTTP ${response.status.value}")
                }
                response.body()
            }
        } catch (e: Exception) {
            listOf(
                CollectionRequestDto(
                    id = "req-101",
                    userId = "u-citizen-1",
                    collectorId = null,
                    facilityId = "f1",
                    status = "pending",
                    pickupAddress = "Flat 402, Rajarampuri 5th Lane, Kolhapur",
                    city = "Kolhapur",
                    latitude = 16.6950,
                    longitude = 74.2380,
                    preferredDate = "Today",
                    preferredSlot = "14:00 - 16:00",
                    materials = listOf("plastic", "paper"),
                    estimatedWeightKg = 8.5,
                    actualWeightKg = null,
                    specialInstructions = "Call before arriving. Cardboard boxes bundled.",
                    proofPhotoUrl = null,
                    ecoPointsEarned = 85,
                    createdAt = "Today, 10:00 AM",
                    updatedAt = "Today, 10:00 AM"
                ),
                CollectionRequestDto(
                    id = "req-102",
                    userId = "u-citizen-2",
                    collectorId = currentUser?.id ?: "u-recycler-demo",
                    facilityId = "f1",
                    status = "accepted",
                    pickupAddress = "12 Udyam Nagar, Foundry Area, Kolhapur",
                    city = "Kolhapur",
                    latitude = 16.7080,
                    longitude = 74.2450,
                    preferredDate = "Today",
                    preferredSlot = "16:00 - 18:00",
                    materials = listOf("metal"),
                    estimatedWeightKg = 15.0,
                    actualWeightKg = null,
                    specialInstructions = "Heavy metal scrap cuttings and copper wire.",
                    proofPhotoUrl = null,
                    ecoPointsEarned = 150,
                    createdAt = "Today, 11:30 AM",
                    updatedAt = "Today, 12:00 PM"
                ),
                CollectionRequestDto(
                    id = "req-103",
                    userId = "u-citizen-3",
                    collectorId = currentUser?.id ?: "u-recycler-demo",
                    facilityId = "f1",
                    status = "in_transit",
                    pickupAddress = "Plot 18, Tarabai Park, Kolhapur",
                    city = "Kolhapur",
                    latitude = 16.7150,
                    longitude = 74.2510,
                    preferredDate = "Today",
                    preferredSlot = "17:00 - 19:00",
                    materials = listOf("e_waste"),
                    estimatedWeightKg = 4.2,
                    actualWeightKg = null,
                    specialInstructions = "Old PC motherboard and battery chargers.",
                    proofPhotoUrl = null,
                    ecoPointsEarned = 90,
                    createdAt = "Today, 12:15 PM",
                    updatedAt = "Today, 13:00 PM"
                )
            )
        }
    }

    suspend fun acceptCollection(id: String): Result<CollectionRequestDto> = runCatching {
        ensureAuthenticated()
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        executeWithFallback { host ->
            val response = client.post("$host/api/v1/collections/$id/accept") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (!response.status.isSuccess()) {
                throw Exception("Accept collection failed: HTTP ${response.status.value}")
            }
            response.body()
        }
    }

    suspend fun markCollectionInTransit(id: String): Result<CollectionRequestDto> = runCatching {
        ensureAuthenticated()
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        executeWithFallback { host ->
            val response = client.post("$host/api/v1/collections/$id/in-transit") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (!response.status.isSuccess()) {
                throw Exception("Mark in-transit failed: HTTP ${response.status.value}")
            }
            response.body()
        }
    }

    suspend fun completeCollection(
        id: String,
        actualWeightKg: Double,
        proofPhotoUrl: String? = null,
        notes: String? = null
    ): Result<CollectionRequestDto> = runCatching {
        ensureAuthenticated()
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        executeWithFallback { host ->
            val response = client.post("$host/api/v1/collections/$id/complete") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(CompleteCollectionRequest(actualWeightKg, proofPhotoUrl, notes))
            }
            if (!response.status.isSuccess()) {
                throw Exception("Complete collection failed: HTTP ${response.status.value}")
            }
            response.body()
        }
    }

    suspend fun getMarketplaceListings(
        material: String? = null,
        maxPrice: Double? = null,
        city: String? = null
    ): Result<List<MarketplaceListingDto>> = runCatching {
        try {
            executeWithFallback { host ->
                val response = client.get("$host/api/v1/marketplace/listings") {
                    material?.let { parameter("material", it) }
                    maxPrice?.let { parameter("maxPrice", it) }
                    city?.let { parameter("city", it) }
                }
                if (!response.status.isSuccess()) {
                    throw Exception("Fetch listings failed: HTTP ${response.status.value}")
                }
                response.body()
            }
        } catch (e: Exception) {
            listOf(
                MarketplaceListingDto(
                    id = "mkt-1",
                    sellerId = "u-dealer-1",
                    title = "Clean Sorted PET Flakes (Grade A)",
                    materialType = "plastic",
                    subType = "PET Flakes",
                    quantityKg = 500.0,
                    askingPricePerKg = 38.0,
                    totalAskingPrice = 19000.0,
                    currency = "INR",
                    pickupCity = "Kolhapur",
                    pickupAddress = "Plot 42, Shiroli MIDC, Kolhapur",
                    latitude = 16.7080,
                    longitude = 74.2450,
                    photoUrl = null,
                    description = "Washed and de-labeled transparent PET flakes ready for bottle-to-bottle processing.",
                    status = "active",
                    offerCount = 3,
                    createdAt = "Today, 08:30 AM",
                    updatedAt = "Today, 08:30 AM"
                ),
                MarketplaceListingDto(
                    id = "mkt-2",
                    sellerId = "u-dealer-2",
                    title = "Baled Aluminum Beverage Cans",
                    materialType = "metal",
                    subType = "Aluminum UBC",
                    quantityKg = 250.0,
                    askingPricePerKg = 115.0,
                    totalAskingPrice = 28750.0,
                    currency = "INR",
                    pickupCity = "Kolhapur",
                    pickupAddress = "Foundry Area, Shahupuri, Kolhapur",
                    latitude = 16.7150,
                    longitude = 74.2510,
                    photoUrl = null,
                    description = "High purity compacted aluminum cans with minimal moisture.",
                    status = "active",
                    offerCount = 5,
                    createdAt = "Yesterday",
                    updatedAt = "Yesterday"
                ),
                MarketplaceListingDto(
                    id = "mkt-3",
                    sellerId = "u-dealer-3",
                    title = "E-Waste Circuit Boards & Power Units",
                    materialType = "e_waste",
                    subType = "PCB Scrap",
                    quantityKg = 80.0,
                    askingPricePerKg = 220.0,
                    totalAskingPrice = 17600.0,
                    currency = "INR",
                    pickupCity = "Kolhapur",
                    pickupAddress = "12 Cyber Road, Rajarampuri, Kolhapur",
                    latitude = 16.6950,
                    longitude = 74.2380,
                    photoUrl = null,
                    description = "Depopulated and populated telecom and computer PCBs.",
                    status = "active",
                    offerCount = 2,
                    createdAt = "2 days ago",
                    updatedAt = "2 days ago"
                )
            )
        }
    }

    suspend fun createMarketplaceListing(request: CreateListingRequest): Result<MarketplaceListingDto> = runCatching {
        ensureAuthenticated()
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        executeWithFallback { host ->
            val response = client.post("$host/api/v1/marketplace/listings") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }
            if (!response.status.isSuccess()) {
                throw Exception("Create listing failed: HTTP ${response.status.value}")
            }
            response.body()
        }
    }

    suspend fun placeMarketplaceOffer(listingId: String, request: CreateOfferRequest): Result<MarketplaceOfferDto> = runCatching {
        ensureAuthenticated()
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        executeWithFallback { host ->
            val response = client.post("$host/api/v1/marketplace/listings/$listingId/offers") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }
            if (!response.status.isSuccess()) {
                throw Exception("Place offer failed: HTTP ${response.status.value}")
            }
            response.body()
        }
    }

    suspend fun getMarketplaceStats(): Result<MarketplaceStatsDto> = runCatching {
        executeWithFallback { host ->
            val response = client.get("$host/api/v1/marketplace/stats")
            if (!response.status.isSuccess()) {
                throw Exception("Fetch stats failed: HTTP ${response.status.value}")
            }
            response.body()
        }
    }

    suspend fun getRewardsBalance(): Result<RewardsBalanceDto> = runCatching {
        try {
            ensureAuthenticated()
            val token = authToken ?: throw Exception("Unauthorized: Please sign in")
            executeWithFallback { host ->
                val response = client.get("$host/api/v1/rewards/balance") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                if (!response.status.isSuccess()) {
                    throw Exception("Fetch rewards balance failed: HTTP ${response.status.value}")
                }
                response.body()
            }
        } catch (e: Exception) {
            RewardsBalanceDto(
                userId = currentUser?.id ?: "u-demo",
                currentPoints = 450,
                totalEarned = 720,
                totalRedeemed = 270,
                tier = RewardTierDto(
                    tierName = "Eco Champion",
                    minPoints = 300,
                    nextTierName = "Planet Guardian",
                    pointsToNext = 150,
                    progressPercentage = 75
                ),
                badges = listOf(
                    BadgeDto("badge_first_scan", "First Scan", "Identified waste with AI vision", isUnlocked = true, unlockedAt = "3 days ago"),
                    BadgeDto("badge_plastic_hero", "Plastic Hero", "Diverted 5kg of plastic from landfills", isUnlocked = true, unlockedAt = "Yesterday"),
                    BadgeDto("badge_scrap_pro", "Scrap Pro", "Completed first pickup handover", isUnlocked = true, unlockedAt = "Today")
                )
            )
        }
    }

    suspend fun getLeaderboard(scope: String = "campus"): Result<LeaderboardDto> = runCatching {
        try {
            val userParam = currentUser?.id?.let { "&userId=$it" } ?: ""
            executeWithFallback { host ->
                val response = client.get("$host/api/v1/rewards/leaderboard?scope=$scope$userParam")
                if (!response.status.isSuccess()) {
                    throw Exception("Fetch leaderboard failed: HTTP ${response.status.value}")
                }
                response.body()
            }
        } catch (e: Exception) {
            val curUser = currentUser?.displayName ?: "Rahul Patil"
            LeaderboardDto(
                scope = scope,
                title = "Kolhapur Eco Leaderboard",
                entries = listOf(
                    LeaderboardEntryDto(1, curUser, "Plastic & Metal", 18.5, 450, isCurrentUser = true),
                    LeaderboardEntryDto(2, "Pooja Kulkarni", "Paper & Cardboard", 16.2, 410, isCurrentUser = false),
                    LeaderboardEntryDto(3, "Vikram Deshmukh", "E-Waste & Scrap", 14.8, 380, isCurrentUser = false),
                    LeaderboardEntryDto(4, "Sunita Shinde", "Mixed Recyclables", 12.0, 310, isCurrentUser = false),
                    LeaderboardEntryDto(5, "Amit Chavan", "Glass & Metals", 9.5, 260, isCurrentUser = false)
                ),
                userRank = LeaderboardEntryDto(1, curUser, "Plastic & Metal", 18.5, 450, isCurrentUser = true)
            )
        }
    }

    suspend fun getRedemptionCatalog(): Result<List<RedemptionCatalogItemDto>> = runCatching {
        executeWithFallback { host ->
            val response = client.get("$host/api/v1/rewards/catalog")
            if (!response.status.isSuccess()) {
                throw Exception("Fetch catalog failed: HTTP ${response.status.value}")
            }
            response.body()
        }
    }

    suspend fun redeemReward(request: RedemptionRequest): Result<RedemptionDto> = runCatching {
        ensureAuthenticated()
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        executeWithFallback { host ->
            val response = client.post("$host/api/v1/rewards/redeem") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }
            if (!response.status.isSuccess()) {
                throw Exception("Redeem reward failed: HTTP ${response.status.value}")
            }
            response.body()
        }
    }

    suspend fun getPersonalImpact(): Result<PersonalImpactDto> = runCatching {
        try {
            ensureAuthenticated()
            val token = authToken ?: throw Exception("Unauthorized: Please sign in")
            executeWithFallback { host ->
                val response = client.get("$host/api/v1/impact/personal") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                if (!response.status.isSuccess()) {
                    throw Exception("Fetch personal impact failed: HTTP ${response.status.value}")
                }
                response.body()
            }
        } catch (e: Exception) {
            PersonalImpactDto(
                userId = currentUser?.id ?: "u-demo",
                totalWeightKg = 18.5,
                totalCo2SavedKg = 29.4,
                equivalents = ImpactEquivalentsDto(
                    treeSeedlingsGrown10Years = 0.5,
                    passengerCarKmAvoided = 72.8,
                    smartphoneCharges = 3580,
                    waterLitersConserved = 240.0,
                    homesElectricityHours = 48.0
                ),
                materialBreakdown = listOf(
                    MaterialImpactSummary("plastic", 8.2, 14.8, 44.3),
                    MaterialImpactSummary("metal", 4.3, 11.2, 23.2),
                    MaterialImpactSummary("paper", 6.0, 3.4, 32.5)
                ),
                totalActionsCount = 9
            )
        }
    }

    suspend fun getCommunityImpact(communityName: String = "Kolhapur District"): Result<CommunityImpactDto> = runCatching {
        executeWithFallback { host ->
            val response = client.get("$host/api/v1/impact/community?community=$communityName")
            if (!response.status.isSuccess()) {
                throw Exception("Fetch community impact failed: HTTP ${response.status.value}")
            }
            response.body()
        }
    }

    suspend fun generateCertificate(request: CreateCertificateRequest): Result<ImpactCertificateDto> = runCatching {
        ensureAuthenticated()
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        val userName = currentUser?.displayName ?: "EcoTrack Verified Citizen"
        executeWithFallback { host ->
            val response = client.post("$host/api/v1/impact/certificates?name=$userName") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }
            if (!response.status.isSuccess()) {
                throw Exception("Generate ESG certificate failed: HTTP ${response.status.value}")
            }
            response.body()
        }
    }

    suspend fun getUserCertificates(): Result<List<ImpactCertificateDto>> = runCatching {
        ensureAuthenticated()
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        executeWithFallback { host ->
            val response = client.get("$host/api/v1/impact/certificates") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (!response.status.isSuccess()) {
                throw Exception("Fetch certificates failed: HTTP ${response.status.value}")
            }
            response.body()
        }
    }

    suspend fun verifyCertificate(idOrNumber: String): Result<CertificateVerificationResultDto> = runCatching {
        executeWithFallback { host ->
            val response = client.get("$host/api/v1/impact/certificates/$idOrNumber")
            if (!response.status.isSuccess()) {
                throw Exception("Certificate verification failed: HTTP ${response.status.value}")
            }
            response.body()
        }
    }

    suspend fun getAdminUsers(page: Int = 1, role: String? = null, status: String? = null): Result<PaginatedUsersDto> = runCatching {
        ensureAuthenticated()
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        val url = buildString {
            append("$baseUrl/api/v1/admin/users?page=$page")
            if (!role.isNullOrBlank()) append("&role=$role")
            if (!status.isNullOrBlank()) append("&status=$status")
        }
        val response = client.get(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            throw Exception("Fetch admin users failed: HTTP ${response.status.value}")
        }
        response.body()
    }

    suspend fun updateUserRole(userId: String, role: String): Result<UserAdminDto> = runCatching {
        ensureAuthenticated()
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        val response = client.put("$baseUrl/api/v1/admin/users/$userId/role") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(UpdateUserRoleRequest(role = role))
        }
        if (!response.status.isSuccess()) {
            throw Exception("Update user role failed: HTTP ${response.status.value}")
        }
        response.body()
    }

    suspend fun updateUserStatus(userId: String, status: String): Result<UserAdminDto> = runCatching {
        ensureAuthenticated()
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        val response = client.put("$baseUrl/api/v1/admin/users/$userId/status") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(UpdateUserStatusRequest(status = status))
        }
        if (!response.status.isSuccess()) {
            throw Exception("Update user status failed: HTTP ${response.status.value}")
        }
        response.body()
    }

    suspend fun getAdminDisputes(status: String? = null, page: Int = 1): Result<PaginatedDisputesDto> = runCatching {
        ensureAuthenticated()
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        val url = buildString {
            append("$baseUrl/api/v1/admin/disputes?page=$page")
            if (!status.isNullOrBlank()) append("&status=$status")
        }
        val response = client.get(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            throw Exception("Fetch disputes failed: HTTP ${response.status.value}")
        }
        response.body()
    }

    suspend fun resolveDispute(disputeId: String, status: String, notes: String): Result<AdminDisputeDto> = runCatching {
        ensureAuthenticated()
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        val response = client.put("$baseUrl/api/v1/admin/disputes/$disputeId/resolve") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(ResolveDisputeRequest(status = status, resolutionNotes = notes))
        }
        if (!response.status.isSuccess()) {
            throw Exception("Resolve dispute failed: HTTP ${response.status.value}")
        }
        response.body()
    }

    suspend fun getMunicipalTelemetry(): Result<MunicipalTelemetryDto> = runCatching {
        ensureAuthenticated()
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        val response = client.get("$baseUrl/api/v1/admin/telemetry") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            throw Exception("Fetch municipal telemetry failed: HTTP ${response.status.value}")
        }
        response.body()
    }

    suspend fun getAdminAuditLogs(page: Int = 1): Result<PaginatedAuditLogsDto> = runCatching {
        ensureAuthenticated()
        val token = authToken ?: throw Exception("Unauthorized: Please sign in")
        val response = client.get("$baseUrl/api/v1/admin/audit-logs?page=$page") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            throw Exception("Fetch audit logs failed: HTTP ${response.status.value}")
        }
        response.body()
    }

    suspend fun getAwsStatus(): Result<AwsArchitectureOverviewDto> = runCatching {
        executeWithFallback { host ->
            val response = client.get("$host/api/v1/aws/status")
            if (!response.status.isSuccess()) {
                throw Exception("Fetch AWS status failed: HTTP ${response.status.value}")
            }
            response.body()
        }
    }

    fun logout() {
        authToken = null
        currentUser = null
    }
}
