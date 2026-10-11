package com.ecotrack.rewards

import com.ecotrack.rewards.repository.RewardsRepository
import com.ecotrack.rewards.service.RewardsService
import java.util.UUID
import kotlin.test.*

class RewardsServiceTest {

    private val repository = RewardsRepository()
    private val service = RewardsService(repository)

    @Test
    fun `test award collection points calculates correct points and updates balance`() {
        val userId = UUID.randomUUID()
        val entry = service.awardCollectionPoints(userId, "ET-REQ-101", 12.5)

        assertEquals(userId.toString(), entry.userId)
        assertEquals(125, entry.points) // 12.5 * 10 = 125
        assertEquals(125, entry.balanceAfter)
        assertEquals("COLLECTION_VERIFIED", entry.reason)
        assertEquals("ET-REQ-101", entry.referenceId)

        val balance = service.getBalance(userId)
        assertEquals(125, balance.currentPoints)
        assertEquals(125, balance.totalEarned)
        assertEquals(0, balance.totalRedeemed)
        assertEquals("Eco Warrior", balance.tier.tierName)
    }

    @Test
    fun `test tier progression from Sprout to Eco Warrior and Zero Waste Hero`() {
        val userId = UUID.randomUUID()

        // 1. Sprout (0 to 99 points)
        service.creditPoints(userId, 40, "DAILY_STREAK")
        var balance = service.getBalance(userId)
        assertEquals("Sprout", balance.tier.tierName)
        assertEquals(40, balance.currentPoints)
        assertEquals(60, balance.tier.pointsToNext)

        // 2. Eco Warrior (100 to 499 points)
        service.creditPoints(userId, 80, "COLLECTION_VERIFIED")
        balance = service.getBalance(userId)
        assertEquals("Eco Warrior", balance.tier.tierName)
        assertEquals(120, balance.currentPoints)
        assertEquals(380, balance.tier.pointsToNext)

        // 3. Zero Waste Hero (500+ points)
        service.creditPoints(userId, 400, "COMMUNITY_CHALLENGE")
        balance = service.getBalance(userId)
        assertEquals("Zero Waste Hero", balance.tier.tierName)
        assertEquals(520, balance.currentPoints)
        assertNull(balance.tier.nextTierName)
        assertEquals(100, balance.tier.progressPercentage)
    }

    @Test
    fun `test automatic badge unlocks at recycling milestones`() {
        val userId = UUID.randomUUID()

        service.creditPoints(userId, 10, "COLLECTION_VERIFIED")
        var balance = service.getBalance(userId)
        val firstBadge = balance.badges.find { it.key == "FIRST_RECYCLING" }
        assertNotNull(firstBadge)
        assertTrue(firstBadge.isUnlocked)

        val contributorBadgeBefore = balance.badges.find { it.key == "COMMUNITY_CONTRIBUTOR" }
        assertNotNull(contributorBadgeBefore)
        assertFalse(contributorBadgeBefore.isUnlocked)

        // Reach 100 points
        service.creditPoints(userId, 100, "COLLECTION_VERIFIED")
        balance = service.getBalance(userId)
        val contributorBadgeAfter = balance.badges.find { it.key == "COMMUNITY_CONTRIBUTOR" }
        assertNotNull(contributorBadgeAfter)
        assertTrue(contributorBadgeAfter.isUnlocked)
    }

    @Test
    fun `test redemption succeeds when user has sufficient points`() {
        val userId = UUID.randomUUID()
        service.creditPoints(userId, 150, "COLLECTION_VERIFIED")

        val redemption = service.redeem(userId, "CAMPUS_CANTEEN_VOUCHER")

        assertEquals(userId.toString(), redemption.userId)
        assertEquals("CAMPUS_CANTEEN_VOUCHER", redemption.rewardType)
        assertEquals(50, redemption.pointsSpent)
        assertTrue(redemption.voucherCode.startsWith("ECO-"))
        assertEquals("active", redemption.status)

        val balance = service.getBalance(userId)
        assertEquals(100, balance.currentPoints)
        assertEquals(150, balance.totalEarned)
        assertEquals(50, balance.totalRedeemed)
    }

    @Test
    fun `test redemption fails when user has insufficient points`() {
        val userId = UUID.randomUUID()
        service.creditPoints(userId, 30, "COLLECTION_VERIFIED")

        assertFailsWith<IllegalArgumentException> {
            service.redeem(userId, "PLANT_A_TREE") // Costs 100 points
        }

        // Verify balance was not decremented
        val balance = service.getBalance(userId)
        assertEquals(30, balance.currentPoints)
    }

    @Test
    fun `test paginated history order is newest first`() {
        val userId = UUID.randomUUID()
        service.creditPoints(userId, 20, "SCAN_1")
        service.creditPoints(userId, 40, "SCAN_2")
        service.creditPoints(userId, 60, "SCAN_3")

        val history = service.getHistory(userId, page = 1, limit = 2)

        assertEquals(3, history.total)
        assertEquals(2, history.items.size)
        assertEquals(2, history.totalPages)
        assertEquals(120, history.items[0].balanceAfter) // Most recent first
        assertEquals(60, history.items[0].points)
        assertEquals(60, history.items[1].balanceAfter)
    }

    @Test
    fun `test leaderboard returns ranked entries`() {
        val leaderboard = service.getLeaderboard(scope = "campus")

        assertEquals("Campus Recycling Challenge (Kolhapur Region)", leaderboard.title)
        assertTrue(leaderboard.entries.isNotEmpty())
        assertEquals(1, leaderboard.entries[0].rank)
        assertTrue(leaderboard.entries[0].kgRecovered >= leaderboard.entries[1].kgRecovered)
    }
}
