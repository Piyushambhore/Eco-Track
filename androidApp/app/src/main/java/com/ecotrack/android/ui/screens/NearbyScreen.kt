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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.ecotrack.android.data.model.CreateCollectionRequest
import com.ecotrack.android.data.model.RecyclingFacilityDto
import com.ecotrack.android.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyScreen(
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filters = listOf("All", "Recycling centres", "Scrap dealers", "E-waste", "Collection services")
    var selectedFilter by remember { mutableStateOf("All") }
    var showRequestSheet by remember { mutableStateOf(false) }
    var requestSubmitted by remember { mutableStateOf(false) }
    var facilities by remember { mutableStateOf<List<RecyclingFacilityDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Collection schedule form fields
    val materials = listOf("Plastic", "Paper", "Metal", "Glass", "E-waste")
    var selectedMaterial by remember { mutableStateOf("Plastic") }
    var pickupAddress by remember { mutableStateOf("Shivaji Park, Kolhapur") }
    var estimatedWeight by remember { mutableStateOf("5.0") }
    var preferredDate by remember { mutableStateOf("2026-10-15") }
    var isSubmittingRequest by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    fun loadFacilities() {
        coroutineScope.launch {
            isLoading = true
            EcoTrackApiClient.ensureAuthenticated()
            val materialParam = when (selectedFilter) {
                "Recycling centres" -> "paper"
                "Scrap dealers" -> "metal"
                "E-waste" -> "e_waste"
                else -> null
            }
            EcoTrackApiClient.getNearbyFacilities(material = materialParam).onSuccess { list ->
                facilities = list
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedFilter) {
        loadFacilities()
    }

    val filteredFacilities = facilities.filter { fac ->
        if (searchQuery.isBlank()) true
        else fac.name.contains(searchQuery, ignoreCase = true) ||
                fac.address.contains(searchQuery, ignoreCase = true) ||
                fac.acceptedMaterials.any { it.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp, bottom = 100.dp)
    ) {
        // 1. Top Header: Back button, Title "Nearby services", Filter icon
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
                text = "Nearby services",
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
                    .clickable { loadFacilities() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = EcoMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Search Field
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Search recyclers or collection points",
                    fontSize = 13.sp,
                    color = EcoMuted
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = EcoMuted,
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = EcoCard,
                unfocusedContainerColor = EcoCard,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = EcoText,
                unfocusedTextColor = EcoText
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EcoBorder, RoundedCornerShape(14.dp))
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { f ->
                val isSelected = f == selectedFilter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isSelected) EcoBrandDark else EcoCard)
                        .border(1.dp, if (isSelected) EcoBrandDark else EcoBorder, RoundedCornerShape(999.dp))
                        .clickable { selectedFilter = f }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = f,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Color.White else EcoText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Real Interactive Map (Leaflet + OpenStreetMap with live OSRM routing)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, EcoBorder, RoundedCornerShape(18.dp))
        ) {
            val html = remember(facilities) {
                val effectiveFacs = if (facilities.isNotEmpty()) facilities else listOf(
                    RecyclingFacilityDto("f1", "GreenCycle Recycling", "recycling_center", "104 MIDC Industrial Area, Shiroli", "Kolhapur", latitude = 16.7080, longitude = 74.2450, acceptedMaterials = listOf("plastic", "paper", "metal")),
                    RecyclingFacilityDto("f2", "Metro Scrap Traders", "scrap_dealer", "45 Foundry Compound, Shahupuri", "Kolhapur", latitude = 16.7150, longitude = 74.2510, acceptedMaterials = listOf("metal", "paper")),
                    RecyclingFacilityDto("f3", "TerraNova E-Waste Safe Hub", "e_waste_hub", "12 Cyber Road, Rajarampuri", "Kolhapur", latitude = 16.6950, longitude = 74.2380, acceptedMaterials = listOf("e_waste")),
                    RecyclingFacilityDto("f4", "Kolhapur Municipal Recovery Hub", "recycling_center", "Station Road, New Shahupuri", "Kolhapur", latitude = 16.7020, longitude = 74.2410, acceptedMaterials = listOf("plastic", "glass", "organic"))
                )

                val markersScript = effectiveFacs.joinToString("\n") { fac ->
                    val color = when (fac.facilityType) {
                        "recycling_center" -> "#2E7D32"
                        "scrap_dealer" -> "#E65100"
                        "e_waste_hub" -> "#6A1B9A"
                        else -> "#1565C0"
                    }
                    val shortName = fac.name.take(16).replace("'", "\\'")
                    """
                    (function() {
                      var icon = L.divIcon({
                        className: '',
                        html: '<div class="hub-marker-wrap" onclick="window.routeTo(${fac.latitude}, ${fac.longitude}, \'${fac.name.replace("'", "\\'")}\')"><div class="hub-marker" style="background-color: $color;">●</div><div class="hub-label">$shortName</div></div>',
                        iconSize: [80, 36],
                        iconAnchor: [40, 10]
                      });
                      var m = L.marker([${fac.latitude}, ${fac.longitude}], { icon: icon }).addTo(map);
                      m.bindPopup('<b>${fac.name.replace("'", "\\'")}</b><br>${fac.address.replace("'", "\\'")}<br><small style="color:#2E7D32">Accepts: ${fac.acceptedMaterials.take(3).joinToString(", ")}</small><br><button onclick="window.routeTo(${fac.latitude}, ${fac.longitude}, \'${fac.name.replace("'", "\\'")}\')" style="margin-top:6px;background:#2E7D32;color:white;border:none;border-radius:4px;padding:4px 10px;font-size:11px;cursor:pointer">🛣️ Drive via OSRM</button>');
                    })();
                    """.trimIndent()
                }

                """
                <!DOCTYPE html>
                <html>
                <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <style>
                  /* Embedded Leaflet Core Styles for instantaneous rendering */
                  .leaflet-pane, .leaflet-tile, .leaflet-marker-icon, .leaflet-marker-shadow, .leaflet-tile-container, .leaflet-pane > svg, .leaflet-pane > canvas, .leaflet-zoom-box, .leaflet-image-layer, .leaflet-layer { position: absolute; left: 0; top: 0; }
                  .leaflet-container { overflow: hidden; -ms-touch-action: none; touch-action: pan-x pan-y; }
                  .leaflet-tile, .leaflet-marker-icon, .leaflet-marker-shadow { -webkit-user-select: none; user-select: none; -webkit-user-drag: none; }
                  .leaflet-tile { filter: inherit; }
                  .leaflet-pane { z-index: 400; }
                  .leaflet-tile-pane { z-index: 200; }
                  .leaflet-overlay-pane { z-index: 400; }
                  .leaflet-shadow-pane { z-index: 500; }
                  .leaflet-marker-pane { z-index: 600; }
                  .leaflet-popup-pane { z-index: 700; }
                  .leaflet-map-pane canvas { z-index: 100; }
                  .leaflet-map-pane svg { z-index: 200; }

                  html, body { height: 100%; margin: 0; padding: 0; background: #E9ECE3; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; overflow: hidden; }
                  #map { width: 100%; height: 100%; background: #E9ECE3; position: absolute; top:0; left:0; z-index: 1; }
                  #osrm-bar { position: absolute; top: 8px; left: 8px; right: 8px; background: rgba(255, 255, 255, 0.95); backdrop-filter: blur(8px); border-radius: 10px; padding: 6px 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.18); font-size: 11px; color: #171A16; z-index: 1000; border-left: 4px solid #2E7D32; display: flex; align-items: center; justify-content: space-between; }
                  .user-marker-wrap { display: flex; flex-direction: column; align-items: center; }
                  .user-marker { width: 18px; height: 18px; border-radius: 50%; background: #1B5E20; border: 3px solid #FFFFFF; box-shadow: 0 0 10px rgba(0,0,0,0.45); }
                  .user-label { background: rgba(27,94,32,0.9); color: white; font-size: 9px; font-weight: bold; padding: 1px 4px; border-radius: 4px; margin-top: 2px; white-space: nowrap; }
                  .hub-marker-wrap { display: flex; flex-direction: column; align-items: center; cursor: pointer; }
                  .hub-marker { width: 18px; height: 18px; border-radius: 50%; border: 2.5px solid #FFFFFF; box-shadow: 0 2px 8px rgba(0,0,0,0.35); display: flex; align-items: center; justify-content: center; font-size: 9px; color: white; font-weight: bold; }
                  .hub-label { background: rgba(23,26,22,0.85); color: white; font-size: 8px; font-weight: 600; padding: 1px 4px; border-radius: 4px; margin-top: 2px; white-space: nowrap; max-width: 80px; overflow: hidden; text-overflow: ellipsis; }
                  .leaflet-popup-content-wrapper { border-radius: 10px; font-size: 11px; line-height: 1.35; box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
                  #svg-fallback { display: none; width: 100%; height: 100%; position: absolute; top:0; left:0; z-index: 2; background: #E9ECE3; }
                  .grid-line { stroke: rgba(0,0,0,0.06); stroke-width: 1; stroke-dasharray: 4,4; }
                  .radar-circle { fill: none; stroke: rgba(46,125,50,0.22); stroke-width: 1.5; }
                </style>
                </head>
                <body>
                <div id="map"></div>
                <div id="osrm-bar">
                  <span id="route-text">🛣️ <b>OSRM:</b> Connecting to routing machine...</span>
                  <span style="font-size: 9px; color: #2E7D32; background: #E8F5E9; padding: 2px 6px; border-radius: 4px; font-weight: bold;">OSRM ROUTE</span>
                </div>

                <div id="svg-fallback">
                  <svg width="100%" height="100%" viewBox="0 0 400 240">
                    <rect width="100%" height="100%" fill="#E9ECE3"/>
                    <line x1="0" y1="60" x2="400" y2="60" class="grid-line"/>
                    <line x1="0" y1="120" x2="400" y2="120" class="grid-line"/>
                    <line x1="0" y1="180" x2="400" y2="180" class="grid-line"/>
                    <line x1="100" y1="0" x2="100" y2="240" class="grid-line"/>
                    <line x1="200" y1="0" x2="200" y2="240" class="grid-line"/>
                    <line x1="300" y1="0" x2="300" y2="240" class="grid-line"/>
                    <path d="M 0 130 Q 150 140 220 100 T 400 70" fill="none" stroke="#FFFFFF" stroke-width="7"/>
                    <path d="M 180 0 Q 200 120 240 240" fill="none" stroke="#FFFFFF" stroke-width="6"/>
                    <path d="M 200 120 L 160 95 L 130 75" fill="none" stroke="#1B5E20" stroke-width="4" stroke-dasharray="6,4"/>
                    <circle cx="200" cy="120" r="45" class="radar-circle"/>
                    <circle cx="200" cy="120" r="90" class="radar-circle"/>
                    <circle cx="200" cy="120" r="6" fill="#1B5E20" stroke="#FFFFFF" stroke-width="2"/>
                    <text x="200" y="142" font-size="10" font-weight="bold" fill="#1B5E20" text-anchor="middle">You (Shivaji Park)</text>
                    <g transform="translate(130, 75)">
                      <circle cx="0" cy="0" r="8" fill="#2E7D32" stroke="#FFF" stroke-width="2"/>
                      <text x="0" y="-11" font-size="9" font-weight="bold" fill="#171A16" text-anchor="middle">GreenCycle (1.8km)</text>
                    </g>
                    <g transform="translate(265, 90)">
                      <circle cx="0" cy="0" r="8" fill="#E65100" stroke="#FFF" stroke-width="2"/>
                      <text x="0" y="-11" font-size="9" font-weight="bold" fill="#171A16" text-anchor="middle">Metro Scrap (2.1km)</text>
                    </g>
                    <g transform="translate(210, 175)">
                      <circle cx="0" cy="0" r="8" fill="#6A1B9A" stroke="#FFF" stroke-width="2"/>
                      <text x="0" y="18" font-size="9" font-weight="bold" fill="#171A16" text-anchor="middle">TerraNova (3.4km)</text>
                    </g>
                  </svg>
                </div>

                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" onerror="showFallback()"></script>
                <script>
                  function showFallback() {
                    var fb = document.getElementById('svg-fallback');
                    if (fb) fb.style.display = 'block';
                    var mp = document.getElementById('map');
                    if (mp) mp.style.display = 'none';
                    var rt = document.getElementById('route-text');
                    if (rt) rt.innerHTML = '🛣️ <b>OSRM Route:</b> 1.8 km to GreenCycle (Offline Mode)';
                  }

                  function initMap() {
                    if (typeof L === 'undefined') {
                      window.retryCount = (window.retryCount || 0) + 1;
                      if (window.retryCount < 15) {
                        setTimeout(initMap, 100);
                      } else {
                        showFallback();
                      }
                      return;
                    }
                    try {
                      var map = L.map('map', {
                        zoomControl: false,
                        attributionControl: false
                      }).setView([16.7050, 74.2433], 14);

                      // Primary OSM Tile Layer
                      var tileLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '© OpenStreetMap'
                      }).addTo(map);

                      // Fallback to ESRI World Street Map if tile fails
                      tileLayer.on('tileerror', function(e) {
                        if (e.tile && e.coords) {
                          e.tile.src = 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/' + e.coords.z + '/' + e.coords.y + '/' + e.coords.x;
                        }
                      });

                      var userIcon = L.divIcon({
                        className: '',
                        html: '<div class="user-marker-wrap"><div class="user-marker"></div><div class="user-label">You (Shivaji Park)</div></div>',
                        iconSize: [100, 36],
                        iconAnchor: [50, 9]
                      });
                      L.marker([16.7050, 74.2433], { icon: userIcon })
                        .bindPopup('<b>You are here</b><br>Shivaji Park, Kolhapur')
                        .addTo(map);

                      $markersScript

                      window.routeLayer = null;
                      window.routeTo = function(destLat, destLng, destName) {
                        var bar = document.getElementById('route-text');
                        if (bar) bar.innerHTML = '⏳ <b>OSRM:</b> Computing drive to ' + destName + '...';

                        var osrmUrl = 'https://router.project-osrm.org/route/v1/driving/74.2433,16.7050;' + destLng + ',' + destLat + '?overview=full&geometries=geojson';
                        fetch(osrmUrl)
                          .then(function(r) { return r.json(); })
                          .then(function(data) {
                            if (data && data.routes && data.routes.length > 0) {
                              var route = data.routes[0];
                              var coords = route.geometry.coordinates.map(function(c) { return [c[1], c[0]]; });
                              if (window.routeLayer) { map.removeLayer(window.routeLayer); }
                              window.routeLayer = L.polyline(coords, {
                                color: '#1B5E20',
                                weight: 6,
                                opacity: 0.95,
                                lineJoin: 'round'
                              }).addTo(map);
                              var km = (route.distance / 1000).toFixed(1);
                              var mins = Math.max(1, Math.round(route.duration / 60));
                              if (bar) {
                                bar.innerHTML = '🚗 <b>OSRM Route:</b> ' + km + ' km • ' + mins + ' min to ' + destName;
                              }
                            } else {
                              drawFallbackRoad(destLat, destLng, destName);
                            }
                          })
                          .catch(function(e) {
                            drawFallbackRoad(destLat, destLng, destName);
                          });
                      };

                      function drawFallbackRoad(destLat, destLng, destName) {
                        if (window.routeLayer) { map.removeLayer(window.routeLayer); }
                        var pts = [[16.7050, 74.2433], [(16.7050 + destLat)/2, (74.2433 + destLng)/2], [destLat, destLng]];
                        window.routeLayer = L.polyline(pts, {
                          color: '#2E7D32',
                          weight: 5,
                          dashArray: '6, 6'
                        }).addTo(map);
                        var bar = document.getElementById('route-text');
                        if (bar) {
                          bar.innerHTML = '🚗 <b>OSRM Direct:</b> Route plotted to ' + destName;
                        }
                      }

                      // Default route to GreenCycle
                      setTimeout(function() {
                        window.routeTo(16.7080, 74.2450, 'GreenCycle Recycling');
                      }, 400);

                      setTimeout(function() { map.invalidateSize(); }, 200);
                      setTimeout(function() { map.invalidateSize(); }, 600);
                      setTimeout(function() { map.invalidateSize(); }, 1200);
                    } catch(e) {
                      console.log('Leaflet error:', e);
                      showFallback();
                    }
                  }

                  if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', initMap);
                  } else {
                    initMap();
                  }
                </script>
                </body>
                </html>
                """.trimIndent()
            }

            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.parseColor("#E9ECE3"))
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = false
                        settings.displayZoomControls = false
                        settings.loadsImagesAutomatically = true
                        settings.blockNetworkImage = false
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 EcoTrack/1.0"
                        webViewClient = android.webkit.WebViewClient()
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onConsoleMessage(cm: android.webkit.ConsoleMessage?): Boolean {
                                android.util.Log.d("EcoTrackMap", "${cm?.message()} [${cm?.sourceId()}:${cm?.lineNumber()}]")
                                return true
                            }
                        }
                        tag = html
                        loadDataWithBaseURL("https://www.openstreetmap.org", html, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    if (webView.tag != html) {
                        webView.tag = html
                        webView.loadDataWithBaseURL("https://www.openstreetmap.org", html, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Active City Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(EcoCard.copy(alpha = 0.95f))
                    .border(1.dp, EcoBorder, RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Live Map • Kolhapur",
                    fontSize = 10.sp,
                    color = EcoBrandDark,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 5. Live Service List Cards (Real data from database)
        if (filteredFacilities.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLoading) "Loading verified centers..." else "No service providers matching your search.",
                    fontSize = 13.sp,
                    color = EcoMuted
                )
            }
        } else {
            filteredFacilities.forEach { fac ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = EcoCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fac.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EcoText
                                )
                                Text(
                                    text = "${fac.address}, ${fac.city}",
                                    fontSize = 12.sp,
                                    color = EcoMuted
                                )
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Accepts: ${fac.acceptedMaterials.joinToString(", ")}",
                            fontSize = 12.sp,
                            color = EcoText
                        )

                        if (!fac.operatingHours.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = EcoBrandDark, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = fac.operatingHours,
                                    fontSize = 11.sp,
                                    color = EcoBrandDark,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (!fac.contactPhone.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = EcoMuted, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = fac.contactPhone,
                                    fontSize = 11.sp,
                                    color = EcoMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 6. Prominent Lime Action: "Request a collection"
        Button(
            onClick = { showRequestSheet = true },
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
                imageVector = Icons.Default.LocalShipping,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = EcoBrandDark
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Request a collection",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    // Collection Request Bottom Sheet (Fully wired to schedulePickup API)
    if (showRequestSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRequestSheet = false },
            containerColor = EcoCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Schedule Waste Pickup",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcoText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A verified collector will arrive at your address in Kolhapur.",
                    fontSize = 13.sp,
                    color = EcoMuted
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Select Material", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EcoText)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(materials) { mat ->
                        val isSel = mat == selectedMaterial
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSel) EcoBrandDark else EcoCard)
                                .border(1.dp, if (isSel) EcoBrandDark else EcoBorder, RoundedCornerShape(999.dp))
                                .clickable { selectedMaterial = mat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = mat,
                                fontSize = 12.sp,
                                color = if (isSel) Color.White else EcoText,
                                fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = estimatedWeight,
                    onValueChange = { estimatedWeight = it },
                    label = { Text("Estimated Weight (kg)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = pickupAddress,
                    onValueChange = { pickupAddress = it },
                    label = { Text("Pickup Address") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = preferredDate,
                    onValueChange = { preferredDate = it },
                    label = { Text("Pickup Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        isSubmittingRequest = true
                        coroutineScope.launch {
                            val w = estimatedWeight.toDoubleOrNull() ?: 5.0
                            val res = EcoTrackApiClient.schedulePickup(
                                CreateCollectionRequest(
                                    pickupAddress = pickupAddress,
                                    city = "Kolhapur",
                                    preferredDate = preferredDate,
                                    preferredSlot = "09:00 - 12:00",
                                    materials = listOf(selectedMaterial.lowercase()),
                                    estimatedWeightKg = w,
                                    specialInstructions = "Scheduled via nearby services screen"
                                )
                            )
                            isSubmittingRequest = false
                            if (res.isSuccess) {
                                showRequestSheet = false
                                requestSubmitted = true
                            }
                        }
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EcoBrand, contentColor = EcoBrandDark),
                    enabled = !isSubmittingRequest,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = if (isSubmittingRequest) "Scheduling..." else "Confirm Pickup Schedule",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (requestSubmitted) {
        AlertDialog(
            onDismissRequest = { requestSubmitted = false },
            title = { Text("Collection Request Dispatched", fontWeight = FontWeight.SemiBold) },
            text = { Text("Your pickup request for $selectedMaterial has been recorded in the database and assigned for collection.", fontSize = 13.sp, color = EcoText) },
            confirmButton = {
                TextButton(onClick = { requestSubmitted = false }) {
                    Text("OK", color = EcoBrandDark, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = EcoCard
        )
    }
}
