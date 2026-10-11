package com.ecotrack.collections.repository

import com.ecotrack.collections.model.RecyclingFacilityDto
import com.ecotrack.collections.model.CreateFacilityRequest
import com.ecotrack.database.RecyclingFacilities
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.math.*

class FacilityRepository {

    // Fallback seed directory if database is running without Flyway or during standalone testing
    private val defaultFacilities = listOf(
        RecyclingFacilityDto(
            id = "f1111111-1111-1111-1111-111111111111",
            name = "GreenCycle Recycling",
            facilityType = "recycling_center",
            address = "104 MIDC Industrial Area, Shiroli",
            city = "Kolhapur",
            postalCode = "416122",
            latitude = 16.7080,
            longitude = 74.2450,
            contactPhone = "+91 98221 45678",
            operatingHours = "Mon-Sat 08:00 - 18:00",
            acceptedMaterials = listOf("plastic", "paper", "metal", "glass"),
            pricingInfo = "Cardboard: ₹12/kg | PET Plastic: ₹18/kg | Aluminum: ₹95/kg",
            rating = 4.90,
            verified = true
        ),
        RecyclingFacilityDto(
            id = "f2222222-2222-2222-2222-222222222222",
            name = "Metro Scrap Traders",
            facilityType = "scrap_dealer",
            address = "45 Foundry Compound, Shahupuri",
            city = "Kolhapur",
            postalCode = "416001",
            latitude = 16.7150,
            longitude = 74.2510,
            contactPhone = "+91 98222 56789",
            operatingHours = "Mon-Sat 08:30 - 19:00",
            acceptedMaterials = listOf("metal", "paper", "plastic"),
            pricingInfo = "Copper: ₹480/kg | Brass: ₹320/kg | Batteries: ₹80/kg",
            rating = 4.80,
            verified = true
        ),
        RecyclingFacilityDto(
            id = "f3333333-3333-3333-3333-333333333333",
            name = "TerraNova E-Waste Safe Hub",
            facilityType = "e_waste_hub",
            address = "12 Cyber Road, Rajarampuri",
            city = "Kolhapur",
            postalCode = "416008",
            latitude = 16.6950,
            longitude = 74.2380,
            contactPhone = "+91 98223 67890",
            operatingHours = "Tue-Sun 10:00 - 18:00",
            acceptedMaterials = listOf("e_waste", "hazardous"),
            pricingInfo = "Certified PCB & lithium battery drop-off | Safe destruction receipt",
            rating = 4.95,
            verified = true
        ),
        RecyclingFacilityDto(
            id = "f4444444-4444-4444-4444-444444444444",
            name = "BioLoop Community Compost Center",
            facilityType = "compost_facility",
            address = "Rankala Lake Eco Park, Near Padmaraje Garden",
            city = "Kolhapur",
            postalCode = "416012",
            latitude = 16.6890,
            longitude = 74.2180,
            contactPhone = "+91 98224 78901",
            operatingHours = "Mon-Sun 07:30 - 16:30",
            acceptedMaterials = listOf("organic"),
            pricingInfo = "Free organic biomass drop-off | Take home free compost soil",
            rating = 4.85,
            verified = true
        ),
        RecyclingFacilityDto(
            id = "f5555555-5555-5555-5555-555555555555",
            name = "Laxmipuri Circular Glass & Paper Depot",
            facilityType = "recycling_center",
            address = "230 Main Market Yard, Laxmipuri",
            city = "Kolhapur",
            postalCode = "416002",
            latitude = 16.7020,
            longitude = 74.2350,
            contactPhone = "+91 98225 89012",
            operatingHours = "Mon-Fri 08:30 - 18:00",
            acceptedMaterials = listOf("glass", "paper", "plastic"),
            pricingInfo = "Kraft Cartons: ₹11/kg | Glass Bottles: ₹4/bottle",
            rating = 4.75,
            verified = true
        )
    )

    fun findAll(city: String? = null, material: String? = null): List<RecyclingFacilityDto> {
        return try {
            transaction {
                val query = RecyclingFacilities.selectAll()
                city?.let { query.andWhere { RecyclingFacilities.city eq it } }
                val results = query.map { it.toDto() }
                if (results.isNotEmpty()) {
                    filterByMaterial(results, material)
                } else {
                    filterByMaterial(defaultFacilities, material)
                }
            }
        } catch (e: Exception) {
            filterByMaterial(defaultFacilities, material)
        }
    }

