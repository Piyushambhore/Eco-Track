package com.ecotrack.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object AppUsers : Table("app_users") {
    val id = uuid("id")
    val email = text("email")
    val passwordHash = text("password_hash")
    val displayName = text("display_name")
    val status = text("status").default("active")
    val role = text("role").default("CITIZEN")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object ActivityRecords : Table("activity_records") {
    val id = uuid("id")
    val ownerUserId = uuid("owner_user_id").references(AppUsers.id)
    val recordType = text("record_type")
    val title = text("title")
    val quantity = decimal("quantity", 12, 3).nullable()
    val unit = text("unit").nullable()
    val carbonImpact = decimal("carbon_impact", 10, 2).nullable()
    val occurredAt = timestampWithTimeZone("occurred_at")
    val notes = text("notes").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object WasteReports : Table("waste_reports") {
    val id = uuid("id")
    val userId = uuid("user_id").references(AppUsers.id)
    val category = text("category")
    val subType = text("sub_type").nullable()
    val photoUrl = text("photo_url").nullable()
    val latitude = decimal("latitude", 9, 6).nullable()
    val longitude = decimal("longitude", 9, 6).nullable()
    val quantity = decimal("quantity", 12, 3).nullable()
    val unit = text("unit").nullable()
    val confidence = decimal("confidence", 5, 2).nullable()
    val status = text("status").default("identified")
    val disposalGuide = text("disposal_guide").nullable()
    val carbonImpact = decimal("carbon_impact", 10, 2).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object RecyclingFacilities : Table("recycling_facilities") {
    val id = uuid("id")
    val name = text("name")
    val facilityType = text("facility_type")
    val address = text("address")
    val city = text("city")
    val postalCode = text("postal_code").nullable()
    val latitude = decimal("latitude", 9, 6)
    val longitude = decimal("longitude", 9, 6)
    val contactPhone = text("contact_phone").nullable()
    val operatingHours = text("operating_hours").nullable()
    val acceptedMaterials = text("accepted_materials")
    val pricingInfo = text("pricing_info").nullable()
    val rating = decimal("rating", 3, 2).nullable()
    val verified = bool("verified").default(true)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object CollectionRequests : Table("collection_requests") {
    val id = uuid("id")
    val userId = uuid("user_id").references(AppUsers.id)
    val collectorId = uuid("collector_id").references(AppUsers.id).nullable()
    val facilityId = uuid("facility_id").references(RecyclingFacilities.id).nullable()
    val status = text("status").default("submitted")
    val pickupAddress = text("pickup_address")
    val city = text("city").default("Metropolis")
    val latitude = decimal("latitude", 9, 6).nullable()
    val longitude = decimal("longitude", 9, 6).nullable()
    val preferredDate = date("preferred_date")
    val preferredSlot = text("preferred_slot").default("09:00 - 12:00")
    val materials = text("materials")
    val estimatedWeightKg = decimal("estimated_weight_kg", 10, 2).nullable()
    val actualWeightKg = decimal("actual_weight_kg", 10, 2).nullable()
    val specialInstructions = text("special_instructions").nullable()
    val proofPhotoUrl = text("proof_photo_url").nullable()
    val ecoPointsEarned = integer("eco_points_earned").default(0)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object MarketplaceListings : Table("marketplace_listings") {
    val id = uuid("id")
    val sellerId = uuid("seller_id").references(AppUsers.id)
    val title = text("title")
    val materialType = text("material_type")
    val subType = text("sub_type").nullable()
    val quantityKg = decimal("quantity_kg", 10, 2)
    val askingPricePerKg = decimal("asking_price_per_kg", 10, 2).nullable()
    val totalAskingPrice = decimal("total_asking_price", 10, 2)
    val currency = text("currency").default("USD")
    val pickupCity = text("pickup_city").default("Metropolis")
    val pickupAddress = text("pickup_address")
    val latitude = decimal("latitude", 9, 6).nullable()
    val longitude = decimal("longitude", 9, 6).nullable()
    val photoUrl = text("photo_url").nullable()
    val description = text("description").nullable()
    val status = text("status").default("active")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object MarketplaceOffers : Table("marketplace_offers") {
    val id = uuid("id")
    val listingId = uuid("listing_id").references(MarketplaceListings.id)
    val buyerId = uuid("buyer_id").references(AppUsers.id)
    val offeredPricePerKg = decimal("offered_price_per_kg", 10, 2).nullable()
    val totalOfferedPrice = decimal("total_offered_price", 10, 2)
    val message = text("message").nullable()
    val status = text("status").default("pending")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object MarketplaceTransactions : Table("marketplace_transactions") {
    val id = uuid("id")
    val listingId = uuid("listing_id").references(MarketplaceListings.id)
    val offerId = uuid("offer_id").references(MarketplaceOffers.id)
    val sellerId = uuid("seller_id").references(AppUsers.id)
    val buyerId = uuid("buyer_id").references(AppUsers.id)
    val agreedPrice = decimal("agreed_price", 10, 2)
    val verifiedWeightKg = decimal("verified_weight_kg", 10, 2).nullable()
    val settlementStatus = text("settlement_status").default("escrow")
    val ecoPointsAwarded = integer("eco_points_awarded").default(0)
    val createdAt = timestampWithTimeZone("created_at")
    val settledAt = timestampWithTimeZone("settled_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object RewardsLedger : Table("rewards_ledger") {
    val id = uuid("id")
    val userId = uuid("user_id").references(AppUsers.id)
    val points = integer("points")
    val balanceAfter = integer("balance_after")
    val reason = text("reason")
    val referenceId = text("reference_id").nullable()
    val description = text("description").nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

object RewardsRedemptions : Table("rewards_redemptions") {
    val id = uuid("id")
    val userId = uuid("user_id").references(AppUsers.id)
    val rewardType = text("reward_type")
    val pointsSpent = integer("points_spent")
    val voucherCode = text("voucher_code")
    val status = text("status").default("active")
    val expiresAt = timestampWithTimeZone("expires_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

object UserBadges : Table("user_badges") {
    val id = uuid("id")
    val userId = uuid("user_id").references(AppUsers.id)
    val badgeKey = text("badge_key")
    val title = text("title")
    val description = text("description").nullable()
    val iconName = text("icon_name").default("badge_eco")
    val unlockedAt = timestampWithTimeZone("unlocked_at")

    override val primaryKey = PrimaryKey(id)
}

object ImpactRecords : Table("impact_records") {
    val id = uuid("id")
    val userId = uuid("user_id").references(AppUsers.id)
    val sourceType = text("source_type")
    val sourceId = text("source_id").nullable()
    val materialType = text("material_type")
    val weightKg = decimal("weight_kg", 10, 2)
    val co2SavedKg = decimal("co2_saved_kg", 10, 3)
    val waterSavedLiters = decimal("water_saved_liters", 10, 2).default(java.math.BigDecimal.ZERO)
    val energySavedKwh = decimal("energy_saved_kwh", 10, 2).default(java.math.BigDecimal.ZERO)
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

object ImpactCertificates : Table("impact_certificates") {
    val id = uuid("id")
    val certificateNumber = text("certificate_number")
    val userId = uuid("user_id").references(AppUsers.id)
    val title = text("title")
    val periodStart = timestampWithTimeZone("period_start")
    val periodEnd = timestampWithTimeZone("period_end")
    val totalWeightKg = decimal("total_weight_kg", 10, 2)
    val totalCo2Kg = decimal("total_co2_kg", 10, 3)
    val treesEquivalent = decimal("trees_equivalent", 10, 2).default(java.math.BigDecimal.ZERO)
    val waterSavedLiters = decimal("water_saved_liters", 10, 2).default(java.math.BigDecimal.ZERO)
    val energySavedKwh = decimal("energy_saved_kwh", 10, 2).default(java.math.BigDecimal.ZERO)
    val verificationHash = text("verification_hash")
    val status = text("status").default("VERIFIED")
    val issuedAt = timestampWithTimeZone("issued_at")

    override val primaryKey = PrimaryKey(id)
}

object AdminDisputes : Table("admin_disputes") {
    val id = uuid("id")
    val reporterId = uuid("reporter_id").references(AppUsers.id)
    val disputeType = text("dispute_type")
    val referenceId = text("reference_id")
    val title = text("title")
    val description = text("description")
    val status = text("status").default("open")
    val resolutionNotes = text("resolution_notes").nullable()
    val resolvedBy = uuid("resolved_by").references(AppUsers.id).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val resolvedAt = timestampWithTimeZone("resolved_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object AdminAuditLogs : Table("admin_audit_logs") {
    val id = uuid("id")
    val actorId = uuid("actor_id").references(AppUsers.id)
    val action = text("action")
    val targetType = text("target_type")
    val targetId = text("target_id")
    val details = text("details").default("{}")
    val ipAddress = text("ip_address").nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}




