package com.ecotrack.records.service

import com.ecotrack.common.*
import com.ecotrack.records.api.*
import com.ecotrack.records.repository.RecordEntity
import com.ecotrack.records.repository.RecordRepository
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

class RecordService(private val recordRepository: RecordRepository) {

    companion object {
        val VALID_RECORD_TYPES = setOf(
            "carbon_footprint", "energy_usage", "water_usage",
            "waste_recycled", "transport", "food_consumption",
            "tree_planting", "other"
        )
        val VALID_UNITS = setOf(
            "kg", "g", "liters", "ml", "kwh", "miles", "km",
            "hours", "count", "tons", "gallons"
        )
        const val MAX_PAGE_SIZE = 100
        const val DEFAULT_PAGE_SIZE = 20
    }

    fun listRecords(
        ownerId: UUID,
        page: Int = 1,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        recordType: String? = null
    ): PaginatedRecordsResponse {
        val safePage = maxOf(1, page)
        val safePageSize = minOf(maxOf(1, pageSize), MAX_PAGE_SIZE)

        recordType?.let {
            if (it !in VALID_RECORD_TYPES) {
                throw ValidationException("Invalid record type: $it")
            }
        }

        val (records, totalCount) = recordRepository.findByOwner(ownerId, safePage, safePageSize, recordType)
        val totalPages = if (totalCount == 0L) 1 else ((totalCount + safePageSize - 1) / safePageSize).toInt()

        return PaginatedRecordsResponse(
            data = records.map { it.toResponse() },
            page = safePage,
            pageSize = safePageSize,
            totalCount = totalCount,
            totalPages = totalPages
        )
    }

    fun getRecord(id: UUID, ownerId: UUID): RecordResponse {
        val record = recordRepository.findByIdAndOwner(id, ownerId)
            ?: throw NotFoundException("Record not found")
        return record.toResponse()
    }

    fun createRecord(ownerId: UUID, request: CreateRecordRequest): RecordResponse {
        validateCreateRequest(request)

        val occurredAt = parseDateTime(request.occurredAt)
        val carbonImpact = request.carbonImpact?.let { BigDecimal.valueOf(it) }
            ?: estimateCarbonImpact(request.recordType, request.quantity, request.unit)

        val record = recordRepository.create(
            id = UUID.randomUUID(),
            ownerId = ownerId,
            recordType = request.recordType,
            title = request.title.trim(),
            quantity = request.quantity?.let { BigDecimal.valueOf(it) },
            unit = request.unit,
            carbonImpact = carbonImpact,
            occurredAt = occurredAt,
            notes = request.notes?.trim()
        )
        return record.toResponse()
    }

    fun updateRecord(id: UUID, ownerId: UUID, request: UpdateRecordRequest): RecordResponse {
        val errors = mutableListOf<FieldError>()
        request.title?.let {
            if (it.isBlank()) errors.add(FieldError("title", "must not be blank"))
            if (it.length > 200) errors.add(FieldError("title", "must not exceed 200 characters"))
        }
        request.quantity?.let {
            if (it < 0) errors.add(FieldError("quantity", "must not be negative"))
        }
        request.unit?.let {
            if (it !in VALID_UNITS) errors.add(FieldError("unit", "invalid unit: $it"))
        }
        if (errors.isNotEmpty()) {
            throw ValidationException("The request contains invalid fields.", errors)
        }

        val occurredAt = request.occurredAt?.let { parseDateTime(it) }

        val record = recordRepository.update(
            id = id,
            ownerId = ownerId,
            title = request.title?.trim(),
            quantity = request.quantity?.let { BigDecimal.valueOf(it) },
            unit = request.unit,
            carbonImpact = request.carbonImpact?.let { BigDecimal.valueOf(it) },
            occurredAt = occurredAt,
            notes = request.notes?.trim()
        ) ?: throw NotFoundException("Record not found")

        return record.toResponse()
    }

    fun deleteRecord(id: UUID, ownerId: UUID) {
        val deleted = recordRepository.softDelete(id, ownerId)
        if (!deleted) throw NotFoundException("Record not found")
    }

