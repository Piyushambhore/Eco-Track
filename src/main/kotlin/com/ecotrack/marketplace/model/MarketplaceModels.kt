package com.ecotrack.marketplace.model

import kotlinx.serialization.Serializable

@Serializable
data class MarketplaceListingDto(
    val id: String,
    val sellerId: String,
    val title: String,
    val materialType: String,
    val subType: String? = null,
    val quantityKg: Double,
    val askingPricePerKg: Double? = null,
    val totalAskingPrice: Double,
    val currency: String = "USD",
    val pickupCity: String = "Metropolis",
    val pickupAddress: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoUrl: String? = null,
    val description: String? = null,
    val status: String = "active", // active, reserved, sold, cancelled
    val offerCount: Int = 0,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateListingRequest(
    val title: String,
    val materialType: String,
    val subType: String? = null,
    val quantityKg: Double,
    val askingPricePerKg: Double? = null,
    val totalAskingPrice: Double? = null,
    val currency: String = "USD",
    val pickupCity: String = "Metropolis",
    val pickupAddress: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoUrl: String? = null,
    val description: String? = null
)

@Serializable
data class MarketplaceOfferDto(
    val id: String,
    val listingId: String,
    val buyerId: String,
    val offeredPricePerKg: Double? = null,
    val totalOfferedPrice: Double,
    val message: String? = null,
    val status: String = "pending", // pending, accepted, rejected, withdrawn
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateOfferRequest(
    val offeredPricePerKg: Double? = null,
    val totalOfferedPrice: Double,
    val message: String? = null
)

@Serializable
data class MarketplaceTransactionDto(
    val id: String,
    val listingId: String,
    val offerId: String,
    val sellerId: String,
    val buyerId: String,
    val agreedPrice: Double,
    val verifiedWeightKg: Double? = null,
    val settlementStatus: String = "escrow", // escrow, settled, disputed, cancelled
    val ecoPointsAwarded: Int = 0,
    val createdAt: String,
    val settledAt: String? = null
)

@Serializable
data class SettleTransactionRequest(
    val verifiedWeightKg: Double,
    val notes: String? = null
)

@Serializable
data class MarketplaceStatsDto(
    val activeListingsCount: Long,
    val totalMaterialsAvailableKg: Double,
    val totalVolumeTradedKg: Double,
    val totalSettledTransactions: Long,
    val totalValueTraded: Double
)
