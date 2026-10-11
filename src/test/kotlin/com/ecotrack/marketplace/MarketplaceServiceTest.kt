package com.ecotrack.marketplace

import com.ecotrack.marketplace.model.CreateListingRequest
import com.ecotrack.marketplace.model.CreateOfferRequest
import com.ecotrack.marketplace.model.SettleTransactionRequest
import com.ecotrack.marketplace.repository.MarketplaceRepository
import com.ecotrack.marketplace.service.MarketplaceService
import kotlin.test.*
import java.util.UUID

class MarketplaceServiceTest {

    private val repository = MarketplaceRepository()
    private val service = MarketplaceService(repository)

    @Test
    fun `test create listing successfully`() {
        val sellerId = UUID.randomUUID()
        val request = CreateListingRequest(
            title = "Sorted HDPE Milk Jugs (200 kg)",
            materialType = "plastic",
            subType = "HDPE #2",
            quantityKg = 200.0,
            askingPricePerKg = 0.35,
            pickupCity = "Metropolis",
            pickupAddress = "Eco Depot Dock 2",
            description = "Washed and shredded HDPE flakes ready for processing."
        )

        val listing = service.createListing(sellerId, request)

        assertNotNull(listing.id)
        assertEquals(sellerId.toString(), listing.sellerId)
        assertEquals("plastic", listing.materialType)
        assertEquals(200.0, listing.quantityKg)
        assertEquals(0.35, listing.askingPricePerKg)
        assertEquals(70.0, listing.totalAskingPrice) // 200 * 0.35
        assertEquals("active", listing.status)
    }

    @Test
    fun `test create listing validation rejects empty title and negative weight`() {
        val sellerId = UUID.randomUUID()

        assertFailsWith<IllegalArgumentException> {
            service.createListing(
                sellerId,
                CreateListingRequest(
                    title = "",
                    materialType = "metal",
                    quantityKg = 50.0,
                    pickupAddress = "Dock 1"
                )
            )
        }

        assertFailsWith<IllegalArgumentException> {
            service.createListing(
                sellerId,
                CreateListingRequest(
                    title = "Aluminum Cans",
                    materialType = "metal",
                    quantityKg = -5.0,
                    pickupAddress = "Dock 1"
                )
            )
        }
    }

    @Test
    fun `test place offer and offer count increments`() {
        val sellerId = UUID.randomUUID()
        val buyerId = UUID.randomUUID()

        val listing = service.createListing(
            sellerId,
            CreateListingRequest(
                title = "Mixed Office Waste Paper",
                materialType = "paper",
                quantityKg = 500.0,
                askingPricePerKg = 0.10,
                pickupAddress = "Station 5"
            )
        )

        val offer = service.placeOffer(
            buyerId,
            listing.id,
            CreateOfferRequest(
                offeredPricePerKg = 0.09,
                totalOfferedPrice = 45.0,
                message = "Can pick up tomorrow morning with our own truck."
            )
        )

        assertNotNull(offer.id)
        assertEquals(listing.id, offer.listingId)
        assertEquals(buyerId.toString(), offer.buyerId)
        assertEquals(45.0, offer.totalOfferedPrice)
        assertEquals("pending", offer.status)

        // Verify offers list for seller
        val sellerOffers = service.getOffersForListing(sellerId, listing.id)
        assertEquals(1, sellerOffers.size)
        assertEquals(offer.id, sellerOffers[0].id)
    }

    @Test
    fun `test self-bid is strictly prohibited`() {
        val sellerId = UUID.randomUUID()

        val listing = service.createListing(
            sellerId,
            CreateListingRequest(
                title = "Copper Cable Scrap",
                materialType = "metal",
                quantityKg = 30.0,
                askingPricePerKg = 5.0,
                pickupAddress = "Warehouse Yard"
            )
        )

        // Attempting to bid on own listing must fail
        assertFailsWith<IllegalArgumentException> {
            service.placeOffer(
                sellerId, // Same user
                listing.id,
                CreateOfferRequest(
                    offeredPricePerKg = 5.5,
                    totalOfferedPrice = 165.0
                )
            )
        }
    }

    @Test
    fun `test accept offer and complete settlement lifecycle`() {
        val sellerId = UUID.randomUUID()
        val buyerId = UUID.randomUUID()

        val listing = service.createListing(
            sellerId,
            CreateListingRequest(
                title = "Automotive Lead-Acid Batteries",
                materialType = "e_waste",
                quantityKg = 150.0,
                askingPricePerKg = 1.20,
                pickupAddress = "Battery Recycling Terminal"
            )
        )

        val offer = service.placeOffer(
            buyerId,
            listing.id,
            CreateOfferRequest(
                offeredPricePerKg = 1.10,
                totalOfferedPrice = 165.0,
                message = "Certified HAZMAT transport ready"
            )
        )

        // Seller accepts offer
        val (acceptedOffer, transaction) = service.acceptOffer(sellerId, offer.id)
        assertEquals("accepted", acceptedOffer.status)
        assertEquals("escrow", transaction.settlementStatus)
        assertEquals(165.0, transaction.agreedPrice)

        // Listing must now be reserved
        val updatedListing = service.getListingById(listing.id)
        assertNotNull(updatedListing)
        assertEquals("reserved", updatedListing.status)

        // Settle transaction upon physical handover
        val settledTransaction = service.settleTransaction(
            buyerId,
            transaction.id,
            SettleTransactionRequest(
                verifiedWeightKg = 155.0,
                notes = "Scale certified at 155 kg. Payment released."
            )
        )

        assertEquals("settled", settledTransaction.settlementStatus)
        assertEquals(155.0, settledTransaction.verifiedWeightKg)
        assertTrue(settledTransaction.ecoPointsAwarded > 0, "Eco points must be awarded upon settlement")

        // Listing must now be sold
        val soldListing = service.getListingById(listing.id)
        assertNotNull(soldListing)
        assertEquals("sold", soldListing.status)
    }

    @Test
    fun `test marketplace browse and filters`() {
        val plastics = service.browseListings(materialType = "plastic")
        assertTrue(plastics.isNotEmpty())
        plastics.forEach {
            assertEquals("plastic", it.materialType)
        }

        val allActive = service.browseListings(status = "active")
        assertTrue(allActive.isNotEmpty())

        val stats = service.getStats()
        assertTrue(stats.activeListingsCount > 0)
        assertTrue(stats.totalMaterialsAvailableKg > 0.0)
    }
}
