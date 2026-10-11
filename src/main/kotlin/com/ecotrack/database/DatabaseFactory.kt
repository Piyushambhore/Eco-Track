package com.ecotrack.database

import at.favre.lib.crypto.bcrypt.BCrypt
import com.ecotrack.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.File
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

    lateinit var dataSource: HikariDataSource
        private set

    fun init(config: DatabaseConfig) {
        logger.info("Initializing database connection...")

        try {
            dataSource = createHikariDataSource(config)
            Database.connect(dataSource)
            runMigrations(config)
            logger.info("Database initialized successfully with PostgreSQL at ${config.url}")
        } catch (e: Exception) {
            logger.warn("Could not connect to external PostgreSQL (${e.message}). Falling back to local embedded database...")
            initH2Fallback()
        }
    }

    private fun initH2Fallback() {
        val dataDir = File("./data")
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }

        val h2Config = HikariConfig().apply {
            jdbcUrl = "jdbc:h2:file:./data/ecotrack;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"
            driverClassName = "org.h2.Driver"
            username = "sa"
            password = ""
            maximumPoolSize = 5
            minimumIdle = 1
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
        }

        dataSource = HikariDataSource(h2Config)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                AppUsers,
                ActivityRecords,
                WasteReports,
                RecyclingFacilities,
                CollectionRequests,
                MarketplaceListings,
                MarketplaceOffers,
                MarketplaceTransactions,
                RewardsLedger,
                RewardsRedemptions,
                UserBadges,
                ImpactRecords,
                AdminDisputes,
                AdminAuditLogs
            )
        }

        seedInitialData()
        logger.info("Local database initialized successfully via embedded H2 engine (PostgreSQL mode).")
    }

    private fun seedInitialData() {
        transaction {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            val demoUserId = UUID.fromString("00000000-0000-0000-0000-000000000003")
            val adminUserId = UUID.fromString("00000000-0000-0000-0000-000000000001")

            val hashedPassword = BCrypt.withDefaults().hashToString(10, "password123".toCharArray())
            val hashedAdminPassword = BCrypt.withDefaults().hashToString(10, "admin123".toCharArray())

            if (AppUsers.selectAll().where { AppUsers.email eq "user@ecotrack.dev" }.count() == 0L) {
                AppUsers.insert {
                    it[id] = demoUserId
                    it[email] = "user@ecotrack.dev"
                    it[passwordHash] = hashedPassword
                    it[displayName] = "Rahul Patil"
                    it[status] = "active"
                    it[role] = "CITIZEN"
                    it[createdAt] = now.minusDays(10)
                    it[updatedAt] = now.minusDays(10)
                }
            }

            if (AppUsers.selectAll().where { AppUsers.email eq "admin@ecotrack.org" }.count() == 0L) {
                AppUsers.insert {
                    it[id] = adminUserId
                    it[email] = "admin@ecotrack.org"
                    it[passwordHash] = hashedAdminPassword
                    it[displayName] = "System Administrator"
                    it[status] = "active"
                    it[role] = "ADMIN"
                    it[createdAt] = now.minusDays(30)
                    it[updatedAt] = now.minusDays(30)
                }
            }

            val collectorUserId = UUID.fromString("00000000-0000-0000-0000-000000000002")
            if (AppUsers.selectAll().where { AppUsers.email eq "collector@ecotrack.dev" }.count() == 0L) {
                AppUsers.insert {
                    it[id] = collectorUserId
                    it[email] = "collector@ecotrack.dev"
                    it[passwordHash] = hashedPassword
                    it[displayName] = "Vikram Scrap & Recycling"
                    it[status] = "active"
                    it[role] = "COLLECTOR"
                    it[createdAt] = now.minusDays(20)
                    it[updatedAt] = now.minusDays(20)
                }
            }

            // Seed initial facilities for Kolhapur / Maharashtra
            if (RecyclingFacilities.selectAll().count() == 0L) {
                val f1 = UUID.fromString("f1111111-1111-1111-1111-111111111111")
                val f2 = UUID.fromString("f2222222-2222-2222-2222-222222222222")
                val f3 = UUID.fromString("f3333333-3333-3333-3333-333333333333")

                RecyclingFacilities.insert {
                    it[id] = f1
                    it[name] = "GreenCycle Recycling"
                    it[facilityType] = "recycling_center"
                    it[address] = "104 MIDC Industrial Area, Shiroli"
                    it[city] = "Kolhapur"
                    it[postalCode] = "416122"
                    it[latitude] = 16.7080.toBigDecimal()
                    it[longitude] = 74.2450.toBigDecimal()
                    it[contactPhone] = "+91 98221 45678"
                    it[operatingHours] = "Mon-Sat 08:00 - 18:00"
                    it[acceptedMaterials] = "plastic,paper,metal,glass"
                    it[pricingInfo] = "Cardboard: ₹12/kg | PET Plastic: ₹18/kg | Aluminum: ₹95/kg"
                    it[rating] = 4.90.toBigDecimal()
                    it[verified] = true
                    it[createdAt] = now
                    it[updatedAt] = now
                }

                RecyclingFacilities.insert {
                    it[id] = f2
                    it[name] = "Metro Scrap Traders"
                    it[facilityType] = "scrap_dealer"
                    it[address] = "45 Foundry Compound, Shahupuri"
                    it[city] = "Kolhapur"
                    it[postalCode] = "416001"
                    it[latitude] = 16.7150.toBigDecimal()
                    it[longitude] = 74.2510.toBigDecimal()
                    it[contactPhone] = "+91 98222 56789"
                    it[operatingHours] = "Mon-Sat 08:30 - 19:00"
                    it[acceptedMaterials] = "metal,paper,plastic"
                    it[pricingInfo] = "Copper: ₹480/kg | Brass: ₹320/kg | Batteries: ₹80/kg"
                    it[rating] = 4.80.toBigDecimal()
                    it[verified] = true
                    it[createdAt] = now
                    it[updatedAt] = now
                }

                RecyclingFacilities.insert {
                    it[id] = f3
                    it[name] = "TerraNova E-Waste Safe Hub"
                    it[facilityType] = "e_waste_hub"
                    it[address] = "12 Cyber Road, Rajarampuri"
                    it[city] = "Kolhapur"
                    it[postalCode] = "416008"
                    it[latitude] = 16.6950.toBigDecimal()
                    it[longitude] = 74.2380.toBigDecimal()
                    it[contactPhone] = "+91 98223 67890"
                    it[operatingHours] = "Tue-Sun 10:00 - 18:00"
                    it[acceptedMaterials] = "e_waste,hazardous"
                    it[pricingInfo] = "Certified PCB & Lithium drop-off | Safe destruction certificate"
                    it[rating] = 4.95.toBigDecimal()
                    it[verified] = true
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }

            // Seed initial marketplace listings
            if (MarketplaceListings.selectAll().count() == 0L) {
                MarketplaceListings.insert {
                    it[id] = UUID.randomUUID()
                    it[sellerId] = demoUserId
                    it[title] = "Corrugated Cardboard Boxes"
                    it[materialType] = "paper"
                    it[subType] = "Cardboard"
                    it[quantityKg] = 8.00.toBigDecimal()
                    it[askingPricePerKg] = 14.00.toBigDecimal()
                    it[totalAskingPrice] = 112.00.toBigDecimal()
                    it[currency] = "INR"
                    it[pickupCity] = "Kolhapur"
                    it[pickupAddress] = "Shivaji Park, Kolhapur"
                    it[status] = "active"
                    it[description] = "Clean, flattened packaging boxes from recent deliveries."
                    it[createdAt] = now.minusDays(2)
                    it[updatedAt] = now.minusDays(2)
                }

                MarketplaceListings.insert {
                    it[id] = UUID.randomUUID()
                    it[sellerId] = demoUserId
                    it[title] = "Sorted PET Plastic Bottles"
                    it[materialType] = "plastic"
                    it[subType] = "PET Water Bottles"
                    it[quantityKg] = 5.00.toBigDecimal()
                    it[askingPricePerKg] = 20.00.toBigDecimal()
                    it[totalAskingPrice] = 100.00.toBigDecimal()
                    it[currency] = "INR"
                    it[pickupCity] = "Kolhapur"
                    it[pickupAddress] = "Rajarampuri, Kolhapur"
                    it[status] = "active"
                    it[description] = "Rinsed transparent PET bottles segregated for recycling."
                    it[createdAt] = now.minusDays(1)
                    it[updatedAt] = now.minusDays(1)
                }
            }

            // Seed initial collection requests in Kolhapur
            if (CollectionRequests.selectAll().count() == 0L) {
                val today = java.time.LocalDate.now()
                CollectionRequests.insert {
                    it[id] = UUID.randomUUID()
                    it[userId] = demoUserId
                    it[collectorId] = null
                    it[facilityId] = UUID.fromString("f2222222-2222-2222-2222-222222222222")
                    it[status] = "submitted"
                    it[pickupAddress] = "Flat 402, Shiv Shambhu Apts, Shahupuri 3rd Lane"
                    it[city] = "Kolhapur"
                    it[latitude] = 16.7050.toBigDecimal()
                    it[longitude] = 74.2420.toBigDecimal()
                    it[preferredDate] = today.plusDays(1)
                    it[preferredSlot] = "10:00 - 13:00"
                    it[materials] = "metal,plastic"
                    it[estimatedWeightKg] = 14.50.toBigDecimal()
                    it[specialInstructions] = "Segregated copper scrap wires and plastic crates"
                    it[createdAt] = now.minusHours(4)
                    it[updatedAt] = now.minusHours(4)
                }

                CollectionRequests.insert {
                    it[id] = UUID.randomUUID()
                    it[userId] = demoUserId
                    it[collectorId] = null
                    it[facilityId] = UUID.fromString("f1111111-1111-1111-1111-111111111111")
                    it[status] = "submitted"
                    it[pickupAddress] = "Plot 18, Near Cyber College, Rajarampuri"
                    it[city] = "Kolhapur"
                    it[latitude] = 16.6980.toBigDecimal()
                    it[longitude] = 74.2390.toBigDecimal()
                    it[preferredDate] = today.plusDays(2)
                    it[preferredSlot] = "14:00 - 17:00"
                    it[materials] = "paper,cardboard"
                    it[estimatedWeightKg] = 9.00.toBigDecimal()
                    it[specialInstructions] = "Flattened corrugated packaging boxes ready for baling"
                    it[createdAt] = now.minusHours(8)
                    it[updatedAt] = now.minusHours(8)
                }
            }

            // Seed initial waste reports for the demo user
            if (WasteReports.selectAll().where { WasteReports.userId eq demoUserId }.count() == 0L) {
                WasteReports.insert {
                    it[id] = UUID.randomUUID()
                    it[userId] = demoUserId
                    it[category] = "plastic"
                    it[subType] = "PET Plastic Water Bottle"
                    it[quantity] = 4.00.toBigDecimal()
                    it[unit] = "kg"
                    it[confidence] = 95.0.toBigDecimal()
                    it[status] = "collected"
                    it[disposalGuide] = "Rinse bottle, crush to reduce volume, place in dry recycling bin."
                    it[carbonImpact] = 6.00.toBigDecimal()
                    it[createdAt] = now.minusDays(5)
                    it[updatedAt] = now.minusDays(5)
                }

                WasteReports.insert {
                    it[id] = UUID.randomUUID()
                    it[userId] = demoUserId
                    it[category] = "paper"
                    it[subType] = "Corrugated Cardboard"
                    it[quantity] = 5.00.toBigDecimal()
                    it[unit] = "kg"
                    it[confidence] = 98.0.toBigDecimal()
                    it[status] = "collected"
                    it[disposalGuide] = "Keep dry and flatten boxes to minimize collection space."
                    it[carbonImpact] = 5.50.toBigDecimal()
                    it[createdAt] = now.minusDays(3)
                    it[updatedAt] = now.minusDays(3)
                }

                WasteReports.insert {
                    it[id] = UUID.randomUUID()
                    it[userId] = demoUserId
                    it[category] = "metal"
                    it[subType] = "Aluminum Beverage Can"
                    it[quantity] = 2.00.toBigDecimal()
                    it[unit] = "kg"
                    it[confidence] = 96.0.toBigDecimal()
                    it[status] = "collected"
                    it[disposalGuide] = "Rinse and compress aluminum cans for scrap metal collection."
                    it[carbonImpact] = 18.20.toBigDecimal()
                    it[createdAt] = now.minusDays(2)
                    it[updatedAt] = now.minusDays(2)
                }
            }

            // Seed user rewards ledger
            if (RewardsLedger.selectAll().where { RewardsLedger.userId eq demoUserId }.count() == 0L) {
                RewardsLedger.insert {
                    it[id] = UUID.randomUUID()
                    it[userId] = demoUserId
                    it[points] = 40
                    it[balanceAfter] = 40
                    it[reason] = "Plastic collection verified"
                    it[referenceId] = null
                    it[description] = "Verified 4 kg PET plastic collection"
                    it[createdAt] = now.minusDays(5)
                }

                RewardsLedger.insert {
                    it[id] = UUID.randomUUID()
                    it[userId] = demoUserId
                    it[points] = 30
                    it[balanceAfter] = 70
                    it[reason] = "Cardboard handover confirmed"
                    it[referenceId] = null
                    it[description] = "Handover of 5 kg corrugated cardboard"
                    it[createdAt] = now.minusDays(3)
                }

                RewardsLedger.insert {
                    it[id] = UUID.randomUUID()
                    it[userId] = demoUserId
                    it[points] = 50
                    it[balanceAfter] = 120
                    it[reason] = "Aluminum scrap trade settlement"
                    it[referenceId] = null
                    it[description] = "Certified metal recovery with Metro Scrap Traders"
                    it[createdAt] = now.minusDays(1)
                }
            }

            // Seed user badges
            if (UserBadges.selectAll().where { UserBadges.userId eq demoUserId }.count() == 0L) {
                UserBadges.insert {
                    it[id] = UUID.randomUUID()
                    it[userId] = demoUserId
                    it[badgeKey] = "FIRST_RECYCLING"
                    it[title] = "First verified recycling"
                    it[description] = "Completed your first verified waste collection"
                    it[iconName] = "badge_eco"
                    it[unlockedAt] = now.minusDays(5)
                }

                UserBadges.insert {
                    it[id] = UUID.randomUUID()
                    it[userId] = demoUserId
                    it[badgeKey] = "COMMUNITY_CONTRIBUTOR"
                    it[title] = "Community contributor"
                    it[description] = "Contributed to neighborhood waste diversion targets"
                    it[iconName] = "badge_community"
                    it[unlockedAt] = now.minusDays(3)
                }

                UserBadges.insert {
                    it[id] = UUID.randomUUID()
                    it[userId] = demoUserId
                    it[badgeKey] = "CONSISTENT_RECYCLER"
                    it[title] = "Consistent recycler"
                    it[description] = "Logged waste segregation activities 3 weeks in a row"
                    it[iconName] = "badge_streak"
                    it[unlockedAt] = now.minusDays(1)
                }
            }
        }
    }

    private fun createHikariDataSource(config: DatabaseConfig): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            maximumPoolSize = config.maxPoolSize
            minimumIdle = 1
            idleTimeout = 600000
            connectionTimeout = 3000
            maxLifetime = 1800000
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            driverClassName = "org.postgresql.Driver"

            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }
        return HikariDataSource(hikariConfig)
    }

    private fun runMigrations(config: DatabaseConfig) {
        logger.info("Running Flyway migrations on PostgreSQL...")
        val flyway = Flyway.configure()
            .dataSource(config.url, config.user, config.password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load()

        val result = flyway.migrate()
        logger.info("Flyway migration completed: ${result.migrationsExecuted} migrations executed")
    }

    fun isHealthy(): Boolean {
        return try {
            dataSource.connection.use { conn ->
                conn.prepareStatement("SELECT 1").use { stmt ->
                    stmt.executeQuery().use { rs ->
                        rs.next()
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Database health check failed", e)
            false
        }
    }
}
