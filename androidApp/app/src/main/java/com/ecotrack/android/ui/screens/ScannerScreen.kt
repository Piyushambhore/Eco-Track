package com.ecotrack.android.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ecotrack.android.data.api.EcoTrackApiClient
import com.ecotrack.android.data.model.IdentifyWasteRequest
import com.ecotrack.android.ui.theme.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Locale

fun analyzeBitmapColorCategory(bitmap: Bitmap): String {
    val width = bitmap.width
    val height = bitmap.height
    var totalR = 0L
    var totalG = 0L
    var totalB = 0L
    var sampleCount = 0

    val stepX = maxOf(1, width / 12)
    val stepY = maxOf(1, height / 12)

    for (x in (width / 4)..(width * 3 / 4) step stepX) {
        for (y in (height / 4)..(height * 3 / 4) step stepY) {
            val pixel = bitmap.getPixel(x, y)
            totalR += (pixel shr 16) and 0xFF
            totalG += (pixel shr 8) and 0xFF
            totalB += pixel and 0xFF
            sampleCount++
        }
    }

    if (sampleCount == 0) return "Plastic"
    val avgR = totalR / sampleCount
    val avgG = totalG / sampleCount
    val avgB = totalB / sampleCount

    return when {
        avgG > avgR * 1.15 && avgG > avgB * 1.15 && avgG > 60 -> "Organic"
        avgR > 110 && avgG in 70..140 && avgB < 85 && (avgR - avgB) > 40 -> "Paper"
        avgR < 55 && avgG < 55 && avgB < 55 -> "E-waste"
        Math.abs(avgR - avgG) < 12 && Math.abs(avgG - avgB) < 12 && avgR in 80..180 -> "Metal"
        else -> "Plastic"
    }
}

