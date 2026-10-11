package com.ecotrack.collections

import com.ecotrack.collections.model.CompleteCollectionRequest
import com.ecotrack.collections.model.CreateCollectionRequest
import com.ecotrack.collections.repository.FacilityRepository
import com.ecotrack.collections.service.CollectionService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import java.util.UUID

class FacilitySpatialTest {

    private val facilityRepo = FacilityRepository()
    private val collectionService = CollectionService(facilityRepository = facilityRepo)

    @Test
    fun `test haversine distance calculation is accurate`() {
        // Distance between NYC Times Square (40.7580, -73.9855) and Brooklyn Bridge (40.7061, -73.9969)
        // Expected distance is approximately 5.8 km
        val distance = FacilityRepository.calculateHaversineDistance(
            40.7580, -73.9855,
            40.7061, -73.9969
        )
        assertTrue(distance in 5.5..6.2, "Expected distance between ~5.5 and 6.2 km, got $distance km")
    }

    @Test
    fun `test nearby facilities within 10km radius`() {
        // Search centered at downtown Metropolis (40.7128, -74.0060)
        val nearby = facilityRepo.findNearby(
            userLat = 40.7128,
            userLng = -74.0060,
            radiusKm = 10.0
        )

        assertFalse(nearby.isEmpty(), "Should find at least 1 facility within 10km")
        // Check sorted by distance ascending
        for (i in 0 until nearby.size - 1) {
            val d1 = nearby[i].distanceKm ?: 0.0
            val d2 = nearby[i + 1].distanceKm ?: 0.0
            assertTrue(d1 <= d2, "Facilities must be sorted by distance ascending: $d1 <= $d2")
        }
    }

    @Test
    fun `test facilities filtered by accepted material`() {
        // Search specifically for e-waste facilities
        val eWasteHubs = facilityRepo.findAll(material = "e_waste")
        assertFalse(eWasteHubs.isEmpty(), "Should find e-waste facilities")

        eWasteHubs.forEach { facility ->
            assertTrue(
                facility.acceptedMaterials.any { it.equals("e_waste", ignoreCase = true) },
                "Facility ${facility.name} must accept e_waste"
            )
        }
    }

    @Test
    fun `test schedule pickup validates empty address and materials`() {
        val dummyUserId = UUID.randomUUID()

        assertFailsWith<IllegalArgumentException> {
            collectionService.schedulePickup(
                dummyUserId,
                CreateCollectionRequest(
                    pickupAddress = "",
                    preferredDate = "2026-10-15",
                    materials = listOf("plastic"),
                    estimatedWeightKg = 5.0
                )
            )
        }

        assertFailsWith<IllegalArgumentException> {
            collectionService.schedulePickup(
                dummyUserId,
                CreateCollectionRequest(
                    pickupAddress = "123 Green St",
                    preferredDate = "2026-10-15",
                    materials = emptyList(),
                    estimatedWeightKg = 5.0
                )
            )
        }
    }

    @Test
    fun `test collection lifecycle validation enforces state guards`() {
        val dummyCollectorId = UUID.randomUUID()
        val nonExistentId = UUID.randomUUID().toString()

        // Accepting non-existent request throws NoSuchElementException
        assertFailsWith<NoSuchElementException> {
            collectionService.acceptPickup(dummyCollectorId, nonExistentId)
        }

        // Marking in-transit non-existent request throws NoSuchElementException
        assertFailsWith<NoSuchElementException> {
            collectionService.markInTransit(dummyCollectorId, nonExistentId)
        }

        // Completing with non-existent request throws NoSuchElementException
        assertFailsWith<NoSuchElementException> {
            collectionService.completePickup(
                dummyCollectorId,
                nonExistentId,
                CompleteCollectionRequest(actualWeightKg = 0.0)
            )
        }
    }
}