    fun findNearby(
        userLat: Double,
        userLng: Double,
        radiusKm: Double = 25.0,
        material: String? = null
    ): List<RecyclingFacilityDto> {
        val all = findAll(material = material)
        return all
            .map { facility ->
                val distance = calculateHaversineDistance(userLat, userLng, facility.latitude, facility.longitude)
                facility.copy(distanceKm = distance)
            }
            .filter { (it.distanceKm ?: Double.MAX_VALUE) <= radiusKm }
            .sortedBy { it.distanceKm }
    }

    fun findById(id: UUID): RecyclingFacilityDto? {
        return try {
            transaction {
                RecyclingFacilities.selectAll()
                    .where { RecyclingFacilities.id eq id }
                    .map { it.toDto() }
                    .singleOrNull()
            } ?: defaultFacilities.find { it.id == id.toString() }
        } catch (e: Exception) {
            defaultFacilities.find { it.id == id.toString() }
        }
    }

    fun createFacility(req: CreateFacilityRequest): RecyclingFacilityDto = transaction {
        val newId = UUID.randomUUID()
        val now = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
        RecyclingFacilities.insert {
            it[RecyclingFacilities.id] = newId
            it[RecyclingFacilities.name] = req.name.trim()
            it[RecyclingFacilities.facilityType] = req.facilityType.trim()
            it[RecyclingFacilities.address] = req.address.trim()
            it[RecyclingFacilities.city] = req.city.trim()
            it[RecyclingFacilities.postalCode] = req.postalCode?.trim()
            it[RecyclingFacilities.latitude] = req.latitude.toBigDecimal()
            it[RecyclingFacilities.longitude] = req.longitude.toBigDecimal()
            it[RecyclingFacilities.contactPhone] = req.contactPhone?.trim()
            it[RecyclingFacilities.operatingHours] = req.operatingHours?.trim()
            it[RecyclingFacilities.acceptedMaterials] = req.acceptedMaterials.joinToString(",")
            it[RecyclingFacilities.pricingInfo] = req.pricingInfo?.trim()
            it[RecyclingFacilities.rating] = 5.0.toBigDecimal()
            it[RecyclingFacilities.verified] = true
            it[RecyclingFacilities.createdAt] = now
            it[RecyclingFacilities.updatedAt] = now
        }
        RecyclingFacilityDto(
            id = newId.toString(),
            name = req.name.trim(),
            facilityType = req.facilityType.trim(),
            address = req.address.trim(),
            city = req.city.trim(),
            postalCode = req.postalCode?.trim(),
            latitude = req.latitude,
            longitude = req.longitude,
            contactPhone = req.contactPhone?.trim(),
            operatingHours = req.operatingHours?.trim(),
            acceptedMaterials = req.acceptedMaterials,
            pricingInfo = req.pricingInfo?.trim(),
            rating = 5.0,
            verified = true
        )
    }

    private fun filterByMaterial(list: List<RecyclingFacilityDto>, material: String?): List<RecyclingFacilityDto> {
        if (material.isNullOrBlank()) return list
        val target = material.trim().lowercase()
        return list.filter { facility ->
            facility.acceptedMaterials.any { it.lowercase() == target }
        }
    }

    private fun ResultRow.toDto(): RecyclingFacilityDto {
        return RecyclingFacilityDto(
            id = this[RecyclingFacilities.id].toString(),
            name = this[RecyclingFacilities.name],
            facilityType = this[RecyclingFacilities.facilityType],
            address = this[RecyclingFacilities.address],
            city = this[RecyclingFacilities.city],
            postalCode = this[RecyclingFacilities.postalCode],
            latitude = this[RecyclingFacilities.latitude].toDouble(),
            longitude = this[RecyclingFacilities.longitude].toDouble(),
            contactPhone = this[RecyclingFacilities.contactPhone],
            operatingHours = this[RecyclingFacilities.operatingHours],
            acceptedMaterials = this[RecyclingFacilities.acceptedMaterials].split(",").map { it.trim() },
            pricingInfo = this[RecyclingFacilities.pricingInfo],
            rating = this[RecyclingFacilities.rating]?.toDouble() ?: 4.8,
            verified = this[RecyclingFacilities.verified]
        )
    }

    companion object {
        fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371.0 // Radius of earth in km
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            val dist = r * c
            return (dist * 100.0).roundToInt() / 100.0
        }
    }
}
