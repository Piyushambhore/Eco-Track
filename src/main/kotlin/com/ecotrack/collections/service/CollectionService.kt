package com.ecotrack.collections.service

import com.ecotrack.collections.model.*
import com.ecotrack.collections.repository.CollectionRepository
import com.ecotrack.collections.repository.FacilityRepository
import com.ecotrack.rewards.service.RewardsService
import java.util.UUID

class CollectionService(
    private val facilityRepository: FacilityRepository = FacilityRepository(),
    private val collectionRepository: CollectionRepository = CollectionRepository(),
    private val rewardsService: RewardsService? = null
) {

    fun searchFacilities(
        userLat: Double?,
        userLng: Double?,
        radiusKm: Double = 25.0,
        city: String? = null,
        material: String? = null
    ): List<RecyclingFacilityDto> {
        return if (userLat != null && userLng != null) {
            facilityRepository.findNearby(userLat, userLng, radiusKm, material)
        } else {
            facilityRepository.findAll(city, material)
        }
    }

    fun getFacilityById(id: String): RecyclingFacilityDto? {
        val uuid = try { UUID.fromString(id) } catch (e: Exception) { return null }
        return facilityRepository.findById(uuid)
    }

    fun createFacility(req: CreateFacilityRequest): RecyclingFacilityDto {
        require(req.name.isNotBlank()) { "Facility name cannot be blank" }
        require(req.address.isNotBlank()) { "Address cannot be blank" }
        return facilityRepository.createFacility(req)
    }

    fun schedulePickup(userId: UUID, req: CreateCollectionRequest): CollectionRequestDto {
        require(req.pickupAddress.isNotBlank()) { "Pickup address cannot be blank" }
        require(req.materials.isNotEmpty()) { "At least one recyclable material category must be selected" }
        require(req.estimatedWeightKg > 0) { "Estimated weight must be greater than zero" }

        val facilityUuid = req.facilityId?.let {
            try { UUID.fromString(it) } catch (e: Exception) { null }
        }

        return collectionRepository.create(
            userId = userId,
            pickupAddress = req.pickupAddress.trim(),
            city = req.city.trim().ifBlank { "Metropolis" },
            latitude = req.latitude,
            longitude = req.longitude,
            preferredDate = req.preferredDate,
            preferredSlot = req.preferredSlot,
            materials = req.materials,
            estimatedWeightKg = req.estimatedWeightKg,
            specialInstructions = req.specialInstructions?.trim(),
            facilityId = facilityUuid
        )
    }

    fun getUserCollections(userId: UUID, status: String? = null): List<CollectionRequestDto> {
        return collectionRepository.findByUser(userId, status)
    }

    fun getCollectionById(userId: UUID, id: String): CollectionRequestDto? {
        val uuid = try { UUID.fromString(id) } catch (e: Exception) { return null }
        val item = collectionRepository.findById(uuid) ?: return null
        // Ensure either owner or assigned collector can view
        if (item.userId != userId.toString() && item.collectorId != userId.toString()) {
            return null
        }
        return item
    }

    fun getCollectorQueue(city: String? = null): List<CollectionRequestDto> {
        return collectionRepository.findOpenQueue(city)
    }

    fun acceptPickup(collectorId: UUID, collectionId: String): CollectionRequestDto {
        val uuid = try { UUID.fromString(collectionId) } catch (e: Exception) {
            throw IllegalArgumentException("Invalid collection ID format")
        }
        val existing = collectionRepository.findById(uuid)
            ?: throw NoSuchElementException("Collection request not found")

        if (existing.status != "submitted") {
            throw IllegalStateException("Only requests with 'submitted' status can be accepted (current: ${existing.status})")
        }

        return collectionRepository.assignCollector(uuid, collectorId)
            ?: throw IllegalStateException("Failed to assign collector to request")
    }

    fun markInTransit(collectorId: UUID, collectionId: String): CollectionRequestDto {
        val uuid = try { UUID.fromString(collectionId) } catch (e: Exception) {
            throw IllegalArgumentException("Invalid collection ID format")
        }
        val existing = collectionRepository.findById(uuid)
            ?: throw NoSuchElementException("Collection request not found")

        if (existing.collectorId != collectorId.toString()) {
            throw IllegalStateException("Only the assigned collector can update transit status")
        }
        if (existing.status != "assigned") {
            throw IllegalStateException("Request must be in 'assigned' status to mark in transit (current: ${existing.status})")
        }

        return collectionRepository.updateStatus(uuid, "in_transit")
            ?: throw IllegalStateException("Failed to transition status to in_transit")
    }

    fun completePickup(
        collectorId: UUID,
        collectionId: String,
        req: CompleteCollectionRequest
    ): CollectionRequestDto {
        val uuid = try { UUID.fromString(collectionId) } catch (e: Exception) {
            throw IllegalArgumentException("Invalid collection ID format")
        }
        val existing = collectionRepository.findById(uuid)
            ?: throw NoSuchElementException("Collection request not found")

        if (existing.collectorId != collectorId.toString()) {
            throw IllegalStateException("Only the assigned collector can complete this pickup")
        }
        if (existing.status != "assigned" && existing.status != "in_transit") {
            throw IllegalStateException("Cannot complete request from state: ${existing.status}")
        }
        require(req.actualWeightKg > 0) { "Actual verified weight must be greater than zero" }

        val completed = collectionRepository.completeRequest(
            id = uuid,
            actualWeightKg = req.actualWeightKg,
            proofPhotoUrl = req.proofPhotoUrl,
            notes = req.notes
        ) ?: throw IllegalStateException("Failed to complete collection request")

        try {
            val userUuid = UUID.fromString(existing.userId)
            rewardsService?.awardCollectionPoints(userUuid, collectionId, req.actualWeightKg)
        } catch (_: Exception) {
            // Non-blocking
        }

        return completed
    }

    fun getStats(userId: UUID? = null): CollectionSummaryStats {
        return collectionRepository.getSummaryStats(userId)
    }
}
