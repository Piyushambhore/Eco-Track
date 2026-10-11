package com.ecotrack.collections.model

import kotlinx.serialization.Serializable

@Serializable
data class RecyclingFacilityDto(
    val id: String,
    val name: String,
    val facilityType: String,
    val address: String,
    val city: String,
    val postalCode: String? = null,
    val latitude: Double,
    val longitude: Double,
    val contactPhone: String? = null,
    val operatingHours: String? = null,
    val acceptedMaterials: List<String>,
    val pricingInfo: String? = null,
    val rating: Double = 4.8,
    val verified: Boolean = true,
    val distanceKm: Double? = null
)

@Serializable
data class CreateFacilityRequest(
    val name: String,
    val facilityType: String = "recycling_center",
    val address: String,
    val city: String = "Kolhapur",
    val postalCode: String? = null,
    val latitude: Double = 16.7050,
    val longitude: Double = 74.2433,
    val contactPhone: String? = null,
    val operatingHours: String? = "Mon-Sat 09:00 - 18:00",
    val acceptedMaterials: List<String> = listOf("plastic", "paper", "metal", "e_waste"),
    val pricingInfo: String? = null
)

@Serializable
data class CreateCollectionRequest(
    val pickupAddress: String,
    val city: String = "Metropolis",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val preferredDate: String, // YYYY-MM-DD
    val preferredSlot: String = "09:00 - 12:00",
    val materials: List<String>,
    val estimatedWeightKg: Double = 5.0,
    val specialInstructions: String? = null,
    val facilityId: String? = null
)

@Serializable
data class CollectionRequestDto(
    val id: String,
    val userId: String,
    val collectorId: String? = null,
    val facilityId: String? = null,
    val status: String,
    val pickupAddress: String,
    val city: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val preferredDate: String,
    val preferredSlot: String,
    val materials: List<String>,
    val estimatedWeightKg: Double? = null,
    val actualWeightKg: Double? = null,
    val specialInstructions: String? = null,
    val proofPhotoUrl: String? = null,
    val ecoPointsEarned: Int = 0,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class AcceptCollectionRequest(
    val estimatedArrival: String? = null
)

@Serializable
data class CompleteCollectionRequest(
    val actualWeightKg: Double,
    val proofPhotoUrl: String? = null,
    val notes: String? = null
)

@Serializable
data class CollectionSummaryStats(
    val totalSubmitted: Long,
    val totalAssigned: Long,
    val totalCompleted: Long,
    val totalWeightDivertedKg: Double,
    val totalEcoPointsEarned: Int
)
