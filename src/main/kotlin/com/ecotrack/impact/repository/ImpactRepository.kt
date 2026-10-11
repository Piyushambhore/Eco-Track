package com.ecotrack.impact.repository

import com.ecotrack.database.ImpactCertificates
import com.ecotrack.database.ImpactRecords
import com.ecotrack.impact.model.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class ImpactRecordEntity(
    val id: String,
    val userId: String,
    val sourceType: String,
    val sourceId: String?,
    val materialType: String,
    val weightKg: Double,
    val co2SavedKg: Double,
    val waterSavedLiters: Double,
    val energySavedKwh: Double,
    val createdAt: String
)

class ImpactRepository {

    private val recordsStore = ConcurrentHashMap<UUID, CopyOnWriteArrayList<ImpactRecordEntity>>()
    private val certificatesStore = ConcurrentHashMap<String, ImpactCertificateDto>()

    init {
        seedInitialDemoImpact()
    }

    private fun seedInitialDemoImpact() {
        val demoUserId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        val list = CopyOnWriteArrayList<ImpactRecordEntity>()
        list.add(
            ImpactRecordEntity(
                id = UUID.randomUUID().toString(),
                userId = demoUserId.toString(),
                sourceType = "COLLECTION",
                sourceId = "ET-CB-9042",
                materialType = "paper",
                weightKg = 5.0,
                co2SavedKg = 5.5,
                waterSavedLiters = 130.0,
                energySavedKwh = 20.0,
                createdAt = now.minusDays(10).toString()
            )
        )
        list.add(
            ImpactRecordEntity(
                id = UUID.randomUUID().toString(),
                userId = demoUserId.toString(),
                sourceType = "COLLECTION",
                sourceId = "ET-PL-8821",
                materialType = "plastic",
                weightKg = 4.0,
                co2SavedKg = 6.0,
                waterSavedLiters = 48.0,
                energySavedKwh = 23.2,
                createdAt = now.minusDays(5).toString()
            )
        )
        list.add(
            ImpactRecordEntity(
                id = UUID.randomUUID().toString(),
                userId = demoUserId.toString(),
                sourceType = "COLLECTION",
                sourceId = "ET-MT-7712",
                materialType = "metal",
                weightKg = 2.0,
                co2SavedKg = 3.6,
                waterSavedLiters = 30.0,
                energySavedKwh = 28.0,
                createdAt = now.minusDays(2).toString()
            )
        )
        list.add(
            ImpactRecordEntity(
                id = UUID.randomUUID().toString(),
                userId = demoUserId.toString(),
                sourceType = "COLLECTION",
                sourceId = "ET-GL-6601",
                materialType = "glass",
                weightKg = 1.0,
                co2SavedKg = 0.3,
                waterSavedLiters = 5.0,
                energySavedKwh = 1.5,
                createdAt = now.minusDays(1).toString()
            )
        )
        recordsStore[demoUserId] = list
    }

    fun recordImpact(
        userId: UUID,
        sourceType: String,
        sourceId: String?,
        materialType: String,
        weightKg: Double,
        co2SavedKg: Double,
        waterSavedLiters: Double,
        energySavedKwh: Double
    ): ImpactRecordEntity {
        val entryId = UUID.randomUUID()
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        val entity = ImpactRecordEntity(
            id = entryId.toString(),
            userId = userId.toString(),
            sourceType = sourceType,
            sourceId = sourceId,
            materialType = materialType.lowercase().trim(),
            weightKg = weightKg,
            co2SavedKg = co2SavedKg,
            waterSavedLiters = waterSavedLiters,
            energySavedKwh = energySavedKwh,
            createdAt = now.toString()
        )

        recordsStore.computeIfAbsent(userId) { CopyOnWriteArrayList() }.add(entity)

        try {
            transaction {
                ImpactRecords.insert {
                    it[ImpactRecords.id] = entryId
                    it[ImpactRecords.userId] = userId
                    it[ImpactRecords.sourceType] = sourceType
                    it[ImpactRecords.sourceId] = sourceId
                    it[ImpactRecords.materialType] = materialType.lowercase().trim()
                    it[ImpactRecords.weightKg] = BigDecimal.valueOf(weightKg)
                    it[ImpactRecords.co2SavedKg] = BigDecimal.valueOf(co2SavedKg)
                    it[ImpactRecords.waterSavedLiters] = BigDecimal.valueOf(waterSavedLiters)
                    it[ImpactRecords.energySavedKwh] = BigDecimal.valueOf(energySavedKwh)
                    it[ImpactRecords.createdAt] = now
                }
            }
        } catch (_: Exception) {
            // Memory fallback
        }

        return entity
    }

    fun getUserImpactRecords(userId: UUID): List<ImpactRecordEntity> {
        return recordsStore[userId] ?: emptyList()
    }

    fun saveCertificate(cert: ImpactCertificateDto) {
        certificatesStore[cert.id] = cert
        certificatesStore[cert.certificateNumber] = cert

        try {
            transaction {
                ImpactCertificates.insert {
                    it[ImpactCertificates.id] = UUID.fromString(cert.id)
                    it[ImpactCertificates.certificateNumber] = cert.certificateNumber
                    it[ImpactCertificates.userId] = UUID.fromString(cert.userId)
                    it[ImpactCertificates.title] = cert.title
                    it[ImpactCertificates.periodStart] = OffsetDateTime.parse(cert.periodStart)
                    it[ImpactCertificates.periodEnd] = OffsetDateTime.parse(cert.periodEnd)
                    it[ImpactCertificates.totalWeightKg] = BigDecimal.valueOf(cert.totalWeightKg)
                    it[ImpactCertificates.totalCo2Kg] = BigDecimal.valueOf(cert.totalCo2Kg)
                    it[ImpactCertificates.treesEquivalent] = BigDecimal.valueOf(cert.treesEquivalent)
                    it[ImpactCertificates.waterSavedLiters] = BigDecimal.valueOf(cert.waterSavedLiters)
                    it[ImpactCertificates.energySavedKwh] = BigDecimal.valueOf(cert.energySavedKwh)
                    it[ImpactCertificates.verificationHash] = cert.verificationHash
                    it[ImpactCertificates.status] = cert.status
                    it[ImpactCertificates.issuedAt] = OffsetDateTime.parse(cert.issuedAt)
                }
            }
        } catch (_: Exception) {
            // Memory fallback
        }
    }

    fun findCertificate(idOrNumber: String): ImpactCertificateDto? {
        return certificatesStore[idOrNumber]
    }

    fun getUserCertificates(userId: UUID): List<ImpactCertificateDto> {
        return certificatesStore.values.filter { it.userId == userId.toString() }.distinctBy { it.id }
    }
}
