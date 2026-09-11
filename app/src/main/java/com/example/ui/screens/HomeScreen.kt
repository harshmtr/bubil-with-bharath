package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ScanEntity
import com.example.domain.models.ComplianceStatus
import com.example.domain.models.SamplePreset
import com.example.domain.models.SamplePresets
import com.example.ui.components.StatusChip
import com.example.ui.components.TrustScoreGauge
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleBlueContainer
import com.example.ui.theme.GoogleBlueDark
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.ScannerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    scannerViewModel: ScannerViewModel,
    historyViewModel: HistoryViewModel,
    onNavigateToScanner: () -> Unit,
    onNavigateToResult: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recentScans by historyViewModel.scans.collectAsState()
    var showHelpDialog by remember { mutableStateOf(false) }

    // Photo picker for uploading package image
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                scannerViewModel.processImageUri(uri, bitmap)
                onNavigateToResult()
            } catch (e: Exception) {
                // Handled in ViewModel
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = GoogleBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Safety & Metrology",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showHelpDialog = true },
                        modifier = Modifier.testTag("help_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Quick Help",
                            tint = GoogleBlueDark
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("home_screen_content"),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Card Section
            item {
                HeroBannerCard(
                    onQuickScanClick = onNavigateToScanner,
                    onUploadImageClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }

            // 2. Sample Presets for Instant Testing (Emulator Friendly)
            item {
                SamplePresetsSection(
                    onSelectPreset = { preset ->
                        scannerViewModel.loadSamplePreset(preset)
                        onNavigateToResult()
                    }
                )
            }

            // 3. Quick Consumer Metric Overview
            item {
                ComplianceMetricsCard(scans = recentScans)
            }

            // 4. Recent Activity Feed
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Scans",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (recentScans.isNotEmpty()) {
                        TextButton(onClick = onNavigateToHistory) {
                            Text("See all (${recentScans.size})")
                        }
                    }
                }
            }

            if (recentScans.isEmpty()) {
                item {
                    EmptyActivityPlaceholder(
                        onQuickScan = onNavigateToScanner,
                        onTestPreset = {
                            SamplePresets.list.firstOrNull()?.let { preset ->
                                scannerViewModel.loadSamplePreset(preset)
                                onNavigateToResult()
                            }
                        }
                    )
                }
            } else {
                items(recentScans.take(5), key = { it.id }) { scan ->
                    RecentScanCard(
                        scan = scan,
                        onClick = {
                            // Re-run analysis on cached raw OCR to open full result screen
                            val preset = SamplePreset(
                                title = scan.product_name,
                                subtitle = scan.brand,
                                category = when (scan.category) {
                                    "FOOD" -> com.example.domain.models.ProductCategory.FOOD
                                    "MEDICINE" -> com.example.domain.models.ProductCategory.MEDICINE
                                    else -> com.example.domain.models.ProductCategory.GENERAL_PACKAGED
                                },
                                rawText = scan.raw_ocr_text,
                                expectedTrustScore = scan.trust_score
                            )
                            scannerViewModel.loadSamplePreset(preset)
                            onNavigateToResult()
                        },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(
                    text = "Legal Metrology & FSSAI Standards",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This scanner validates consumer packages against statutory rules:")
                    Text("• Legal Metrology: Mandatory MRP (inclusive of taxes), Expiry/Best Before, Net Quantity, Manufacturer Name & Address, Helpline.")
                    Text("• FSSAI: Mandatory 14-digit Food License and Green/Brown Veg declaration.")
                    Text("• Medicine: Schedule H/H1 caution box warnings and active compositions.")
                    Text("• Offline ML Kit OCR ensures your product scans operate with zero data loss.")
                }
            },
            confirmButton = {
                Button(onClick = { showHelpDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}

@Composable
private fun HeroBannerCard(
    onQuickScanClick: () -> Unit,
    onUploadImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = GoogleBlueContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hero_scanner_banner),
                    contentDescription = "Scanner Hero Illustration",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GoogleBlue.copy(alpha = 0.2f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "Real-Time Product Compliance",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Instantly inspect MRP overcharging, missing FSSAI licenses, expiry dates, and Schedule H prescription warnings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onQuickScanClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_scan_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Quick Scan", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onUploadImageClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("upload_image_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = GoogleBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Image", color = GoogleBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SamplePresetsSection(
    onSelectPreset: (SamplePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PaddingValues(horizontal = 20.dp).let {
            Text(
                text = "Quick Demo Package Presets",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(SamplePresets.list) { preset ->
                Card(
                    modifier = Modifier
                        .width(220.dp)
                        .clickable { onSelectPreset(preset) }
                        .testTag("sample_preset_${preset.title.take(6)}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = preset.category.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = GoogleBlue
                            )
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Test Preset",
                                tint = GoogleBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = preset.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val status = if (preset.expectedTrustScore >= 80) ComplianceStatus.COMPLIANT else ComplianceStatus.VIOLATION
                            StatusChip(
                                status = status,
                                customText = "${preset.expectedTrustScore} pts"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComplianceMetricsCard(
    scans: List<ScanEntity>,
    modifier: Modifier = Modifier
) {
    val totalCount = scans.size
    val compliantCount = scans.count { it.trust_score >= 80 }
    val avgScore = if (totalCount > 0) (scans.sumOf { it.trust_score } / totalCount) else 100

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricItem(
                label = "Total Scanned",
                value = "$totalCount",
                accentColor = GoogleBlue
            )
            MetricItem(
                label = "Compliant",
                value = "$compliantCount",
                accentColor = GoogleGreen
            )
            MetricItem(
                label = "Avg Trust",
                value = "$avgScore%",
                accentColor = if (avgScore >= 80) GoogleGreen else GoogleRed
            )
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = accentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecentScanCard(
    scan: ScanEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(scan.scanned_at))
    val status = when {
        scan.trust_score >= 80 -> ComplianceStatus.COMPLIANT
        scan.trust_score >= 50 -> ComplianceStatus.WARNING
        else -> ComplianceStatus.VIOLATION
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("recent_scan_${scan.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrustScoreGauge(
                score = scan.trust_score,
                size = 54.dp,
                strokeWidth = 5.dp,
                showLabel = false
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scan.product_name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${scan.brand} • $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusChip(status = status)
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EmptyActivityPlaceholder(
    onQuickScan: () -> Unit,
    onTestPreset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(GoogleBlueContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = GoogleBlue,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No scans yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Scan any food, medicine, or packaged commodity to verify legal declarations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onQuickScan) {
                    Text("Scan Label")
                }
                OutlinedButton(onClick = onTestPreset) {
                    Text("Try Demo Preset")
                }
            }
        }
    }
}
