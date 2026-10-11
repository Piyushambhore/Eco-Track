package com.ecotrack.impact.model

import kotlinx.serialization.Serializable

@Serializable
data class EmissionFactor(
    val material: String,
    val co2KgPerKg: Double,
    val waterLitersPerKg: Double,
    val energyKwhPerKg: Double,
    val citation: String
)

@Serializable
data class MaterialImpactSummary(
    val material: String,
    val weightKg: Double,
    val co2SavedKg: Double,
    val percentage: Double
)

@Serializable
data class ImpactEquivalentsDto(
    val treeSeedlingsGrown10Years: Double, // 1 tree ~ 21.77 kg CO2/yr
    val passengerCarKmAvoided: Double,      // ~ 0.192 kg CO2 / km
    val smartphoneCharges: Long,           // ~ 0.008 kg CO2 / charge
    val waterLitersConserved: Double,
    val homesElectricityHours: Double      // ~ 0.385 kg CO2 / kWh
)

@Serializable
data class PersonalImpactDto(
    val userId: String,
    val totalWeightKg: Double,
    val totalCo2SavedKg: Double,
    val equivalents: ImpactEquivalentsDto,
    val materialBreakdown: List<MaterialImpactSummary>,
    val totalActionsCount: Int
)

@Serializable
data class CommunityImpactDto(
    val communityName: String,
    val totalWeightKg: Double,
    val totalCo2SavedKg: Double,
    val activeParticipantsCount: Int,
    val treesEquivalent: Double,
    val topMaterial: String,
    val equivalents: ImpactEquivalentsDto
)

@Serializable
data class CreateCertificateRequest(
    val title: String? = null,
    val periodMonths: Int = 3
)

@Serializable
data class ImpactCertificateDto(
    val id: String,
    val certificateNumber: String,
    val userId: String,
    val userName: String,
    val title: String,
    val periodStart: String,
    val periodEnd: String,
    val totalWeightKg: Double,
    val totalCo2Kg: Double,
    val treesEquivalent: Double,
    val waterSavedLiters: Double,
    val energySavedKwh: Double,
    val verificationHash: String,
    val status: String,
    val issuedAt: String,
    val verificationUrl: String
)

@Serializable
data class CertificateVerificationResultDto(
    val valid: Boolean,
    val certificate: ImpactCertificateDto?,
    val message: String
)
