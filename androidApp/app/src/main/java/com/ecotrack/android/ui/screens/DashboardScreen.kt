package com.ecotrack.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecotrack.android.data.api.EcoTrackApiClient
import com.ecotrack.android.data.model.PersonalImpactDto
import com.ecotrack.android.data.model.RecyclingFacilityDto
import com.ecotrack.android.data.model.WasteReportDto
import com.ecotrack.android.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToScanner: () -> Unit,
    onNavigateToNearby: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onNavigateToImpact: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {}
) {
    var selectedLocation by remember { mutableStateOf("Kolhapur, Maharashtra") }
    var showLocationSheet by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }

    var impactData by remember { mutableStateOf<PersonalImpactDto?>(null) }
    var facilitiesList by remember { mutableStateOf<List<RecyclingFacilityDto>>(emptyList()) }
    var latestReport by remember { mutableStateOf<WasteReportDto?>(null) }
    var collectionsCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()

    val locations = listOf(
        "Kolhapur, Maharashtra",
        "Sangli, Maharashtra",
        "Pune, Maharashtra",
        "Mumbai, Maharashtra"
    )

    fun loadLiveDashboardData() {
        coroutineScope.launch {
            isLoading = true
            EcoTrackApiClient.ensureAuthenticated()

            // 1. Fetch real personal impact
            EcoTrackApiClient.getPersonalImpact().onSuccess { imp ->
                impactData = imp
            }

            // 2. Fetch real facilities from DB
            EcoTrackApiClient.getNearbyFacilities().onSuccess { facs ->
                facilitiesList = facs
            }

            // 3. Fetch real collections count
            EcoTrackApiClient.getMyCollections().onSuccess { cols ->
                collectionsCount = cols.size
            }

            // 4. Fetch real waste reports
            EcoTrackApiClient.getWasteReports(page = 1).onSuccess { reps ->
                latestReport = reps.items.firstOrNull()
            }

            isLoading = false
        }
    }

    LaunchedEffect(selectedLocation) {
        loadLiveDashboardData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp, bottom = 100.dp)
    ) {
        // 1. Top Header Row: Logo, Location Selector, Profile Avatar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // EcoTrack Brand
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(EcoBrandDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = "Logo",
                        tint = EcoBrand,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EcoTrack",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcoText
                )
            }

            // Location Selector Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(EcoCard)
                    .border(1.dp, EcoBorder, RoundedCornerShape(999.dp))
                    .clickable { showLocationSheet = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = EcoMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = selectedLocation,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = EcoText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = EcoMuted,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Settings & Profile Avatar
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings & AWS",
                        tint = EcoText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                val userInitial = EcoTrackApiClient.currentUser?.displayName?.take(1)?.uppercase() ?: "R"
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(EcoBrand)
                        .clickable { showAccountMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInitial,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoBrandDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Greeting & Supporting Text
        Text(
            text = "Make a little difference today.",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = EcoText,
            lineHeight = 28.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Let's give your waste a better destination.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = EcoMuted
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Large Lime-Tinted Scan Card (Hero Action)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = EcoTint),
            border = androidx.compose.foundation.BorderStroke(1.dp, EcoTintBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(EcoBrandDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scan",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "What are you throwing away?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcoText
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Identify waste and find the right way to dispose of it.",
                    fontSize = 13.sp,
                    color = EcoText.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onNavigateToScanner,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EcoBrandDark,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scan your waste",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. "Your impact" Section with Real Backend Metrics (NO fake data badges)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your impact",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcoText
            )
            Text(
                text = "View details",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcoSuccess,
                modifier = Modifier.clickable { onNavigateToImpact() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val recoveredKg = impactData?.totalWeightKg ?: 0.0
        val actionsCount = impactData?.totalActionsCount ?: 0

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToImpact() },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = EcoCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = String.format(Locale.US, "%.1f kg", recoveredKg),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoText
                    )
                    Text(
                        text = "materials\nrecovered",
                        fontSize = 11.sp,
                        color = EcoMuted,
                        lineHeight = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(EcoBorder)
                )

                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "$actionsCount",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoText
                    )
                    Text(
                        text = "actions\ncompleted",
                        fontSize = 11.sp,
                        color = EcoMuted,
                        lineHeight = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(EcoBorder)
                )

                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "$collectionsCount",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoText
                    )
                    Text(
                        text = "collection\nrequests",
                        fontSize = 11.sp,
                        color = EcoMuted,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. "Nearby services" Section from live database
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nearby services",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcoText
            )
            Text(
                text = "See all",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcoSuccess,
                modifier = Modifier.clickable { onNavigateToNearby() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (facilitiesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLoading) "Connecting to live services..." else "No facilities registered in this area yet.",
                    fontSize = 12.sp,
                    color = EcoMuted
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(facilitiesList) { fac ->
                    Card(
                        modifier = Modifier
                            .width(240.dp)
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
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EcoTint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Recycling,
                                    contentDescription = null,
                                    tint = EcoBrandDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fac.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EcoText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = fac.acceptedMaterials.take(3).joinToString(", "),
                                    fontSize = 11.sp,
                                    color = EcoMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = fac.city,
                                        fontSize = 11.sp,
                                        color = EcoMuted
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (fac.verified) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Color(0xFFE3F0E7))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
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
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. "Continue your impact" Section backed by real Waste Report
        Text(
            text = "Continue your impact",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = EcoText
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (latestReport != null) onNavigateToAnalysis() else onNavigateToScanner()
                },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = EcoCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EcoTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = EcoBrandDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val reportTitle = latestReport?.subType ?: latestReport?.category?.replaceFirstChar { it.uppercase() } ?: "Scan to log your waste"
                        Text(
                            text = reportTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EcoText
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(EcoTint)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            val statusText = latestReport?.status?.replaceFirstChar { it.uppercase() } ?: "Ready to scan"
                            Text(
                                text = statusText,
                                fontSize = 11.sp,
                                color = EcoBrandDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = EcoMuted
                )
            }
        }
    }

    // Location Change Bottom Sheet
    if (showLocationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLocationSheet = false },
            containerColor = EcoCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Select Location",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcoText
                )
                Spacer(modifier = Modifier.height(14.dp))
                locations.forEach { loc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedLocation = loc
                                showLocationSheet = false
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = loc,
                            fontSize = 15.sp,
                            color = if (loc == selectedLocation) EcoBrandDark else EcoText,
                            fontWeight = if (loc == selectedLocation) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (loc == selectedLocation) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = EcoBrandDark)
                        }
                    }
                }
            }
        }
    }

    if (showAccountMenu) {
        val user = EcoTrackApiClient.currentUser
        AlertDialog(
            onDismissRequest = { showAccountMenu = false },
            title = {
                Text(
                    text = user?.displayName ?: "Eco Citizen",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(text = "Email: ${user?.email ?: "user@ecotrack.dev"}", fontSize = 13.sp, color = EcoMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Role: ${user?.role ?: "CITIZEN"}",
                        fontSize = 13.sp,
                        color = EcoBrandDark,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            showAccountMenu = false
                            onNavigateToSettings()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EcoTint, contentColor = EcoBrandDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Settings & AWS Cloud", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccountMenu = false
                        EcoTrackApiClient.logout()
                        onNavigateToAuth()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EcoBrandDark)
                ) {
                    Text("Switch / Sign Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountMenu = false }) {
                    Text("Close")
                }
            }
        )
    }
}
