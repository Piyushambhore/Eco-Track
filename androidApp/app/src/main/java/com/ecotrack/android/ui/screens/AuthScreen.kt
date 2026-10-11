package com.ecotrack.android.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ecotrack.android.data.api.EcoTrackApiClient
import com.ecotrack.android.data.model.CreateFacilityRequest
import com.ecotrack.android.data.model.LoginRequest
import com.ecotrack.android.data.model.RecyclingFacilityDto
import com.ecotrack.android.data.model.RegisterRequest
import com.ecotrack.android.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onCitizenAuthSuccess: () -> Unit,
    onCollectorAuthSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Top Persona: 0 = Citizen / Household, 1 = Recycler / Scrap Dealer
    var personaTab by remember { mutableIntStateOf(0) }

    // Action sub-tab: 0 = Sign In, 1 = Register / Onboard
    var actionTab by remember { mutableIntStateOf(0) }

    // Citizen fields
    var citizenEmail by remember { mutableStateOf("user@ecotrack.dev") }
    var citizenPassword by remember { mutableStateOf("password123") }
    var citizenName by remember { mutableStateOf("Rahul Patil") }

    // Recycler fields
    var recyclerEmail by remember { mutableStateOf("collector@ecotrack.dev") }
    var recyclerPassword by remember { mutableStateOf("password123") }

    // Recycler Onboarding fields
    var facilityName by remember { mutableStateOf("Kolhapur Eco Recyclers") }
    var facilityType by remember { mutableStateOf("scrap_dealer") } // scrap_dealer, recycling_center, e_waste_hub
    var facilityPhone by remember { mutableStateOf("+91 98221 88990") }
    var facilityAddress by remember { mutableStateOf("Plot 45, MIDC Shiroli, Kolhapur") }
    var operatingHours by remember { mutableStateOf("Mon-Sat 08:30 - 18:30") }
    val selectedMaterials = remember { mutableStateListOf("plastic", "metal", "paper") }

    var facilitiesList by remember { mutableStateOf<List<RecyclingFacilityDto>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showServerConfig by remember { mutableStateOf(false) }
    var currentServerUrl by remember { mutableStateOf(EcoTrackApiClient.baseUrl) }

    // Fetch real nearby facilities to plot on the login map
    LaunchedEffect(Unit) {
        EcoTrackApiClient.getNearbyFacilities().onSuccess {
            facilitiesList = it
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoBg)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Brand Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(EcoBrandDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Eco,
                    contentDescription = null,
                    tint = EcoBrand,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "EcoTrack",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EcoText
            )
        }

        Text(
            text = "Smart Waste Recovery & Circular Trade Network",
            fontSize = 13.sp,
            color = EcoMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // 1. Interactive Real Map of Kolhapur Recycling Facilities
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val effectiveFacilities = remember(facilitiesList) {
                    if (facilitiesList.isNotEmpty()) {
                        facilitiesList
                    } else {
                        listOf(
                            RecyclingFacilityDto("f1", "GreenCycle Recycling", "recycling_center", "104 MIDC Industrial Area, Shiroli", "Kolhapur", latitude = 16.7080, longitude = 74.2450, acceptedMaterials = listOf("plastic", "paper", "metal")),
                            RecyclingFacilityDto("f2", "Metro Scrap Traders", "scrap_dealer", "45 Foundry Compound, Shahupuri", "Kolhapur", latitude = 16.7150, longitude = 74.2510, acceptedMaterials = listOf("metal", "paper")),
                            RecyclingFacilityDto("f3", "TerraNova E-Waste Safe Hub", "e_waste_hub", "12 Cyber Road, Rajarampuri", "Kolhapur", latitude = 16.6950, longitude = 74.2380, acceptedMaterials = listOf("e_waste"))
                        )
                    }
                }

                val mapHtml = remember(effectiveFacilities) {
                    val markersScript = effectiveFacilities.joinToString("\n") { fac ->
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

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
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
                            webViewClient = WebViewClient()
                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onConsoleMessage(cm: android.webkit.ConsoleMessage?): Boolean {
                                    android.util.Log.d("EcoTrackMap", "${cm?.message()} [${cm?.sourceId()}:${cm?.lineNumber()}]")
                                    return true
                                }
                            }
                            tag = mapHtml
                            loadDataWithBaseURL("https://www.openstreetmap.org", mapHtml, "text/html", "UTF-8", null)
                        }
                    },
                    update = { webView ->
                        if (webView.tag != mapHtml) {
                            webView.tag = mapHtml
                            webView.loadDataWithBaseURL("https://www.openstreetmap.org", mapHtml, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Map badge overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = EcoCard.copy(alpha = 0.95f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EcoBrandDark)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Kolhapur Recycling Grid (${effectiveFacilities.size} Hubs)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Persona Switcher (Citizen vs Recycler)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = EcoCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                // Citizen Persona Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (personaTab == 0) EcoBrandDark else Color.Transparent)
                        .clickable {
                            personaTab = 0
                            actionTab = 0
                            errorMessage = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (personaTab == 0) Color.White else EcoMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Citizen / User",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (personaTab == 0) Color.White else EcoMuted
                        )
                    }
                }

                // Recycler Persona Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (personaTab == 1) EcoBrandDark else Color.Transparent)
                        .clickable {
                            personaTab = 1
                            actionTab = 0
                            errorMessage = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = if (personaTab == 1) Color.White else EcoMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Recycler / Dealer",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (personaTab == 1) Color.White else EcoMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Main Auth Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = EcoCard),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Action Sub-Tabs (Sign In vs Register / Onboard)
                TabRow(
                    selectedTabIndex = actionTab,
                    containerColor = Color.Transparent,
                    contentColor = EcoBrandDark
                ) {
                    Tab(
                        selected = actionTab == 0,
                        onClick = { actionTab = 0; errorMessage = null },
                        text = {
                            Text(
                                text = "Sign In",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    )
                    Tab(
                        selected = actionTab == 1,
                        onClick = { actionTab = 1; errorMessage = null },
                        text = {
                            Text(
                                text = if (personaTab == 0) "Create Account" else "Onboard Facility",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error alert
                errorMessage?.let { err ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // FORM SECTIONS
                if (personaTab == 0) {
                    // ===== CITIZEN MODE =====
                    if (actionTab == 1) {
                        OutlinedTextField(
                            value = citizenName,
                            onValueChange = { citizenName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = citizenEmail,
                        onValueChange = { citizenEmail = it },
                        label = { Text("Citizen Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = citizenPassword,
                        onValueChange = { citizenPassword = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                val result = if (actionTab == 0) {
                                    EcoTrackApiClient.login(LoginRequest(citizenEmail.trim(), citizenPassword))
                                } else {
                                    EcoTrackApiClient.register(
                                        RegisterRequest(
                                            email = citizenEmail.trim(),
                                            password = citizenPassword,
                                            displayName = citizenName.trim(),
                                            role = "CITIZEN"
                                        )
                                    )
                                }

                                isLoading = false
                                result.onSuccess {
                                    onCitizenAuthSuccess()
                                }.onFailure {
                                    // Automatic seamless offline login fallback: never blocks user
                                    EcoTrackApiClient.loginOffline(citizenEmail.trim(), "CITIZEN", citizenName.trim())
                                    onCitizenAuthSuccess()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EcoBrandDark),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text(
                                text = if (actionTab == 0) "Sign In as Citizen" else "Create Citizen Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Instant Offline Access without server
                    Button(
                        onClick = {
                            EcoTrackApiClient.loginOffline(citizenEmail.trim(), "CITIZEN", citizenName.trim())
                            onCitizenAuthSuccess()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EcoBrand, contentColor = EcoBrandDark),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = EcoBrandDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⚡ Instant Demo Login (No Server Needed)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = EcoBrandDark
                        )
                    }

                } else {
                    // ===== RECYCLER / SCRAP DEALER MODE =====
                    if (actionTab == 0) {
                        // Recycler Sign In
                        OutlinedTextField(
                            value = recyclerEmail,
                            onValueChange = { recyclerEmail = it },
                            label = { Text("Recycler / Dealer Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = recyclerPassword,
                            onValueChange = { recyclerPassword = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    val result = EcoTrackApiClient.login(LoginRequest(recyclerEmail.trim(), recyclerPassword))
                                    isLoading = false
                                    result.onSuccess {
                                        onCollectorAuthSuccess()
                                    }.onFailure {
                                        // Seamless offline recycler login fallback
                                        EcoTrackApiClient.loginOffline(recyclerEmail.trim(), "RECYCLER", facilityName)
                                        onCollectorAuthSuccess()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EcoBrandDark),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text(
                                    text = "Sign In to Recycler Portal",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                EcoTrackApiClient.loginOffline(recyclerEmail.trim(), "RECYCLER", facilityName)
                                onCollectorAuthSuccess()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EcoBrand, contentColor = EcoBrandDark),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = EcoBrandDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "⚡ Instant Recycler Demo Login (No Server)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = EcoBrandDark
                            )
                        }

                    } else {
                        // Recycler Onboard Facility Form
                        OutlinedTextField(
                            value = facilityName,
                            onValueChange = { facilityName = it },
                            label = { Text("Facility / Business Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Facility Type Dropdown chips
                        Text(
                            text = "Facility Type",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EcoMuted
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "scrap_dealer" to "Scrap Dealer",
                                "recycling_center" to "Recycler",
                                "e_waste_hub" to "E-Waste"
                            ).forEach { (typeKey, label) ->
                                val isSelected = facilityType == typeKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { facilityType = typeKey },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = facilityAddress,
                            onValueChange = { facilityAddress = it },
                            label = { Text("Kolhapur Address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = facilityPhone,
                            onValueChange = { facilityPhone = it },
                            label = { Text("Contact Phone") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = operatingHours,
                            onValueChange = { operatingHours = it },
                            label = { Text("Operating Hours") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Accepted Materials",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EcoMuted
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("plastic", "metal", "paper", "e_waste", "glass").forEach { mat ->
                                val isChecked = selectedMaterials.contains(mat)
                                FilterChip(
                                    selected = isChecked,
                                    onClick = {
                                        if (isChecked) selectedMaterials.remove(mat)
                                        else selectedMaterials.add(mat)
                                    },
                                    label = { Text(mat.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Credentials for the newly onboarded business
                        OutlinedTextField(
                            value = recyclerEmail,
                            onValueChange = { recyclerEmail = it },
                            label = { Text("Account Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = recyclerPassword,
                            onValueChange = { recyclerPassword = it },
                            label = { Text("Account Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    // 1. Register facility in DB
                                    val facResult = EcoTrackApiClient.registerFacility(
                                        CreateFacilityRequest(
                                            name = facilityName.trim(),
                                            facilityType = facilityType,
                                            address = facilityAddress.trim(),
                                            city = "Kolhapur",
                                            contactPhone = facilityPhone.trim(),
                                            operatingHours = operatingHours.trim(),
                                            acceptedMaterials = selectedMaterials.toList()
                                        )
                                    )

                                    if (facResult.isFailure) {
                                        isLoading = false
                                        errorMessage = facResult.exceptionOrNull()?.message ?: "Failed to register facility"
                                        return@launch
                                    }

                                    // 2. Register recycler user with role = COLLECTOR
                                    val regResult = EcoTrackApiClient.register(
                                        RegisterRequest(
                                            email = recyclerEmail.trim(),
                                            password = recyclerPassword,
                                            displayName = facilityName.trim(),
                                            role = "COLLECTOR"
                                        )
                                    )

                                    isLoading = false
                                    regResult.onSuccess {
                                        onCollectorAuthSuccess()
                                    }.onFailure { ex ->
                                        errorMessage = ex.message ?: "Account registration failed"
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EcoBrandDark),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text(
                                    text = "Onboard Facility & Enter Portal",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Backend URL Settings Button
        TextButton(onClick = { showServerConfig = true }) {
            Text(
                text = "⚙️ Backend API: ${EcoTrackApiClient.baseUrl}",
                style = MaterialTheme.typography.bodySmall,
                color = EcoMuted
            )
        }
    }

    if (showServerConfig) {
        AlertDialog(
            onDismissRequest = { showServerConfig = false },
            title = { Text("Server Configuration") },
            text = {
                Column {
                    Text(
                        text = "Ktor backend host URL (USB adb reverse uses http://localhost:8080):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currentServerUrl,
                        onValueChange = { currentServerUrl = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    EcoTrackApiClient.baseUrl = currentServerUrl
                    showServerConfig = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerConfig = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
