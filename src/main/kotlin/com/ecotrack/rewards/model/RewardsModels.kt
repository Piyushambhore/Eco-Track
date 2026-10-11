package com.ecotrack.rewards.model

import kotlinx.serialization.Serializable

@Serializable
data class LedgerEntryDto(
    val id: String,
    val userId: String,
    val points: Int,
    val balanceAfter: Int,
    val reason: String,
    val referenceId: String? = null,
    val description: String? = null,
    val createdAt: String
)

@Serializable
data class PaginatedLedgerDto(
    val items: List<LedgerEntryDto>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

@Serializable
data class BadgeDto(
    val key: String,
    val title: String,
    val description: String,
    val iconName: String = "badge_eco",
    val isUnlocked: Boolean = false,
    val unlockedAt: String? = null
)

@Serializable
data class RewardTierDto(
    val tierName: String, // "Sprout", "Eco Warrior", "Zero Waste Hero"
    val minPoints: Int,
    val nextTierName: String?,
    val pointsToNext: Int,
    val progressPercentage: Int
)

@Serializable
data class RewardsBalanceDto(
    val userId: String,
    val currentPoints: Int,
    val totalEarned: Int,
    val totalRedeemed: Int,
    val tier: RewardTierDto,
    val badges: List<BadgeDto>
)

@Serializable
data class RedemptionCatalogItemDto(
    val rewardType: String,
    val title: String,
    val description: String,
    val pointsRequired: Int,
    val category: String,
    val partnerName: String
)

@Serializable
data class RedemptionRequest(
    val rewardType: String
)

@Serializable
data class RedemptionDto(
    val id: String,
    val userId: String,
    val rewardType: String,
    val title: String,
    val pointsSpent: Int,
    val voucherCode: String,
    val status: String, // "active", "redeemed", "expired"
    val expiresAt: String? = null,
    val createdAt: String
)

@Serializable
data class LeaderboardEntryDto(
    val rank: Int,
    val name: String,
    val category: String, // "Campus", "Citizen", "Neighborhood"
    val kgRecovered: Double,
    val points: Int,
    val isCurrentUser: Boolean = false
)

@Serializable
data class LeaderboardDto(
    val scope: String, // "campus", "city", "all"
    val title: String,
    val entries: List<LeaderboardEntryDto>,
    val userRank: LeaderboardEntryDto? = null
)
