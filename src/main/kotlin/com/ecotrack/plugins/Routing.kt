package com.ecotrack.plugins

import com.ecotrack.auth.api.authRoutes
import com.ecotrack.auth.repository.UserRepository
import com.ecotrack.auth.service.AuthService
import com.ecotrack.config.JwtConfig
import com.ecotrack.database.DatabaseFactory
import com.ecotrack.collections.api.collectionRoutes
import com.ecotrack.collections.service.CollectionService
import com.ecotrack.marketplace.api.marketplaceRoutes
import com.ecotrack.marketplace.service.MarketplaceService
import com.ecotrack.rewards.api.rewardsRoutes
import com.ecotrack.rewards.service.RewardsService
import com.ecotrack.impact.api.impactRoutes
import com.ecotrack.impact.service.ImpactService
import com.ecotrack.admin.api.adminRoutes
import com.ecotrack.admin.repository.AdminRepository
import com.ecotrack.admin.service.AdminService
import com.ecotrack.records.api.recordRoutes
import com.ecotrack.records.repository.RecordRepository
import com.ecotrack.records.service.RecordService
import com.ecotrack.waste.api.wasteRoutes
import com.ecotrack.waste.service.WasteReportService
import com.ecotrack.aws.awsRoutes
import com.ecotrack.aws.AwsCloudService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val service: String = "ecotrack-api",
    val version: String = "0.1.0"
)

@Serializable
data class ReadyResponse(
    val status: String,
    val service: String = "ecotrack-api",
    val database: String
)

fun Application.configureRouting(jwtConfig: JwtConfig) {
    // Initialize repositories and services
    val userRepository = UserRepository()
    val authService = AuthService(userRepository, jwtConfig)
    val recordRepository = RecordRepository()
    val recordService = RecordService(recordRepository)
    val wasteReportService = WasteReportService()
    val rewardsService = RewardsService()
    val collectionService = CollectionService(rewardsService = rewardsService)
    val marketplaceService = MarketplaceService(rewardsService = rewardsService)
    val impactService = ImpactService()
    val adminRepository = AdminRepository()
    val adminService = AdminService(adminRepository)

    routing {
        // Health endpoints
        get("/health/live") {
            call.respond(HttpStatusCode.OK, HealthResponse(status = "UP"))
        }

        get("/health/ready") {
            val dbHealthy = DatabaseFactory.isHealthy()
            val status = if (dbHealthy) "UP" else "DOWN"
            val httpStatus = if (dbHealthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
            call.respond(httpStatus, ReadyResponse(
                status = status,
                database = if (dbHealthy) "connected" else "disconnected"
            ))
        }

        // API info
        get("/api") {
            call.respond(HttpStatusCode.OK, mapOf(
                "service" to "EcoTrack API",
                "version" to "v1",
                "docs" to "/api/v1",
                "health" to "/health/live"
            ))
        }

        // Auth routes
        authRoutes(authService)

        // Record routes
        recordRoutes(recordService)

        // Waste & AI Scanner routes
        wasteRoutes(wasteReportService)

        // Phase 3: Location-Aware Recycler Search & Collection Logistics
        collectionRoutes(collectionService)

        // Phase 4: Circular Marketplace & Material Trading
        marketplaceRoutes(marketplaceService)

        // Phase 5: Gamified Rewards Ledger & Community Leaderboards
        rewardsRoutes(rewardsService)

        // Phase 6: Ecological Impact Calculator & ESG Analytics
        impactRoutes(impactService)

        // Phase 7: Admin Governance, Municipal Telemetry & Production Go-Live
        adminRoutes(adminService)

        // AWS Cloud Services: Amazon S3, Rekognition, SNS, CloudWatch
        val awsService = AwsCloudService()
        awsRoutes(awsService)
    }
}

