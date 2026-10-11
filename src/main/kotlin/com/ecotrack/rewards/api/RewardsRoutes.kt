package com.ecotrack.rewards.api

import com.ecotrack.auth.api.extractUserId
import com.ecotrack.common.ValidationException
import com.ecotrack.rewards.model.RedemptionRequest
import com.ecotrack.rewards.service.RewardsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.rewardsRoutes(service: RewardsService) {

    // Public / semi-public catalog & leaderboard
    route("/api/v1/rewards") {
        get("/catalog") {
            val catalog = service.getCatalog()
            call.respond(HttpStatusCode.OK, catalog)
        }

        get("/leaderboard") {
            val scope = call.request.queryParameters["scope"] ?: "campus"
            val userIdParam = call.request.queryParameters["userId"]?.let {
                runCatching { java.util.UUID.fromString(it) }.getOrNull()
            }
            val leaderboard = service.getLeaderboard(scope, userIdParam)
            call.respond(HttpStatusCode.OK, leaderboard)
        }
    }

    // Authenticated user rewards & ledger endpoints
    authenticate("auth-jwt") {
        route("/api/v1/rewards") {

            get("/balance") {
                val userId = call.extractUserId()
                val balance = service.getBalance(userId)
                call.respond(HttpStatusCode.OK, balance)
            }

            get("/history") {
                val userId = call.extractUserId()
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val history = service.getHistory(userId, page, limit)
                call.respond(HttpStatusCode.OK, history)
            }

            post("/redeem") {
                val userId = call.extractUserId()
                val request = call.receive<RedemptionRequest>()
                if (request.rewardType.isBlank()) {
                    throw ValidationException("rewardType must not be blank")
                }
                val redemption = service.redeem(userId, request.rewardType)
                call.respond(HttpStatusCode.Created, redemption)
            }
        }
    }
}
