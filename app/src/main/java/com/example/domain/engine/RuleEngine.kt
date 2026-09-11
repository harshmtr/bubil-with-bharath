package com.example.domain.engine

import android.content.Context
import com.example.domain.models.ChecklistItem
import com.example.domain.models.FoodSafetyDetails
import com.example.domain.models.MedicineSafetyDetails
import com.example.domain.models.MetrologyDetails
import com.example.domain.models.ProductCategory
import com.example.domain.models.ScanAnalysisResult
import com.example.domain.models.ViolationItem
import com.example.domain.models.ViolationSeverity
import org.json.JSONObject
import java.util.regex.Pattern

class RuleEngine(private val context: Context) {

    private val rulesJson: JSONObject by lazy {
        try {
            val jsonString = context.assets.open("rules_config.json").bufferedReader().use { it.readText() }
            JSONObject(jsonString)
        } catch (e: Exception) {
            JSONObject()
        }
    }

    fun analyzeText(
        rawText: String,
        imageUri: String? = null,
        userAllergies: Set<String> = emptySet()
    ): ScanAnalysisResult {
        val normalized = rawText.replace("\n", " ").trim()

        // 1. Detect Category
        val category = detectCategory(rawText)

        // 2. Extract Metrology Details
        val mrp = extractMatch(normalized, "(?:MRP|Max(?:imum)?\\.?\\s*Ret(?:ail)?\\.?\\s*Price|M\\.R\\.P\\.?)\\s*:?\\s*(?:₹|Rs\\.?|INR)?\\s*([0-9]+(?:\\.[0-9]{1,2})?)")
        val expiry = extractMatch(normalized, "(?:EXP|Expiry|Use\\s*By|Best\\s*Before|Date\\s*of\\s*Expiry)\\s*:?\\s*([0-9]{1,2}[\\/\\.-][0-9]{1,2}[\\/\\.-][0-9]{2,4}|[A-Za-z]{3,9}\\s*[0-9]{4}|[0-9]{1,2}\\s+[A-Za-z]{3,9}\\s+[0-9]{2,4})")
        val netQty = extractMatch(normalized, "(?:Net\\s*(?:Wt|Weight|Vol|Volume|Qty|Quantity)|Weight)\\s*:?\\s*([0-9]+(?:\\.[0-9]+)?\\s*(?:g|gm|gms|kg|ml|l|ltr|litres|mg|pieces|pcs|N|count))")
        val mfg = extractMatch(normalized, "(?:Mfg(?:\\.|\\s*by)?|Manufactured\\s*by|Packed\\s*by|Marketed\\s*by)\\s*:?\\s*([A-Za-z0-9\\s,.-]{4,40})")
        val consumerCare = extractMatch(normalized, "(?:Consumer\\s*Care|Customer\\s*Care|Helpline|Toll\\s*Free|Feedback)\\s*:?\\s*([0-9\\-\\+\\s]{6,15}|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})")

        val metrology = MetrologyDetails(
            mrp = mrp?.let { "₹$it" },
            expiryDate = expiry,
            netQuantity = netQty,
            mfgDetails = mfg,
            consumerCare = consumerCare
        )

        // 3. Extract Food Safety Details
        val fssaiNum = extractMatch(normalized, "(?:FSSAI|Lic(?:ence)?\\.?\\s*(?:No|Number)?\\.?)\\s*:?\\s*([0-9]{14})")
        val isVeg = when {
            normalized.contains("100% Vegetarian", ignoreCase = true) ||
            normalized.contains("Vegetarian", ignoreCase = true) ||
            normalized.contains("Green Dot", ignoreCase = true) -> "Vegetarian"
            normalized.contains("Non-Vegetarian", ignoreCase = true) ||
            normalized.contains("Non-Veg", ignoreCase = true) ||
            normalized.contains("Brown Dot", ignoreCase = true) -> "Non-Vegetarian"
            else -> null
        }
        val allergens = detectAllergens(normalized)
        val foodDetails = FoodSafetyDetails(
            fssaiNumber = fssaiNum,
            isFssaiValid = fssaiNum?.length == 14,
            vegStatus = isVeg,
            allergensDetected = allergens,
            nutritionalFactsSummary = if (normalized.contains("Energy", ignoreCase = true) || normalized.contains("Nutrition", ignoreCase = true)) "Nutrition table declared" else null
        )

        // 4. Extract Medicine Safety Details
        val scheduleH = normalized.contains("SCHEDULE H", ignoreCase = true) ||
                normalized.contains("SCHEDULE H1", ignoreCase = true) ||
                normalized.contains("prescription of a Registered Medical Practitioner", ignoreCase = true)
        val activeIngredients = extractActiveIngredients(normalized)
        val dosage = extractMatch(normalized, "(?:Dosage|Storage)\\s*:?\\s*([A-Za-z0-9\\s,.-]{10,60})")
        val medicineDetails = MedicineSafetyDetails(
            scheduleHWarnDetected = scheduleH,
            activeIngredients = activeIngredients,
            storageDosageInfo = dosage
        )

        // 5. Evaluate Violations & Compute Trust Score
        val violations = mutableListOf<ViolationItem>()
        var score = 100

        // Metrology checks
        if (metrology.mrp == null) {
            score -= 25
            violations.add(
                ViolationItem(
                    ruleId = "MET_001",
                    field = "MRP",
                    title = "Missing Mandatory MRP",
                    severity = ViolationSeverity.CRITICAL,
                    description = "Maximum Retail Price (inclusive of all taxes) is not clearly declared on package."
                )
            )
        }

        if (metrology.expiryDate == null) {
            score -= 20
            violations.add(
                ViolationItem(
                    ruleId = "MET_002",
                    field = "EXPIRY_DATE",
                    title = "Missing Expiry / Best Before Date",
                    severity = ViolationSeverity.MAJOR,
                    description = "Date of expiry or Best Before declaration is missing or obscured."
                )
            )
        }

        if (metrology.netQuantity == null) {
            score -= 15
            violations.add(
                ViolationItem(
                    ruleId = "MET_003",
                    field = "NET_QUANTITY",
                    title = "Missing Net Quantity Declaration",
                    severity = ViolationSeverity.MAJOR,
                    description = "Net weight, count, or metric volume is not declared."
                )
            )
        }

        if (metrology.mfgDetails == null) {
            score -= 15
            violations.add(
                ViolationItem(
                    ruleId = "MET_004",
                    field = "MFG_DETAILS",
                    title = "Missing Manufacturer Address",
                    severity = ViolationSeverity.MAJOR,
                    description = "Name and complete registered address of manufacturer / packer is omitted."
                )
            )
        }

        if (metrology.consumerCare == null) {
            score -= 10
            violations.add(
                ViolationItem(
                    ruleId = "MET_005",
                    field = "CONSUMER_CARE",
                    title = "Missing Consumer Care Helpline",
                    severity = ViolationSeverity.MINOR,
                    description = "Consumer redressal telephone number or email address was not found."
                )
            )
        }

        // Food specific checks
        if (category == ProductCategory.FOOD) {
            if (foodDetails.fssaiNumber == null) {
                score -= 25
                violations.add(
                    ViolationItem(
                        ruleId = "FSS_001",
                        field = "FSSAI_LICENSE",
                        title = "Missing 14-Digit FSSAI License",
                        severity = ViolationSeverity.CRITICAL,
                        description = "Mandatory Food Safety and Standards Authority of India 14-digit license number missing."
                    )
                )
            } else if (!foodDetails.isFssaiValid) {
                score -= 15
                violations.add(
                    ViolationItem(
                        ruleId = "FSS_001_INVALID",
                        field = "FSSAI_LICENSE",
                        title = "Invalid FSSAI License Format",
                        severity = ViolationSeverity.MAJOR,
                        description = "License number does not conform to the 14-digit statutory structure: ${foodDetails.fssaiNumber}"
                    )
                )
            }

            if (foodDetails.vegStatus == null) {
                score -= 10
                violations.add(
                    ViolationItem(
                        ruleId = "FSS_002",
                        field = "VEG_NONVEG",
                        title = "Missing Veg / Non-Veg Indicator",
                        severity = ViolationSeverity.MINOR,
                        description = "Mandatory green/brown dot or vegetarian declaration is missing."
                    )
                )
            }

            // Check against user allergies
            val matchedUserAllergens = userAllergies.filter { allergen ->
                allergens.any { it.contains(allergen, ignoreCase = true) }
            }
            if (matchedUserAllergens.isNotEmpty()) {
                violations.add(
                    ViolationItem(
                        ruleId = "USER_ALLERGEN_ALERT",
                        field = "HEALTH_RISK",
                        title = "Personal Health Alert: Allergens Detected",
                        severity = ViolationSeverity.CRITICAL,
                        description = "Product package contains ${matchedUserAllergens.joinToString()} matching your dietary profile!"
                    )
                )
            }
        }

        // Medicine specific checks
        if (category == ProductCategory.MEDICINE) {
            if (activeIngredients.isNotEmpty() && !scheduleH) {
                // If it looks like a prescription drug without Schedule H warning
                if (normalized.contains("Amoxicillin", ignoreCase = true) ||
                    normalized.contains("Paracetamol", ignoreCase = true) ||
                    normalized.contains("Ciprofloxacin", ignoreCase = true) ||
                    normalized.contains("Azithromycin", ignoreCase = true)) {
                    score -= 30
                    violations.add(
                        ViolationItem(
                            ruleId = "MED_001",
                            field = "SCHEDULE_H_WARNING",
                            title = "Missing Mandatory Schedule H Warning",
                            severity = ViolationSeverity.CRITICAL,
                            description = "Prescription antibiotic/substance detected without statutory red box Schedule H warning."
                        )
                    )
                }
            }
        }

        val clampedTrustScore = score.coerceIn(0, 100)

        // 6. Extract Product and Brand Name
        val (brand, productName) = extractBrandAndProduct(rawText, category)

        return ScanAnalysisResult(
            productName = productName,
            brand = brand,
            category = category,
            trustScore = clampedTrustScore,
            violations = violations,
            metrologyDetails = metrology,
            foodDetails = foodDetails,
            medicineDetails = medicineDetails,
            rawOcrText = rawText,
            imageUri = imageUri
        )
    }

