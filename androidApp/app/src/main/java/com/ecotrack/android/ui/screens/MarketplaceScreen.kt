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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecotrack.android.data.api.EcoTrackApiClient
import com.ecotrack.android.data.model.CreateListingRequest
import com.ecotrack.android.data.model.CreateOfferRequest
import com.ecotrack.android.data.model.MarketplaceListingDto
import com.ecotrack.android.data.model.MarketplaceStatsDto
import com.ecotrack.android.data.model.RecyclingFacilityDto
import com.ecotrack.android.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    onNavigateBack: () -> Unit = {}
) {
    val categories = listOf("All", "Paper", "Plastic", "Metal", "Glass", "E-waste")
    var selectedCategory by remember { mutableStateOf("All") }
    var showListModal by remember { mutableStateOf(false) }
    var listingCreated by remember { mutableStateOf(false) }

    var listings by remember { mutableStateOf<List<MarketplaceListingDto>>(emptyList()) }
    var registeredDealers by remember { mutableStateOf<List<RecyclingFacilityDto>>(emptyList()) }
    var marketplaceStats by remember { mutableStateOf<MarketplaceStatsDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Selected listing detail sheet & offer dialog
    var selectedListing by remember { mutableStateOf<MarketplaceListingDto?>(null) }
    var offerPricePerKg by remember { mutableStateOf("") }
    var offerMessage by remember { mutableStateOf("") }
    var isSubmittingOffer by remember { mutableStateOf(false) }
    var offerSubmittedSuccess by remember { mutableStateOf(false) }

    // Form states for creating a new listing (Dynamic inputs with clean placeholders)
    var formTitle by remember { mutableStateOf("") }
    var formCategory by remember { mutableStateOf("Plastic") }
    var formQuantity by remember { mutableStateOf("") }
    var formPricePerKg by remember { mutableStateOf("") }
    var formAddress by remember { mutableStateOf("Kolhapur") }
    var formDescription by remember { mutableStateOf("") }
    var isPublishing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    fun loadMarketplaceData() {
        coroutineScope.launch {
            isLoading = true
            EcoTrackApiClient.ensureAuthenticated()

            val filterParam = if (selectedCategory == "All") null else selectedCategory.lowercase()
            EcoTrackApiClient.getMarketplaceListings(material = filterParam).onSuccess { list ->
                listings = list
            }

            EcoTrackApiClient.getNearbyFacilities().onSuccess { facs ->
                registeredDealers = facs
            }

            EcoTrackApiClient.getMarketplaceStats().onSuccess { stats ->
                marketplaceStats = stats
            }

            isLoading = false
        }
    }

    LaunchedEffect(selectedCategory) {
        loadMarketplaceData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp, bottom = 100.dp)
    ) {
        // 1. Top Header: Title "Recycle & earn", Refresh button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recycle & earn",
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
                    .clickable { loadMarketplaceData() },
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
            text = "Direct peer-to-dealer trade for verified recyclable materials.",
            fontSize = 14.sp,
            color = EcoMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Live Marketplace Statistics Card (100% computed from live DB)
        val activeCount = marketplaceStats?.activeListingsCount ?: listings.size.toLong()
        val totalAvailableKg = marketplaceStats?.totalMaterialsAvailableKg ?: listings.sumOf { it.quantityKg }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EcoCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$activeCount",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoText
                    )
                    Text(
                        text = "Active Lots",
                        fontSize = 11.sp,
                        color = EcoMuted
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(30.dp).background(EcoBorder))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.US, "%.1f kg", totalAvailableKg),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoText
                    )
                    Text(
                        text = "Total Available",
                        fontSize = 11.sp,
                        color = EcoMuted
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(30.dp).background(EcoBorder))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${registeredDealers.size}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoText
                    )
                    Text(
                        text = "Verified Dealers",
                        fontSize = 11.sp,
                        color = EcoMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isSelected) EcoBrandDark else EcoCard)
                        .border(1.dp, if (isSelected) EcoBrandDark else EcoBorder, RoundedCornerShape(999.dp))
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Color.White else EcoText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Primary Button: "+ List recyclable waste"
        Button(
            onClick = { showListModal = true },
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EcoBrand,
                contentColor = EcoBrandDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "+ List recyclable waste",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. Section "Recent listings" (Live from backend DB)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Marketplace Lots",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcoText
            )
            Text(
                text = "${listings.size} lots available",
                fontSize = 12.sp,
                color = EcoMuted,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (listings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLoading) "Loading marketplace lots..." else "No active listings in this category. Tap '+ List recyclable waste' to post.",
                    fontSize = 13.sp,
                    color = EcoMuted
                )
            }
        } else {
            listings.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable { selectedListing = item },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = EcoCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
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
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = EcoBrandDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EcoText
                            )
                            val priceStr = if (item.askingPricePerKg != null) "₹${item.askingPricePerKg}/kg" else "Open quote"
                            Text(
                                text = "${item.quantityKg} kg • $priceStr • ${item.pickupAddress}",
                                fontSize = 12.sp,
                                color = EcoMuted
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFFE3F0E7))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = item.status.replaceFirstChar { it.uppercase() },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EcoSuccess
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 7. Section "Registered dealers"
        Text(
            text = "Registered scrap dealers",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = EcoText
        )

        Spacer(modifier = Modifier.height(10.dp))

        registeredDealers.forEach { dealer ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = EcoCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE9ECE3)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = EcoBrandDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = dealer.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EcoText
                        )
                        Text(
                            text = "${dealer.address}, ${dealer.city}",
                            fontSize = 12.sp,
                            color = EcoMuted
                        )
                        if (!dealer.pricingInfo.isNullOrBlank()) {
                            Text(
                                text = dealer.pricingInfo,
                                fontSize = 11.sp,
                                color = EcoBrandDark
                            )
                        }
                    }
                    if (dealer.verified) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFFE3F0E7))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Verified",
                                fontSize = 10.sp,
                                color = EcoSuccess,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Create New Listing Form (Dynamic inputs with placeholders)
    if (showListModal) {
        ModalBottomSheet(
            onDismissRequest = { showListModal = false },
            containerColor = EcoCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Post Recyclable Lot",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcoText
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = formTitle,
                    onValueChange = { formTitle = it },
                    label = { Text("Material Name") },
                    placeholder = { Text("e.g. Cardboard Boxes, Copper Wire", color = EcoMuted) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EcoText)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Plastic", "Paper", "Metal", "Glass", "E-waste")) { c ->
                        val isSel = c == formCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSel) EcoBrandDark else EcoCard)
                                .border(1.dp, if (isSel) EcoBrandDark else EcoBorder, RoundedCornerShape(999.dp))
                                .clickable { formCategory = c }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = c,
                                fontSize = 12.sp,
                                color = if (isSel) Color.White else EcoText,
                                fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = formQuantity,
                    onValueChange = { formQuantity = it },
                    label = { Text("Quantity (kg)") },
                    placeholder = { Text("e.g. 5.0", color = EcoMuted) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = formPricePerKg,
                    onValueChange = { formPricePerKg = it },
                    label = { Text("Asking Price (₹/kg)") },
                    placeholder = { Text("e.g. 15.0", color = EcoMuted) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = formAddress,
                    onValueChange = { formAddress = it },
                    label = { Text("Pickup Location") },
                    placeholder = { Text("e.g. Shivaji Park, Kolhapur", color = EcoMuted) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (formTitle.isNotBlank() && formQuantity.isNotBlank()) {
                            isPublishing = true
                            coroutineScope.launch {
                                val q = formQuantity.toDoubleOrNull() ?: 1.0
                                val p = formPricePerKg.toDoubleOrNull() ?: 10.0
                                val res = EcoTrackApiClient.createMarketplaceListing(
                                    CreateListingRequest(
                                        title = formTitle,
                                        materialType = formCategory.lowercase(),
                                        subType = formTitle,
                                        quantityKg = q,
                                        askingPricePerKg = p,
                                        totalAskingPrice = q * p,
                                        pickupCity = "Kolhapur",
                                        pickupAddress = if (formAddress.isNotBlank()) formAddress else "Kolhapur",
                                        description = if (formDescription.isNotBlank()) formDescription else "Clean segregated recyclable lot"
                                    )
                                )
                                isPublishing = false
                                if (res.isSuccess) {
                                    formTitle = ""
                                    formQuantity = ""
                                    formPricePerKg = ""
                                    showListModal = false
                                    listingCreated = true
                                    loadMarketplaceData()
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EcoBrand, contentColor = EcoBrandDark),
                    enabled = !isPublishing && formTitle.isNotBlank() && formQuantity.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = if (isPublishing) "Publishing..." else "Publish to Marketplace",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Listing Detail Bottom Sheet (Inspect listing and make offers)
    if (selectedListing != null) {
        val lot = selectedListing!!
        ModalBottomSheet(
            onDismissRequest = { selectedListing = null },
            containerColor = EcoCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = lot.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcoText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${lot.materialType.uppercase()} • ${lot.quantityKg} kg • Pickup: ${lot.pickupAddress}",
                    fontSize = 13.sp,
                    color = EcoMuted
                )
                if (!lot.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = lot.description, fontSize = 12.sp, color = EcoText)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Make a Buyer Quote",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcoText
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = offerPricePerKg,
                    onValueChange = { offerPricePerKg = it },
                    label = { Text("Offered Price (₹/kg)") },
                    placeholder = { Text("e.g. 18.0", color = EcoMuted) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = offerMessage,
                    onValueChange = { offerMessage = it },
                    label = { Text("Message / Pickup Availability") },
                    placeholder = { Text("e.g. Can pick up tomorrow at 10 AM", color = EcoMuted) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val p = offerPricePerKg.toDoubleOrNull() ?: (lot.askingPricePerKg ?: 10.0)
                        isSubmittingOffer = true
                        coroutineScope.launch {
                            val res = EcoTrackApiClient.placeMarketplaceOffer(
                                listingId = lot.id,
                                request = CreateOfferRequest(
                                    offeredPricePerKg = p,
                                    totalOfferedPrice = p * lot.quantityKg,
                                    message = offerMessage
                                )
                            )
                            isSubmittingOffer = false
                            if (res.isSuccess) {
                                selectedListing = null
                                offerSubmittedSuccess = true
                                loadMarketplaceData()
                            }
                        }
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EcoBrand, contentColor = EcoBrandDark),
                    enabled = !isSubmittingOffer,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = if (isSubmittingOffer) "Submitting Quote..." else "Submit Quote Offer",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (listingCreated) {
        AlertDialog(
            onDismissRequest = { listingCreated = false },
            title = { Text("Lot Listed Successfully", fontWeight = FontWeight.SemiBold) },
            text = { Text("Your recyclable lot is live in the database and visible to registered collectors.", fontSize = 13.sp, color = EcoText) },
            confirmButton = {
                TextButton(onClick = { listingCreated = false }) {
                    Text("OK", color = EcoBrandDark, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = EcoCard
        )
    }

    if (offerSubmittedSuccess) {
        AlertDialog(
            onDismissRequest = { offerSubmittedSuccess = false },
            title = { Text("Quote Submitted", fontWeight = FontWeight.SemiBold) },
            text = { Text("Your purchase offer has been logged and sent to the lot owner.", fontSize = 13.sp, color = EcoText) },
            confirmButton = {
                TextButton(onClick = { offerSubmittedSuccess = false }) {
                    Text("Done", color = EcoBrandDark, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = EcoCard
        )
    }
}
