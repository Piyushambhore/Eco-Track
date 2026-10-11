package com.ecotrack.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecotrack.android.data.api.EcoTrackApiClient
import com.ecotrack.android.data.model.CollectionRequestDto
import com.ecotrack.android.data.model.CreateCollectionRequest
import com.ecotrack.android.data.model.RecyclingFacilityDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    onNavigateToDashboard: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0: Nearby Recyclers, 1: My Pickups
    var facilities by remember { mutableStateOf<List<RecyclingFacilityDto>>(emptyList()) }
    var myCollections by remember { mutableStateOf<List<CollectionRequestDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var selectedFacilityForBooking by remember { mutableStateOf<RecyclingFacilityDto?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun loadData() {
        coroutineScope.launch {
            isLoading = true
            EcoTrackApiClient.getNearbyFacilities(lat = 16.7050, lng = 74.2433, radius = 50.0)
                .onSuccess { facilities = it }
            EcoTrackApiClient.getMyCollections()
                .onSuccess { myCollections = it }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Recyclers & Logistics", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Locate hubs & schedule zero-waste pickups", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    selectedFacilityForBooking = null
                    showScheduleDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Schedule") },
                text = { Text("Schedule Pickup", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Nearby Recyclers (${facilities.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("My Pickups (${myCollections.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (selectedTab == 0) {
                // Tab 0: Facilities Directory
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Showing certified recycling hubs and scrap aggregators sorted by proximity to your current zone.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    items(facilities) { facility ->
                        FacilityCard(
                            facility = facility,
                            onBookPickup = {
                                selectedFacilityForBooking = facility
                                showScheduleDialog = true
                            }
                        )
                    }
                }
            } else {
                // Tab 1: Collection Requests
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (myCollections.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("No Pickups Scheduled Yet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Book a door-to-door collector to pickup recyclables and earn eco-points.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        items(myCollections) { item ->
                            CollectionRequestCard(request = item)
                        }
                    }
                }
            }
        }
    }

    if (showScheduleDialog) {
        SchedulePickupDialog(
            presetFacility = selectedFacilityForBooking,
            onDismiss = { showScheduleDialog = false },
            onConfirmed = { req ->
                coroutineScope.launch {
                    EcoTrackApiClient.schedulePickup(req).onSuccess {
                        showScheduleDialog = false
                        statusMessage = "Pickup scheduled successfully!"
                        loadData()
                    }
                }
            }
        )
    }
}

@Composable
fun FacilityCard(
    facility: RecyclingFacilityDto,
    onBookPickup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(facility.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(facility.facilityType.replace("_", " ").uppercase(), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        "${facility.distanceKm ?: 1.2} km",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text(facility.address, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            facility.pricingInfo?.let { pricing ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(pricing, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${facility.rating}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onBookPickup,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Request Pickup", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun CollectionRequestCard(request: CollectionRequestDto) {
    val statusColor = when (request.status) {
        "completed" -> Color(0xFF10B981)
        "assigned", "in_transit" -> Color(0xFF06B6D4)
        else -> Color(0xFFF59E0B)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Slot: ${request.preferredDate} (${request.preferredSlot})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        request.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("📍 ${request.pickupAddress}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text("♻️ Materials: ${request.materials.joinToString(", ")}", fontSize = 13.sp)

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Est. Weight: ${request.estimatedWeightKg ?: 5.0} kg", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("+${request.ecoPointsEarned} Eco Points", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulePickupDialog(
    presetFacility: RecyclingFacilityDto?,
    onDismiss: () -> Unit,
    onConfirmed: (CreateCollectionRequest) -> Unit
) {
    var address by remember { mutableStateOf("45 Maple Avenue, Apt 3B") }
    var preferredSlot by remember { mutableStateOf("09:00 - 12:00") }
    var selectedMaterials by remember { mutableStateOf(setOf("plastic", "paper")) }
    var weightText by remember { mutableStateOf("5.0") }
    var instructions by remember { mutableStateOf("Leave near building lobby gate") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Doorstep Pickup", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Pickup Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = preferredSlot,
                    onValueChange = { preferredSlot = it },
                    label = { Text("Preferred Time Slot") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Estimated Weight (kg)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Special Instructions") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weight = weightText.toDoubleOrNull() ?: 5.0
                    onConfirmed(
                        CreateCollectionRequest(
                            pickupAddress = address,
                            preferredDate = "2026-10-12",
                            preferredSlot = preferredSlot,
                            materials = selectedMaterials.toList(),
                            estimatedWeightKg = weight,
                            specialInstructions = instructions,
                            facilityId = presetFacility?.id
                        )
                    )
                }
            ) {
                Text("Confirm Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
