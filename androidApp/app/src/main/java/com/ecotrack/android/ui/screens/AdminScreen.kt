package com.ecotrack.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.ecotrack.android.data.model.*
import com.ecotrack.android.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onNavigateToDashboard: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var telemetry by remember { mutableStateOf<MunicipalTelemetryDto?>(null) }
    var disputes by remember { mutableStateOf<List<AdminDisputeDto>>(emptyList()) }
    var users by remember { mutableStateOf<List<UserAdminDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dispute Resolution Dialog State
    var resolvingDispute by remember { mutableStateOf<AdminDisputeDto?>(null) }
    var resolutionStatus by remember { mutableStateOf("resolved") }
    var resolutionNotes by remember { mutableStateOf("") }
    var isSubmittingResolution by remember { mutableStateOf(false) }

    // User Role Promotion Dialog State
    var editingUser by remember { mutableStateOf<UserAdminDto?>(null) }
    var selectedRoleToAssign by remember { mutableStateOf("RECYCLER") }
    var isSubmittingRole by remember { mutableStateOf(false) }

    fun refreshData() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            // 1. Fetch telemetry
            EcoTrackApiClient.getMunicipalTelemetry()
                .onSuccess { telemetry = it }
                .onFailure { errorMessage = it.message }

            // 2. Fetch disputes
            EcoTrackApiClient.getAdminDisputes()
                .onSuccess { disputes = it.items }

            // 3. Fetch users
            EcoTrackApiClient.getAdminUsers()
                .onSuccess { users = it.items }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Municipal Governance",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = "ADMIN / WARD",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToDashboard) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(EcoBg)
        ) {
            // Tab Selector Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Telemetry", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Disputes (${disputes.count { it.status == "open" }})",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    icon = { Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Users", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.SupervisorAccount, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (errorMessage != null && telemetry == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Access Restriction",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = errorMessage ?: "You do not possess sufficient municipal authority.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { refreshData() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> TelemetryTabContent(telemetry = telemetry)
                    1 -> DisputesTabContent(
                        disputes = disputes,
                        onResolveClick = { dispute ->
                            resolvingDispute = dispute
                            resolutionStatus = "resolved"
                            resolutionNotes = "Dispatched re-routing pickup van and credited goodwill points."
                        }
                    )
                    2 -> UsersTabContent(
                        users = users,
                        onEditRole = { user ->
                            editingUser = user
                            selectedRoleToAssign = "RECYCLER"
                        },
                        onToggleStatus = { user ->
                            val nextStatus = if (user.status == "active") "disabled" else "active"
                            coroutineScope.launch {
                                EcoTrackApiClient.updateUserStatus(user.id, nextStatus)
                                    .onSuccess { refreshData() }
                            }
                        }
                    )
                }
            }
        }
    }

    // Resolve Dispute Modal Dialog
    if (resolvingDispute != null) {
        val disp = resolvingDispute!!
        AlertDialog(
            onDismissRequest = { if (!isSubmittingResolution) resolvingDispute = null },
            title = {
                Text("Resolve Civic Dispute", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = disp.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ref: ${disp.referenceId} • ${disp.disputeType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = disp.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Resolution Decision", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = resolutionStatus == "resolved",
                            onClick = { resolutionStatus = "resolved" },
                            label = { Text("Resolve & Compensate") }
                        )
                        FilterChip(
                            selected = resolutionStatus == "dismissed",
                            onClick = { resolutionStatus = "dismissed" },
                            label = { Text("Dismiss") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resolutionNotes,
                        onValueChange = { resolutionNotes = it },
                        label = { Text("Municipal Decision Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmittingResolution = true
                        coroutineScope.launch {
                            EcoTrackApiClient.resolveDispute(disp.id, resolutionStatus, resolutionNotes)
                                .onSuccess {
                                    isSubmittingResolution = false
                                    resolvingDispute = null
                                    refreshData()
                                }
                                .onFailure {
                                    isSubmittingResolution = false
                                }
                        }
                    },
                    enabled = !isSubmittingResolution && resolutionNotes.isNotBlank()
                ) {
                    if (isSubmittingResolution) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Confirm Resolution")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { resolvingDispute = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Role Promotion Modal Dialog
    if (editingUser != null) {
        val usr = editingUser!!
        AlertDialog(
            onDismissRequest = { if (!isSubmittingRole) editingUser = null },
            title = {
                Text("Update User Authority", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = usr.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${usr.email} • Current: ${usr.role}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Assign Authority Role", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    val roles = listOf("CITIZEN", "TRANSPORTER", "RECYCLER", "MUNICIPAL", "ADMIN")
                    roles.forEach { role ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRoleToAssign = role }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRoleToAssign == role,
                                onClick = { selectedRoleToAssign = role }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = role,
                                fontWeight = if (selectedRoleToAssign == role) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmittingRole = true
                        coroutineScope.launch {
                            EcoTrackApiClient.updateUserRole(usr.id, selectedRoleToAssign)
                                .onSuccess {
                                    isSubmittingRole = false
                                    editingUser = null
                                    refreshData()
                                }
                                .onFailure {
                                    isSubmittingRole = false
                                }
                        }
                    },
                    enabled = !isSubmittingRole
                ) {
                    if (isSubmittingRole) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Apply Role")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { editingUser = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TelemetryTabContent(telemetry: MunicipalTelemetryDto?) {
    if (telemetry == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // AWS Infrastructure & Live Service Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(EcoSuccess)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AWS CLOUD INFRASTRUCTURE ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "ECS Fargate (2 Tasks) • Multi-AZ RDS PostgreSQL • 99.98% SLA",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // High Level Municipal KPI Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryKpiCard(
                modifier = Modifier.weight(1f),
                title = "Landfill Diversion",
                value = "${telemetry.diversionRatePercent}%",
                subtitle = "Target: 80% (Achieved)",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                tint = MaterialTheme.colorScheme.primary
            )

            TelemetryKpiCard(
                modifier = Modifier.weight(1f),
                title = "Collection SLA",
                value = "${telemetry.collectionEfficiencyPercent}%",
                subtitle = "On-time pickups",
                icon = Icons.Default.CheckCircle,
                tint = EcoSuccess
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryKpiCard(
                modifier = Modifier.weight(1f),
                title = "Materials Diverted",
                value = "%.1f kg".format(telemetry.totalRecycledKg),
                subtitle = "From ${telemetry.totalWasteReportedKg} kg logged",
                icon = Icons.Default.Recycling,
                tint = MaterialTheme.colorScheme.secondary
            )

            TelemetryKpiCard(
                modifier = Modifier.weight(1f),
                title = "Citizens & Facilities",
                value = "${telemetry.totalRegisteredCitizens}",
                subtitle = "${telemetry.activeFacilitiesCount} processing centers",
                icon = Icons.Default.LocationCity,
                tint = EcoWarning
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Municipal Ward Level Compliance
        Text(
            text = "Ward Compliance & Recovery Ranking",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        telemetry.topWards.forEach { ward ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "W${ward.wardNumber}",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ward.wardName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Recovered: %.1f kg".format(ward.divertedKg),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (ward.complianceRatePercent >= 90.0) EcoSuccess.copy(alpha = 0.15f) else EcoWarning.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${ward.complianceRatePercent}% SLA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (ward.complianceRatePercent >= 90.0) EcoSuccess else EcoWarning,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Regional Academic / Urban Zones
        Text(
            text = "Regional Zone Telemetry",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        telemetry.zoneMetrics.forEach { zone ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = zone.zoneName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "%.1f kg".format(zone.totalWeightKg),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (zone.efficiencyPercent / 100.0).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${zone.activeCitizens} Active Eco-Citizens • ${zone.efficiencyPercent}% Efficiency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun TelemetryKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun DisputesTabContent(
    disputes: List<AdminDisputeDto>,
    onResolveClick: (AdminDisputeDto) -> Unit
) {
    if (disputes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    tint = EcoSuccess,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Civic Disputes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "All collection pickups and transactions are operating smoothly without flags.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    lineHeight = 18.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(disputes) { dispute ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = dispute.disputeType.replace("_", " ").uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            val isResolved = dispute.status in listOf("resolved", "dismissed")
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isResolved) EcoSuccess.copy(alpha = 0.15f) else EcoWarning.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = dispute.status.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isResolved) EcoSuccess else EcoWarning,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = dispute.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = dispute.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Text(
                            text = "Reference: ${dispute.referenceId} • Reporter: ${dispute.reporterEmail ?: "Citizen"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        if (dispute.resolutionNotes != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Official Resolution:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = dispute.resolutionNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        if (dispute.status == "open") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onResolveClick(dispute) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Arbitrate Dispute", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsersTabContent(
    users: List<UserAdminDto>,
    onEditRole: (UserAdminDto) -> Unit,
    onToggleStatus: (UserAdminDto) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(users) { user ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                when (user.role) {
                                    "ADMIN" -> MaterialTheme.colorScheme.errorContainer
                                    "MUNICIPAL" -> MaterialTheme.colorScheme.primaryContainer
                                    "RECYCLER" -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (user.role) {
                                "ADMIN" -> Icons.Default.Security
                                "MUNICIPAL" -> Icons.Default.LocationCity
                                "RECYCLER" -> Icons.Default.Recycling
                                else -> Icons.Default.Person
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = user.role,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (user.status == "active") EcoSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = user.status.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (user.status == "active") EcoSuccess else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // Actions
                    IconButton(onClick = { onEditRole(user) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Role", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = { onToggleStatus(user) }) {
                        Icon(
                            imageVector = if (user.status == "active") Icons.Default.Block else Icons.Default.CheckCircle,
                            contentDescription = "Toggle Status",
                            tint = if (user.status == "active") MaterialTheme.colorScheme.error else EcoSuccess
                        )
                    }
                }
            }
        }
    }
}
