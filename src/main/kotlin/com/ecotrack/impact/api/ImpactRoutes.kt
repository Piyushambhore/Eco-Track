package com.ecotrack.impact.api

import com.ecotrack.auth.api.extractUserId
import com.ecotrack.common.NotFoundException
import com.ecotrack.common.ValidationException
import com.ecotrack.impact.model.CreateCertificateRequest
import com.ecotrack.impact.service.ImpactService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.impactRoutes(service: ImpactService) {

    // Public environmental factors, community benchmarks, and verification endpoints
    route("/api/v1/impact") {
        get("/factors") {
            val factors = service.getEmissionFactors()
            call.respond(HttpStatusCode.OK, factors)
        }

        get("/community") {
            val name = call.request.queryParameters["community"] ?: "Kolhapur Campus & Municipal District"
            val community = service.getCommunityImpact(name)
            call.respond(HttpStatusCode.OK, community)
        }

        get("/certificates/{id}") {
            val id = call.parameters["id"] ?: throw ValidationException("Missing certificate ID or number")
            val result = service.verifyCertificate(id)
            if (!result.valid) {
                call.respond(HttpStatusCode.NotFound, result)
            } else {
                call.respond(HttpStatusCode.OK, result)
            }
        }
    }

    // Authenticated user personal accounting & certification
    authenticate("auth-jwt") {
        route("/api/v1/impact") {

            get("/personal") {
                val userId = call.extractUserId()
                val impact = service.getPersonalImpact(userId)
                call.respond(HttpStatusCode.OK, impact)
            }

            get("/certificates") {
                val userId = call.extractUserId()
                val certs = service.getUserCertificates(userId)
                call.respond(HttpStatusCode.OK, certs)
            }

            post("/certificates") {
                val userId = call.extractUserId()
                val request = try { call.receive<CreateCertificateRequest>() } catch (_: Exception) { CreateCertificateRequest() }
                val userName = call.request.queryParameters["name"] ?: "EcoTrack Verified Citizen"
                val certificate = service.generateCertificate(userId, userName, request)
                call.respond(HttpStatusCode.Created, certificate)
            }
        }
    }
}
