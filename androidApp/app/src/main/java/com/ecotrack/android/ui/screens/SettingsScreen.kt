package com.ecotrack.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecotrack.android.data.api.EcoTrackApiClient
import com.ecotrack.android.data.model.AwsArchitectureOverviewDto
import com.ecotrack.android.data.model.AwsServiceStatusDto
import com.ecotrack.android.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var awsOverview by remember { mutableStateOf<AwsArchitectureOverviewDto?>(null) }
    var isLoadingAws by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("Connected to Localhost / LAN") }
    var isPinging by remember { mutableStateOf(false) }

    // User preference toggles
    var pushNotificationsEnabled by remember { mutableStateOf(true) }
    var highPrecisionGpsEnabled by remember { mutableStateOf(true) }
    var offlineSyncEnabled by remember { mutableStateOf(true) }
    var darkEcoMode by remember { mutableStateOf(false) }

    fun refreshAwsStatus() {
        coroutineScope.launch {
            isLoadingAws = true
            EcoTrackApiClient.getAwsStatus().onSuccess { overview ->
                awsOverview = overview
                connectionStatus = "Connected (200 OK • Live Cloud Hub)"
            }.onFailure {
                connectionStatus = "Connected to local node (AWS Cloud Mock active)"
            }
            isLoadingAws = false
        }
    }

    LaunchedEffect(Unit) {
        refreshAwsStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Settings & AWS Cloud",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoText
                        )
                        Text(
                            text = "Cloud configuration & system preferences",
                            fontSize = 12.sp,
                            color = EcoMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EcoText
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refreshAwsStatus() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = EcoBrandDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EcoCard)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EcoBg)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // 1. AWS Cloud Architecture Hero Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF232F3E)), // Official AWS Dark Blue
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF9900)), // Official AWS Squid Ink Orange
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = "AWS",
                                    tint = Color(0xFF232F3E),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AWS Cloud Architecture",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Region: ${awsOverview?.region ?: "ap-south-1 (Mumbai)"}",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFF107C41),
                            shape = RoundedCornerShape(99.dp)
                        ) {
                            Text(
                                text = "LIVE READY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "EcoTrack leverages Amazon Web Services for end-to-end circular waste traceability, AI visual classification, real-time logistics notifications, and carbon footprint telemetry.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4 Core AWS Services Integrated
                    val services = awsOverview?.services ?: listOf(
                        AwsServiceStatusDto("Object Storage", "Amazon S3", "ACTIVE", "ap-south-1", "Encrypted storage for waste scan photos and collection certificates.", 24, true),
                        AwsServiceStatusDto("Computer Vision AI", "Amazon Rekognition", "ACTIVE", "ap-south-1", "Deep learning inference identifying municipal recyclables & contamination.", 48, true),
                        AwsServiceStatusDto("Messaging & Alerts", "Amazon SNS", "ACTIVE", "ap-south-1", "Real-time alerts for scheduled collections & scrap bids.", 18, true),
                        AwsServiceStatusDto("Emissions Telemetry", "Amazon CloudWatch", "ACTIVE", "ap-south-1", "Carbon offset metrics, KPI dashboards & alarms.", 15, true)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        services.forEach { s ->
                            AwsServiceRow(s)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            isPinging = true
                            refreshAwsStatus()
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(800)
                                isPinging = false
                            }
                        },
                        shape = RoundedCornerShape(99.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9900),
                            contentColor = Color(0xFF232F3E)
                        ),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPinging) "Testing AWS Services Ping..." else "Test AWS Services Connection",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Connectivity & API Gateway Status
            Text(
                text = "Backend Connectivity",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = EcoText
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EcoCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Active Node Endpoint",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EcoText
                            )
                            Text(
                                text = "http://127.0.0.1:8080 (ADB Reverse) • 10.236.130.13:8080 (LAN)",
                                fontSize = 11.sp,
                                color = EcoMuted
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(EcoSuccess)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Status: $connectionStatus",
                        fontSize = 12.sp,
                        color = EcoBrandDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. User Preferences & System Toggles
            Text(
                text = "Application Preferences",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = EcoText
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EcoCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SettingToggleRow(
                        icon = Icons.Default.NotificationsActive,
                        title = "Push & SMS Notifications",
                        subtitle = "Alerts for pickup schedule & dealer bids (Amazon SNS)",
                        checked = pushNotificationsEnabled,
                        onCheckedChange = { pushNotificationsEnabled = it }
                    )
                    HorizontalDivider(color = EcoBorder, modifier = Modifier.padding(vertical = 8.dp))
                    SettingToggleRow(
                        icon = Icons.Default.GpsFixed,
                        title = "High-Accuracy GPS Logistics",
                        subtitle = "Precision location for facility search in Kolhapur region",
                        checked = highPrecisionGpsEnabled,
                        onCheckedChange = { highPrecisionGpsEnabled = it }
                    )
                    HorizontalDivider(color = EcoBorder, modifier = Modifier.padding(vertical = 8.dp))
                    SettingToggleRow(
                        icon = Icons.Default.Sync,
                        title = "Offline Resilient Sync",
                        subtitle = "Cache scans when network is disconnected & upload on reconnect",
                        checked = offlineSyncEnabled,
                        onCheckedChange = { offlineSyncEnabled = it }
                    )
                    HorizontalDivider(color = EcoBorder, modifier = Modifier.padding(vertical = 8.dp))
                    SettingToggleRow(
                        icon = Icons.Default.DarkMode,
                        title = "Dark / Eco OLED Mode",
                        subtitle = "High contrast energy-saving interface theme",
                        checked = darkEcoMode,
                        onCheckedChange = { darkEcoMode = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. About & Hackathon Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EcoTint),
                border = androidx.compose.foundation.BorderStroke(1.dp, EcoTintBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = EcoBrandDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EcoTrack Circular Waste Platform",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoBrandDark
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Built for AWS Hackathon 2026. Empowering citizens, aggregators, and municipal corporations in Kolhapur with verifiable zero-waste circular loops.",
                        fontSize = 11.sp,
                        color = EcoText.copy(alpha = 0.85f),
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "App Version: 1.0.0 (Production Release)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun AwsServiceRow(service: AwsServiceStatusDto) {
    Surface(
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = service.awsProduct,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9900)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${service.serviceName}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = service.details,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFF107C41).copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${service.latencyMs}ms",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(EcoTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EcoBrandDark,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcoText
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = EcoMuted,
                    lineHeight = 14.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = EcoBrandDark,
                uncheckedThumbColor = EcoMuted,
                uncheckedTrackColor = EcoBorder
            )
        )
    }
}
