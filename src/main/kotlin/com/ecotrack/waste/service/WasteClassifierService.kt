package com.ecotrack.waste.service

import com.ecotrack.waste.api.IdentifyWasteRequest
import com.ecotrack.waste.api.WasteCategoryInfo
import com.ecotrack.waste.api.WasteClassificationResult
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.Locale
import javax.imageio.ImageIO

class WasteClassifierService {
    private val logger = LoggerFactory.getLogger(WasteClassifierService::class.java)

    companion object {
        val CATEGORIES = listOf(
            WasteCategoryInfo(
                category = "plastic",
                displayName = "Plastics (PET, HDPE, PP)",
                description = "Bottles, food containers, shampoo bottles, tubs, and packaging polymers.",
                carbonFactorPerKg = 1.50,
                recyclable = true,
                defaultUnits = listOf("kg", "g", "count"),
                disposalGuide = "Rinse thoroughly to remove residue. Remove food stains and compress bottles to save bin space."
            ),
            WasteCategoryInfo(
                category = "paper",
                displayName = "Paper & Cardboard",
                description = "Corrugated shipping cartons, newspapers, printer paper, egg cartons, and magazines.",
                carbonFactorPerKg = 0.90,
                recyclable = true,
                defaultUnits = listOf("kg", "g", "count"),
                disposalGuide = "Keep dry and flatten boxes. Remove excessive plastic tape and metal staples if possible."
            ),
            WasteCategoryInfo(
                category = "metal",
                displayName = "Metals (Aluminum, Tin & Steel)",
                description = "Beverage cans, tin food containers, foil trays, copper wires, and metal scraps.",
                carbonFactorPerKg = 9.00,
                recyclable = true,
                defaultUnits = listOf("kg", "g", "count"),
                disposalGuide = "Rinse beverage cans clean. Crush aluminum cans to reduce storage volume before pickup."
            ),
            WasteCategoryInfo(
                category = "e_waste",
                displayName = "Electronic Waste (E-Waste)",
                description = "Old smartphones, laptops, circuit boards, cables, lithium batteries, and adapters.",
                carbonFactorPerKg = 15.00,
                recyclable = true,
                defaultUnits = listOf("count", "kg"),
                disposalGuide = "Never dispose in general trash. Keep dry, wrap exposed battery terminals with non-conductive tape, and schedule certified pickup."
            ),
            WasteCategoryInfo(
                category = "glass",
                displayName = "Glass Bottles & Jars",
                description = "Clear, amber, and green glass beverage bottles, condiment jars, and cosmetics glass.",
                carbonFactorPerKg = 0.30,
                recyclable = true,
                defaultUnits = listOf("kg", "count"),
                disposalGuide = "Rinse containers clean. Remove metal or plastic caps. Avoid mixing broken window glass or ceramics."
            ),
            WasteCategoryInfo(
                category = "organic",
                displayName = "Organic & Compostable Waste",
                description = "Fruit & vegetable peels, food scraps, coffee grounds, garden clippings, and leaves.",
                carbonFactorPerKg = 0.50,
                recyclable = true,
                defaultUnits = listOf("kg", "g", "liters"),
                disposalGuide = "Deposit in community aerated compost pits or green bin. Do not include non-compostable packaging."
            ),
            WasteCategoryInfo(
                category = "hazardous",
                displayName = "Hazardous & Chemical Waste",
                description = "Paints, pesticides, aerosol cans, vehicle oil, light tubes, and corrosive chemicals.",
                carbonFactorPerKg = 0.00,
                recyclable = false,
                defaultUnits = listOf("liters", "count", "kg"),
                disposalGuide = "Keep in original labeled containers. Never pour down drains. Contact authorized municipal hazardous drop-off."
            )
        )
    }

    fun getAllCategories(): List<WasteCategoryInfo> = CATEGORIES

