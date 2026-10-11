package com.ecotrack.auth.api

import com.ecotrack.auth.service.AuthService
import com.ecotrack.common.UnauthorizedException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.authRoutes(authService: AuthService) {

    route("/api/v1/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val response = authService.register(request)
            call.respond(HttpStatusCode.Created, response)
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = authService.login(request)
            call.respond(HttpStatusCode.OK, response)
        }
    }

    authenticate("auth-jwt") {
        route("/api/v1") {
            get("/me") {
                val userId = call.extractUserId()
                val profile = authService.getProfile(userId)
                call.respond(HttpStatusCode.OK, profile)
            }

            patch("/me") {
                val userId = call.extractUserId()
                val request = call.receive<UpdateProfileRequest>()
                val profile = authService.updateProfile(userId, request)
                call.respond(HttpStatusCode.OK, profile)
            }
        }
    }
}

fun ApplicationCall.extractUserId(): UUID {
    val principal = principal<JWTPrincipal>()
        ?: throw UnauthorizedException("No valid token found")
    val userId = principal.payload.getClaim("userId").asString()
        ?: throw UnauthorizedException("Token missing userId claim")
    return try {
        UUID.fromString(userId)
    } catch (e: IllegalArgumentException) {
        throw UnauthorizedException("Invalid userId in token")
    }
}
