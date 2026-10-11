package com.ecotrack.marketplace.service

import com.ecotrack.marketplace.model.*
import com.ecotrack.marketplace.repository.MarketplaceRepository
import com.ecotrack.rewards.service.RewardsService
import java.util.UUID

class MarketplaceService(
    private val repository: MarketplaceRepository = MarketplaceRepository(),
    private val rewardsService: RewardsService? = null
) {

    fun createListing(sellerId: UUID, req: CreateListingRequest): MarketplaceListingDto {
        require(req.title.isNotBlank()) { "Listing title cannot be blank" }
        require(req.quantityKg > 0) { "Quantity (kg) must be greater than zero" }
        require(req.pickupAddress.isNotBlank()) { "Pickup address cannot be blank" }

        val totalAskingPrice = req.totalAskingPrice
            ?: ((req.askingPricePerKg ?: 0.0) * req.quantityKg)

        require(totalAskingPrice >= 0) { "Asking price cannot be negative" }

        return repository.createListing(
            sellerId = sellerId,
            title = req.title.trim(),
            materialType = req.materialType,
            subType = req.subType?.trim(),
            quantityKg = req.quantityKg,
            askingPricePerKg = req.askingPricePerKg,
            totalAskingPrice = Math.round(totalAskingPrice * 100.0) / 100.0,
            currency = req.currency,
            pickupCity = req.pickupCity.trim().ifBlank { "Metropolis" },
            pickupAddress = req.pickupAddress.trim(),
            latitude = req.latitude,
            longitude = req.longitude,
            photoUrl = req.photoUrl,
            description = req.description?.trim()
        )
    }

    fun browseListings(
        materialType: String? = null,
        maxPricePerKg: Double? = null,
        city: String? = null,
        status: String? = "active"
    ): List<MarketplaceListingDto> {
        return repository.findListings(materialType, maxPricePerKg, city, status)
    }

    fun getListingById(id: String): MarketplaceListingDto? {
        val uuid = try { UUID.fromString(id) } catch (e: Exception) { return null }
        return repository.findListingById(uuid)
    }

    fun getSellerListings(sellerId: UUID): List<MarketplaceListingDto> {
        return repository.findListingsBySeller(sellerId)
    }

    fun placeOffer(buyerId: UUID, listingId: String, req: CreateOfferRequest): MarketplaceOfferDto {
        val listingUuid = try { UUID.fromString(listingId) } catch (e: Exception) {
            throw IllegalArgumentException("Invalid listing ID format")
        }
        val listing = repository.findListingById(listingUuid)
            ?: throw NoSuchElementException("Listing not found with ID: $listingId")

        // Guard: Seller cannot bid on their own listing
        if (listing.sellerId == buyerId.toString()) {
            throw IllegalArgumentException("Sellers are prohibited from bidding on their own listings")
        }

        // Guard: Only active listings accept offers
        if (listing.status != "active") {
            throw IllegalStateException("Offers can only be submitted on active listings (current: ${listing.status})")
        }

        require(req.totalOfferedPrice > 0) { "Offered price must be greater than zero" }

        return repository.createOffer(
            listingId = listingUuid,
            buyerId = buyerId,
            offeredPricePerKg = req.offeredPricePerKg,
            totalOfferedPrice = req.totalOfferedPrice,
            message = req.message?.trim()
        )
    }

    fun getOffersForListing(requesterId: UUID, listingId: String): List<MarketplaceOfferDto> {
        val listingUuid = try { UUID.fromString(listingId) } catch (e: Exception) {
            throw IllegalArgumentException("Invalid listing ID format")
        }
        val listing = repository.findListingById(listingUuid)
            ?: throw NoSuchElementException("Listing not found with ID: $listingId")

        val allOffers = repository.findOffersByListing(listingUuid)

        // If requester is seller, return all offers. If buyer, only return their own offer.
        return if (listing.sellerId == requesterId.toString()) {
            allOffers
        } else {
            allOffers.filter { it.buyerId == requesterId.toString() }
        }
    }

    fun acceptOffer(sellerId: UUID, offerId: String): Pair<MarketplaceOfferDto, MarketplaceTransactionDto> {
        val offerUuid = try { UUID.fromString(offerId) } catch (e: Exception) {
            throw IllegalArgumentException("Invalid offer ID format")
        }
        val offer = repository.findOfferById(offerUuid)
            ?: throw NoSuchElementException("Offer not found with ID: $offerId")

        val listingUuid = UUID.fromString(offer.listingId)
        val listing = repository.findListingById(listingUuid)
            ?: throw NoSuchElementException("Associated listing not found")

        // Guard: Only the listing seller can accept offers
        if (listing.sellerId != sellerId.toString()) {
            throw IllegalStateException("Only the listing seller can accept offers")
        }

        if (offer.status != "pending") {
            throw IllegalStateException("Only pending offers can be accepted (current: ${offer.status})")
        }

        val acceptedOffer = repository.acceptOffer(offerUuid)
            ?: throw IllegalStateException("Failed to accept offer")

        // Create escrow transaction
        val transaction = repository.createTransaction(
            listingId = listingUuid,
            offerId = offerUuid,
            sellerId = sellerId,
            buyerId = UUID.fromString(offer.buyerId),
            agreedPrice = offer.totalOfferedPrice
        )

        return Pair(acceptedOffer, transaction)
    }

    fun settleTransaction(userId: UUID, transactionId: String, req: SettleTransactionRequest): MarketplaceTransactionDto {
        val transUuid = try { UUID.fromString(transactionId) } catch (e: Exception) {
            throw IllegalArgumentException("Invalid transaction ID format")
        }
        val trans = repository.findTransactionById(transUuid)
            ?: throw NoSuchElementException("Transaction not found with ID: $transactionId")

        // Guard: Only buyer or seller can settle transaction
        if (trans.sellerId != userId.toString() && trans.buyerId != userId.toString()) {
            throw IllegalStateException("Only transaction participants can confirm settlement")
        }

        if (trans.settlementStatus != "escrow") {
            throw IllegalStateException("Transaction must be in escrow to settle (current: ${trans.settlementStatus})")
        }

        val settled = repository.settleTransaction(transUuid, req.verifiedWeightKg)
            ?: throw IllegalStateException("Failed to settle transaction")

        try {
            val sellerUuid = UUID.fromString(trans.sellerId)
            rewardsService?.awardMarketplaceSalePoints(sellerUuid, transactionId, req.verifiedWeightKg)
        } catch (_: Exception) {
            // Non-blocking
        }

        return settled
    }

    fun getStats(): MarketplaceStatsDto {
        return repository.getStats()
    }
}
