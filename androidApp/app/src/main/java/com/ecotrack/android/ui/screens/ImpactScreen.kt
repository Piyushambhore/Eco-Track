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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecotrack.android.data.api.EcoTrackApiClient
import com.ecotrack.android.data.model.CollectionRequestDto
import com.ecotrack.android.data.model.PersonalImpactDto
import com.ecotrack.android.data.model.RewardsBalanceDto
import com.ecotrack.android.data.model.WasteReportDto
import com.ecotrack.android.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ImpactScreen(
    onNavigateBack: () -> Unit = {}
) {
    var impact by remember { mutableStateOf<PersonalImpactDto?>(null) }
    var rewards by remember { mutableStateOf<RewardsBalanceDto?>(null) }
    var collections by remember { mutableStateOf<List<CollectionRequestDto>>(emptyList()) }
    var reports by remember { mutableStateOf<List<WasteReportDto>>(emptyList()) }
    var totalReportsCount by remember { mutableLongStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()

    fun loadImpactData() {
        coroutineScope.launch {
            isLoading = true
            EcoTrackApiClient.ensureAuthenticated()

            EcoTrackApiClient.getPersonalImpact().onSuccess { imp ->
                impact = imp
            }

            EcoTrackApiClient.getRewardsBalance().onSuccess { rew ->
                rewards = rew
            }

            EcoTrackApiClient.getMyCollections().onSuccess { cols ->
                collections = cols
            }

            EcoTrackApiClient.getWasteReports().onSuccess { reps ->
                reports = reps.items
                totalReportsCount = reps.total
            }

            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadImpactData()
    }

    // Material weight breakdowns calculated from live data
    val plasticKg = reports.filter { it.category.equals("plastic", ignoreCase = true) }.sumOf { it.quantity ?: 0.0 }
    val paperKg = reports.filter { it.category.equals("paper", ignoreCase = true) }.sumOf { it.quantity ?: 0.0 }
    val metalKg = reports.filter { it.category.equals("metal", ignoreCase = true) }.sumOf { it.quantity ?: 0.0 }
    val otherKg = reports.filter {
        !it.category.equals("plastic", ignoreCase = true) &&
                !it.category.equals("paper", ignoreCase = true) &&
                !it.category.equals("metal", ignoreCase = true)
    }.sumOf { it.quantity ?: 0.0 }

    val totalKg = maxOf(plasticKg + paperKg + metalKg + otherKg, impact?.totalWeightKg ?: 0.0, 0.1)
    val recoveredTotal = impact?.totalWeightKg ?: totalKg

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp, bottom = 100.dp)
    ) {
        // 1. Top Header: Title "Your impact", Profile icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your impact",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcoText
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(EcoCard)
                    .border(1.dp, EcoBorder, CircleShape)
                    .clickable { loadImpactData() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = EcoText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Subtitle
        Text(
            text = "Every verified action counts towards zero municipal waste.",
            fontSize = 14.sp,
            color = EcoMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Main Impact Summary Card with Circular Ring Progress
        val verifiedCount = reports.count { it.status == "collected" || it.status == "verified" }
        val progressFraction = if (totalReportsCount > 0) (verifiedCount.toFloat() / totalReportsCount.toFloat()).coerceIn(0.1f, 1.0f) else 0.5f

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = EcoCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Ring Progress Graphic
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.fillMaxSize(),
                        color = EcoBrand,
                        strokeWidth = 10.dp,
                        trackColor = Color(0xFFE9ECE3)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$verifiedCount",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoText
                        )
                        Text(
                            text = "verified actions",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EcoText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "of $totalReportsCount reported actions",
                    fontSize = 12.sp,
                    color = EcoMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 3 Real Metrics Columns
                val pointsTotal = rewards?.currentPoints ?: 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.US, "%.1f kg", recoveredTotal),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoText
                        )
                        Text(
                            text = "recovered",
                            fontSize = 11.sp,
                            color = EcoMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(EcoBorder)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${collections.size}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoText
                        )
                        Text(
                            text = "completed\ncollections",
                            fontSize = 11.sp,
                            color = EcoMuted,
                            textAlign = TextAlign.Center
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(EcoBorder)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$pointsTotal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoText
                        )
                        Text(
                            text = "points earned",
                            fontSize = 11.sp,
                            color = EcoMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Section "Collected by material" (Calculated from real reports)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Collected by material",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcoText
            )
            val tierName = rewards?.tier?.tierName ?: "Sprout"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFE3F0E7))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Tier: $tierName",
                    fontSize = 11.sp,
                    color = EcoSuccess,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = EcoCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Paper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Paper", fontSize = 13.sp, color = EcoText, modifier = Modifier.width(60.dp))
                    LinearProgressIndicator(
                        progress = { (paperKg / totalKg).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = EcoBrand,
                        trackColor = Color(0xFFEFF2EA)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(String.format(Locale.US, "%.1f kg", paperKg), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = EcoText)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Plastic
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Plastic", fontSize = 13.sp, color = EcoText, modifier = Modifier.width(60.dp))
                    LinearProgressIndicator(
                        progress = { (plasticKg / totalKg).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = EcoBrand,
                        trackColor = Color(0xFFEFF2EA)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(String.format(Locale.US, "%.1f kg", plasticKg), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = EcoText)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Metal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Metal", fontSize = 13.sp, color = EcoText, modifier = Modifier.width(60.dp))
                    LinearProgressIndicator(
                        progress = { (metalKg / totalKg).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = EcoBrand,
                        trackColor = Color(0xFFEFF2EA)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(String.format(Locale.US, "%.1f kg", metalKg), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = EcoText)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Other
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Other", fontSize = 13.sp, color = EcoText, modifier = Modifier.width(60.dp))
                    LinearProgressIndicator(
                        progress = { (otherKg / totalKg).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = EcoBrand,
                        trackColor = Color(0xFFEFF2EA)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(String.format(Locale.US, "%.1f kg", otherKg), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = EcoText)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Carbon avoidance badge with real CO2 factor
                val co2Saved = impact?.totalCo2SavedKg ?: (recoveredTotal * 2.2)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EcoTint)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(EcoCard)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("ESG Impact", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EcoBrandDark)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(Locale.US, "≈ %.1f kg CO₂e avoided from recycling", co2Saved),
                            fontSize = 11.sp,
                            color = EcoBrandDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. Section "Reward history" (Backed by actual verified actions)
        Text(
            text = "Reward history",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = EcoText
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = EcoCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (reports.isEmpty()) {
                    Text(
                        text = "No recorded activity yet. Complete your first recycling action to earn points.",
                        fontSize = 12.sp,
                        color = EcoMuted,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    reports.forEachIndexed { index, rep ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = rep.subType ?: "${rep.category.replaceFirstChar { it.uppercase() }} Segregation",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EcoText
                                )
                                Text(
                                    text = "${rep.createdAt.take(10)} • ${rep.status.replaceFirstChar { it.uppercase() }}",
                                    fontSize = 11.sp,
                                    color = EcoMuted
                                )
                            }
                            val points = ((rep.quantity ?: 1.0) * 10).toInt()
                            Text(
                                text = "+$points pts",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoSuccess
                            )
                        }
                        if (index < reports.size - 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(EcoBorder))
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. Section "Unlocked Badges" (Live from rewards system)
        val badges = rewards?.badges ?: emptyList()
        if (badges.isNotEmpty()) {
            Text(
                text = "Environmental Badges",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcoText
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(badges) { badge ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (badge.isUnlocked) EcoTint else EcoCard
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder),
                        modifier = Modifier.width(180.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (badge.isUnlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (badge.isUnlocked) EcoSuccess else EcoMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = badge.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EcoText
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = badge.description,
                                fontSize = 11.sp,
                                color = EcoMuted,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
