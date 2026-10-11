package com.ecotrack.waste.api

import com.ecotrack.auth.api.extractUserId
import com.ecotrack.common.ValidationException
import com.ecotrack.waste.service.WasteReportService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.wasteRoutes(wasteReportService: WasteReportService) {

    // Public / discovery endpoints
    route("/api/v1/waste") {
        get("/categories") {
            val categories = wasteReportService.getAllCategories()
            call.respond(HttpStatusCode.OK, categories)
        }

        post("/identify") {
            val request = call.receive<IdentifyWasteRequest>()
            val result = wasteReportService.identify(request)
            call.respond(HttpStatusCode.OK, result)
        }
    }

    // Authenticated reports endpoints
    authenticate("auth-jwt") {
        route("/api/v1/waste/reports") {

            get {
                val userId = call.extractUserId()
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                val category = call.request.queryParameters["category"]

                val response = wasteReportService.getUserReports(userId, page, pageSize, category)
                call.respond(HttpStatusCode.OK, response)
            }

            post {
                val userId = call.extractUserId()
                val request = call.receive<CreateWasteReportRequest>()
                val response = wasteReportService.createReport(userId, request)
                call.respond(HttpStatusCode.Created, response)
            }

            get("/{id}") {
                val userId = call.extractUserId()
                val reportIdStr = call.parameters["id"]
                    ?: throw ValidationException("Missing report ID")
                val reportId = try {
                    UUID.fromString(reportIdStr)
                } catch (e: Exception) {
                    throw ValidationException("Invalid UUID format for report ID")
                }

                val response = wasteReportService.getReportById(reportId, userId)
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}
