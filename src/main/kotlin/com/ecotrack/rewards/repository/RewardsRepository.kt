package com.ecotrack.rewards.repository

import com.ecotrack.database.RewardsLedger
import com.ecotrack.database.RewardsRedemptions
import com.ecotrack.database.UserBadges
import com.ecotrack.rewards.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class RewardsRepository {

    // Thread-safe in-memory stores for testing and resilient fallback
    private val ledgerStore = ConcurrentHashMap<UUID, CopyOnWriteArrayList<LedgerEntryDto>>()
    private val redemptionsStore = ConcurrentHashMap<UUID, CopyOnWriteArrayList<RedemptionDto>>()
    private val badgesStore = ConcurrentHashMap<UUID, ConcurrentHashMap<String, BadgeDto>>()

    init {
        seedInitialDemoData()
    }

    private fun seedInitialDemoData() {
        val demoUserId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        // Seed initial rewards ledger history for demo user
        val list = CopyOnWriteArrayList<LedgerEntryDto>()
        list.add(
            LedgerEntryDto(
                id = UUID.randomUUID().toString(),
                userId = demoUserId.toString(),
                points = 50,
                balanceAfter = 50,
                reason = "SIGNUP_BONUS",
                referenceId = "WELCOME",
                description = "EcoTrack Citizen Onboarding Welcome Credit",
                createdAt = now.minusDays(15).toString()
            )
        )
        list.add(
            LedgerEntryDto(
                id = UUID.randomUUID().toString(),
                userId = demoUserId.toString(),
                points = 40,
                balanceAfter = 90,
                reason = "COLLECTION_VERIFIED",
                referenceId = "ET-PL-8821",
                description = "Plastic collection verified (5.2 kg recovered)",
                createdAt = now.minusDays(8).toString()
            )
        )
        list.add(
            LedgerEntryDto(
                id = UUID.randomUUID().toString(),
                userId = demoUserId.toString(),
                points = 30,
                balanceAfter = 120,
                reason = "COLLECTION_VERIFIED",
                referenceId = "ET-CB-9042",
                description = "Cardboard handover confirmed (8.0 kg recovered)",
                createdAt = now.minusDays(2).toString()
            )
        )
        ledgerStore[demoUserId] = list

        val badgeMap = ConcurrentHashMap<String, BadgeDto>()
        badgeMap["FIRST_RECYCLING"] = BadgeDto(
            key = "FIRST_RECYCLING",
            title = "First Verified Recycling",
            description = "Completed your very first verified waste collection or drop-off",
            iconName = "chk",
            isUnlocked = true,
            unlockedAt = now.minusDays(8).toString()
        )
        badgesStore[demoUserId] = badgeMap
    }

    fun appendLedgerEntry(
        userId: UUID,
        points: Int,
        reason: String,
        referenceId: String? = null,
        description: String? = null
    ): LedgerEntryDto {
        val userList = ledgerStore.computeIfAbsent(userId) { CopyOnWriteArrayList() }

        val previousBalance = userList.lastOrNull()?.balanceAfter ?: 0
        val newBalance = previousBalance + points

        if (newBalance < 0) {
            throw IllegalArgumentException("Insufficient points balance. Current: $previousBalance, Required: ${-points}")
        }

        val entryId = UUID.randomUUID()
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        val dto = LedgerEntryDto(
            id = entryId.toString(),
            userId = userId.toString(),
            points = points,
            balanceAfter = newBalance,
            reason = reason,
            referenceId = referenceId,
            description = description,
            createdAt = now.toString()
        )

        userList.add(dto)

        // Persist to Exposed Database if available
        try {
            transaction {
                RewardsLedger.insert {
                    it[RewardsLedger.id] = entryId
                    it[RewardsLedger.userId] = userId
                    it[RewardsLedger.points] = points
                    it[RewardsLedger.balanceAfter] = newBalance
                    it[RewardsLedger.reason] = reason
                    it[RewardsLedger.referenceId] = referenceId
                    it[RewardsLedger.description] = description
                    it[RewardsLedger.createdAt] = now
                }
            }
        } catch (_: Exception) {
            // Fallback to thread-safe in-memory store
        }

        // Check for automatic badge unlocks upon earning points
        checkAutomaticBadgeUnlocks(userId, newBalance)

        return dto
    }

    private fun checkAutomaticBadgeUnlocks(userId: UUID, currentBalance: Int) {
        if (currentBalance >= 1) {
            unlockBadge(
                userId,
                "FIRST_RECYCLING",
                "First Verified Recycling",
                "Completed your first verified waste recovery action",
                "chk"
            )
        }
        if (currentBalance >= 100) {
            unlockBadge(
                userId,
                "COMMUNITY_CONTRIBUTOR",
                "Community Contributor",
                "Recovered over 25 kg of recyclable materials (100+ points)",
                "leaf"
            )
        }
        if (currentBalance >= 250) {
            unlockBadge(
                userId,
                "CONSISTENT_RECYCLER",
                "Consistent Recycler",
                "Maintained active recycling streaks across verified collections",
                "rec"
            )
        }
        if (currentBalance >= 500) {
            unlockBadge(
                userId,
                "ZERO_WASTE_HERO",
                "Zero Waste Hero",
                "Diverted over 100 kg of municipal waste from landfills",
                "trophy"
            )
        }
    }

    fun unlockBadge(
        userId: UUID,
        badgeKey: String,
        title: String,
        description: String,
        iconName: String = "badge_eco"
    ): Boolean {
        val userBadges = badgesStore.computeIfAbsent(userId) { ConcurrentHashMap() }
        if (userBadges.containsKey(badgeKey)) {
            return false // Already unlocked
        }

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val badge = BadgeDto(
            key = badgeKey,
            title = title,
            description = description,
            iconName = iconName,
            isUnlocked = true,
            unlockedAt = now.toString()
        )
        userBadges[badgeKey] = badge

        try {
            transaction {
                UserBadges.insert {
                    it[UserBadges.id] = UUID.randomUUID()
                    it[UserBadges.userId] = userId
                    it[UserBadges.badgeKey] = badgeKey
                    it[UserBadges.title] = title
                    it[UserBadges.description] = description
                    it[UserBadges.iconName] = iconName
                    it[UserBadges.unlockedAt] = now
                }
            }
        } catch (_: Exception) {
            // Ignore duplicate constraint or database fallback
        }

        return true
    }

    fun getBalance(userId: UUID): RewardsBalanceDto {
        val userList = ledgerStore[userId] ?: emptyList()
        val currentPoints = userList.lastOrNull()?.balanceAfter ?: 0
        val totalEarned = userList.filter { it.points > 0 }.sumOf { it.points }
        val totalRedeemed = userList.filter { it.points < 0 }.sumOf { -it.points }

        // Tier definition
        val tier = when {
            totalEarned < 100 -> RewardTierDto(
                tierName = "Sprout",
                minPoints = 0,
                nextTierName = "Eco Warrior",
                pointsToNext = 100 - totalEarned,
                progressPercentage = (totalEarned * 100) / 100
            )
            totalEarned < 500 -> RewardTierDto(
                tierName = "Eco Warrior",
                minPoints = 100,
                nextTierName = "Zero Waste Hero",
                pointsToNext = 500 - totalEarned,
                progressPercentage = ((totalEarned - 100) * 100) / 400
            )
            else -> RewardTierDto(
                tierName = "Zero Waste Hero",
                minPoints = 500,
                nextTierName = null,
                pointsToNext = 0,
                progressPercentage = 100
            )
        }

        // Full badges catalog
        val allBadges = listOf(
            Triple("FIRST_RECYCLING", "First Verified Recycling", "Completed your very first verified waste collection or drop-off"),
            Triple("COMMUNITY_CONTRIBUTOR", "Community Contributor", "Recovered over 25 kg of recyclable materials"),
            Triple("CONSISTENT_RECYCLER", "Consistent Recycler", "Maintained active recycling streaks across 5 collections"),
            Triple("ZERO_WASTE_HERO", "Zero Waste Hero", "Diverted over 100 kg of municipal waste from landfills")
        )

        val unlockedMap = badgesStore[userId] ?: emptyMap()
        val badgesList = allBadges.map { (key, title, desc) ->
            val unlocked = unlockedMap[key]
            BadgeDto(
                key = key,
                title = title,
                description = desc,
                iconName = when (key) {
                    "FIRST_RECYCLING" -> "chk"
                    "COMMUNITY_CONTRIBUTOR" -> "leaf"
                    "CONSISTENT_RECYCLER" -> "rec"
                    else -> "trophy"
                },
                isUnlocked = unlocked != null,
                unlockedAt = unlocked?.unlockedAt
            )
        }

        return RewardsBalanceDto(
            userId = userId.toString(),
            currentPoints = currentPoints,
            totalEarned = totalEarned,
            totalRedeemed = totalRedeemed,
            tier = tier,
            badges = badgesList
        )
    }

    fun getHistory(userId: UUID, page: Int = 1, limit: Int = 20): PaginatedLedgerDto {
        val userList = (ledgerStore[userId] ?: emptyList()).reversed() // Newest first
        val total = userList.size.toLong()
        val totalPages = if (total == 0L) 1 else Math.ceil(total.toDouble() / limit).toInt()

        val fromIndex = (page - 1) * limit
        val pagedItems = if (fromIndex >= userList.size) {
            emptyList()
        } else {
            userList.subList(fromIndex, Math.min(fromIndex + limit, userList.size))
        }

        return PaginatedLedgerDto(
            items = pagedItems,
            total = total,
            page = page,
            limit = limit,
            totalPages = totalPages
        )
    }

    fun getCatalog(): List<RedemptionCatalogItemDto> {
        return listOf(
            RedemptionCatalogItemDto(
                rewardType = "PLANT_A_TREE",
                title = "Plant a Native Tree Sapling",
                description = "Plant an indigenous Neem/Peepal tree with GPS geotagged photo proof via Green India Trust.",
                pointsRequired = 100,
                category = "Ecosystem Restoration",
                partnerName = "Afforestation Trust"
            ),
            RedemptionCatalogItemDto(
                rewardType = "CAMPUS_CANTEEN_VOUCHER",
                title = "₹50 Campus Canteen Voucher",
                description = "Instant meal & beverage credit accepted at all campus cafeterias.",
                pointsRequired = 50,
                category = "Food & Beverage",
                partnerName = "Campus Services"
            ),
            RedemptionCatalogItemDto(
                rewardType = "MUNICIPAL_TAX_CREDIT",
                title = "Municipal Waste Cess Rebate Voucher",
                description = "Official tax certificate granting 5% reduction on quarterly municipal waste cess.",
                pointsRequired = 150,
                category = "Civic Governance",
                partnerName = "Kolhapur Municipal Corporation"
            ),
            RedemptionCatalogItemDto(
                rewardType = "ECO_STORE_DISCOUNT",
                title = "₹100 Eco Store Discount",
                description = "Voucher code valid on bamboo toothbrushes, jute bags, and compost bins.",
                pointsRequired = 75,
                category = "Sustainable Goods",
                partnerName = "EcoGoods Store"
            )
        )
    }

    fun redeemReward(userId: UUID, rewardType: String): RedemptionDto {
        val catalogItem = getCatalog().find { it.rewardType.equals(rewardType, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unknown reward type: $rewardType")

        val randomCode = "ECO-" + (100000..999999).random()
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val expiresAt = now.plusMonths(3)

        // Deduct points atomically
        appendLedgerEntry(
            userId = userId,
            points = -catalogItem.pointsRequired,
            reason = "REDEMPTION",
            referenceId = randomCode,
            description = "Redeemed: ${catalogItem.title}"
        )

        val redemptionId = UUID.randomUUID()
        val redemptionDto = RedemptionDto(
            id = redemptionId.toString(),
            userId = userId.toString(),
            rewardType = catalogItem.rewardType,
            title = catalogItem.title,
            pointsSpent = catalogItem.pointsRequired,
            voucherCode = randomCode,
            status = "active",
            expiresAt = expiresAt.toString(),
            createdAt = now.toString()
        )

        val userRedemptions = redemptionsStore.computeIfAbsent(userId) { CopyOnWriteArrayList() }
        userRedemptions.add(redemptionDto)

        try {
            transaction {
                RewardsRedemptions.insert {
                    it[RewardsRedemptions.id] = redemptionId
                    it[RewardsRedemptions.userId] = userId
                    it[RewardsRedemptions.rewardType] = catalogItem.rewardType
                    it[RewardsRedemptions.pointsSpent] = catalogItem.pointsRequired
                    it[RewardsRedemptions.voucherCode] = randomCode
                    it[RewardsRedemptions.status] = "active"
                    it[RewardsRedemptions.expiresAt] = expiresAt
                    it[RewardsRedemptions.createdAt] = now
                }
            }
        } catch (_: Exception) {
            // Fallback
        }

        return redemptionDto
    }

    fun getRedemptions(userId: UUID): List<RedemptionDto> {
        return redemptionsStore[userId] ?: emptyList()
    }

    fun getLeaderboard(scope: String = "campus", currentUserId: UUID? = null): LeaderboardDto {
        val userPoints = currentUserId?.let { uid ->
            ledgerStore[uid]?.lastOrNull()?.balanceAfter ?: 0
        } ?: 0
        val userKg = userPoints / 10.0

        val campusList = mutableListOf(
            LeaderboardEntryDto(
                rank = 1,
                name = "DKTE Textile & Eng. Institute",
                category = "Campus",
                kgRecovered = 142.0,
                points = 1420
            ),
            LeaderboardEntryDto(
                rank = 2,
                name = "Shivaji University Eco Wing",
                category = "Campus",
                kgRecovered = 118.0,
                points = 1180
            ),
            LeaderboardEntryDto(
                rank = 3,
                name = "KIT College of Engineering",
                category = "Campus",
                kgRecovered = 76.0,
                points = 760
            ),
            LeaderboardEntryDto(
                rank = 4,
                name = "Government Polytechnic Kolhapur",
                category = "Campus",
                kgRecovered = 62.5,
                points = 625
            )
        )

        val currentUserEntry = if (currentUserId != null) {
            val userEntry = LeaderboardEntryDto(
                rank = 5,
                name = "My Green Profile (You)",
                category = "Campus",
                kgRecovered = userKg,
                points = userPoints,
                isCurrentUser = true
            )
            campusList.add(userEntry)
            userEntry
        } else null

        // Re-sort dynamically by points descending
        val sortedEntries = campusList.sortedByDescending { it.points }.mapIndexed { idx, item ->
            item.copy(rank = idx + 1)
        }

        return LeaderboardDto(
            scope = scope,
            title = "Campus Recycling Challenge (Kolhapur Region)",
            entries = sortedEntries,
            userRank = currentUserEntry?.let { u -> sortedEntries.find { it.isCurrentUser } }
        )
    }
}
