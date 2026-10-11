package com.ecotrack.records.api

import com.ecotrack.auth.api.extractUserId
import com.ecotrack.records.service.RecordService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.recordRoutes(recordService: RecordService) {

    authenticate("auth-jwt") {
        route("/api/v1/records") {

            get {
                val userId = call.extractUserId()
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                val recordType = call.request.queryParameters["type"]

                val response = recordService.listRecords(userId, page, pageSize, recordType)
                call.respond(HttpStatusCode.OK, response)
            }

            post {
                val userId = call.extractUserId()
                val request = call.receive<CreateRecordRequest>()
                val response = recordService.createRecord(userId, request)
                call.respond(HttpStatusCode.Created, response)
            }

            get("/{id}") {
                val userId = call.extractUserId()
                val recordId = UUID.fromString(
                    call.parameters["id"] ?: throw com.ecotrack.common.ValidationException("Missing record ID")
                )
                val response = recordService.getRecord(recordId, userId)
                call.respond(HttpStatusCode.OK, response)
            }

            patch("/{id}") {
                val userId = call.extractUserId()
                val recordId = UUID.fromString(
                    call.parameters["id"] ?: throw com.ecotrack.common.ValidationException("Missing record ID")
                )
                val request = call.receive<UpdateRecordRequest>()
                val response = recordService.updateRecord(recordId, userId, request)
                call.respond(HttpStatusCode.OK, response)
            }

            delete("/{id}") {
                val userId = call.extractUserId()
                val recordId = UUID.fromString(
                    call.parameters["id"] ?: throw com.ecotrack.common.ValidationException("Missing record ID")
                )
                recordService.deleteRecord(recordId, userId)
                call.respond(HttpStatusCode.NoContent)
            }
        }

        get("/api/v1/summary") {
            val userId = call.extractUserId()
            val response = recordService.getSummary(userId)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
