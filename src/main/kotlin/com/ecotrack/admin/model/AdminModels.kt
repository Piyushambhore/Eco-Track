package com.ecotrack.admin.model

import kotlinx.serialization.Serializable

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
data class CreateDisputeRequest(
    val disputeType: String,
    val referenceId: String,
    val title: String,
    val description: String
)

@Serializable
data class ResolveDisputeRequest(
    val status: String, // 'resolved' or 'dismissed'
    val resolutionNotes: String
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
