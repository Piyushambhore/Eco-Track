package com.ecotrack.waste.repository

import com.ecotrack.database.WasteReports
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

data class WasteReportEntity(
    val id: UUID,
    val userId: UUID,
    val category: String,
    val subType: String?,
    val photoUrl: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val quantity: BigDecimal?,
    val unit: String?,
    val confidence: BigDecimal?,
    val status: String,
    val disposalGuide: String?,
    val carbonImpact: BigDecimal?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)

class WasteReportRepository {

    fun create(
        userId: UUID,
        category: String,
        subType: String?,
        photoUrl: String?,
        latitude: BigDecimal?,
        longitude: BigDecimal?,
        quantity: BigDecimal?,
        unit: String?,
        confidence: BigDecimal?,
        status: String = "identified",
        disposalGuide: String?,
        carbonImpact: BigDecimal?
    ): WasteReportEntity = transaction {
        val newId = UUID.randomUUID()
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        WasteReports.insert {
            it[id] = newId
            it[WasteReports.userId] = userId
            it[WasteReports.category] = category
            it[WasteReports.subType] = subType
            it[WasteReports.photoUrl] = photoUrl
            it[WasteReports.latitude] = latitude
            it[WasteReports.longitude] = longitude
            it[WasteReports.quantity] = quantity
            it[WasteReports.unit] = unit
            it[WasteReports.confidence] = confidence
            it[WasteReports.status] = status
            it[WasteReports.disposalGuide] = disposalGuide
            it[WasteReports.carbonImpact] = carbonImpact
            it[createdAt] = now
            it[updatedAt] = now
        }

        WasteReportEntity(
            id = newId,
            userId = userId,
            category = category,
            subType = subType,
            photoUrl = photoUrl,
            latitude = latitude,
            longitude = longitude,
            quantity = quantity,
            unit = unit,
            confidence = confidence,
            status = status,
            disposalGuide = disposalGuide,
            carbonImpact = carbonImpact,
            createdAt = now,
            updatedAt = now
        )
    }

    fun findByIdAndUser(id: UUID, userId: UUID): WasteReportEntity? = transaction {
        WasteReports.selectAll().where {
            (WasteReports.id eq id) and (WasteReports.userId eq userId)
        }.map { it.toEntity() }.singleOrNull()
    }

    fun findByUser(
        userId: UUID,
        page: Int,
        pageSize: Int,
        category: String? = null
    ): Pair<List<WasteReportEntity>, Long> = transaction {
        val baseQuery = WasteReports.selectAll().where {
            WasteReports.userId eq userId
        }

        category?.let {
            baseQuery.andWhere { WasteReports.category eq it }
        }

        val totalCount = baseQuery.count()
        val offset = ((page - 1) * pageSize).toLong()

        val items = baseQuery
            .orderBy(WasteReports.createdAt to SortOrder.DESC)
            .limit(pageSize, offset = offset)
            .map { it.toEntity() }

        Pair(items, totalCount)
    }

    fun updateStatus(id: UUID, userId: UUID, newStatus: String): Boolean = transaction {
        val rowsUpdated = WasteReports.update({
            (WasteReports.id eq id) and (WasteReports.userId eq userId)
        }) {
            it[status] = newStatus
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        rowsUpdated > 0
    }

    private fun ResultRow.toEntity(): WasteReportEntity = WasteReportEntity(
        id = this[WasteReports.id],
        userId = this[WasteReports.userId],
        category = this[WasteReports.category],
        subType = this[WasteReports.subType],
        photoUrl = this[WasteReports.photoUrl],
        latitude = this[WasteReports.latitude],
        longitude = this[WasteReports.longitude],
        quantity = this[WasteReports.quantity],
        unit = this[WasteReports.unit],
        confidence = this[WasteReports.confidence],
        status = this[WasteReports.status],
        disposalGuide = this[WasteReports.disposalGuide],
        carbonImpact = this[WasteReports.carbonImpact],
        createdAt = this[WasteReports.createdAt],
        updatedAt = this[WasteReports.updatedAt]
    )
}
