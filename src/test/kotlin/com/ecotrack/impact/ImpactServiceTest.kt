package com.ecotrack.impact

import com.ecotrack.impact.model.CreateCertificateRequest
import com.ecotrack.impact.repository.ImpactRepository
import com.ecotrack.impact.service.ImpactService
import java.util.UUID
import kotlin.test.*

class ImpactServiceTest {

    private val repository = ImpactRepository()
    private val service = ImpactService(repository)

    @Test
    fun `test emission factors returned with citations`() {
        val factors = service.getEmissionFactors()
        assertTrue(factors.isNotEmpty())

        val plasticFactor = factors.find { it.material == "plastic" }
        assertNotNull(plasticFactor)
        assertEquals(1.50, plasticFactor.co2KgPerKg)
        assertTrue(plasticFactor.citation.contains("EPA WARM"))

        val metalFactor = factors.find { it.material == "metal" }
        assertNotNull(metalFactor)
        assertEquals(2.40, metalFactor.co2KgPerKg)
    }

    @Test
    fun `test record material diversion computes accurate co2 savings`() {
        val userId = UUID.randomUUID()
        service.recordMaterialDiversion(
            userId = userId,
            sourceType = "COLLECTION",
            sourceId = "COLL-101",
            materialType = "plastic",
            weightKg = 10.0 // 10 * 1.5 = 15 kg CO2e
        )

        val personal = service.getPersonalImpact(userId)
        assertEquals(10.0, personal.totalWeightKg)
        assertEquals(15.0, personal.totalCo2SavedKg)
        assertEquals(1, personal.totalActionsCount)

        val plasticSummary = personal.materialBreakdown.find { it.material.equals("Plastic", ignoreCase = true) }
        assertNotNull(plasticSummary)
        assertEquals(10.0, plasticSummary.weightKg)
        assertEquals(15.0, plasticSummary.co2SavedKg)
        assertEquals(100.0, plasticSummary.percentage)
    }

    @Test
    fun `test environmental equivalencies calculation`() {
        val userId = UUID.randomUUID()
        // 20 kg metal => 20 * 2.4 = 48 kg CO2e
        service.recordMaterialDiversion(userId, "COLLECTION", null, "metal", 20.0)

        val personal = service.getPersonalImpact(userId)
        val eq = personal.equivalents

        // Trees: 48 / 21.77 ~= 2.2
        assertEquals(2.2, eq.treeSeedlingsGrown10Years)
        // Car km: 48 / 0.192 = 250.0
        assertEquals(250.0, eq.passengerCarKmAvoided)
        // Phone charges: 48 / 0.008 = 6000
        assertEquals(6000L, eq.smartphoneCharges)
    }

    @Test
    fun `test generate verifiable ESG certificate with SHA-256 hash`() {
        val userId = UUID.randomUUID()
        service.recordMaterialDiversion(userId, "COLLECTION", null, "paper", 50.0)

        val req = CreateCertificateRequest(title = "Quarterly ESG Waste Certificate", periodMonths = 3)
        val cert = service.generateCertificate(userId, "Aarav Sharma", req)

        assertNotNull(cert.id)
        assertTrue(cert.certificateNumber.startsWith("ET-ESG-2026-"))
        assertEquals("Quarterly ESG Waste Certificate", cert.title)
        assertEquals("Aarav Sharma", cert.userName)
        assertEquals("VERIFIED", cert.status)
        assertEquals(64, cert.verificationHash.length) // SHA-256 hex string is 64 chars

        // Verify authenticity via verification endpoint
        val verification = service.verifyCertificate(cert.certificateNumber)
        assertTrue(verification.valid)
        assertEquals(cert.id, verification.certificate?.id)
        assertEquals(cert.verificationHash, verification.certificate?.verificationHash)
    }

    @Test
    fun `test verify invalid certificate returns false`() {
        val verification = service.verifyCertificate("INVALID-NONEXISTENT-CERT")
        assertFalse(verification.valid)
        assertNull(verification.certificate)
    }

    @Test
    fun `test community impact aggregates`() {
        val community = service.getCommunityImpact("Kolhapur Tech Campus")
        assertEquals("Kolhapur Tech Campus", community.communityName)
        assertTrue(community.totalWeightKg > 0)
        assertTrue(community.totalCo2SavedKg > 0)
        assertTrue(community.activeParticipantsCount > 0)
        assertTrue(community.treesEquivalent > 0)
    }

    @Test
    fun `test get user certificates returns generated certificates`() {
        val userId = UUID.randomUUID()
        val cert = service.generateCertificate(userId, "Priya Nair", CreateCertificateRequest("Annual ESG Certificate", 12))
        val certs = service.getUserCertificates(userId)
        assertTrue(certs.isNotEmpty())
        assertEquals(cert.id, certs.first().id)
    }
}
