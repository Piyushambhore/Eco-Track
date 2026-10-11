package com.ecotrack.rewards.service

import com.ecotrack.rewards.model.*
import com.ecotrack.rewards.repository.RewardsRepository
import org.slf4j.LoggerFactory
import java.util.UUID

class RewardsService(
    private val repository: RewardsRepository = RewardsRepository()
) {
    private val logger = LoggerFactory.getLogger(RewardsService::class.java)

    /**
     * Atomically credits points for verified waste collection.
     * Standard formula: 10 points per verified kg (minimum 10 points).
     */
    fun awardCollectionPoints(userId: UUID, collectionId: String, weightKg: Double): LedgerEntryDto {
        val earnedPoints = Math.max(10, Math.round(weightKg * 10).toInt())
        logger.info("Awarding $earnedPoints points to user $userId for collection $collectionId (weight: $weightKg kg)")
        return repository.appendLedgerEntry(
            userId = userId,
            points = earnedPoints,
            reason = "COLLECTION_VERIFIED",
            referenceId = collectionId,
            description = "Verified doorstep collection pickup ($weightKg kg)"
        )
    }

    /**
     * Credits points for verified marketplace sale completion.
     */
    fun awardMarketplaceSalePoints(userId: UUID, transactionId: String, weightKg: Double): LedgerEntryDto {
        val earnedPoints = Math.max(15, Math.round(weightKg * 8).toInt())
        logger.info("Awarding $earnedPoints points to seller $userId for marketplace transaction $transactionId")
        return repository.appendLedgerEntry(
            userId = userId,
            points = earnedPoints,
            reason = "MARKETPLACE_SALE",
            referenceId = transactionId,
            description = "Circular scrap marketplace sale settled ($weightKg kg)"
        )
    }

    fun creditPoints(
        userId: UUID,
        points: Int,
        reason: String,
        referenceId: String? = null,
        description: String? = null
    ): LedgerEntryDto {
        require(points > 0) { "Credit points must be positive" }
        return repository.appendLedgerEntry(userId, points, reason, referenceId, description)
    }

    fun getBalance(userId: UUID): RewardsBalanceDto {
        return repository.getBalance(userId)
    }

    fun getHistory(userId: UUID, page: Int = 1, limit: Int = 20): PaginatedLedgerDto {
        return repository.getHistory(userId, page, limit)
    }

    fun getCatalog(): List<RedemptionCatalogItemDto> {
        return repository.getCatalog()
    }

    fun redeem(userId: UUID, rewardType: String): RedemptionDto {
        logger.info("User $userId requesting redemption for reward $rewardType")
        return repository.redeemReward(userId, rewardType)
    }

    fun getLeaderboard(scope: String = "campus", userId: UUID? = null): LeaderboardDto {
        return repository.getLeaderboard(scope, userId)
    }
}
