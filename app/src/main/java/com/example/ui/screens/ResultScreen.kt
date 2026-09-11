package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.ProductCategory
import com.example.domain.models.ScanAnalysisResult
import com.example.ui.components.ChecklistItemRow
import com.example.ui.components.StatusChip
import com.example.ui.components.TrustScoreGauge
import com.example.ui.components.ViolationCard
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleBlueContainer
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleGreenLight
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleRedLight
import com.example.ui.theme.GoogleYellow
import com.example.ui.viewmodel.ScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val analysis = uiState.currentAnalysis

    var selectedTab by remember { mutableIntStateOf(0) }
    var isRawOcrExpanded by remember { mutableStateOf(false) }

    // State for dormant authority report dialog
    var grievanceAgency by remember { mutableStateOf("National Consumer Helpline (NCH)") }
    var grievanceNotes by remember { mutableStateOf("Package missing mandatory statutory declarations (MRP / FSSAI / Schedule H).") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Compliance Audit",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("result_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.syncMessage != null) {
                        Text(
                            text = uiState.syncMessage ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoogleGreen,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (analysis == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No active scan data found.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onNavigateBack) {
                        Text("Back to Scanner")
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("result_content_list"),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Header Card: Product Name, Brand, Overall Trust Score Gauge
            item {
                HeaderTrustCard(analysis = analysis)
            }

            // 2. Violations Found Alert Banner (if any)
            if (analysis.violations.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReportProblem,
                                contentDescription = null,
                                tint = GoogleRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Legal Violations Detected (${analysis.violations.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = GoogleRed
                            )
                        }

                        analysis.violations.forEach { violation ->
                            ViolationCard(violation = violation)
                        }
                    }
                }
            }

            // 3. AI Understanding / Gemini Verification Card
            item {
                GeminiAuditCard(
                    isLoading = uiState.isGeminiLoading,
                    insightText = uiState.geminiAnalysisText,
                    onRunGemini = { viewModel.runGeminiDeepAnalysis() }
                )
            }

            // 4. Categorized Tab Navigation (Scrollable Tabs)
            // Tab A - Legal Metrology | Tab B - Food Safety | Tab C - Medicine
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 20.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = GoogleBlue
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Scale, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tab A: Legal Metrology")
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tab B: Food Safety")
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tab C: Medicine")
                            }
                        }
                    )
                }
            }

            // 5. Categorized Content Views
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedTab) {
                        0 -> MetrologyTabContent(analysis = analysis)
                        1 -> FoodSafetyTabContent(analysis = analysis)
                        2 -> MedicineTabContent(analysis = analysis)
                    }
                }
            }

            // 6. Action Buttons: "Report Violation" & "Share"
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.openReportDialog() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("report_violation_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (analysis.violations.isNotEmpty()) GoogleRed else GoogleBlue
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReportProblem,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (analysis.violations.isNotEmpty()) "Report Violation" else "Submit Report",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 7. Raw Extracted OCR Text (Expandable Bottom Card)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isRawOcrExpanded = !isRawOcrExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Raw OCR Text Extracted (Offline ML Kit)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (isRawOcrExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }

                        AnimatedVisibility(visible = isRawOcrExpanded) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text(
                                    text = analysis.rawOcrText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dormant "Submit to Authority" dialog state (as specified in UI Stubs & Future Integration Layouts)
    if (uiState.isReportDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.closeReportDialog() },
            title = {
                Text(
                    text = "Report Statutory Violation",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Submit formal complaint regarding ${analysis?.productName ?: "product"} to legal authority:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = grievanceAgency,
                        onValueChange = { grievanceAgency = it },
                        label = { Text("Designated Regulatory Body") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = grievanceNotes,
                        onValueChange = { grievanceNotes = it },
                        label = { Text("Violation Description / Evidence") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Text(
                        text = "Note: Submission endpoint connects to the National Consumer Helpline (NCH) & Legal Metrology Consumer Redressal Portal.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitReportToAuthority(grievanceAgency, grievanceNotes)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoogleRed)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Submit to Authority")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeReportDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.isReportSubmitted) {
        AlertDialog(
            onDismissRequest = { /* Dismiss */ },
            title = { Text("Report Queued Successfully", fontWeight = FontWeight.Bold) },
            text = {
                Text("Your consumer report for '${analysis?.productName}' has been queued for verification with the Legal Metrology Controller & Supabase database.")
            },
            confirmButton = {
                Button(onClick = { viewModel.closeReportDialog() }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun HeaderTrustCard(analysis: ScanAnalysisResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TrustScoreGauge(
                score = analysis.trustScore,
                size = 110.dp,
                strokeWidth = 9.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = analysis.productName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Text(
                text = "Brand: ${analysis.brand} • Category: ${analysis.category.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            StatusChip(
                status = analysis.complianceStatus,
                customText = "${analysis.complianceStatus.name} (${analysis.trustScore}/100)"
            )
        }
    }
}

@Composable
private fun GeminiAuditCard(
    isLoading: Boolean,
    insightText: String?,
    onRunGemini: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = GoogleBlueContainer.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = GoogleBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini 3.1 Pro Label Audit",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = GoogleBlue
                    )
                }

                if (!isLoading && insightText == null) {
                    Button(
                        onClick = onRunGemini,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue),
                        modifier = Modifier.testTag("run_gemini_button")
                    ) {
                        Text("Audit AI", fontSize = 12.sp)
                    }
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = GoogleBlue
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Analyzing label imagery with Gemini Pro multimodal vision...",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoogleBlue
                    )
                }
            } else if (!insightText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = insightText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Request AI image understanding to detect hidden deceptive packaging, chemical additive risks, and regulatory cross-checks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetrologyTabContent(analysis: ScanAnalysisResult) {
    val m = analysis.metrologyDetails

    Text(
        text = "Mandatory Legal Metrology Checklist",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    )

    ChecklistItemRow(
        title = "Maximum Retail Price (MRP)",
        subtitle = "Must be declared inclusive of all taxes",
        value = m.mrp,
        isPresent = m.mrp != null,
        isMandatory = true
    )

    ChecklistItemRow(
        title = "Expiry / Best Before Date",
        subtitle = "Ensures safe consumption timeline",
        value = m.expiryDate,
        isPresent = m.expiryDate != null,
        isMandatory = true
    )

    ChecklistItemRow(
        title = "Net Quantity",
        subtitle = "Metric volume, weight, or unit count",
        value = m.netQuantity,
        isPresent = m.netQuantity != null,
        isMandatory = true
    )

    ChecklistItemRow(
        title = "Manufacturer / Packer Details",
        subtitle = "Complete registered entity name & address",
        value = m.mfgDetails,
        isPresent = m.mfgDetails != null,
        isMandatory = true
    )

    ChecklistItemRow(
        title = "Consumer Care Details",
        subtitle = "Toll-free number or email for grievance redressal",
        value = m.consumerCare,
        isPresent = m.consumerCare != null,
        isMandatory = true
    )
}

@Composable
private fun FoodSafetyTabContent(analysis: ScanAnalysisResult) {
    val f = analysis.foodDetails

    Text(
        text = "FSSAI Food Safety Validation",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    )

    ChecklistItemRow(
        title = "14-Digit FSSAI License Number",
        subtitle = if (f.fssaiNumber != null && f.isFssaiValid) "Valid 14-digit statutory license" else "Statutory license number missing or invalid format",
        value = f.fssaiNumber?.let { "Lic: $it" },
        isPresent = f.fssaiNumber != null && f.isFssaiValid,
        isMandatory = analysis.category == ProductCategory.FOOD
    )

    ChecklistItemRow(
        title = "Veg / Non-Veg Indicator",
        subtitle = "Green dot for Veg or Brown dot for Non-Veg declaration",
        value = f.vegStatus,
        isPresent = f.vegStatus != null,
        isMandatory = analysis.category == ProductCategory.FOOD
    )

    ChecklistItemRow(
        title = "Allergens & Additives",
        subtitle = if (f.allergensDetected.isNotEmpty()) "Contains: ${f.allergensDetected.joinToString()}" else "No declared major allergens detected",
        value = if (f.allergensDetected.isNotEmpty()) "Allergens: ${f.allergensDetected.joinToString()}" else null,
        isPresent = f.allergensDetected.isNotEmpty(),
        isMandatory = false
    )

    ChecklistItemRow(
        title = "Nutritional Facts Table",
        subtitle = f.nutritionalFactsSummary ?: "Energy, sugar, fats, and protein declaration table",
        value = f.nutritionalFactsSummary,
        isPresent = f.nutritionalFactsSummary != null,
        isMandatory = false
    )
}

@Composable
private fun MedicineTabContent(analysis: ScanAnalysisResult) {
    val med = analysis.medicineDetails

    Text(
        text = "Pharmaceutical Safety & Drug Composition",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    )

    ChecklistItemRow(
        title = "Schedule H / H1 Prescription Warning",
        subtitle = if (med.scheduleHWarnDetected) "Warning box present: Rx only dispensing" else "Statutory red caution box warning",
        value = if (med.scheduleHWarnDetected) "Statutory Rx Caution Declared" else null,
        isPresent = med.scheduleHWarnDetected,
        isMandatory = analysis.category == ProductCategory.MEDICINE
    )

    ChecklistItemRow(
        title = "Active Composition & Strength",
        subtitle = if (med.activeIngredients.isNotEmpty()) med.activeIngredients.joinToString() else "Generic name and chemical composition strength",
        value = if (med.activeIngredients.isNotEmpty()) med.activeIngredients.joinToString() else null,
        isPresent = med.activeIngredients.isNotEmpty(),
        isMandatory = true
    )

    ChecklistItemRow(
        title = "Storage & Administration Direction",
        subtitle = med.storageDosageInfo ?: "Keep in cool dark place & dosage instructions",
        value = med.storageDosageInfo,
        isPresent = med.storageDosageInfo != null,
        isMandatory = false
    )
}
