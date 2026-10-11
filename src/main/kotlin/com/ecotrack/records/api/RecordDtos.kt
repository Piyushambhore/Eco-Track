package com.ecotrack.records.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateRecordRequest(
    val recordType: String,
    val title: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val carbonImpact: Double? = null,
    val occurredAt: String,
    val notes: String? = null
)

@Serializable
data class UpdateRecordRequest(
    val title: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val carbonImpact: Double? = null,
    val occurredAt: String? = null,
    val notes: String? = null
)

@Serializable
data class RecordResponse(
    val id: String,
    val ownerUserId: String,
    val recordType: String,
    val title: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val carbonImpact: Double? = null,
    val occurredAt: String,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class RecordSummaryResponse(
    val totalRecords: Long,
    val totalCarbonImpact: Double,
    val byType: Map<String, TypeSummary>
)

@Serializable
data class TypeSummary(
    val count: Long,
    val totalQuantity: Double,
    val totalCarbonImpact: Double
)

@Serializable
data class PaginatedRecordsResponse(
    val data: List<RecordResponse>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Long,
    val totalPages: Int
)
