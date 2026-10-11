package com.ecotrack.marketplace.api

import com.ecotrack.auth.api.extractUserId
import com.ecotrack.common.NotFoundException
import com.ecotrack.common.ValidationException
import com.ecotrack.marketplace.model.CreateListingRequest
import com.ecotrack.marketplace.model.CreateOfferRequest
import com.ecotrack.marketplace.model.SettleTransactionRequest
import com.ecotrack.marketplace.service.MarketplaceService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.marketplaceRoutes(service: MarketplaceService) {

    // Public marketplace browsing
    route("/api/v1/marketplace") {
        get("/listings") {
            val material = call.request.queryParameters["material"]
            val maxPrice = call.request.queryParameters["maxPrice"]?.toDoubleOrNull()
            val city = call.request.queryParameters["city"]
            val status = call.request.queryParameters["status"] ?: "active"

            val results = service.browseListings(
                materialType = material,
                maxPricePerKg = maxPrice,
                city = city,
                status = status
            )
            call.respond(HttpStatusCode.OK, results)
        }

        get("/listings/{id}") {
            val id = call.parameters["id"] ?: throw ValidationException("Missing listing ID")
            val listing = service.getListingById(id)
                ?: throw NotFoundException("Listing not found with ID: $id")
            call.respond(HttpStatusCode.OK, listing)
        }

        get("/stats") {
            val stats = service.getStats()
            call.respond(HttpStatusCode.OK, stats)
        }
    }

    // Authenticated trading and offers
    authenticate("auth-jwt") {
        route("/api/v1/marketplace") {

            post("/listings") {
                val userId = call.extractUserId()
                val request = call.receive<CreateListingRequest>()
                val created = service.createListing(userId, request)
                call.respond(HttpStatusCode.Created, created)
            }

            get("/my-listings") {
                val userId = call.extractUserId()
                val listings = service.getSellerListings(userId)
                call.respond(HttpStatusCode.OK, listings)
            }

            post("/listings/{id}/offers") {
                val buyerId = call.extractUserId()
                val listingId = call.parameters["id"] ?: throw ValidationException("Missing listing ID")
                val request = call.receive<CreateOfferRequest>()
                val offer = service.placeOffer(buyerId, listingId, request)
                call.respond(HttpStatusCode.Created, offer)
            }

            get("/listings/{id}/offers") {
                val requesterId = call.extractUserId()
                val listingId = call.parameters["id"] ?: throw ValidationException("Missing listing ID")
                val offers = service.getOffersForListing(requesterId, listingId)
                call.respond(HttpStatusCode.OK, offers)
            }

            post("/offers/{id}/accept") {
                val sellerId = call.extractUserId()
                val offerId = call.parameters["id"] ?: throw ValidationException("Missing offer ID")
                val (acceptedOffer, transaction) = service.acceptOffer(sellerId, offerId)
                call.respond(HttpStatusCode.OK, mapOf(
                    "offer" to acceptedOffer,
                    "transaction" to transaction
                ))
            }

            post("/transactions/{id}/settle") {
                val userId = call.extractUserId()
                val transId = call.parameters["id"] ?: throw ValidationException("Missing transaction ID")
                val request = call.receive<SettleTransactionRequest>()
                val settled = service.settleTransaction(userId, transId, request)
                call.respond(HttpStatusCode.OK, settled)
            }
        }
    }
}