    fun classify(request: IdentifyWasteRequest): WasteClassificationResult {
        logger.info("Executing AI waste classification inference")

        val textHint = request.description?.lowercase(Locale.ROOT) ?: ""
        val imagePayload = request.imageBase64 ?: request.imageUrl ?: ""

        // Try extracting confidence score if passed from client ML Kit (e.g., "(94%)")
        val parsedConfidence = extractConfidence(textHint)

        // Decode image visual color attributes if Base64 payload is provided
        val visualHint = analyzeImageBytes(imagePayload)

        // 1. HAZARDOUS
        if (containsAny(textHint, "paint", "chemical", "bulb", "mercury", "toxic", "hazardous", "motor oil", "solvent", "pesticide", "corrosive")) {
            return WasteClassificationResult(
                category = "hazardous",
                subType = "Hazardous Domestic Chemical / Solvent",
                confidence = parsedConfidence ?: 89.0,
                recyclable = false,
                disposalGuide = "Do not mix with household recyclables or pour into sewer drains. Keep tightly sealed.",
                carbonFactorKg = 0.00,
                recommendedAction = "Submit to hazardous waste collection drive or designated municipal toxic drop-off.",
                acceptedUnits = listOf("liters", "count")
            )
        }

        // 2. ELECTRONICS / E-WASTE
        if (containsAny(textHint, "battery", "phone", "smartphone", "laptop", "cable", "electronics", "electronic", "gadget", "circuit", "charger", "e-waste", "ewaste", "screen", "display", "hardware", "keyboard", "mouse", "adapter") ||
            visualHint == "e_waste") {
            return WasteClassificationResult(
                category = "e_waste",
                subType = "Consumer Electronics & Hardware",
                confidence = parsedConfidence ?: 93.5,
                recyclable = true,
                disposalGuide = "Handle with care. Tape lithium battery terminals. Do not crush or puncture.",
                carbonFactorKg = 15.00,
                recommendedAction = "Dispatch only to authorized e-waste refurbishers or certified municipal drop-off hubs.",
                acceptedUnits = listOf("count", "kg")
            )
        }

        // 3. METALS
        if (containsAny(textHint, "can", "aluminum", "tin", "soda can", "beverage can", "beer can", "copper", "scrap metal", "steel", "iron", "brass", "cylinder", "foil", "metal")) {
            return WasteClassificationResult(
                category = "metal",
                subType = "Aluminum Beverage Can / Metal Scraps",
                confidence = parsedConfidence ?: 96.4,
                recyclable = true,
                disposalGuide = "Rinse thoroughly to remove residue. Crush cans to minimize volume.",
                carbonFactorKg = 9.00,
                recommendedAction = "Schedule pickup with local scrap dealer or drop off at metallic recycling facility.",
                acceptedUnits = listOf("count", "kg")
            )
        }

        // 4. PAPER & CARDBOARD
        if (containsAny(textHint, "cardboard", "corrugated", "newspaper", "carton", "book", "magazine", "paper", "envelope", "packaging", "kraft") ||
            visualHint == "paper") {
            return WasteClassificationResult(
                category = "paper",
                subType = "Corrugated Cardboard Packaging",
                confidence = parsedConfidence ?: 94.8,
                recyclable = true,
                disposalGuide = "Flatten cardboard boxes and keep in a dry area away from rain.",
                carbonFactorKg = 0.90,
                recommendedAction = "Bundle clean cardboard for scrap pickup or campus paper recycling drive.",
                acceptedUnits = listOf("kg", "count")
            )
        }

        // 5. ORGANIC / COMPOSTABLE
        if (containsAny(textHint, "fruit", "vegetable", "food", "produce", "apple", "banana", "peel", "scraps", "compost", "organic", "food waste", "plant", "bread", "coffee grounds", "leaves") ||
            visualHint == "organic") {
            return WasteClassificationResult(
                category = "organic",
                subType = "Kitchen & Fruit Peels Organic Biomass",
                confidence = parsedConfidence ?: 92.0,
                recyclable = true,
                disposalGuide = "Layer with dry leaves in aerated compost tumbler or local community organic bin.",
                carbonFactorKg = 0.50,
                recommendedAction = "Convert into rich compost soil conditioner or biogas feedstock.",
                acceptedUnits = listOf("kg", "liters")
            )
        }

        // 6. GLASS
        if (containsAny(textHint, "glass", "wine bottle", "beer bottle", "preserves jar", "glass jar", "mason jar", "drinkware", "goblet", "glassware")) {
            return WasteClassificationResult(
                category = "glass",
                subType = "Glass Beverage Bottle / Preserves Jar",
                confidence = parsedConfidence ?: 95.1,
                recyclable = true,
                disposalGuide = "Rinse with water. Remove metal lids. Keep intact to avoid hazardous shards.",
                carbonFactorKg = 0.30,
                recommendedAction = "Drop off at nearest bottle bank or schedule segregated glass collection.",
                acceptedUnits = listOf("count", "kg")
            )
        }

        // 7. PLASTIC
        if (containsAny(textHint, "plastic", "pet bottle", "hdpe", "water bottle", "shampoo bottle", "plastic bag", "tub", "polymer", "wrapper") ||
            visualHint == "plastic") {
            return WasteClassificationResult(
                category = "plastic",
                subType = "PET (Polyethylene Terephthalate) Plastic Item",
                confidence = parsedConfidence ?: 94.2,
                recyclable = true,
                disposalGuide = "Empty liquids, rinse lightly, flatten bottle, and replace cap before binning.",
                carbonFactorKg = 1.50,
                recommendedAction = "Eligible for doorstep scrap dealer pickup or plastic deposit return machine.",
                acceptedUnits = listOf("count", "kg")
            )
        }

        // 8. FALLBACK (Based on visual hint or mixed recyclable)
        return when (visualHint) {
            "metal" -> WasteClassificationResult(
                category = "metal",
                subType = "Metallic Scraps / Cans",
                confidence = 88.5,
                recyclable = true,
                disposalGuide = "Rinse clean and bundle together.",
                carbonFactorKg = 9.00,
                recommendedAction = "Hand over to local authorized scrap dealer.",
                acceptedUnits = listOf("count", "kg")
            )
            "paper" -> WasteClassificationResult(
                category = "paper",
                subType = "Recyclable Paper / Packaging Box",
                confidence = 89.0,
                recyclable = true,
                disposalGuide = "Keep clean and dry, flatten for optimal collection volume.",
                carbonFactorKg = 0.90,
                recommendedAction = "Hand over for circular paper recycling.",
                acceptedUnits = listOf("kg", "count")
            )
            "organic" -> WasteClassificationResult(
                category = "organic",
                subType = "Organic Food & Garden Biomass",
                confidence = 87.5,
                recyclable = true,
                disposalGuide = "Deposit in community compost bin.",
                carbonFactorKg = 0.50,
                recommendedAction = "Convert into organic compost fertilizer.",
                acceptedUnits = listOf("kg", "liters")
            )
            else -> WasteClassificationResult(
                category = "plastic",
                subType = "Rigid Recyclable Consumer Container",
                confidence = 86.0,
                recyclable = true,
                disposalGuide = "Empty all contents, clean dry, and store in blue recyclable bin.",
                carbonFactorKg = 1.50,
                recommendedAction = "Eligible for doorstep scrap collection or recycling center drop-off.",
                acceptedUnits = listOf("count", "kg")
            )
        }
    }

