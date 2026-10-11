package com.ecotrack.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.sp
import com.ecotrack.android.data.api.EcoTrackApiClient
import com.ecotrack.android.data.model.CreateCollectionRequest
import com.ecotrack.android.data.model.CreateWasteReportRequest
import com.ecotrack.android.data.model.RecyclingFacilityDto
import com.ecotrack.android.data.model.WasteClassificationResult
import com.ecotrack.android.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    categoryName: String = "Plastic",
    onNavigateBack: () -> Unit,
    onNavigateToNearby: () -> Unit
) {
    var showCategoryDialog by remember { mutableStateOf(false) }
    var currentCategory by remember { mutableStateOf(categoryName) }
    var classification by remember { mutableStateOf(EcoTrackApiClient.lastClassification) }
    var nearbyFacilities by remember { mutableStateOf<List<RecyclingFacilityDto>>(emptyList()) }
    var isSavingReport by remember { mutableStateOf(false) }
    var reportSaved by remember { mutableStateOf(false) }
    var showPickupSheet by remember { mutableStateOf(false) }
    var pickupScheduled by remember { mutableStateOf(false) }

    // Schedule pickup fields
    var pickupAddress by remember { mutableStateOf("Shivaji Park, Kolhapur") }
    var pickupWeight by remember { mutableStateOf("5.0") }
    var preferredDate by remember { mutableStateOf("2026-10-15") }
    var isSchedulingPickup by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val categories = listOf("Plastic", "Paper", "Metal", "Glass", "E-waste", "Organic")

    fun loadAnalysis(category: String) {
        coroutineScope.launch {
            if (classification == null || classification?.category?.equals(category, ignoreCase = true) != true) {
                val res = EcoTrackApiClient.identifyWaste(
                    com.ecotrack.android.data.model.IdentifyWasteRequest(description = "$category recyclable waste")
                )
                res.onSuccess {
                    classification = it
                }
            }

            // Fetch matching facilities accepting this material
            EcoTrackApiClient.getNearbyFacilities(material = category.lowercase()).onSuccess { facs ->
                nearbyFacilities = facs
            }
        }
    }

    LaunchedEffect(currentCategory) {
        loadAnalysis(currentCategory)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp, bottom = 40.dp)
    ) {
        // 1. Top Header: Back button, Title "Waste analysis", Share icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(EcoCard)
                    .border(1.dp, EcoBorder, CircleShape)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = EcoText,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "Waste analysis",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcoText
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(EcoCard)
                    .border(1.dp, EcoBorder, CircleShape)
                    .clickable { /* Share analysis */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = EcoMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Photo Area (Displays real photo if captured, else material badge)
        val capturedBitmap = EcoTrackApiClient.lastCapturedBitmap
        if (capturedBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = capturedBitmap.asImageBitmap(),
                contentDescription = "Captured Waste Photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(EcoTint),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EcoBrandDark,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = classification?.subType ?: "$currentCategory Verified Item",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoBrandDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. Main Result Heading & Confidence
        val activeSubtype = classification?.subType ?: "$currentCategory Segregated Waste"
        val activeConfidence = classification?.confidence?.toInt() ?: 95

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = activeSubtype,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcoText,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFE3F0E7))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$activeConfidence% confidence",
                    fontSize = 11.sp,
                    color = EcoSuccess,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Badges Row: Recyclable status
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isRecyclable = classification?.recyclable ?: true
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isRecyclable) Color(0xFFE3F0E7) else Color(0xFFFFEBEE))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isRecyclable) Icons.Default.Eco else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isRecyclable) EcoSuccess else Color(0xFFC62828),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRecyclable) "Dry recyclable waste" else "Special disposal required",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isRecyclable) EcoSuccess else Color(0xFFC62828)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Real Disposal Guide from AI
        val disposalGuideText = classification?.disposalGuide
            ?: "Separate from organic wet waste, rinse containers clean, and bundle for collection."
        Text(
            text = disposalGuideText,
            fontSize = 13.sp,
            color = EcoText,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Recommended Action
        val recommendedAction = classification?.recommendedAction
        if (!recommendedAction.isNullOrBlank()) {
            Text(
                text = "Action: $recommendedAction",
                fontSize = 12.sp,
                color = EcoMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // "Not right? Change category"
        Text(
            text = "Not right? Change category",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1B4D2E),
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { showCategoryDialog = true }
        )

        Spacer(modifier = Modifier.height(22.dp))

        // 6. Section "Recommended next step"
        Text(
            text = "Recommended next step",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = EcoText
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Big Lime Pill Button: "Find nearby recyclers"
        Button(
            onClick = onNavigateToNearby,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EcoBrand,
                contentColor = EcoBrandDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = EcoBrandDark
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Find nearby recyclers",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row of 2 Secondary Buttons: "Request a collection" & "Save this report"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showPickupSheet = true },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EcoCard,
                    contentColor = EcoText
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Text(
                    text = "Request a collection",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = {
                    if (!reportSaved && !isSavingReport) {
                        isSavingReport = true
                        coroutineScope.launch {
                            val activeCat = classification?.category ?: currentCategory.lowercase()
                            val activeSub = classification?.subType ?: activeSubtype
                            val carbon = (classification?.carbonFactorKg ?: 1.5) * 2.0
                            val saveRes = EcoTrackApiClient.saveWasteReport(
                                CreateWasteReportRequest(
                                    category = activeCat,
                                    subType = activeSub,
                                    confidence = classification?.confidence ?: 95.0,
                                    quantity = 2.0,
                                    unit = "kg",
                                    disposalGuide = disposalGuideText,
                                    carbonImpact = carbon
                                )
                            )
                            isSavingReport = false
                            if (saveRes.isSuccess) {
                                reportSaved = true
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (reportSaved) Color(0xFFE3F0E7) else EcoCard,
                    contentColor = if (reportSaved) EcoSuccess else EcoText
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (reportSaved) EcoSuccess else EcoBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Text(
                    text = when {
                        isSavingReport -> "Saving..."
                        reportSaved -> "Report saved ✓"
                        else -> "Save this report"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 7. Section "Accepted by nearby services" from live database
        Text(
            text = "Accepted by nearby services",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = EcoText
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (nearbyFacilities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Searching local recyclers accepting $currentCategory...",
                    fontSize = 12.sp,
                    color = EcoMuted
                )
            }
        } else {
            nearbyFacilities.forEach { fac ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable { onNavigateToNearby() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = EcoCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EcoTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Recycling,
                                contentDescription = null,
                                tint = EcoBrandDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fac.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EcoText
                            )
                            Text(
                                text = "${fac.address}, ${fac.city}",
                                fontSize = 11.sp,
                                color = EcoMuted
                            )
                            if (!fac.operatingHours.isNullOrBlank()) {
                                Text(
                                    text = fac.operatingHours,
                                    fontSize = 11.sp,
                                    color = EcoBrandDark
                                )
                            }
                        }
                        if (fac.verified) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0xFFE3F0E7))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Verified",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EcoSuccess
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Schedule pickup directly from analysis
    if (showPickupSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPickupSheet = false },
            containerColor = EcoCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Request Doorstep Pickup",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcoText
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = pickupAddress,
                    onValueChange = { pickupAddress = it },
                    label = { Text("Pickup Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = pickupWeight,
                    onValueChange = { pickupWeight = it },
                    label = { Text("Estimated Weight (kg)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = preferredDate,
                    onValueChange = { preferredDate = it },
                    label = { Text("Preferred Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        isSchedulingPickup = true
                        coroutineScope.launch {
                            val w = pickupWeight.toDoubleOrNull() ?: 5.0
                            val res = EcoTrackApiClient.schedulePickup(
                                CreateCollectionRequest(
                                    pickupAddress = pickupAddress,
                                    city = "Kolhapur",
                                    preferredDate = preferredDate,
                                    preferredSlot = "09:00 - 12:00",
                                    materials = listOf(currentCategory.lowercase()),
                                    estimatedWeightKg = w,
                                    specialInstructions = "Segregated $currentCategory pickup from app analysis"
                                )
                            )
                            isSchedulingPickup = false
                            if (res.isSuccess) {
                                showPickupSheet = false
                                pickupScheduled = true
                            }
                        }
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EcoBrand, contentColor = EcoBrandDark),
                    enabled = !isSchedulingPickup,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = if (isSchedulingPickup) "Scheduling..." else "Confirm Pickup Request",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (pickupScheduled) {
        AlertDialog(
            onDismissRequest = { pickupScheduled = false },
            title = { Text("Pickup Confirmed", fontWeight = FontWeight.SemiBold) },
            text = { Text("A certified collector has been dispatched for your $currentCategory waste.", fontSize = 13.sp, color = EcoText) },
            confirmButton = {
                TextButton(onClick = { pickupScheduled = false }) {
                    Text("Done", color = EcoBrandDark, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = EcoCard
        )
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Select Waste Category", fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    categories.forEach { cat ->
                        Text(
                            text = cat,
                            fontSize = 15.sp,
                            color = if (cat == currentCategory) EcoBrandDark else EcoText,
                            fontWeight = if (cat == currentCategory) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentCategory = cat
                                    classification = null
                                    showCategoryDialog = false
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Cancel", color = EcoMuted)
                }
            },
            containerColor = EcoCard
        )
    }
}
