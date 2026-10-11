package com.ecotrack.android.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ecotrack.android.data.api.EcoTrackApiClient
import com.ecotrack.android.data.model.CollectionRequestDto
import com.ecotrack.android.data.model.MarketplaceListingDto
import com.ecotrack.android.data.model.RecyclingFacilityDto
import com.ecotrack.android.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectorPortalScreen(
    onLogout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Pickup Queue, 1: Scrap Lots, 2: Facility Map

    var queueItems by remember { mutableStateOf<List<CollectionRequestDto>>(emptyList()) }
    var marketplaceLots by remember { mutableStateOf<List<MarketplaceListingDto>>(emptyList()) }
    var myFacility by remember { mutableStateOf<RecyclingFacilityDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    // Dialog for completing collection
    var completingItem by remember { mutableStateOf<CollectionRequestDto?>(null) }
    var verifiedWeightInput by remember { mutableStateOf("") }
    var completionNotes by remember { mutableStateOf("") }
    var isCompleting by remember { mutableStateOf(false) }

    fun refreshData() {
        coroutineScope.launch {
            isLoading = true
            actionMessage = null

            // 1. Fetch live collector queue
            EcoTrackApiClient.getCollectorQueue().onSuccess { queue ->
                queueItems = queue
            }

            // 2. Fetch live marketplace listings
            EcoTrackApiClient.getMarketplaceListings().onSuccess { lots ->
                marketplaceLots = lots
            }

            // 3. Fetch facilities to find matched facility
            EcoTrackApiClient.getNearbyFacilities().onSuccess { facs ->
                myFacility = facs.firstOrNull {
                    it.name.contains("Scrap", ignoreCase = true) || it.facilityType == "scrap_dealer"
                } ?: facs.firstOrNull()
            }

            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "♻️ Recycler & Collector Portal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = EcoText
                            )
                        }
                        val user = EcoTrackApiClient.currentUser
                        Text(
                            text = "${user?.displayName ?: "Certified Recycler"} • Kolhapur Hub",
                            fontSize = 12.sp,
                            color = EcoBrandDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = EcoBrandDark)
                    }
                    IconButton(
                        onClick = {
                            EcoTrackApiClient.logout()
                            onLogout()
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EcoCard)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EcoBg)
                .padding(paddingValues)
        ) {
            // Status banner if an action was executed
            actionMessage?.let { msg ->
                Surface(
                    color = EcoBrand.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoBrandDark,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Quick Stats Metric Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val openCount = queueItems.count { it.status == "submitted" }
                val activeCount = queueItems.count { it.status == "assigned" || it.status == "in_transit" }
                val doneCount = queueItems.count { it.status == "completed" }

                CollectorStatChip("Pending", "$openCount", EcoText, Modifier.weight(1f))
                CollectorStatChip("Active", "$activeCount", Color(0xFFE65100), Modifier.weight(1f))
                CollectorStatChip("Completed", "$doneCount", EcoBrandDark, Modifier.weight(1f))
            }

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = EcoCard,
                contentColor = EcoBrandDark
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Pickups (${queueItems.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Scrap Lots (${marketplaceLots.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Facility Map", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EcoBrandDark)
                }
            } else {
                when (selectedTab) {
                    0 -> CollectorQueueTab(
                        items = queueItems,
                        onAccept = { id ->
                            coroutineScope.launch {
                                EcoTrackApiClient.acceptCollection(id).onSuccess {
                                    actionMessage = "Pickup request accepted! Dispatched to your route."
                                    refreshData()
                                }.onFailure {
                                    actionMessage = "Error: ${it.message}"
                                }
                            }
                        },
                        onMarkInTransit = { id ->
                            coroutineScope.launch {
                                EcoTrackApiClient.markCollectionInTransit(id).onSuccess {
                                    actionMessage = "Status updated to In-Transit."
                                    refreshData()
                                }.onFailure {
                                    actionMessage = "Error: ${it.message}"
                                }
                            }
                        },
                        onOpenComplete = { item ->
                            completingItem = item
                            verifiedWeightInput = (item.estimatedWeightKg ?: 5.0).toString()
                            completionNotes = ""
                        }
                    )
                    1 -> CollectorMarketplaceTab(
                        lots = marketplaceLots,
                        onBid = { listingId ->
                            actionMessage = "Bid placed on listing #${listingId.take(6)}!"
                        }
                    )
                    2 -> CollectorFacilityMapTab(facility = myFacility)
                }
            }
        }
    }

    // Modal Sheet / Dialog to Complete Handover
    completingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!isCompleting) completingItem = null },
            title = {
                Text(
                    text = "Verify & Complete Pickup",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Address: ${item.pickupAddress}",
                        fontSize = 13.sp,
                        color = EcoMuted
                    )
                    Text(
                        text = "Materials: ${item.materials.joinToString(", ")}",
                        fontSize = 13.sp,
                        color = EcoMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = verifiedWeightInput,
                        onValueChange = { verifiedWeightInput = it },
                        label = { Text("Actual Verified Weight (kg)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = completionNotes,
                        onValueChange = { completionNotes = it },
                        label = { Text("Notes / Weighbridge slip (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val weight = verifiedWeightInput.toDoubleOrNull()
                        if (weight != null && weight > 0) {
                            isCompleting = true
                            coroutineScope.launch {
                                EcoTrackApiClient.completeCollection(
                                    id = item.id,
                                    actualWeightKg = weight,
                                    notes = completionNotes.ifBlank { null }
                                ).onSuccess {
                                    actionMessage = "Pickup #${item.id.take(6)} completed and verified ($weight kg)!"
                                    completingItem = null
                                    refreshData()
                                }.onFailure {
                                    actionMessage = "Failed: ${it.message}"
                                }
                                isCompleting = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EcoBrandDark),
                    enabled = !isCompleting
                ) {
                    if (isCompleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Confirm Handover", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { completingItem = null },
                    enabled = !isCompleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CollectorStatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(EcoCard)
            .border(1.dp, EcoBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, fontSize = 11.sp, color = EcoMuted)
        }
    }
}

@Composable
private fun CollectorQueueTab(
    items: List<CollectionRequestDto>,
    onAccept: (String) -> Unit,
    onMarkInTransit: (String) -> Unit,
    onOpenComplete: (CollectionRequestDto) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EcoBrandDark,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Pending Pickup Requests",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = EcoText
                )
                Text(
                    text = "All scheduled pickups in Kolhapur have been fulfilled.",
                    fontSize = 13.sp,
                    color = EcoMuted
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                CollectorQueueCard(
                    item = item,
                    onAccept = { onAccept(item.id) },
                    onMarkInTransit = { onMarkInTransit(item.id) },
                    onComplete = { onOpenComplete(item) }
                )
            }
        }
    }
}

@Composable
private fun CollectorQueueCard(
    item: CollectionRequestDto,
    onAccept: () -> Unit,
    onMarkInTransit: () -> Unit,
    onComplete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = EcoCard),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Status badge & ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Req #${item.id.take(8).uppercase(Locale.ROOT)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = EcoText
                )

                val (badgeBg, badgeText, label) = when (item.status) {
                    "submitted" -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "NEW REQUEST")
                    "assigned" -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "ACCEPTED")
                    "in_transit" -> Triple(Color(0xFFEDE7F6), Color(0xFF512DA8), "IN TRANSIT")
                    "completed" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "COMPLETED")
                    else -> Triple(EcoBg, EcoText, item.status.uppercase(Locale.ROOT))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeText)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Address
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = EcoBrandDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${item.pickupAddress}, ${item.city}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcoText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Preferred slot & weight
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📅 ${item.preferredDate} (${item.preferredSlot})",
                    fontSize = 12.sp,
                    color = EcoMuted
                )
                Text(
                    text = "⚖️ Est: ${item.estimatedWeightKg ?: 5.0} kg",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcoBrandDark
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Materials
            Text(
                text = "Materials: ${item.materials.joinToString(" • ") { it.replaceFirstChar { c -> c.uppercase() } }}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = EcoText
            )

            item.specialInstructions?.let { instr ->
                if (instr.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Note: \"$instr\"",
                        fontSize = 11.sp,
                        color = EcoMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            when (item.status) {
                "submitted" -> {
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EcoBrandDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Accept Pickup", fontWeight = FontWeight.Bold)
                    }
                }
                "assigned" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onMarkInTransit,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Start Route", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = EcoBrandDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Verify Weight", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "in_transit" -> {
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EcoBrandDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Scale, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Arrived & Verify Weight", fontWeight = FontWeight.Bold)
                    }
                }
                "completed" -> {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✅ Handover Completed • Verified ${item.actualWeightKg ?: item.estimatedWeightKg} kg",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectorMarketplaceTab(
    lots: List<MarketplaceListingDto>,
    onBid: (String) -> Unit
) {
    if (lots.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active scrap lots listed in Kolhapur.", color = EcoMuted)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(lots) { lot ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = EcoCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lot.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = EcoText
                            )
                            Text(
                                text = "₹${lot.askingPricePerKg}/kg",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = EcoBrandDark
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Lot Size: ${lot.quantityKg} kg • Total ₹${lot.totalAskingPrice}",
                            fontSize = 13.sp,
                            color = EcoText,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Location: ${lot.pickupAddress}, ${lot.pickupCity}",
                            fontSize = 12.sp,
                            color = EcoMuted
                        )

                        lot.description?.let { desc ->
                            if (desc.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = desc,
                                    fontSize = 12.sp,
                                    color = EcoText.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onBid(lot.id) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = EcoBrandDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Claim & Purchase Lot", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectorFacilityMapTab(facility: RecyclingFacilityDto?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        facility?.let { fac ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = EcoCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = fac.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = EcoText
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(EcoBrand.copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "VERIFIED HUB",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoBrandDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "📍 ${fac.address}, ${fac.city}", fontSize = 13.sp, color = EcoMuted)
                    fac.contactPhone?.let { phone ->
                        Text(text = "📞 $phone", fontSize = 12.sp, color = EcoMuted)
                    }
                    fac.operatingHours?.let { hours ->
                        Text(text = "⏰ $hours", fontSize = 12.sp, color = EcoMuted)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Materials Handled: ${fac.acceptedMaterials.joinToString(", ")}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoBrandDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Leaflet Map for this facility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, EcoBorder, RoundedCornerShape(16.dp))
            ) {
                val mapHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                    <link href="https://unpkg.com/maplibre-gl@3.6.2/dist/maplibre-gl.css" rel="stylesheet" />
                    <script src="https://unpkg.com/maplibre-gl@3.6.2/dist/maplibre-gl.js"></script>
                    <style>
                      html, body, #map { height: 100%; margin: 0; padding: 0; background: #e5e3df; font-family: sans-serif; overflow: hidden; }
                      .hub-pin { width: 20px; height: 20px; border-radius: 50%; background: #E65100; border: 3px solid #FFFFFF; box-shadow: 0 2px 8px rgba(0,0,0,0.4); }
                      .maplibregl-popup-content { font-family: inherit; font-size: 12px; }
                    </style>
                    </head>
                    <body>
                    <div id="map"></div>
                    <script>
                      function initCollectorMap() {
                        if (typeof maplibregl === 'undefined') {
                          setTimeout(initCollectorMap, 100);
                          return;
                        }
                        try {
                          var map = new maplibregl.Map({
                            container: 'map',
                            style: {
                              version: 8,
                              sources: {
                                'osm': {
                                  type: 'raster',
                                  tiles: [
                                    'https://basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png',
                                    'https://tile.openstreetmap.org/{z}/{x}/{y}.png'
                                  ],
                                  tileSize: 256,
                                  attribution: '© OpenStreetMap, © CARTO'
                                }
                              },
                              layers: [{ id: 'osm', type: 'raster', source: 'osm', minzoom: 0, maxzoom: 19 }]
                            },
                            center: [${fac.longitude}, ${fac.latitude}],
                            zoom: 14,
                            attributionControl: false
                          });
                          var pin = document.createElement('div');
                          pin.className = 'hub-pin';
                          new maplibregl.Marker({ element: pin })
                            .setLngLat([${fac.longitude}, ${fac.latitude}])
                            .setPopup(new maplibregl.Popup({ offset: 12 }).setHTML('<b>${fac.name.replace("'", "\\'")}</b><br>${fac.address.replace("'", "\\'")}<br>Kolhapur'))
                            .addTo(map);
                          setTimeout(function() { map.resize(); }, 300);
                        } catch(err) {
                          console.log('Collector map error:', err);
                        }
                      }
                      if (document.readyState === 'loading') {
                        document.addEventListener('DOMContentLoaded', initCollectorMap);
                      } else {
                        initCollectorMap();
                      }
                    </script>
                    </body>
                    </html>
                """.trimIndent()

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 EcoTrack/1.0"
                            webViewClient = WebViewClient()
                            loadDataWithBaseURL("https://unpkg.com", mapHtml, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Facility details loading...", color = EcoMuted)
            }
        }
    }
}