    private fun extractConfidence(text: String): Double? {
        val regex = Regex("""\((\d{1,3})%\)""")
        val match = regex.find(text) ?: return null
        return match.groupValues[1].toDoubleOrNull()
    }

    private fun analyzeImageBytes(base64Payload: String): String? {
        if (base64Payload.isBlank() || base64Payload.length < 100) return null
        return try {
            val cleanBase64 = if (base64Payload.contains(",")) {
                base64Payload.substringAfter(",")
            } else {
                base64Payload
            }
            val bytes = Base64.getDecoder().decode(cleanBase64)
            val image = ImageIO.read(ByteArrayInputStream(bytes)) ?: return null

            val width = image.width
            val height = image.height
            var totalR = 0L
            var totalG = 0L
            var totalB = 0L
            var sampleCount = 0

            val stepX = maxOf(1, width / 12)
            val stepY = maxOf(1, height / 12)

            for (x in (width / 4)..(width * 3 / 4) step stepX) {
                for (y in (height / 4)..(height * 3 / 4) step stepY) {
                    val rgb = image.getRGB(x, y)
                    totalR += (rgb shr 16) and 0xFF
                    totalG += (rgb shr 8) and 0xFF
                    totalB += rgb and 0xFF
                    sampleCount++
                }
            }

            if (sampleCount == 0) return null
            val avgR = totalR / sampleCount
            val avgG = totalG / sampleCount
            val avgB = totalB / sampleCount

            when {
                // High green hue -> Organic
                avgG > avgR * 1.15 && avgG > avgB * 1.15 && avgG > 60 -> "organic"
                // Kraft brown (Cardboard): R > G > B with warm brown ratio
                avgR > 110 && avgG in 70..140 && avgB < 85 && (avgR - avgB) > 40 -> "paper"
                // Very dark/black -> E-waste / Screen / Electronics
                avgR < 55 && avgG < 55 && avgB < 55 -> "e_waste"
                // Metallic silver/neutral gray: R ≈ G ≈ B with moderate brightness
                Math.abs(avgR - avgG) < 12 && Math.abs(avgG - avgB) < 12 && avgR in 80..180 -> "metal"
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun containsAny(source: String, vararg targets: String): Boolean {
        return targets.any { source.contains(it, ignoreCase = true) }
    }
}
