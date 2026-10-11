package com.ecotrack.marketplace.repository

import com.ecotrack.database.MarketplaceListings
import com.ecotrack.database.MarketplaceOffers
import com.ecotrack.database.MarketplaceTransactions
import com.ecotrack.marketplace.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MarketplaceRepository {

    // Thread-safe in-memory stores for testing and resilient fallback
    private val listingsStore = ConcurrentHashMap<UUID, MarketplaceListingDto>()
    private val offersStore = ConcurrentHashMap<UUID, MutableList<MarketplaceOfferDto>>()
    private val transactionsStore = ConcurrentHashMap<UUID, MarketplaceTransactionDto>()

    init {
        // Seed default batches for testing & instant discovery
        seedDefaultListings()
    }

    private fun seedDefaultListings() {
        val dummySellerId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val now = OffsetDateTime.now(ZoneOffset.UTC).toString()

        val seeds = listOf(
            MarketplaceListingDto(
                id = "a1111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                sellerId = dummySellerId.toString(),
                title = "Clean PET Flakes & Crushed Bottles (350 kg)",
                materialType = "plastic",
                subType = "PET Flakes",
                quantityKg = 350.0,
                askingPricePerKg = 22.0,
                totalAskingPrice = 7700.0,
                currency = "INR",
                pickupCity = "Kolhapur",
                pickupAddress = "MIDC Shiroli Industrial Area, Gate 3",
                description = "Washed transparent PET flakes packed in super-sacks. Moisture < 1%. Ready for extruder.",
                status = "active",
                offerCount = 2,
                createdAt = now,
                updatedAt = now
            ),
            MarketplaceListingDto(
                id = "b2222222-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                sellerId = dummySellerId.toString(),
                title = "Baled Corrugated Cardboard Cartons (500 kg)",
                materialType = "paper",
                subType = "OCC Cardboard",
                quantityKg = 500.0,
                askingPricePerKg = 13.50,
                totalAskingPrice = 6750.0,
                currency = "INR",
                pickupCity = "Kolhapur",
                pickupAddress = "Shahupuri Commercial Yard, Near Railway Goods Shed",
                description = "Tight wire-bound high-density OCC cardboard bales. Dry warehouse stored.",
                status = "active",
                offerCount = 1,
                createdAt = now,
                updatedAt = now
            ),
            MarketplaceListingDto(
                id = "c3333333-cccc-cccc-cccc-cccccccccccc",
                sellerId = dummySellerId.toString(),
                title = "Aluminum Cans & Copper Wiring (85 kg)",
                materialType = "metal",
                subType = "Aluminum & Copper",
                quantityKg = 85.0,
                askingPricePerKg = 110.0,
                totalAskingPrice = 9350.0,
                currency = "INR",
                pickupCity = "Kolhapur",
                pickupAddress = "Udyamnagar Foundry District, Unit 14",
                description = "Clean sorted beverage cans and stripped copper wire. High purity recovery lot.",
                status = "active",
                offerCount = 3,
                createdAt = now,
                updatedAt = now
            ),
            MarketplaceListingDto(
                id = "d4444444-dddd-dddd-dddd-dddddddddddd",
                sellerId = dummySellerId.toString(),
                title = "Computer Motherboards & Server PCBs (40 kg)",
                materialType = "e_waste",
                subType = "Server & Telecom E-Waste",
                quantityKg = 40.0,
                askingPricePerKg = 180.0,
                totalAskingPrice = 7200.0,
                currency = "INR",
                pickupCity = "Kolhapur",
                pickupAddress = "Cyber Zone, Rajarampuri 3rd Lane",
                description = "Decommissioned desktop motherboards and server power supplies for safe component recovery.",
                status = "active",
                offerCount = 0,
                createdAt = now,
                updatedAt = now
            )
        )

        seeds.forEach { listing ->
            listingsStore[UUID.fromString(listing.id)] = listing
        }
    }

    fun createListing(
        sellerId: UUID,
        title: String,
        materialType: String,
        subType: String?,
        quantityKg: Double,
        askingPricePerKg: Double?,
        totalAskingPrice: Double,
        currency: String = "USD",
        pickupCity: String = "Metropolis",
        pickupAddress: String,
        latitude: Double?,
        longitude: Double?,
        photoUrl: String?,
        description: String?
    ): MarketplaceListingDto {
        val newId = UUID.randomUUID()
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        val dto = MarketplaceListingDto(
            id = newId.toString(),
            sellerId = sellerId.toString(),
            title = title,
            materialType = materialType.lowercase().trim(),
            subType = subType,
            quantityKg = quantityKg,
            askingPricePerKg = askingPricePerKg,
            totalAskingPrice = totalAskingPrice,
            currency = currency,
            pickupCity = pickupCity,
            pickupAddress = pickupAddress,
            latitude = latitude,
            longitude = longitude,
            photoUrl = photoUrl,
            description = description,
            status = "active",
            offerCount = 0,
            createdAt = now.toString(),
            updatedAt = now.toString()
        )

        try {
            transaction {
                MarketplaceListings.insert {
                    it[id] = newId
                    it[MarketplaceListings.sellerId] = sellerId
                    it[MarketplaceListings.title] = title
                    it[MarketplaceListings.materialType] = materialType.lowercase().trim()
                    it[MarketplaceListings.subType] = subType
                    it[MarketplaceListings.quantityKg] = quantityKg.toBigDecimal()
                    it[MarketplaceListings.askingPricePerKg] = askingPricePerKg?.toBigDecimal()
                    it[MarketplaceListings.totalAskingPrice] = totalAskingPrice.toBigDecimal()
                    it[MarketplaceListings.currency] = currency
                    it[MarketplaceListings.pickupCity] = pickupCity
                    it[MarketplaceListings.pickupAddress] = pickupAddress
                    it[MarketplaceListings.latitude] = latitude?.toBigDecimal()
                    it[MarketplaceListings.longitude] = longitude?.toBigDecimal()
                    it[MarketplaceListings.photoUrl] = photoUrl
                    it[MarketplaceListings.description] = description
                    it[status] = "active"
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
        } catch (_: Exception) {
            // DB not reachable, fall back to memory store
        }

        listingsStore[newId] = dto
        return dto
    }

    fun findListings(
        materialType: String? = null,
        maxPricePerKg: Double? = null,
        city: String? = null,
        status: String? = "active"
    ): List<MarketplaceListingDto> {
        val dbResults = try {
            transaction {
                val query = MarketplaceListings.selectAll()
                materialType?.takeIf { it.isNotBlank() }?.let { query.andWhere { MarketplaceListings.materialType eq it.lowercase().trim() } }
                city?.takeIf { it.isNotBlank() }?.let { query.andWhere { MarketplaceListings.pickupCity eq it } }
                status?.takeIf { it.isNotBlank() }?.let { query.andWhere { MarketplaceListings.status eq it } }
                maxPricePerKg?.let { query.andWhere { MarketplaceListings.askingPricePerKg lessEq it.toBigDecimal() } }

                query.orderBy(MarketplaceListings.createdAt to SortOrder.DESC).map { it.toDto() }
            }
        } catch (_: Exception) {
            emptyList()
        }

        if (dbResults.isNotEmpty()) return dbResults

        return listingsStore.values.filter { item ->
            val matchMaterial = materialType.isNullOrBlank() || item.materialType.equals(materialType.trim(), ignoreCase = true)
            val matchCity = city.isNullOrBlank() || item.pickupCity.equals(city.trim(), ignoreCase = true)
            val matchStatus = status.isNullOrBlank() || item.status.equals(status.trim(), ignoreCase = true)
            val matchPrice = maxPricePerKg == null || (item.askingPricePerKg ?: Double.MAX_VALUE) <= maxPricePerKg
            matchMaterial && matchCity && matchStatus && matchPrice
        }.sortedByDescending { it.createdAt }
    }

    fun findListingById(id: UUID): MarketplaceListingDto? {
        val fromDb = try {
            transaction {
                MarketplaceListings.selectAll()
                    .where { MarketplaceListings.id eq id }
                    .map { it.toDto() }
                    .singleOrNull()
            }
        } catch (_: Exception) {
            null
        }

        return fromDb ?: listingsStore[id]
    }

    fun findListingsBySeller(sellerId: UUID): List<MarketplaceListingDto> {
        val fromDb = try {
            transaction {
                MarketplaceListings.selectAll()
                    .where { MarketplaceListings.sellerId eq sellerId }
                    .orderBy(MarketplaceListings.createdAt to SortOrder.DESC)
                    .map { it.toDto() }
            }
        } catch (_: Exception) {
            emptyList()
        }

        if (fromDb.isNotEmpty()) return fromDb

        return listingsStore.values.filter { it.sellerId == sellerId.toString() }
            .sortedByDescending { it.createdAt }
    }

    fun updateListingStatus(id: UUID, newStatus: String): MarketplaceListingDto? {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        try {
            transaction {
                MarketplaceListings.update({ MarketplaceListings.id eq id }) {
                    it[status] = newStatus
                    it[updatedAt] = now
                }
            }
        } catch (_: Exception) {
            // Memory store update below
        }

        val existing = listingsStore[id]
        if (existing != null) {
            val updated = existing.copy(status = newStatus, updatedAt = now.toString())
            listingsStore[id] = updated
            return updated
        }
        return findListingById(id)
    }

    fun createOffer(
        listingId: UUID,
        buyerId: UUID,
        offeredPricePerKg: Double?,
        totalOfferedPrice: Double,
        message: String?
    ): MarketplaceOfferDto {
        val newId = UUID.randomUUID()
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        val dto = MarketplaceOfferDto(
            id = newId.toString(),
            listingId = listingId.toString(),
            buyerId = buyerId.toString(),
            offeredPricePerKg = offeredPricePerKg,
            totalOfferedPrice = totalOfferedPrice,
            message = message,
            status = "pending",
            createdAt = now.toString(),
            updatedAt = now.toString()
        )

        try {
            transaction {
                MarketplaceOffers.insert {
                    it[id] = newId
                    it[MarketplaceOffers.listingId] = listingId
                    it[MarketplaceOffers.buyerId] = buyerId
                    it[MarketplaceOffers.offeredPricePerKg] = offeredPricePerKg?.toBigDecimal()
                    it[MarketplaceOffers.totalOfferedPrice] = totalOfferedPrice.toBigDecimal()
                    it[MarketplaceOffers.message] = message
                    it[status] = "pending"
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
        } catch (_: Exception) {
            // Memory store fallback
        }

        val list = offersStore.computeIfAbsent(listingId) { mutableListOf() }
        list.add(dto)

        // Increment offerCount on listing
        listingsStore[listingId]?.let { listing ->
            listingsStore[listingId] = listing.copy(offerCount = listing.offerCount + 1)
        }

        return dto
    }

    fun findOffersByListing(listingId: UUID): List<MarketplaceOfferDto> {
        val fromDb = try {
            transaction {
                MarketplaceOffers.selectAll()
                    .where { MarketplaceOffers.listingId eq listingId }
                    .orderBy(MarketplaceOffers.createdAt to SortOrder.DESC)
                    .map { it.toOfferDto() }
            }
        } catch (_: Exception) {
            emptyList()
        }

        if (fromDb.isNotEmpty()) return fromDb

        return offersStore[listingId] ?: emptyList()
    }

    fun findOfferById(offerId: UUID): MarketplaceOfferDto? {
        val fromDb = try {
            transaction {
                MarketplaceOffers.selectAll()
                    .where { MarketplaceOffers.id eq offerId }
                    .map { it.toOfferDto() }
                    .singleOrNull()
            }
        } catch (_: Exception) {
            null
        }

        if (fromDb != null) return fromDb

        return offersStore.values.flatten().find { it.id == offerId.toString() }
    }

    fun acceptOffer(offerId: UUID): MarketplaceOfferDto? {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        try {
            transaction {
                MarketplaceOffers.update({ MarketplaceOffers.id eq offerId }) {
                    it[status] = "accepted"
                    it[updatedAt] = now
                }
            }
        } catch (_: Exception) {
            // Memory store update below
        }

        val allOffers = offersStore.values.flatten()
        val target = allOffers.find { it.id == offerId.toString() } ?: return null
        val updated = target.copy(status = "accepted", updatedAt = now.toString())

        // Update in list
        val listingId = UUID.fromString(target.listingId)
        val list = offersStore[listingId]
        if (list != null) {
            val idx = list.indexOfFirst { it.id == offerId.toString() }
            if (idx >= 0) list[idx] = updated
        }

        // Lock listing to reserved
        updateListingStatus(listingId, "reserved")

        return updated
    }

    fun createTransaction(
        listingId: UUID,
        offerId: UUID,
        sellerId: UUID,
        buyerId: UUID,
        agreedPrice: Double
    ): MarketplaceTransactionDto {
        val newId = UUID.randomUUID()
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        val dto = MarketplaceTransactionDto(
            id = newId.toString(),
            listingId = listingId.toString(),
            offerId = offerId.toString(),
            sellerId = sellerId.toString(),
            buyerId = buyerId.toString(),
            agreedPrice = agreedPrice,
            verifiedWeightKg = null,
            settlementStatus = "escrow",
            ecoPointsAwarded = 0,
            createdAt = now.toString(),
            settledAt = null
        )

        try {
            transaction {
                MarketplaceTransactions.insert {
                    it[id] = newId
                    it[MarketplaceTransactions.listingId] = listingId
                    it[MarketplaceTransactions.offerId] = offerId
                    it[MarketplaceTransactions.sellerId] = sellerId
                    it[MarketplaceTransactions.buyerId] = buyerId
                    it[MarketplaceTransactions.agreedPrice] = agreedPrice.toBigDecimal()
                    it[settlementStatus] = "escrow"
                    it[createdAt] = now
                }
            }
        } catch (_: Exception) {
            // Memory store fallback
        }

        transactionsStore[newId] = dto
        return dto
    }

    fun findTransactionById(id: UUID): MarketplaceTransactionDto? {
        val fromDb = try {
            transaction {
                MarketplaceTransactions.selectAll()
                    .where { MarketplaceTransactions.id eq id }
                    .map { it.toTransactionDto() }
                    .singleOrNull()
            }
        } catch (_: Exception) {
            null
        }

        return fromDb ?: transactionsStore[id]
    }

    fun settleTransaction(id: UUID, verifiedWeightKg: Double): MarketplaceTransactionDto? {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val points = (verifiedWeightKg * 15).toInt() + 50 // 15 eco-points per kg traded + 50 bonus

        try {
            transaction {
                MarketplaceTransactions.update({ MarketplaceTransactions.id eq id }) {
                    it[settlementStatus] = "settled"
                    it[MarketplaceTransactions.verifiedWeightKg] = verifiedWeightKg.toBigDecimal()
                    it[ecoPointsAwarded] = points
                    it[settledAt] = now
                }
            }
        } catch (_: Exception) {
            // Memory store fallback
        }

        val existing = transactionsStore[id]
        if (existing != null) {
            val updated = existing.copy(
                settlementStatus = "settled",
                verifiedWeightKg = verifiedWeightKg,
                ecoPointsAwarded = points,
                settledAt = now.toString()
            )
            transactionsStore[id] = updated

            // Mark listing as sold
            val listingUuid = UUID.fromString(existing.listingId)
            updateListingStatus(listingUuid, "sold")

            return updated
        }
        return findTransactionById(id)
    }

    fun getStats(): MarketplaceStatsDto {
        val activeListings = listingsStore.values.filter { it.status == "active" }
        val settledTransactions = transactionsStore.values.filter { it.settlementStatus == "settled" }

        val availableKg = activeListings.sumOf { it.quantityKg }
        val tradedKg = settledTransactions.sumOf { it.verifiedWeightKg ?: 0.0 }
        val valueTraded = settledTransactions.sumOf { it.agreedPrice }

        return MarketplaceStatsDto(
            activeListingsCount = activeListings.size.toLong(),
            totalMaterialsAvailableKg = Math.round(availableKg * 100.0) / 100.0,
            totalVolumeTradedKg = Math.round(tradedKg * 100.0) / 100.0,
            totalSettledTransactions = settledTransactions.size.toLong(),
            totalValueTraded = Math.round(valueTraded * 100.0) / 100.0
        )
    }

    private fun ResultRow.toDto(): MarketplaceListingDto {
        val idVal = this[MarketplaceListings.id]
        val offers = offersStore[idVal]?.size ?: 0

        return MarketplaceListingDto(
            id = idVal.toString(),
            sellerId = this[MarketplaceListings.sellerId].toString(),
            title = this[MarketplaceListings.title],
            materialType = this[MarketplaceListings.materialType],
            subType = this[MarketplaceListings.subType],
            quantityKg = this[MarketplaceListings.quantityKg].toDouble(),
            askingPricePerKg = this[MarketplaceListings.askingPricePerKg]?.toDouble(),
            totalAskingPrice = this[MarketplaceListings.totalAskingPrice].toDouble(),
            currency = this[MarketplaceListings.currency],
            pickupCity = this[MarketplaceListings.pickupCity],
            pickupAddress = this[MarketplaceListings.pickupAddress],
            latitude = this[MarketplaceListings.latitude]?.toDouble(),
            longitude = this[MarketplaceListings.longitude]?.toDouble(),
            photoUrl = this[MarketplaceListings.photoUrl],
            description = this[MarketplaceListings.description],
            status = this[MarketplaceListings.status],
            offerCount = offers,
            createdAt = this[MarketplaceListings.createdAt].toString(),
            updatedAt = this[MarketplaceListings.updatedAt].toString()
        )
    }

    private fun ResultRow.toOfferDto(): MarketplaceOfferDto {
        return MarketplaceOfferDto(
            id = this[MarketplaceOffers.id].toString(),
            listingId = this[MarketplaceOffers.listingId].toString(),
            buyerId = this[MarketplaceOffers.buyerId].toString(),
            offeredPricePerKg = this[MarketplaceOffers.offeredPricePerKg]?.toDouble(),
            totalOfferedPrice = this[MarketplaceOffers.totalOfferedPrice].toDouble(),
            message = this[MarketplaceOffers.message],
            status = this[MarketplaceOffers.status],
            createdAt = this[MarketplaceOffers.createdAt].toString(),
            updatedAt = this[MarketplaceOffers.updatedAt].toString()
        )
    }

    private fun ResultRow.toTransactionDto(): MarketplaceTransactionDto {
        return MarketplaceTransactionDto(
            id = this[MarketplaceTransactions.id].toString(),
            listingId = this[MarketplaceTransactions.listingId].toString(),
            offerId = this[MarketplaceTransactions.offerId].toString(),
            sellerId = this[MarketplaceTransactions.sellerId].toString(),
            buyerId = this[MarketplaceTransactions.buyerId].toString(),
            agreedPrice = this[MarketplaceTransactions.agreedPrice].toDouble(),
            verifiedWeightKg = this[MarketplaceTransactions.verifiedWeightKg]?.toDouble(),
            settlementStatus = this[MarketplaceTransactions.settlementStatus],
            ecoPointsAwarded = this[MarketplaceTransactions.ecoPointsAwarded],
            createdAt = this[MarketplaceTransactions.createdAt].toString(),
            settledAt = this[MarketplaceTransactions.settledAt]?.toString()
        )
    }
}
