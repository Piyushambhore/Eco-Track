package com.ecotrack.admin.api

import com.ecotrack.admin.model.*
import com.ecotrack.admin.service.AdminService
import com.ecotrack.auth.api.extractUserId
import com.ecotrack.common.ValidationException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.adminRoutes(service: AdminService) {

    authenticate("auth-jwt") {
        route("/api/v1/admin") {

            // 1. User Governance
            get("/users") {
                val actorId = call.extractUserId()
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                val role = call.request.queryParameters["role"]
                val status = call.request.queryParameters["status"]

                val result = service.listUsers(actorId, page, pageSize, role, status)
                call.respond(HttpStatusCode.OK, result)
            }

            put("/users/{id}/role") {
                val actorId = call.extractUserId()
                val targetIdStr = call.parameters["id"] ?: throw ValidationException("Missing user ID")
                val targetId = try {
                    UUID.fromString(targetIdStr)
                } catch (e: Exception) {
                    throw ValidationException("Invalid UUID format for user ID")
                }

                val request = call.receive<UpdateUserRoleRequest>()
                val ip = call.request.origin.remoteHost
                val updated = service.updateUserRole(actorId, targetId, request, ip)
                call.respond(HttpStatusCode.OK, updated)
            }

            put("/users/{id}/status") {
                val actorId = call.extractUserId()
                val targetIdStr = call.parameters["id"] ?: throw ValidationException("Missing user ID")
                val targetId = try {
                    UUID.fromString(targetIdStr)
                } catch (e: Exception) {
                    throw ValidationException("Invalid UUID format for user ID")
                }

                val request = call.receive<UpdateUserStatusRequest>()
                val ip = call.request.origin.remoteHost
                val updated = service.updateUserStatus(actorId, targetId, request, ip)
                call.respond(HttpStatusCode.OK, updated)
            }

            // 2. Disputes Management
            get("/disputes") {
                val actorId = call.extractUserId()
                val status = call.request.queryParameters["status"]
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

                val result = service.listDisputes(actorId, status, page, pageSize)
                call.respond(HttpStatusCode.OK, result)
            }

            post("/disputes") {
                val reporterId = call.extractUserId()
                val request = call.receive<CreateDisputeRequest>()
                val created = service.submitDispute(reporterId, request)
                call.respond(HttpStatusCode.Created, created)
            }

            put("/disputes/{id}/resolve") {
                val actorId = call.extractUserId()
                val disputeIdStr = call.parameters["id"] ?: throw ValidationException("Missing dispute ID")
                val disputeId = try {
                    UUID.fromString(disputeIdStr)
                } catch (e: Exception) {
                    throw ValidationException("Invalid UUID format for dispute ID")
                }

                val request = call.receive<ResolveDisputeRequest>()
                val ip = call.request.origin.remoteHost
                val resolved = service.resolveDispute(actorId, disputeId, request, ip)
                call.respond(HttpStatusCode.OK, resolved)
            }

            // 3. Municipal Telemetry & Heatmap
            get("/telemetry") {
                val actorId = call.extractUserId()
                val telemetry = service.getMunicipalTelemetry(actorId)
                call.respond(HttpStatusCode.OK, telemetry)
            }

            // 4. Audit Trail
            get("/audit-logs") {
                val actorId = call.extractUserId()
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

                val logs = service.listAuditLogs(actorId, page, pageSize)
                call.respond(HttpStatusCode.OK, logs)
            }
        }
    }
}
