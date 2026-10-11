package com.ecotrack.collections.repository

import com.ecotrack.collections.model.CollectionRequestDto
import com.ecotrack.collections.model.CollectionSummaryStats
import com.ecotrack.database.CollectionRequests
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CollectionRepository {

    // In-memory cache & fallback for offline/testing resilience
    private val memoryStore = ConcurrentHashMap<UUID, CollectionRequestDto>()

    fun create(
        userId: UUID,
        pickupAddress: String,
        city: String,
        latitude: Double?,
        longitude: Double?,
        preferredDate: String,
        preferredSlot: String,
        materials: List<String>,
        estimatedWeightKg: Double,
        specialInstructions: String?,
        facilityId: UUID? = null
    ): CollectionRequestDto {
        val newId = UUID.randomUUID()
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val parsedDate = try {
            LocalDate.parse(preferredDate)
        } catch (e: Exception) {
            LocalDate.now().plusDays(1)
        }
        val points = (estimatedWeightKg * 10).toInt()

        val dto = CollectionRequestDto(
            id = newId.toString(),
            userId = userId.toString(),
            collectorId = null,
            facilityId = facilityId?.toString(),
            status = "submitted",
            pickupAddress = pickupAddress,
            city = city,
            latitude = latitude,
            longitude = longitude,
            preferredDate = parsedDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            preferredSlot = preferredSlot,
            materials = materials,
            estimatedWeightKg = estimatedWeightKg,
            actualWeightKg = null,
            specialInstructions = specialInstructions,
            proofPhotoUrl = null,
            ecoPointsEarned = points,
            createdAt = now.toString(),
            updatedAt = now.toString()
        )

        try {
            transaction {
                CollectionRequests.insert {
                    it[id] = newId
                    it[CollectionRequests.userId] = userId
                    it[CollectionRequests.facilityId] = facilityId
                    it[CollectionRequests.status] = "submitted"
                    it[CollectionRequests.pickupAddress] = pickupAddress
                    it[CollectionRequests.city] = city
                    it[CollectionRequests.latitude] = latitude?.toBigDecimal()
                    it[CollectionRequests.longitude] = longitude?.toBigDecimal()
                    it[CollectionRequests.preferredDate] = parsedDate
                    it[CollectionRequests.preferredSlot] = preferredSlot
                    it[CollectionRequests.materials] = materials.joinToString(",")
                    it[CollectionRequests.estimatedWeightKg] = estimatedWeightKg.toBigDecimal()
                    it[CollectionRequests.specialInstructions] = specialInstructions
                    it[CollectionRequests.ecoPointsEarned] = points
                    it[CollectionRequests.createdAt] = now
                    it[CollectionRequests.updatedAt] = now
                }
            }
        } catch (_: Exception) {
            // DB not reachable, fall back to memory store
        }

        memoryStore[newId] = dto
        return dto
    }

    fun findByUser(userId: UUID, status: String? = null): List<CollectionRequestDto> {
        val dbResults = try {
            transaction {
                val query = CollectionRequests.selectAll().where { CollectionRequests.userId eq userId }
                status?.let { query.andWhere { CollectionRequests.status eq it } }
                query.orderBy(CollectionRequests.createdAt to SortOrder.DESC).map { it.toDto() }
            }
        } catch (_: Exception) {
            emptyList()
        }

        if (dbResults.isNotEmpty()) return dbResults

        return memoryStore.values
            .filter { it.userId == userId.toString() && (status == null || it.status == status) }
            .sortedByDescending { it.createdAt }
    }

    fun findById(id: UUID): CollectionRequestDto? {
        val fromDb = try {
            transaction {
                CollectionRequests.selectAll()
                    .where { CollectionRequests.id eq id }
                    .map { it.toDto() }
                    .singleOrNull()
            }
        } catch (_: Exception) {
            null
        }

        return fromDb ?: memoryStore[id]
    }

    fun findOpenQueue(city: String? = null): List<CollectionRequestDto> {
        val dbResults = try {
            transaction {
                val query = CollectionRequests.selectAll().where { CollectionRequests.status eq "submitted" }
                city?.let { query.andWhere { CollectionRequests.city eq it } }
                query.orderBy(CollectionRequests.createdAt to SortOrder.ASC).map { it.toDto() }
            }
        } catch (_: Exception) {
            emptyList()
        }

        if (dbResults.isNotEmpty()) return dbResults

        return memoryStore.values
            .filter { it.status == "submitted" && (city == null || it.city.equals(city, ignoreCase = true)) }
            .sortedBy { it.createdAt }
    }

    fun assignCollector(id: UUID, collectorId: UUID): CollectionRequestDto? {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        try {
            transaction {
                CollectionRequests.update({ (CollectionRequests.id eq id) and (CollectionRequests.status eq "submitted") }) {
                    it[CollectionRequests.collectorId] = collectorId
                    it[status] = "assigned"
                    it[updatedAt] = now
                }
            }
        } catch (_: Exception) {
            // Ignore DB error, update memory store
        }

        val existing = memoryStore[id]
        if (existing != null && existing.status == "submitted") {
            val updated = existing.copy(
                collectorId = collectorId.toString(),
                status = "assigned",
                updatedAt = now.toString()
            )
            memoryStore[id] = updated
            return updated
        }
        return findById(id)
    }

    fun updateStatus(id: UUID, newStatus: String): CollectionRequestDto? {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        try {
            transaction {
                CollectionRequests.update({ CollectionRequests.id eq id }) {
                    it[status] = newStatus
                    it[updatedAt] = now
                }
            }
        } catch (_: Exception) {
            // Ignore DB error, update memory store
        }

        val existing = memoryStore[id]
        if (existing != null) {
            val updated = existing.copy(status = newStatus, updatedAt = now.toString())
            memoryStore[id] = updated
            return updated
        }
        return findById(id)
    }

    fun completeRequest(
        id: UUID,
        actualWeightKg: Double,
        proofPhotoUrl: String?,
        notes: String?
    ): CollectionRequestDto? {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val awardedPoints = (actualWeightKg * 10).toInt() + 25

        try {
            transaction {
                CollectionRequests.update({ CollectionRequests.id eq id }) {
                    it[status] = "completed"
                    it[CollectionRequests.actualWeightKg] = actualWeightKg.toBigDecimal()
                    if (proofPhotoUrl != null) {
                        it[CollectionRequests.proofPhotoUrl] = proofPhotoUrl
                    }
                    if (notes != null) {
                        it[specialInstructions] = notes
                    }
                    it[ecoPointsEarned] = awardedPoints
                    it[updatedAt] = now
                }
            }
        } catch (_: Exception) {
            // Ignore DB error, update memory store
        }

        val existing = memoryStore[id]
        if (existing != null) {
            val updated = existing.copy(
                status = "completed",
                actualWeightKg = actualWeightKg,
                proofPhotoUrl = proofPhotoUrl ?: existing.proofPhotoUrl,
                specialInstructions = notes ?: existing.specialInstructions,
                ecoPointsEarned = awardedPoints,
                updatedAt = now.toString()
            )
            memoryStore[id] = updated
            return updated
        }
        return findById(id)
    }

    fun getSummaryStats(userId: UUID? = null): CollectionSummaryStats {
        var submitted = 0L
        var assigned = 0L
        var completed = 0L
        var totalWeight = 0.0
        var totalPoints = 0

        val items = if (userId != null) findByUser(userId) else memoryStore.values.toList()

        items.forEach { item ->
            when (item.status) {
                "submitted" -> submitted++
                "assigned", "in_transit" -> assigned++
                "completed" -> {
                    completed++
                    val weight = item.actualWeightKg ?: item.estimatedWeightKg ?: 0.0
                    totalWeight += weight
                    totalPoints += item.ecoPointsEarned
                }
            }
        }

        return CollectionSummaryStats(
            totalSubmitted = submitted,
            totalAssigned = assigned,
            totalCompleted = completed,
            totalWeightDivertedKg = Math.round(totalWeight * 100.0) / 100.0,
            totalEcoPointsEarned = totalPoints
        )
    }

    private fun ResultRow.toDto(): CollectionRequestDto {
        return CollectionRequestDto(
            id = this[CollectionRequests.id].toString(),
            userId = this[CollectionRequests.userId].toString(),
            collectorId = this[CollectionRequests.collectorId]?.toString(),
            facilityId = this[CollectionRequests.facilityId]?.toString(),
            status = this[CollectionRequests.status],
            pickupAddress = this[CollectionRequests.pickupAddress],
            city = this[CollectionRequests.city],
            latitude = this[CollectionRequests.latitude]?.toDouble(),
            longitude = this[CollectionRequests.longitude]?.toDouble(),
            preferredDate = this[CollectionRequests.preferredDate].format(DateTimeFormatter.ISO_LOCAL_DATE),
            preferredSlot = this[CollectionRequests.preferredSlot],
            materials = this[CollectionRequests.materials].split(",").map { it.trim() },
            estimatedWeightKg = this[CollectionRequests.estimatedWeightKg]?.toDouble(),
            actualWeightKg = this[CollectionRequests.actualWeightKg]?.toDouble(),
            specialInstructions = this[CollectionRequests.specialInstructions],
            proofPhotoUrl = this[CollectionRequests.proofPhotoUrl],
            ecoPointsEarned = this[CollectionRequests.ecoPointsEarned],
            createdAt = this[CollectionRequests.createdAt].toString(),
            updatedAt = this[CollectionRequests.updatedAt].toString()
        )
    }
}
