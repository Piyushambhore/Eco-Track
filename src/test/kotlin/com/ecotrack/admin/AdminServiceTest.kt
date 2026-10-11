package com.ecotrack.admin

import com.ecotrack.admin.model.*
import com.ecotrack.admin.repository.AdminRepository
import com.ecotrack.admin.service.AdminService
import com.ecotrack.common.ForbiddenException
import com.ecotrack.common.ValidationException
import java.util.UUID
import kotlin.test.*

class AdminServiceTest {

    private val repository = AdminRepository()
    private val service = AdminService(repository)

    private val adminId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val municipalId = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val citizenId = UUID.fromString("00000000-0000-0000-0000-000000000003")

    @Test
    fun `test citizen denied access to admin users list with 403 Forbidden`() {
        assertFailsWith<ForbiddenException> {
            service.listUsers(actorId = citizenId)
        }
    }

    @Test
    fun `test super admin can list registered platform users`() {
        val result = service.listUsers(actorId = adminId)
        assertTrue(result.items.isNotEmpty())
        assertTrue(result.total >= 3)

        val adminUser = result.items.find { it.id == adminId.toString() }
        assertNotNull(adminUser)
        assertEquals("ADMIN", adminUser.role)
    }

    @Test
    fun `test municipal officer can view telemetry but cannot alter user roles`() {
        // Municipal officer CAN view telemetry
        val telemetry = service.getMunicipalTelemetry(actorId = municipalId)
        assertNotNull(telemetry)
        assertTrue(telemetry.diversionRatePercent > 0.0)
        assertTrue(telemetry.zoneMetrics.isNotEmpty())

        // Municipal officer CANNOT promote user roles (Super-Admin only)
        assertFailsWith<ForbiddenException> {
            service.updateUserRole(
                actorId = municipalId,
                targetUserId = citizenId,
                request = UpdateUserRoleRequest(role = "RECYCLER")
            )
        }
    }

    @Test
    fun `test super admin promotes user role and audit log is automatically appended`() {
        val newUserId = UUID.randomUUID()
        repository.seedUser(newUserId, "newrecycler@ecotrack.org", "Vikram Plastics", "CITIZEN")

        val updated = service.updateUserRole(
            actorId = adminId,
            targetUserId = newUserId,
            request = UpdateUserRoleRequest(role = "RECYCLER"),
            ipAddress = "192.168.1.100"
        )

        assertEquals("RECYCLER", updated.role)

        // Verify audit log
        val logs = service.listAuditLogs(actorId = adminId)
        val roleLog = logs.items.find { it.targetId == newUserId.toString() && it.action == "UPDATE_USER_ROLE" }
        assertNotNull(roleLog)
        assertEquals("USER", roleLog.targetType)
        assertTrue(roleLog.details.contains("RECYCLER"))
    }

    @Test
    fun `test invalid role promotion is rejected with ValidationException`() {
        assertFailsWith<ValidationException> {
            service.updateUserRole(
                actorId = adminId,
                targetUserId = citizenId,
                request = UpdateUserRoleRequest(role = "SUPER_LORD_OVERLORD")
            )
        }
    }

    @Test
    fun `test user status change to disabled and audit trail verification`() {
        val targetId = UUID.randomUUID()
        repository.seedUser(targetId, "spammer@ecotrack.org", "Spam Reporter", "CITIZEN")

        val updated = service.updateUserStatus(
            actorId = adminId,
            targetUserId = targetId,
            request = UpdateUserStatusRequest(status = "disabled"),
            ipAddress = "10.0.0.1"
        )

        assertEquals("disabled", updated.status)

        val logs = service.listAuditLogs(actorId = adminId)
        val statusLog = logs.items.find { it.targetId == targetId.toString() && it.action == "UPDATE_USER_STATUS" }
        assertNotNull(statusLog)
        assertTrue(statusLog.details.contains("disabled"))
    }

    @Test
    fun `test dispute submission, listing, and resolution lifecycle`() {
        // 1. Citizen submits a pickup dispute
        val dispute = service.submitDispute(
            reporterId = citizenId,
            request = CreateDisputeRequest(
                disputeType = "pickup_dispute",
                referenceId = "ET-REQ-9901",
                title = "Driver arrived 2 hours late",
                description = "Door locked and materials left unattended in rainy conditions."
            )
        )

        assertNotNull(dispute.id)
        assertEquals("open", dispute.status)
        assertEquals("pickup_dispute", dispute.disputeType)

        // 2. Municipal officer lists open disputes
        val openDisputes = service.listDisputes(actorId = municipalId, statusFilter = "open")
        assertTrue(openDisputes.items.any { it.id == dispute.id })

        // 3. Municipal officer resolves dispute
        val resolved = service.resolveDispute(
            actorId = municipalId,
            disputeId = UUID.fromString(dispute.id),
            request = ResolveDisputeRequest(
                status = "resolved",
                resolutionNotes = "Dispatched re-routing pickup van and credited 50 eco-points goodwill rebate."
            )
        )

        assertEquals("resolved", resolved.status)
        assertNotNull(resolved.resolutionNotes)
        assertEquals(municipalId.toString(), resolved.resolvedBy)

        // 4. Verify audit log entry
        val logs = service.listAuditLogs(actorId = adminId)
        val disputeLog = logs.items.find { it.targetId == dispute.id && it.action == "RESOLVE_DISPUTE" }
        assertNotNull(disputeLog)
        assertTrue(disputeLog.details.contains("resolved"))
    }

    @Test
    fun `test municipal telemetry calculates diversion rate and ward compliance`() {
        val telemetry = service.getMunicipalTelemetry(actorId = municipalId)

        assertTrue(telemetry.totalWasteReportedKg > 0.0)
        assertTrue(telemetry.totalRecycledKg > 0.0)
        assertTrue(telemetry.diversionRatePercent in 0.0..100.0)
        assertTrue(telemetry.collectionEfficiencyPercent in 0.0..100.0)

        assertEquals(3, telemetry.zoneMetrics.size)
        assertTrue(telemetry.zoneMetrics.all { it.efficiencyPercent > 80.0 })

        assertTrue(telemetry.topWards.isNotEmpty())
        val ward12 = telemetry.topWards.find { it.wardNumber == 12 }
        assertNotNull(ward12)
        assertEquals("Tech Corridor Ward", ward12.wardName)
        assertEquals(96.0, ward12.complianceRatePercent)
    }
}