@Composable
fun ScannerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val categories = listOf("Auto-Detect", "Plastic", "Paper", "Metal", "Glass", "E-waste", "Organic", "Mixed waste")
    var selectedCategory by remember { mutableStateOf("Auto-Detect") }
    var detectedLabelBadge by remember { mutableStateOf<String?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun processCapturedBitmap(bitmap: Bitmap) {
        EcoTrackApiClient.lastCapturedBitmap = bitmap
        isAnalyzing = true
        errorMessage = null
        detectedLabelBadge = null

        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        labeler.process(inputImage)
            .addOnSuccessListener { labels ->
                val detectedTags = labels.map { "${it.text} (${(it.confidence * 100).toInt()}%)" }
                val labelTexts = labels.map { it.text.lowercase(Locale.ROOT) }

                // Intelligent AI inference from real Google ML Kit tags
                val inferredCategory = when {
                    labelTexts.any { it.contains("can") || it.contains("aluminum") || it.contains("tin") || it.contains("metal") || it.contains("cylinder") || it.contains("steel") } -> "Metal"
                    labelTexts.any { it.contains("box") || it.contains("cardboard") || it.contains("paper") || it.contains("carton") || it.contains("book") || it.contains("newspaper") || it.contains("magazine") } -> "Paper"
                    labelTexts.any { it.contains("laptop") || it.contains("computer") || it.contains("phone") || it.contains("electronics") || it.contains("gadget") || it.contains("screen") || it.contains("display") || it.contains("hardware") || it.contains("keyboard") || it.contains("battery") || it.contains("cable") } -> "E-waste"
                    labelTexts.any { it.contains("fruit") || it.contains("vegetable") || it.contains("food") || it.contains("apple") || it.contains("banana") || it.contains("produce") || it.contains("plant") || it.contains("bread") } -> "Organic"
                    labelTexts.any { it.contains("glass") || it.contains("wine bottle") || it.contains("beer bottle") || it.contains("jar") || it.contains("glassware") } -> "Glass"
                    labelTexts.any { it.contains("bottle") || it.contains("plastic") } -> "Plastic"
                    selectedCategory != "Auto-Detect" -> selectedCategory
                    else -> analyzeBitmapColorCategory(bitmap)
                }

                val targetCategory = if (selectedCategory != "Auto-Detect") selectedCategory else inferredCategory
                selectedCategory = targetCategory

                val topLabel = labels.firstOrNull()
                if (topLabel != null) {
                    detectedLabelBadge = "AI Identified: ${topLabel.text} (${(topLabel.confidence * 100).toInt()}%)"
                }

                coroutineScope.launch {
                    try {
                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

                        val descriptionPayload = if (detectedTags.isNotEmpty()) {
                            "AI Vision: ${detectedTags.take(4).joinToString(", ")}. Classified: $targetCategory"
                        } else {
                            "$targetCategory item"
                        }

                        val res = EcoTrackApiClient.identifyWaste(
                            IdentifyWasteRequest(
                                imageBase64 = base64,
                                description = descriptionPayload
                            )
                        )
                        isAnalyzing = false
                        res.onSuccess { classification ->
                            onNavigateToAnalysis(classification.category)
                        }.onFailure {
                            onNavigateToAnalysis(targetCategory)
                        }
                    } catch (e: Exception) {
                        isAnalyzing = false
                        onNavigateToAnalysis(targetCategory)
                    }
                }
            }
            .addOnFailureListener {
                val fallbackCategory = if (selectedCategory != "Auto-Detect") selectedCategory else analyzeBitmapColorCategory(bitmap)
                selectedCategory = fallbackCategory

                coroutineScope.launch {
                    try {
                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

                        val res = EcoTrackApiClient.identifyWaste(
                            IdentifyWasteRequest(
                                imageBase64 = base64,
                                description = "$fallbackCategory recyclable item"
                            )
                        )
                        isAnalyzing = false
                        res.onSuccess { classification ->
                            onNavigateToAnalysis(classification.category)
                        }.onFailure {
                            onNavigateToAnalysis(fallbackCategory)
                        }
                    } catch (e: Exception) {
                        isAnalyzing = false
                        onNavigateToAnalysis(fallbackCategory)
                    }
                }
            }
    }

    // 1. Real Camera Launcher (Takes real photo via native camera activity)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { capturedBitmap: Bitmap? ->
        if (capturedBitmap != null) {
            processCapturedBitmap(capturedBitmap)
        }
    }

    // 2. Camera Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            errorMessage = "Camera permission is required to photograph waste."
        }
    }

    // 3. Real Gallery Image Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    processCapturedBitmap(bitmap)
                } else {
                    errorMessage = "Unable to read selected photo."
                }
            } catch (e: Exception) {
                errorMessage = "Error loading image: ${e.message}"
            }
        }
    }

    fun openDeviceCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp, bottom = 40.dp)
    ) {
        // 1. Top Header: Back button, Title "Identify waste", Help icon
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
                text = "Identify waste",
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
                    .clickable { showHelpDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Help",
                    tint = EcoMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Stepper: "Step 1 of 3" with 3-segment progress indicator
        Text(
            text = "Step 1 of 3",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = EcoMuted
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(EcoBrandDark)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFD6DAD0))
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFD6DAD0))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Viewfinder Container (Tapping directly launches the REAL phone camera)
        val infiniteTransition = rememberInfiniteTransition(label = "scannerAnim")
        val laserProgress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "laserProgress"
        )
        val iconPulse by infiniteTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "iconPulse"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF263223))
                .clickable { openDeviceCamera() },
            contentAlignment = Alignment.Center
        ) {
            // White corner brackets overlay
            Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                // Top-Left Corner
                Box(modifier = Modifier.align(Alignment.TopStart).size(24.dp)) {
                    Box(modifier = Modifier.size(24.dp, 3.dp).background(Color.White))
                    Box(modifier = Modifier.size(3.dp, 24.dp).background(Color.White))
                }
                // Top-Right Corner
                Box(modifier = Modifier.align(Alignment.TopEnd).size(24.dp)) {
                    Box(modifier = Modifier.align(Alignment.TopEnd).size(24.dp, 3.dp).background(Color.White))
                    Box(modifier = Modifier.align(Alignment.TopEnd).size(3.dp, 24.dp).background(Color.White))
                }
                // Bottom-Left Corner
                Box(modifier = Modifier.align(Alignment.BottomStart).size(24.dp)) {
                    Box(modifier = Modifier.align(Alignment.BottomStart).size(24.dp, 3.dp).background(Color.White))
                    Box(modifier = Modifier.align(Alignment.BottomStart).size(3.dp, 24.dp).background(Color.White))
                }
                // Bottom-Right Corner
                Box(modifier = Modifier.align(Alignment.BottomEnd).size(24.dp)) {
                    Box(modifier = Modifier.align(Alignment.BottomEnd).size(24.dp, 3.dp).background(Color.White))
                    Box(modifier = Modifier.align(Alignment.BottomEnd).size(3.dp, 24.dp).background(Color.White))
                }
            }

            // High-Tech Animated Scanning Laser Beam
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = ((laserProgress * 200) - 100).dp)
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                EcoBrand.copy(alpha = 0.3f),
                                EcoBrand,
                                Color.White,
                                EcoBrand,
                                EcoBrand.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )

            if (isAnalyzing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = EcoBrand, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Analyzing with Google ML Vision & AWS...",
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = iconPulse
                                scaleY = iconPulse
                            }
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(EcoBrand.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = EcoBrand,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tap to open camera",
                        fontSize = 15.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Point camera at waste item (Can, Box, Bottle, Electronics...)",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (detectedLabelBadge != null) {
            Surface(
                color = EcoTint,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EcoTintBorder),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = EcoBrandDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = detectedLabelBadge ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoBrandDark
                    )
                }
            }
        }

        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFC62828),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // 4. Category Selector Chips
        Text(
            text = "Target Category Hint (Auto or manual)",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = EcoText
        )
        Spacer(modifier = Modifier.height(8.dp))
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

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Instruction & Safety Note
        Text(
            text = "Take a clear photo of the item or waste pile.",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = EcoText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "For mixed waste, photograph the pile from a safe distance without touching hazardous items.",
            fontSize = 13.sp,
            color = EcoMuted
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 6. Primary Button: Opens REAL phone camera
        Button(
            onClick = { openDeviceCamera() },
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EcoBrand,
                contentColor = EcoBrandDark
            ),
            enabled = !isAnalyzing,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = EcoBrandDark
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isAnalyzing) "Scanning with AI..." else "Take a photo with camera",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 7. Secondary Button: Opens REAL Android gallery image picker
        Button(
            onClick = { galleryLauncher.launch("image/*") },
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EcoCard,
                contentColor = EcoText
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder),
            enabled = !isAnalyzing,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = EcoText
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Upload from gallery",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    // Help Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("How to Photograph Waste", fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text("• Position the item under adequate lighting.", fontSize = 13.sp, color = EcoText)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Keep resin codes (PET 1, HDPE 2) or labels facing the camera.", fontSize = 13.sp, color = EcoText)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Never touch unknown chemicals, sharp metals, or leaking batteries.", fontSize = 13.sp, color = EcoText)
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Understood", color = EcoBrandDark, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = EcoCard
        )
    }
}
