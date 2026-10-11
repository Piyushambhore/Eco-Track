package com.ecotrack.waste.api

import kotlinx.serialization.Serializable
import java.math.BigDecimal

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
    val acceptedUnits: List<String>
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
    val subType: String?,
    val photoUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    val quantity: Double?,
    val unit: String?,
    val confidence: Double?,
    val status: String,
    val disposalGuide: String?,
    val carbonImpact: Double?,
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
