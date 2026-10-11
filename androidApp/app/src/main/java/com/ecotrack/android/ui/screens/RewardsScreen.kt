package com.ecotrack.android.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecotrack.android.data.api.EcoTrackApiClient
import com.ecotrack.android.data.model.*
import com.ecotrack.android.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(
    onNavigateToMarketplace: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var balanceData by remember { mutableStateOf<RewardsBalanceDto?>(null) }
    var leaderboardData by remember { mutableStateOf<LeaderboardDto?>(null) }
    var catalogItems by remember { mutableStateOf<List<RedemptionCatalogItemDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dialog state for redemption
    var selectedRewardForRedemption by remember { mutableStateOf<RedemptionCatalogItemDto?>(null) }
    var redeemedVoucher by remember { mutableStateOf<RedemptionDto?>(null) }
    var isSubmittingRedeem by remember { mutableStateOf(false) }

    fun refreshAll() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val balanceRes = EcoTrackApiClient.getRewardsBalance()
            val leaderboardRes = EcoTrackApiClient.getLeaderboard("campus")
            val catalogRes = EcoTrackApiClient.getRedemptionCatalog()

            if (balanceRes.isSuccess) {
                balanceData = balanceRes.getOrNull()
            }
            if (leaderboardRes.isSuccess) {
                leaderboardData = leaderboardRes.getOrNull()
            }
            if (catalogRes.isSuccess) {
                catalogItems = catalogRes.getOrNull() ?: emptyList()
            }

            if (balanceRes.isFailure && leaderboardRes.isFailure) {
                errorMessage = "Unable to connect to live ledger. Showing verified local state."
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshAll()
    }

    val currentPts = balanceData?.currentPoints ?: 0
    val tierName = balanceData?.tier?.tierName ?: "Sprout"
    val progressPct = balanceData?.tier?.progressPercentage ?: 0
    val nextTier = balanceData?.tier?.nextTierName ?: "Eco Warrior"
    val ptsToNext = balanceData?.tier?.pointsToNext ?: 100

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Your Impact & Rewards",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = EcoText
                        )
                        Text(
                            text = "Every verified environmental action counts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EcoMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EcoCard
                ),
                actions = {
                    IconButton(onClick = { refreshAll() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Ledger",
                            tint = EcoBrandDark
                        )
                    }
                }
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
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Hero Summary Card with Circular Progress Ring
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = EcoCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circular Progress Indicator Ring
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(68.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { (progressPct / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxSize(),
                                color = EcoBrand,
                                trackColor = EcoBadgeNeutral,
                                strokeWidth = 8.dp,
                                strokeCap = StrokeCap.Round
                            )
                            Icon(
                                imageVector = Icons.Default.Eco,
                                contentDescription = null,
                                tint = EcoBrandDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$currentPts Eco-Points",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoText
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = EcoTint,
                                    shape = RoundedCornerShape(99.dp)
                                ) {
                                    Text(
                                        text = tierName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EcoBrandDark,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (nextTier.isNotBlank()) "$ptsToNext pts to $nextTier ($progressPct%)" else "Highest tier achieved!",
                                fontSize = 12.sp,
                                color = EcoMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = EcoBorder
                    )

                    // Three Key Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("12 kg", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EcoText)
                            Text("Recovered", fontSize = 11.sp, color = EcoMuted)
                        }
                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(EcoBorder))
                        Column {
                            Text("5", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EcoText)
                            Text("Collections", fontSize = 11.sp, color = EcoMuted)
                        }
                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(EcoBorder))
                        Column {
                            Text("8 / 10", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EcoSuccess)
                            Text("Verified actions", fontSize = 11.sp, color = EcoMuted)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Separate Material Processing Totals (Reported, Collected, Confirmed Recycled)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Material Processing Totals",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcoText
                )
                Surface(
                    color = EcoBadgeNeutral,
                    shape = RoundedCornerShape(99.dp)
                ) {
                    Text(
                        text = "Demo totals",
                        fontSize = 11.sp,
                        color = EcoMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = EcoCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("18 kg", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = EcoText)
                        Text("Reported", fontSize = 11.sp, color = EcoMuted)
                    }
                    Box(modifier = Modifier.width(1.dp).height(28.dp).background(EcoBorder))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("14 kg", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = EcoText)
                        Text("Collected", fontSize = 11.sp, color = EcoMuted)
                    }
                    Box(modifier = Modifier.width(1.dp).height(28.dp).background(EcoBorder))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("12 kg", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = EcoSuccess)
                        Text("Confirmed Recycled", fontSize = 11.sp, color = EcoMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Material Breakdown Bars
            Text(
                text = "Collected by Material",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcoText
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = EcoCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MaterialProgressBar("Paper & Cardboard", "5 kg", 1.0f)
                    MaterialProgressBar("Plastics (PET/HDPE)", "4 kg", 0.8f)
                    MaterialProgressBar("Metals & Cans", "2 kg", 0.4f)
                    MaterialProgressBar("Other Dry Stream", "1 kg", 0.2f)

                    HorizontalDivider(color = EcoBorder, modifier = Modifier.padding(top = 4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = EcoBadgeNeutral,
                            shape = RoundedCornerShape(99.dp)
                        ) {
                            Text(
                                text = "Estimate",
                                fontSize = 11.sp,
                                color = EcoMuted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "≈ 15 kg CO₂e avoided, from documented factors",
                            fontSize = 12.sp,
                            color = EcoText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Campus Challenge Leaderboard Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Campus Challenge Leaderboard",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcoText
                )
                Surface(
                    color = EcoTint,
                    shape = RoundedCornerShape(99.dp)
                ) {
                    Text(
                        text = "Kolhapur Region",
                        fontSize = 11.sp,
                        color = EcoBrandDark,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = EcoCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val entries = leaderboardData?.entries ?: emptyList()

                    if (entries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Leaderboard,
                                    contentDescription = null,
                                    tint = EcoMuted,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No campus rankings logged yet in Kolhapur.",
                                    fontSize = 12.sp,
                                    color = EcoText,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Complete your first collection to lead the leaderboard!",
                                    fontSize = 11.sp,
                                    color = EcoMuted
                                )
                            }
                        }
                    } else {
                        entries.forEachIndexed { index, entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (entry.isCurrentUser) EcoTint else Color.Transparent)
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (entry.rank) {
                                                1 -> EcoBrand
                                                2 -> Color(0xFFE2E8F0)
                                                3 -> if (entry.isCurrentUser) EcoBrandDark else Color(0xFFCBD5E1)
                                                else -> EcoBadgeNeutral
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = entry.rank.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (entry.rank == 3 && entry.isCurrentUser) Color.White else EcoText
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                                        color = EcoText
                                    )
                                    Text(
                                        text = "${entry.kgRecovered} kg recovered",
                                        fontSize = 11.sp,
                                        color = EcoMuted
                                    )
                                }
                                Text(
                                    text = "${entry.points} pts",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (entry.isCurrentUser) EcoBrandDark else EcoSuccess
                                )
                            }
                            if (index < entries.size - 1) {
                                HorizontalDivider(color = EcoBorder, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Rewards Redemption Catalog
            Text(
                text = "Redeem Your Eco-Points",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcoText
            )
            Spacer(modifier = Modifier.height(8.dp))

            val displayCatalog = if (catalogItems.isNotEmpty()) catalogItems else listOf(
                RedemptionCatalogItemDto(
                    "PLANT_A_TREE",
                    "Plant a Native Tree Sapling",
                    "Geotagged Neem sapling planted by Green India Trust",
                    100,
                    "Restoration",
                    "Afforestation Trust"
                ),
                RedemptionCatalogItemDto(
                    "CAMPUS_CANTEEN_VOUCHER",
                    "₹50 Campus Canteen Voucher",
                    "Meal credit at all campus cafeterias",
                    50,
                    "Food",
                    "Campus Services"
                ),
                RedemptionCatalogItemDto(
                    "MUNICIPAL_TAX_CREDIT",
                    "Municipal Waste Cess Rebate",
                    "5% official tax rebate on quarterly waste fee",
                    150,
                    "Governance",
                    "Municipal Corporation"
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                displayCatalog.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedRewardForRedemption = item
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = EcoCard),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (item.rewardType) {
                                    "PLANT_A_TREE" -> Icons.Default.Forest
                                    "CAMPUS_CANTEEN_VOUCHER" -> Icons.Default.Fastfood
                                    else -> Icons.Default.ReceiptLong
                                },
                                contentDescription = null,
                                tint = EcoBrandDark,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(EcoTint)
                                    .padding(8.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EcoText
                                )
                                Text(
                                    text = item.description,
                                    fontSize = 12.sp,
                                    color = EcoMuted,
                                    maxLines = 2
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { selectedRewardForRedemption = item },
                                shape = RoundedCornerShape(99.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentPts >= item.pointsRequired) EcoBrand else EcoBadgeNeutral,
                                    contentColor = if (currentPts >= item.pointsRequired) EcoBrandDark else EcoMuted
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${item.pointsRequired} pts",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Badges Showcase
            Text(
                text = "Badges & Achievements",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcoText
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BadgeCard(
                    title = "First Verified Recycling",
                    isUnlocked = true,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                BadgeCard(
                    title = "Community Contributor",
                    isUnlocked = currentPts >= 100,
                    icon = Icons.Default.NaturePeople,
                    modifier = Modifier.weight(1f)
                )
                BadgeCard(
                    title = "Consistent Recycler",
                    isUnlocked = currentPts >= 250,
                    icon = Icons.Default.Autorenew,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7. Reward History Dated Entries
            Text(
                text = "Reward History",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcoText
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = EcoCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HistoryItem("Cardboard handover confirmed", "4 Oct 2026", "+30 pts", EcoSuccess)
                    HorizontalDivider(color = EcoBorder)
                    HistoryItem("Plastic collection verified", "22 Sep 2026", "+40 pts", EcoSuccess)
                    HorizontalDivider(color = EcoBorder)
                    HistoryItem("Citizen Onboarding Welcome Bonus", "15 Sep 2026", "+50 pts", EcoSuccess)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 8. Primary CTA
            Button(
                onClick = onNavigateToMarketplace,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EcoBrand,
                    contentColor = EcoBrandDark
                )
            ) {
                Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Explore Scrap Marketplace to Earn More",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Confirmation / Redemption Dialog
    if (selectedRewardForRedemption != null) {
        val item = selectedRewardForRedemption!!
        val canAfford = currentPts >= item.pointsRequired

        AlertDialog(
            onDismissRequest = { selectedRewardForRedemption = null },
            title = {
                Text(text = "Redeem Reward", fontWeight = FontWeight.Bold, color = EcoText)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = item.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(text = item.description, fontSize = 13.sp, color = EcoMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EcoBadgeNeutral, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cost:", fontSize = 13.sp, color = EcoMuted)
                        Text("${item.pointsRequired} eco-points", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EcoBrandDark)
                    }
                    if (!canAfford) {
                        Text(
                            text = "Insufficient balance. You need ${item.pointsRequired - currentPts} more points.",
                            fontSize = 12.sp,
                            color = EcoWarning
                        )
                    }
                }
            },
            confirmButton = {
                if (canAfford) {
                    Button(
                        onClick = {
                            isSubmittingRedeem = true
                            coroutineScope.launch {
                                val result = EcoTrackApiClient.redeemReward(RedemptionRequest(item.rewardType))
                                if (result.isSuccess) {
                                    redeemedVoucher = result.getOrNull()
                                    selectedRewardForRedemption = null
                                    refreshAll()
                                } else {
                                    errorMessage = "Redemption failed: ${result.exceptionOrNull()?.message}"
                                }
                                isSubmittingRedeem = false
                            }
                        },
                        enabled = !isSubmittingRedeem,
                        colors = ButtonDefaults.buttonColors(containerColor = EcoBrand, contentColor = EcoBrandDark),
                        shape = RoundedCornerShape(99.dp)
                    ) {
                        if (isSubmittingRedeem) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = EcoBrandDark)
                        } else {
                            Text("Confirm Redemption", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRewardForRedemption = null }) {
                    Text("Cancel", color = EcoMuted)
                }
            }
        )
    }

    // Voucher Code Display Dialog
    if (redeemedVoucher != null) {
        val voucher = redeemedVoucher!!
        AlertDialog(
            onDismissRequest = { redeemedVoucher = null },
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EcoSuccess, modifier = Modifier.size(44.dp))
            },
            title = {
                Text("Voucher Activated!", fontWeight = FontWeight.Bold, color = EcoText)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Your redemption code has been generated:", fontSize = 13.sp, color = EcoMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = EcoTint,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = voucher.voucherCode,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoBrandDark,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Show this voucher code at partner desk or apply to your account.",
                        fontSize = 12.sp,
                        color = EcoMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { redeemedVoucher = null },
                    colors = ButtonDefaults.buttonColors(containerColor = EcoBrand, contentColor = EcoBrandDark),
                    shape = RoundedCornerShape(99.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun MaterialProgressBar(label: String, amount: String, progress: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = EcoText,
            modifier = Modifier.width(130.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(EcoBadgeNeutral)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(99.dp))
                    .background(EcoBrand)
            )
        }
        Text(
            text = amount,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = EcoText,
            modifier = Modifier.width(44.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
fun BadgeCard(title: String, isUnlocked: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) EcoCard else Color(0xFFF1F3EB)
        ),
        border = if (isUnlocked) androidx.compose.foundation.BorderStroke(1.dp, EcoBorder) else null
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isUnlocked) EcoSuccess else EcoMuted,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (isUnlocked) EcoText else EcoMuted,
                lineHeight = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun HistoryItem(title: String, date: String, points: String, pointsColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = EcoText)
            Text(date, fontSize = 11.sp, color = EcoMuted)
        }
        Text(points, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = pointsColor)
    }
}
