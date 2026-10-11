package com.ecotrack.admin.service

import com.ecotrack.admin.model.*
import com.ecotrack.admin.repository.AdminRepository
import com.ecotrack.common.ForbiddenException
import com.ecotrack.common.NotFoundException
import com.ecotrack.common.ValidationException
import java.util.UUID

class AdminService(
    private val adminRepository: AdminRepository
) {

    private val allowedRoles = setOf("CITIZEN", "TRANSPORTER", "RECYCLER", "MUNICIPAL", "ADMIN")
    private val allowedStatuses = setOf("active", "disabled", "pending")
    private val allowedDisputeTypes = setOf("pickup_dispute", "marketplace_transaction", "fraudulent_report", "weighbridge_mismatch", "other")
    private val allowedDisputeResolutions = setOf("resolved", "dismissed")

    fun verifyAdminOrMunicipal(actorId: UUID): String {
        val role = adminRepository.getUserRole(actorId)
            ?: throw ForbiddenException("User not found or unverified")
        if (role !in listOf("ADMIN", "MUNICIPAL")) {
            throw ForbiddenException("Administrative access required. Current role: $role")
        }
        return role
    }

    fun verifySuperAdmin(actorId: UUID): String {
        val role = adminRepository.getUserRole(actorId)
            ?: throw ForbiddenException("User not found or unverified")
        if (role != "ADMIN") {
            throw ForbiddenException("Super-admin privilege required. Current role: $role")
        }
        return role
    }

    fun listUsers(
        actorId: UUID,
        page: Int = 1,
        pageSize: Int = 20,
        roleFilter: String? = null,
        statusFilter: String? = null
    ): PaginatedUsersDto {
        verifyAdminOrMunicipal(actorId)

        val (items, total) = adminRepository.listUsers(
            page = page.coerceAtLeast(1),
            pageSize = pageSize.coerceIn(1, 100),
            roleFilter = roleFilter,
            statusFilter = statusFilter
        )

        return PaginatedUsersDto(
            items = items,
            total = total,
            page = page,
            pageSize = pageSize
        )
    }

    fun updateUserRole(
        actorId: UUID,
        targetUserId: UUID,
        request: UpdateUserRoleRequest,
        ipAddress: String? = null
    ): UserAdminDto {
        verifySuperAdmin(actorId)

        val newRole = request.role.uppercase().trim()
        if (newRole !in allowedRoles) {
            throw ValidationException("Invalid role '$newRole'. Allowed: ${allowedRoles.joinToString()}")
        }

        val success = adminRepository.updateUserRole(targetUserId, newRole)
        if (!success) {
            throw NotFoundException("Target user not found")
        }

        adminRepository.recordAuditLog(
            id = UUID.randomUUID(),
            actorId = actorId,
            action = "UPDATE_USER_ROLE",
            targetType = "USER",
            targetId = targetUserId.toString(),
            details = "{\"newRole\":\"$newRole\"}",
            ipAddress = ipAddress
        )

        return adminRepository.getUserById(targetUserId)
            ?: throw NotFoundException("User not found after role update")
    }

    fun updateUserStatus(
        actorId: UUID,
        targetUserId: UUID,
        request: UpdateUserStatusRequest,
        ipAddress: String? = null
    ): UserAdminDto {
        verifyAdminOrMunicipal(actorId)

        val newStatus = request.status.lowercase().trim()
        if (newStatus !in allowedStatuses) {
            throw ValidationException("Invalid status '$newStatus'. Allowed: ${allowedStatuses.joinToString()}")
        }

        val success = adminRepository.updateUserStatus(targetUserId, newStatus)
        if (!success) {
            throw NotFoundException("Target user not found")
        }

        adminRepository.recordAuditLog(
            id = UUID.randomUUID(),
            actorId = actorId,
            action = "UPDATE_USER_STATUS",
            targetType = "USER",
            targetId = targetUserId.toString(),
            details = "{\"newStatus\":\"$newStatus\"}",
            ipAddress = ipAddress
        )

        return adminRepository.getUserById(targetUserId)
            ?: throw NotFoundException("User not found after status update")
    }

    fun submitDispute(reporterId: UUID, request: CreateDisputeRequest): AdminDisputeDto {
        val type = request.disputeType.lowercase().trim()
        if (type !in allowedDisputeTypes) {
            throw ValidationException("Invalid disputeType '$type'. Allowed: ${allowedDisputeTypes.joinToString()}")
        }
        if (request.title.isBlank() || request.title.length > 255) {
            throw ValidationException("Title must be between 1 and 255 characters")
        }
        if (request.description.isBlank()) {
            throw ValidationException("Description cannot be blank")
        }

        val disputeId = UUID.randomUUID()
        return adminRepository.createDispute(
            id = disputeId,
            reporterId = reporterId,
            disputeType = type,
            referenceId = request.referenceId.trim(),
            title = request.title.trim(),
            description = request.description.trim()
        )
    }

    fun listDisputes(
        actorId: UUID,
        statusFilter: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): PaginatedDisputesDto {
        verifyAdminOrMunicipal(actorId)

        val (items, total) = adminRepository.listDisputes(
            statusFilter = statusFilter,
            page = page.coerceAtLeast(1),
            pageSize = pageSize.coerceIn(1, 100)
        )

        return PaginatedDisputesDto(
            items = items,
            total = total,
            page = page,
            pageSize = pageSize
        )
    }

    fun resolveDispute(
        actorId: UUID,
        disputeId: UUID,
        request: ResolveDisputeRequest,
        ipAddress: String? = null
    ): AdminDisputeDto {
        verifyAdminOrMunicipal(actorId)

        val status = request.status.lowercase().trim()
        if (status !in allowedDisputeResolutions) {
            throw ValidationException("Resolution status must be either 'resolved' or 'dismissed'")
        }
        if (request.resolutionNotes.isBlank()) {
            throw ValidationException("Resolution notes must be provided")
        }

        val existing = adminRepository.getDisputeById(disputeId)
            ?: throw NotFoundException("Dispute not found")

        val success = adminRepository.resolveDispute(
            id = disputeId,
            resolvedBy = actorId,
            status = status,
            notes = request.resolutionNotes.trim()
        )

        if (!success) {
            throw NotFoundException("Dispute could not be updated")
        }

        adminRepository.recordAuditLog(
            id = UUID.randomUUID(),
            actorId = actorId,
            action = "RESOLVE_DISPUTE",
            targetType = "DISPUTE",
            targetId = disputeId.toString(),
            details = "{\"outcome\":\"$status\",\"notes\":\"${request.resolutionNotes.replace("\"", "\\\"")}\"}",
            ipAddress = ipAddress
        )

        return adminRepository.getDisputeById(disputeId)
            ?: existing.copy(status = status, resolutionNotes = request.resolutionNotes)
    }

    fun getMunicipalTelemetry(actorId: UUID): MunicipalTelemetryDto {
        verifyAdminOrMunicipal(actorId)
        return adminRepository.computeMunicipalTelemetry()
    }

    fun listAuditLogs(
        actorId: UUID,
        page: Int = 1,
        pageSize: Int = 20
    ): PaginatedAuditLogsDto {
        verifySuperAdmin(actorId)

        val (items, total) = adminRepository.listAuditLogs(
            page = page.coerceAtLeast(1),
            pageSize = pageSize.coerceIn(1, 100)
        )

        return PaginatedAuditLogsDto(
            items = items,
            total = total,
            page = page,
            pageSize = pageSize
        )
    }
}
