package com.example.domain.models

import com.squareup.moshi.JsonClass

enum class ViolationSeverity {
    CRITICAL,
    MAJOR,
    MINOR
}

enum class ComplianceStatus {
    COMPLIANT,
    WARNING,
    VIOLATION
}

enum class ProductCategory {
    FOOD,
    MEDICINE,
    GENERAL_PACKAGED
}

@JsonClass(generateAdapter = true)
data class ViolationItem(
    val ruleId: String,
    val field: String,
    val title: String,
    val severity: ViolationSeverity,
    val description: String,
    val detectedValue: String? = null
)

data class ChecklistItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val value: String?,
    val isPresent: Boolean,
    val isMandatory: Boolean,
    val ruleId: String? = null
)

data class MetrologyDetails(
    val mrp: String? = null,
    val expiryDate: String? = null,
    val netQuantity: String? = null,
    val mfgDetails: String? = null,
    val consumerCare: String? = null
)

data class FoodSafetyDetails(
    val fssaiNumber: String? = null,
    val isFssaiValid: Boolean = false,
    val vegStatus: String? = null, // "Vegetarian", "Non-Vegetarian", or null
    val allergensDetected: List<String> = emptyList(),
    val nutritionalFactsSummary: String? = null
)

data class MedicineSafetyDetails(
    val scheduleHWarnDetected: Boolean = false,
    val activeIngredients: List<String> = emptyList(),
    val storageDosageInfo: String? = null
)

data class ScanAnalysisResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val productName: String,
    val brand: String,
    val category: ProductCategory,
    val trustScore: Int,
    val violations: List<ViolationItem>,
    val metrologyDetails: MetrologyDetails,
    val foodDetails: FoodSafetyDetails,
    val medicineDetails: MedicineSafetyDetails,
    val rawOcrText: String,
    val imageUri: String? = null,
    val geminiAnalysis: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val complianceStatus: ComplianceStatus
        get() = when {
            trustScore >= 80 -> ComplianceStatus.COMPLIANT
            trustScore >= 50 -> ComplianceStatus.WARNING
            else -> ComplianceStatus.VIOLATION
        }
}
