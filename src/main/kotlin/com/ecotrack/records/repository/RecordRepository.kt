package com.ecotrack.records.repository

import com.ecotrack.database.ActivityRecords
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

data class RecordEntity(
    val id: UUID,
    val ownerUserId: UUID,
    val recordType: String,
    val title: String,
    val quantity: BigDecimal?,
    val unit: String?,
    val carbonImpact: BigDecimal?,
    val occurredAt: OffsetDateTime,
    val notes: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)

class RecordRepository {

    private val activeRecords = ActivityRecords.deletedAt.isNull()

    fun findByOwner(
        ownerId: UUID,
        page: Int,
        pageSize: Int,
        recordType: String? = null
    ): Pair<List<RecordEntity>, Long> = transaction {
        val baseQuery = ActivityRecords.selectAll().where {
            (ActivityRecords.ownerUserId eq ownerId) and activeRecords
        }

        recordType?.let {
            baseQuery.andWhere { ActivityRecords.recordType eq it }
        }

        val totalCount = baseQuery.count()
        val offset = ((page - 1) * pageSize).toLong()
        val records = baseQuery
            .orderBy(ActivityRecords.occurredAt to SortOrder.DESC)
            .limit(pageSize, offset = offset)
            .map { it.toRecordEntity() }

        Pair(records, totalCount)
    }

    fun findByIdAndOwner(id: UUID, ownerId: UUID): RecordEntity? = transaction {
        ActivityRecords.selectAll().where {
            (ActivityRecords.id eq id) and
            (ActivityRecords.ownerUserId eq ownerId) and
            activeRecords
        }.map { it.toRecordEntity() }.singleOrNull()
    }

    fun create(
        id: UUID,
        ownerId: UUID,
        recordType: String,
        title: String,
        quantity: BigDecimal?,
        unit: String?,
        carbonImpact: BigDecimal?,
        occurredAt: OffsetDateTime,
        notes: String?
    ): RecordEntity = transaction {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        ActivityRecords.insert {
            it[ActivityRecords.id] = id
            it[ownerUserId] = ownerId
            it[ActivityRecords.recordType] = recordType
            it[ActivityRecords.title] = title
            it[ActivityRecords.quantity] = quantity
            it[ActivityRecords.unit] = unit
            it[ActivityRecords.carbonImpact] = carbonImpact
            it[ActivityRecords.occurredAt] = occurredAt
            it[ActivityRecords.notes] = notes
            it[createdAt] = now
            it[updatedAt] = now
        }
        RecordEntity(id, ownerId, recordType, title, quantity, unit, carbonImpact, occurredAt, notes, now, now)
    }

    fun update(
        id: UUID,
        ownerId: UUID,
        title: String?,
        quantity: BigDecimal?,
        unit: String?,
        carbonImpact: BigDecimal?,
        occurredAt: OffsetDateTime?,
        notes: String?
    ): RecordEntity? = transaction {
        val updated = ActivityRecords.update({
            (ActivityRecords.id eq id) and
            (ActivityRecords.ownerUserId eq ownerId) and
            activeRecords
        }) {
            title?.let { v -> it[ActivityRecords.title] = v }
            quantity?.let { v -> it[ActivityRecords.quantity] = v }
            unit?.let { v -> it[ActivityRecords.unit] = v }
            carbonImpact?.let { v -> it[ActivityRecords.carbonImpact] = v }
            occurredAt?.let { v -> it[ActivityRecords.occurredAt] = v }
            notes?.let { v -> it[ActivityRecords.notes] = v }
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        if (updated > 0) findByIdAndOwner(id, ownerId) else null
    }

    fun softDelete(id: UUID, ownerId: UUID): Boolean = transaction {
        val updated = ActivityRecords.update({
            (ActivityRecords.id eq id) and
            (ActivityRecords.ownerUserId eq ownerId) and
            activeRecords
        }) {
            it[deletedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        updated > 0
    }

    fun countByOwner(ownerId: UUID): Long = transaction {
        ActivityRecords.selectAll().where {
            (ActivityRecords.ownerUserId eq ownerId) and activeRecords
        }.count()
    }

    fun getSummaryByOwner(ownerId: UUID): Map<String, Triple<Long, BigDecimal, BigDecimal>> = transaction {
        ActivityRecords.select(
            ActivityRecords.recordType,
            ActivityRecords.recordType.count(),
            ActivityRecords.quantity.sum(),
            ActivityRecords.carbonImpact.sum()
        ).where {
            (ActivityRecords.ownerUserId eq ownerId) and activeRecords
        }.groupBy(ActivityRecords.recordType)
        .associate {
            it[ActivityRecords.recordType] to Triple(
                it[ActivityRecords.recordType.count()],
                it[ActivityRecords.quantity.sum()] ?: BigDecimal.ZERO,
                it[ActivityRecords.carbonImpact.sum()] ?: BigDecimal.ZERO
            )
        }
    }

    fun getTotalCarbonImpact(ownerId: UUID): BigDecimal = transaction {
        ActivityRecords.select(ActivityRecords.carbonImpact.sum())
            .where { (ActivityRecords.ownerUserId eq ownerId) and activeRecords }
            .map { it[ActivityRecords.carbonImpact.sum()] ?: BigDecimal.ZERO }
            .first()
    }

    private fun ResultRow.toRecordEntity() = RecordEntity(
        id = this[ActivityRecords.id],
        ownerUserId = this[ActivityRecords.ownerUserId],
        recordType = this[ActivityRecords.recordType],
        title = this[ActivityRecords.title],
        quantity = this[ActivityRecords.quantity],
        unit = this[ActivityRecords.unit],
        carbonImpact = this[ActivityRecords.carbonImpact],
        occurredAt = this[ActivityRecords.occurredAt],
        notes = this[ActivityRecords.notes],
        createdAt = this[ActivityRecords.createdAt],
        updatedAt = this[ActivityRecords.updatedAt]
    )
}