    private fun detectCategory(rawText: String): ProductCategory {
        val lower = rawText.lowercase()
        return when {
            lower.contains("schedule h") || lower.contains("rx ") || lower.contains("mg") && (lower.contains("tablet") || lower.contains("capsule") || lower.contains("syrup") || lower.contains("ip") || lower.contains("bp") || lower.contains("usp")) -> {
                ProductCategory.MEDICINE
            }
            lower.contains("fssai") || lower.contains("nutrition") || lower.contains("ingredients") || lower.contains("energy") || lower.contains("veg") || lower.contains("best before") || lower.contains("flavour") -> {
                ProductCategory.FOOD
            }
            else -> ProductCategory.GENERAL_PACKAGED
        }
    }

    private fun extractMatch(text: String, regexPattern: String): String? {
        val pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)?.trim()
        } else null
    }

    private fun detectAllergens(text: String): List<String> {
        val lower = text.lowercase()
        val potential = listOf("gluten", "wheat", "peanut", "tree nuts", "almond", "cashew", "milk", "dairy", "lactose", "soy", "soya", "egg", "fish", "crustacean", "sesame", "sulphite")
        return potential.filter { lower.contains(it) }
    }

    private fun extractActiveIngredients(text: String): List<String> {
        val list = mutableListOf<String>()
        val pattern = Pattern.compile("(?:contains|contains\\s*:?|Each\\s*tablet\\s*contains)\\s*([A-Za-z0-9\\s,.-]+(?:mg|mcg|IU|g|%))", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            matcher.group(1)?.let { list.add(it.trim()) }
        }
        return list
    }

    private fun extractBrandAndProduct(rawText: String, category: ProductCategory): Pair<String, String> {
        val lines = rawText.lines().map { it.trim() }.filter { it.length > 2 }
        if (lines.isEmpty()) {
            return Pair("Unknown Brand", "Scanned Product")
        }

        // Often top lines contain Brand or Product
        val firstLine = lines.firstOrNull { !it.contains("MRP", ignoreCase = true) && !it.contains("Lic", ignoreCase = true) } ?: "Consumer Product"
        val secondLine = lines.getOrNull(1)?.takeIf { !it.contains("MRP", ignoreCase = true) && it.length < 30 }

        val brand = if (secondLine != null && firstLine.length <= 16) firstLine else "Verified Brand"
        val prod = if (secondLine != null && firstLine.length <= 16) secondLine else firstLine

        return Pair(brand, prod)
    }
}
