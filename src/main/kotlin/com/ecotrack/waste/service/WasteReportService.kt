package com.ecotrack.waste.service

import com.ecotrack.common.NotFoundException
import com.ecotrack.common.ValidationException
import com.ecotrack.waste.api.*
import com.ecotrack.waste.repository.WasteReportEntity
import com.ecotrack.waste.repository.WasteReportRepository
import java.math.BigDecimal
import java.util.UUID

class WasteReportService(
    private val repository: WasteReportRepository = WasteReportRepository(),
    private val classifier: WasteClassifierService = WasteClassifierService()
) {

    fun identify(request: IdentifyWasteRequest): WasteClassificationResult {
        return classifier.classify(request)
    }

    fun getAllCategories(): List<WasteCategoryInfo> {
        return classifier.getAllCategories()
    }

    fun createReport(userId: UUID, request: CreateWasteReportRequest): WasteReportDto {
        val validCategories = listOf("plastic", "paper", "metal", "e_waste", "glass", "organic", "hazardous")
        if (request.category.lowercase() !in validCategories) {
            throw ValidationException("Invalid waste category '${request.category}'. Must be one of $validCategories")
        }

        // Auto calculate carbon impact if not provided based on category & quantity
        val calculatedImpact = request.carbonImpact ?: run {
            val factor = classifier.getAllCategories().find { it.category == request.category.lowercase() }?.carbonFactorPerKg ?: 1.0
            val qty = request.quantity ?: 1.0
            factor * qty
        }

        val entity = repository.create(
            userId = userId,
            category = request.category.lowercase(),
            subType = request.subType,
            photoUrl = request.photoUrl,
            latitude = request.latitude?.let { BigDecimal.valueOf(it) },
            longitude = request.longitude?.let { BigDecimal.valueOf(it) },
            quantity = request.quantity?.let { BigDecimal.valueOf(it) },
            unit = request.unit ?: "kg",
            confidence = request.confidence?.let { BigDecimal.valueOf(it) },
            disposalGuide = request.disposalGuide,
            carbonImpact = BigDecimal.valueOf(calculatedImpact)
        )

        return entity.toDto()
    }

    fun getUserReports(
        userId: UUID,
        page: Int = 1,
        pageSize: Int = 20,
        category: String? = null
    ): WasteReportsListResponse {
        val safePage = if (page < 1) 1 else page
        val safeSize = pageSize.coerceIn(1, 100)

        val (items, total) = repository.findByUser(userId, safePage, safeSize, category)
        val totalPages = if (total == 0L) 1 else Math.ceil(total.toDouble() / safeSize).toInt()

        return WasteReportsListResponse(
            items = items.map { it.toDto() },
            total = total,
            page = safePage,
            pageSize = safeSize,
            totalPages = totalPages
        )
    }

    fun getReportById(id: UUID, userId: UUID): WasteReportDto {
        val entity = repository.findByIdAndUser(id, userId)
            ?: throw NotFoundException("Waste report with ID $id not found")
        return entity.toDto()
    }

    private fun WasteReportEntity.toDto(): WasteReportDto = WasteReportDto(
        id = this.id.toString(),
        userId = this.userId.toString(),
        category = this.category,
        subType = this.subType,
        photoUrl = this.photoUrl,
        latitude = this.latitude?.toDouble(),
        longitude = this.longitude?.toDouble(),
        quantity = this.quantity?.toDouble(),
        unit = this.unit,
        confidence = this.confidence?.toDouble(),
        status = this.status,
        disposalGuide = this.disposalGuide,
        carbonImpact = this.carbonImpact?.toDouble(),
        createdAt = this.createdAt.toString(),
        updatedAt = this.updatedAt.toString()
    )
}
