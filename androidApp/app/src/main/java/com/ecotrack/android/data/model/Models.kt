package com.ecotrack.android.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
    val role: String = "CITIZEN"
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
data class AuthResponse(
    val token: String,
    val user: UserDto
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val displayName: String,
    val status: String,
    val role: String = "CITIZEN"
)

@Serializable
data class IdentifyWasteRequest(
    val imageBase64: String? = null,
    val imageUrl: String? = null,
    val description: String? = null
)

@Serializable
data class WasteClassificationResult(
    val category: String,
    val subType: String,
    val confidence: Double,
    val recyclable: Boolean,
    val disposalGuide: String,
    val carbonFactorKg: Double,
    val recommendedAction: String,
    val acceptedUnits: List<String> = emptyList()
)

@Serializable
data class CreateWasteReportRequest(
    val category: String,
    val subType: String? = null,
    val photoUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val confidence: Double? = null,
    val disposalGuide: String? = null,
    val carbonImpact: Double? = null
)

@Serializable
data class WasteReportDto(
    val id: String,
    val userId: String,
    val category: String,
    val subType: String? = null,
    val photoUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val confidence: Double? = null,
    val status: String,
    val disposalGuide: String? = null,
    val carbonImpact: Double? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class WasteReportsListResponse(
    val items: List<WasteReportDto>,
    val total: Long,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int
)

@Serializable
data class WasteCategoryInfo(
    val category: String,
    val displayName: String,
    val description: String,
    val carbonFactorPerKg: Double,
    val recyclable: Boolean,
    val defaultUnits: List<String>,
    val disposalGuide: String
)

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
data class CreateCollectionRequest(
    val pickupAddress: String,
    val city: String = "Kolhapur",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val preferredDate: String,
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
    val currency: String = "INR",
    val pickupCity: String = "Kolhapur",
    val pickupAddress: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoUrl: String? = null,
    val description: String? = null,
    val status: String = "active",
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
    val currency: String = "INR",
    val pickupCity: String = "Kolhapur",
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
    val status: String = "pending",
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
data class MarketplaceStatsDto(
    val activeListingsCount: Long,
    val totalMaterialsAvailableKg: Double,
    val totalVolumeTradedKg: Double,
    val totalSettledTransactions: Long,
    val totalValueTraded: Double
)

// Phase 5: Rewards Ledger & Leaderboard Models
@Serializable
data class LedgerEntryDto(
    val id: String,
    val userId: String,
    val points: Int,
    val balanceAfter: Int,
    val reason: String,
    val referenceId: String? = null,
    val description: String? = null,
    val createdAt: String
)

@Serializable
data class BadgeDto(
    val key: String,
    val title: String,
    val description: String,
    val iconName: String = "badge_eco",
    val isUnlocked: Boolean = false,
    val unlockedAt: String? = null
)

@Serializable
data class RewardTierDto(
    val tierName: String,
    val minPoints: Int,
    val nextTierName: String?,
    val pointsToNext: Int,
    val progressPercentage: Int
)

@Serializable
data class RewardsBalanceDto(
    val userId: String,
    val currentPoints: Int,
    val totalEarned: Int,
    val totalRedeemed: Int,
    val tier: RewardTierDto,
    val badges: List<BadgeDto>
)

@Serializable
data class RedemptionCatalogItemDto(
    val rewardType: String,
    val title: String,
    val description: String,
    val pointsRequired: Int,
    val category: String,
    val partnerName: String
)

@Serializable
data class RedemptionRequest(
    val rewardType: String
)

@Serializable
data class RedemptionDto(
    val id: String,
    val userId: String,
    val rewardType: String,
    val title: String,
    val pointsSpent: Int,
    val voucherCode: String,
    val status: String,
    val expiresAt: String? = null,
    val createdAt: String
)

@Serializable
data class LeaderboardEntryDto(
    val rank: Int,
    val name: String,
    val category: String,
    val kgRecovered: Double,
    val points: Int,
    val isCurrentUser: Boolean = false
)

@Serializable
data class LeaderboardDto(
    val scope: String,
    val title: String,
    val entries: List<LeaderboardEntryDto>,
    val userRank: LeaderboardEntryDto? = null
)

// Phase 6: Ecological Impact & ESG Analytics Models
@Serializable
data class ImpactEquivalentsDto(
    val treeSeedlingsGrown10Years: Double,
    val passengerCarKmAvoided: Double,
    val smartphoneCharges: Long,
    val waterLitersConserved: Double,
    val homesElectricityHours: Double
)

@Serializable
data class MaterialImpactSummary(
    val material: String,
    val weightKg: Double,
    val co2SavedKg: Double,
    val percentage: Double
)

@Serializable
data class PersonalImpactDto(
    val userId: String,
    val totalWeightKg: Double,
    val totalCo2SavedKg: Double,
    val equivalents: ImpactEquivalentsDto,
    val materialBreakdown: List<MaterialImpactSummary>,
    val totalActionsCount: Int
)

@Serializable
data class CommunityImpactDto(
    val communityName: String,
    val totalWeightKg: Double,
    val totalCo2SavedKg: Double,
    val activeParticipantsCount: Int,
    val treesEquivalent: Double,
    val topMaterial: String,
    val equivalents: ImpactEquivalentsDto
)

@Serializable
data class CreateCertificateRequest(
    val title: String? = null,
    val periodMonths: Int = 3
)

@Serializable
data class ImpactCertificateDto(
    val id: String,
    val certificateNumber: String,
    val userId: String,
    val userName: String,
    val title: String,
    val periodStart: String,
    val periodEnd: String,
    val totalWeightKg: Double,
    val totalCo2Kg: Double,
    val treesEquivalent: Double,
    val waterSavedLiters: Double,
    val energySavedKwh: Double,
    val verificationHash: String,
    val status: String,
    val issuedAt: String,
    val verificationUrl: String
)

@Serializable
data class CertificateVerificationResultDto(
    val valid: Boolean,
    val certificate: ImpactCertificateDto?,
    val message: String
)

// Phase 7: Admin Governance & Municipal Telemetry Models
@Serializable
data class UserAdminDto(
    val id: String,
    val email: String,
    val displayName: String,
    val status: String,
    val role: String,
    val createdAt: String
)

@Serializable
data class PaginatedUsersDto(
    val items: List<UserAdminDto>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class UpdateUserRoleRequest(
    val role: String
)

@Serializable
data class UpdateUserStatusRequest(
    val status: String
)

@Serializable
data class AdminDisputeDto(
    val id: String,
    val reporterId: String,
    val reporterEmail: String? = null,
    val disputeType: String,
    val referenceId: String,
    val title: String,
    val description: String,
    val status: String,
    val resolutionNotes: String? = null,
    val resolvedBy: String? = null,
    val createdAt: String,
    val resolvedAt: String? = null
)

@Serializable
data class PaginatedDisputesDto(
    val items: List<AdminDisputeDto>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class ResolveDisputeRequest(
    val status: String,
    val resolutionNotes: String
)

@Serializable
data class ZoneMetricDto(
    val zoneName: String,
    val totalWeightKg: Double,
    val activeCitizens: Int,
    val efficiencyPercent: Double
)

@Serializable
data class WardMetricDto(
    val wardNumber: Int,
    val wardName: String,
    val divertedKg: Double,
    val complianceRatePercent: Double
)

@Serializable
data class MunicipalTelemetryDto(
    val totalWasteReportedKg: Double,
    val totalWasteCollectedKg: Double,
    val totalRecycledKg: Double,
    val diversionRatePercent: Double,
    val collectionEfficiencyPercent: Double,
    val openDisputesCount: Long,
    val resolvedDisputesCount: Long,
    val totalRegisteredCitizens: Long,
    val activeFacilitiesCount: Long,
    val zoneMetrics: List<ZoneMetricDto>,
    val topWards: List<WardMetricDto>
)

@Serializable
data class AdminAuditLogDto(
    val id: String,
    val actorId: String,
    val actorEmail: String? = null,
    val action: String,
    val targetType: String,
    val targetId: String,
    val details: String,
    val ipAddress: String? = null,
    val createdAt: String
)

@Serializable
data class PaginatedAuditLogsDto(
    val items: List<AdminAuditLogDto>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class AwsServiceStatusDto(
    val serviceName: String,
    val awsProduct: String,
    val status: String,
    val region: String,
    val details: String,
    val latencyMs: Long,
    val isCloudConnected: Boolean
)

@Serializable
data class AwsArchitectureOverviewDto(
    val region: String,
    val architectureTier: String,
    val services: List<AwsServiceStatusDto>,
    val s3Bucket: String,
    val rekognitionModelVersion: String,
    val snsTopicArn: String,
    val cloudWatchNamespace: String,
    val lastVerifiedAt: String
)




