package com.ecotrack.auth.repository

import com.ecotrack.database.AppUsers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

data class UserEntity(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val displayName: String,
    val status: String,
    val role: String = "CITIZEN",
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)

class UserRepository {

    fun findByEmail(email: String): UserEntity? = transaction {
        AppUsers.selectAll()
            .where { AppUsers.email.lowerCase() eq email.lowercase() }
            .map { it.toUserEntity() }
            .singleOrNull()
    }

    fun findById(id: UUID): UserEntity? = transaction {
        AppUsers.selectAll()
            .where { AppUsers.id eq id }
            .map { it.toUserEntity() }
            .singleOrNull()
    }

    fun create(id: UUID, email: String, passwordHash: String, displayName: String, role: String = "CITIZEN"): UserEntity = transaction {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        AppUsers.insert {
            it[AppUsers.id] = id
            it[AppUsers.email] = email.lowercase().trim()
            it[AppUsers.passwordHash] = passwordHash
            it[AppUsers.displayName] = displayName.trim()
            it[AppUsers.status] = "active"
            it[AppUsers.role] = role
            it[AppUsers.createdAt] = now
            it[AppUsers.updatedAt] = now
        }
        UserEntity(
            id = id,
            email = email.lowercase().trim(),
            passwordHash = passwordHash,
            displayName = displayName.trim(),
            status = "active",
            role = role,
            createdAt = now,
            updatedAt = now
        )
    }

    fun updateProfile(id: UUID, displayName: String?, email: String?): UserEntity? = transaction {
        val updated = AppUsers.update({ AppUsers.id eq id }) {
            if (displayName != null) it[AppUsers.displayName] = displayName.trim()
            if (email != null) it[AppUsers.email] = email.lowercase().trim()
            it[AppUsers.updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        if (updated > 0) findById(id) else null
    }

    fun countUsers(): Long = transaction {
        AppUsers.selectAll().count()
    }

    private fun ResultRow.toUserEntity() = UserEntity(
        id = this[AppUsers.id],
        email = this[AppUsers.email],
        passwordHash = this[AppUsers.passwordHash],
        displayName = this[AppUsers.displayName],
        status = this[AppUsers.status],
        role = this[AppUsers.role],
        createdAt = this[AppUsers.createdAt],
        updatedAt = this[AppUsers.updatedAt]
    )
}
