package com.ecotrack.waste

import com.ecotrack.waste.api.IdentifyWasteRequest
import com.ecotrack.waste.service.WasteClassifierService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WasteClassifierServiceTest {

    private val classifier = WasteClassifierService()

    @Test
    fun `test all 7 supported categories are populated`() {
        val categories = classifier.getAllCategories()
        assertEquals(7, categories.size)
        val categoryKeys = categories.map { it.category }
        assertTrue(categoryKeys.containsAll(listOf("plastic", "paper", "metal", "e_waste", "glass", "organic", "hazardous")))
    }

    @Test
    fun `test plastic bottle classification`() {
        val request = IdentifyWasteRequest(description = "Clear plastic soda bottle with cap")
        val result = classifier.classify(request)
        assertEquals("plastic", result.category)
        assertTrue(result.recyclable)
        assertEquals(1.50, result.carbonFactorKg)
        assertTrue(result.confidence > 90.0)
    }

    @Test
    fun `test aluminum beverage can classification`() {
        val request = IdentifyWasteRequest(description = "Crushed aluminum soda can")
        val result = classifier.classify(request)
        assertEquals("metal", result.category)
        assertTrue(result.recyclable)
        assertEquals(9.00, result.carbonFactorKg)
        assertTrue(result.confidence > 90.0)
    }

    @Test
    fun `test cardboard box packaging classification`() {
        val request = IdentifyWasteRequest(description = "Amazon corrugated cardboard delivery box")
        val result = classifier.classify(request)
        assertEquals("paper", result.category)
        assertTrue(result.recyclable)
        assertEquals(0.90, result.carbonFactorKg)
    }

    @Test
    fun `test electronic waste classification`() {
        val request = IdentifyWasteRequest(description = "Old broken smartphone with swollen lithium battery")
        val result = classifier.classify(request)
        assertEquals("e_waste", result.category)
        assertTrue(result.recyclable)
        assertEquals(15.00, result.carbonFactorKg)
    }

    @Test
    fun `test hazardous chemical container classification`() {
        val request = IdentifyWasteRequest(description = "Can of toxic motor oil and solvent")
        val result = classifier.classify(request)
        assertEquals("hazardous", result.category)
        assertEquals(false, result.recyclable)
        assertEquals(0.00, result.carbonFactorKg)
    }
}
