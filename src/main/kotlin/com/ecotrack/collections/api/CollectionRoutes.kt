package com.ecotrack.collections.api

import com.ecotrack.auth.api.extractUserId
import com.ecotrack.collections.model.AcceptCollectionRequest
import com.ecotrack.collections.model.CompleteCollectionRequest
import com.ecotrack.collections.model.CreateCollectionRequest
import com.ecotrack.collections.model.CreateFacilityRequest
import com.ecotrack.collections.service.CollectionService
import com.ecotrack.common.NotFoundException
import com.ecotrack.common.ValidationException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.collectionRoutes(collectionService: CollectionService) {

    // Public / discovery: nearby recyclers and facilities
    route("/api/v1/facilities") {
        get {
            val city = call.request.queryParameters["city"]
            val material = call.request.queryParameters["material"]
            val results = collectionService.searchFacilities(
                userLat = null,
                userLng = null,
                city = city,
                material = material
            )
            call.respond(HttpStatusCode.OK, results)
        }

        get("/nearby") {
            val lat = call.request.queryParameters["lat"]?.toDoubleOrNull()
            val lng = call.request.queryParameters["lng"]?.toDoubleOrNull()
            val radius = call.request.queryParameters["radius"]?.toDoubleOrNull() ?: 25.0
            val material = call.request.queryParameters["material"]

            if (lat == null || lng == null) {
                // If coordinates omitted, fallback to default city facilities
                val results = collectionService.searchFacilities(null, null, radius, null, material)
                call.respond(HttpStatusCode.OK, results)
            } else {
                val results = collectionService.searchFacilities(lat, lng, radius, null, material)
                call.respond(HttpStatusCode.OK, results)
            }
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: throw ValidationException("Missing facility ID")
            val facility = collectionService.getFacilityById(id)
                ?: throw NotFoundException("Facility not found with ID: $id")
            call.respond(HttpStatusCode.OK, facility)
        }

        post {
            val request = call.receive<CreateFacilityRequest>()
            val created = collectionService.createFacility(request)
            call.respond(HttpStatusCode.Created, created)
        }
    }

    // Authenticated: Citizen pickup scheduling and tracking
    authenticate("auth-jwt") {
        route("/api/v1/collections") {
            post {
                val userId = call.extractUserId()
                val request = call.receive<CreateCollectionRequest>()
                val created = collectionService.schedulePickup(userId, request)
                call.respond(HttpStatusCode.Created, created)
            }

            get {
                val userId = call.extractUserId()
                val status = call.request.queryParameters["status"]
                val list = collectionService.getUserCollections(userId, status)
                call.respond(HttpStatusCode.OK, list)
            }

            get("/stats/summary") {
                val userId = call.extractUserId()
                val stats = collectionService.getStats(userId)
                call.respond(HttpStatusCode.OK, stats)
            }

            get("/{id}") {
                val userId = call.extractUserId()
                val id = call.parameters["id"] ?: throw ValidationException("Missing collection request ID")
                val item = collectionService.getCollectionById(userId, id)
                    ?: throw NotFoundException("Collection request not found or unauthorized")
                call.respond(HttpStatusCode.OK, item)
            }

            // Collector lifecycle transitions
            post("/{id}/accept") {
                val collectorId = call.extractUserId()
                val id = call.parameters["id"] ?: throw ValidationException("Missing collection request ID")
                val accepted = collectionService.acceptPickup(collectorId, id)
                call.respond(HttpStatusCode.OK, accepted)
            }

            post("/{id}/in-transit") {
                val collectorId = call.extractUserId()
                val id = call.parameters["id"] ?: throw ValidationException("Missing collection request ID")
                val inTransit = collectionService.markInTransit(collectorId, id)
                call.respond(HttpStatusCode.OK, inTransit)
            }

            post("/{id}/complete") {
                val collectorId = call.extractUserId()
                val id = call.parameters["id"] ?: throw ValidationException("Missing collection request ID")
                val request = call.receive<CompleteCollectionRequest>()
                val completed = collectionService.completePickup(collectorId, id, request)
                call.respond(HttpStatusCode.OK, completed)
            }
        }

        // Collector dispatch queue
        route("/api/v1/collector") {
            get("/queue") {
                val city = call.request.queryParameters["city"]
                val queue = collectionService.getCollectorQueue(city)
                call.respond(HttpStatusCode.OK, queue)
            }
        }
    }
}