    fun getSummary(ownerId: UUID): RecordSummaryResponse {
        val summaryByType = recordRepository.getSummaryByOwner(ownerId)
        val totalCarbonImpact = recordRepository.getTotalCarbonImpact(ownerId)
        val totalRecords = recordRepository.countByOwner(ownerId)

        return RecordSummaryResponse(
            totalRecords = totalRecords,
            totalCarbonImpact = totalCarbonImpact.toDouble(),
            byType = summaryByType.mapValues { (_, v) ->
                TypeSummary(
                    count = v.first,
                    totalQuantity = v.second.toDouble(),
                    totalCarbonImpact = v.third.toDouble()
                )
            }
        )
    }

    /**
     * Estimate carbon impact based on record type, quantity, and unit.
     * These are rough estimates for demonstration purposes.
     */
    private fun estimateCarbonImpact(recordType: String, quantity: Double?, unit: String?): BigDecimal? {
        if (quantity == null) return null
        val factor = when (recordType) {
            "carbon_footprint" -> 1.0                     // Direct CO2 kg
            "energy_usage" -> when (unit) {
                "kwh" -> 0.42                              // avg grid emission factor kg CO2/kWh
                else -> 0.42
            }
            "water_usage" -> when (unit) {
                "liters" -> 0.0003                         // kg CO2 per liter
                "gallons" -> 0.00114                       // kg CO2 per gallon
                else -> 0.0003
            }
            "waste_recycled" -> when (unit) {
                "kg" -> -0.5                               // negative = CO2 saved
                "tons" -> -500.0
                else -> -0.5
            }
            "transport" -> when (unit) {
                "km" -> 0.21                               // avg car emission kg CO2/km
                "miles" -> 0.34                            // avg car emission kg CO2/mile
                else -> 0.21
            }
            "food_consumption" -> when (unit) {
                "kg" -> 2.5                                // avg food carbon footprint
                else -> 2.5
            }
            "tree_planting" -> when (unit) {
                "count" -> -22.0                           // avg tree absorbs ~22kg CO2/year
                else -> -22.0
            }
            else -> 0.0
        }
        return BigDecimal.valueOf(quantity * factor).setScale(2, java.math.RoundingMode.HALF_UP)
    }

    private fun validateCreateRequest(request: CreateRecordRequest) {
        val errors = mutableListOf<FieldError>()

        if (request.recordType !in VALID_RECORD_TYPES) {
            errors.add(FieldError("recordType", "must be one of: ${VALID_RECORD_TYPES.joinToString(", ")}"))
        }
        if (request.title.isBlank()) {
            errors.add(FieldError("title", "must not be blank"))
        } else if (request.title.length > 200) {
            errors.add(FieldError("title", "must not exceed 200 characters"))
        }
        request.quantity?.let {
            if (it < 0) errors.add(FieldError("quantity", "must not be negative"))
        }
        request.unit?.let {
            if (it !in VALID_UNITS) {
                errors.add(FieldError("unit", "must be one of: ${VALID_UNITS.joinToString(", ")}"))
            }
        }
        if (request.occurredAt.isBlank()) {
            errors.add(FieldError("occurredAt", "must not be blank"))
        }

        if (errors.isNotEmpty()) {
            throw ValidationException("The request contains invalid fields.", errors)
        }
    }

    private fun parseDateTime(value: String): OffsetDateTime {
        return try {
            OffsetDateTime.parse(value)
        } catch (e: DateTimeParseException) {
            throw ValidationException("Invalid date-time format. Use ISO-8601 format (e.g., 2024-01-15T10:30:00Z)",
                listOf(FieldError("occurredAt", "invalid ISO-8601 date-time")))
        }
    }

    private fun RecordEntity.toResponse() = RecordResponse(
        id = id.toString(),
        ownerUserId = ownerUserId.toString(),
        recordType = recordType,
        title = title,
        quantity = quantity?.toDouble(),
        unit = unit,
        carbonImpact = carbonImpact?.toDouble(),
        occurredAt = occurredAt.toString(),
        notes = notes,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString()
    )
}
