package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ScanEntity
import com.example.data.remote.GeminiAnalyzer
import com.example.data.remote.SupabaseRepository
import com.example.domain.engine.RuleEngine
import com.example.domain.models.ProductCategory
import com.example.domain.models.SamplePreset
import com.example.domain.models.ScanAnalysisResult
import com.example.domain.ocr.OcrScannerHelper
import com.example.domain.ocr.TextRecognitionHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScannerUiState(
    val isProcessing: Boolean = false,
    val currentAnalysis: ScanAnalysisResult? = null,
    val currentBitmap: Bitmap? = null,
    val errorMessage: String? = null,
    val isGeminiLoading: Boolean = false,
    val geminiAnalysisText: String? = null,
    val isSaved: Boolean = false,
    val syncMessage: String? = null,
    val isReportDialogOpen: Boolean = false,
    val isReportSubmitted: Boolean = false,
    val liveDetectedWordCount: Int = 0,
    val liveKeywordsSpotted: List<String> = emptyList()
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val scanDao = db.scanDao()
    private val ocrHelper = OcrScannerHelper(application)
    val textRecognitionHelper = TextRecognitionHelper()
    private val ruleEngine = RuleEngine(application)
    private val supabaseRepo = SupabaseRepository(application, scanDao)
    private val geminiAnalyzer = GeminiAnalyzer()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val violationsAdapter = moshi.adapter<List<Any>>(
        Types.newParameterizedType(List::class.java, Any::class.java)
    )

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val prefs = application.getSharedPreferences("user_preferences", Application.MODE_PRIVATE)

    fun getUserAllergies(): Set<String> {
        return prefs.getStringSet("user_allergies", emptySet()) ?: emptySet()
    }

    fun setUserAllergies(allergies: Set<String>) {
        prefs.edit().putStringSet("user_allergies", allergies).apply()
    }

    fun processCapturedBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null, currentBitmap = bitmap) }
            try {
                // Offline Google ML Kit OCR
                val rawText = withContext(Dispatchers.Default) {
                    ocrHelper.recognizeTextFromBitmap(bitmap)
                }

                if (rawText.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "No legible packaging text detected. Adjust camera distance, lighting, or focus on labels."
                        )
                    }
                    return@launch
                }

                val analysis = withContext(Dispatchers.Default) {
                    ruleEngine.analyzeText(rawText, null, getUserAllergies())
                }

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        currentAnalysis = analysis,
                        isSaved = false,
                        syncMessage = null
                    )
                }

                // Automatically save & trigger Supabase sync
                saveScanToDb(analysis, null)

            } catch (e: Exception) {
                Log.e("ScannerVM", "OCR error", e)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "OCR extraction failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun processImageUri(uri: Uri, bitmap: Bitmap?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null, currentBitmap = bitmap) }
            try {
                val rawText = withContext(Dispatchers.Default) {
                    if (bitmap != null) {
                        ocrHelper.recognizeTextFromBitmap(bitmap)
                    } else {
                        ocrHelper.recognizeTextFromUri(uri)
                    }
                }

                if (rawText.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "No text found in selected image. Please choose an image with clear packaging declarations."
                        )
                    }
                    return@launch
                }

                val analysis = withContext(Dispatchers.Default) {
                    ruleEngine.analyzeText(rawText, uri.toString(), getUserAllergies())
                }

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        currentAnalysis = analysis,
                        isSaved = false,
                        syncMessage = null
                    )
                }

                saveScanToDb(analysis, uri.toString())

            } catch (e: Exception) {
                Log.e("ScannerVM", "Uri scan error", e)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Image analysis error: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun loadSamplePreset(preset: SamplePreset) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null, currentBitmap = null) }
            val analysis = withContext(Dispatchers.Default) {
                ruleEngine.analyzeText(preset.rawText, null, getUserAllergies())
            }
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    currentAnalysis = analysis,
                    isSaved = false,
                    syncMessage = null
                )
            }
            saveScanToDb(analysis, null)
        }
    }

    fun runGeminiDeepAnalysis() {
        val analysis = _uiState.value.currentAnalysis ?: return
        val bitmap = _uiState.value.currentBitmap

        viewModelScope.launch {
            _uiState.update { it.copy(isGeminiLoading = true) }
            try {
                val result = if (bitmap != null) {
                    geminiAnalyzer.analyzeImageWithGemini(bitmap, analysis.rawOcrText)
                } else {
                    // Create dummy or text prompt fallback
                    val dummyBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
                    geminiAnalyzer.analyzeImageWithGemini(dummyBitmap, analysis.rawOcrText)
                }

                result.onSuccess { insightText ->
                    _uiState.update {
                        it.copy(
                            isGeminiLoading = false,
                            geminiAnalysisText = insightText
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isGeminiLoading = false,
                            geminiAnalysisText = "Gemini Insight: ${err.localizedMessage}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGeminiLoading = false,
                        geminiAnalysisText = "AI analysis unavailable: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun saveScanToDb(analysis: ScanAnalysisResult, imageUri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Convert violations to JSON string for Postgres JSONB mapping
                val violationsJson = org.json.JSONArray().apply {
                    analysis.violations.forEach { v ->
                        val obj = org.json.JSONObject()
                        obj.put("rule_id", v.ruleId)
                        obj.put("field", v.field)
                        obj.put("title", v.title)
                        obj.put("severity", v.severity.name)
                        obj.put("description", v.description)
                        v.detectedValue?.let { obj.put("detected_value", it) }
                        put(obj)
                    }
                }.toString()

                val entity = ScanEntity(
                    id = analysis.id,
                    user_id = supabaseRepo.getUserId(),
                    product_name = analysis.productName,
                    brand = analysis.brand,
                    trust_score = analysis.trustScore,
                    violations_found = violationsJson,
                    raw_ocr_text = analysis.rawOcrText,
                    scanned_at = analysis.timestamp,
                    category = analysis.category.name,
                    image_uri = imageUri,
                    is_synced = false
                )

                // 1. Insert into local Room database
                scanDao.insertScan(entity)

                _uiState.update { it.copy(isSaved = true) }

                // 2. Sync to Supabase PostgreSQL
                val syncResult = supabaseRepo.syncScanToRemote(entity)
                if (syncResult.isSuccess) {
                    _uiState.update { it.copy(syncMessage = "Synced to Supabase PostgreSQL") }
                } else {
                    _uiState.update { it.copy(syncMessage = "Saved locally (Room)") }
                }
            } catch (e: Exception) {
                Log.e("ScannerVM", "Save scan error", e)
            }
        }
    }

    fun openReportDialog() {
        _uiState.update { it.copy(isReportDialogOpen = true, isReportSubmitted = false) }
    }

    fun closeReportDialog() {
        _uiState.update { it.copy(isReportDialogOpen = false) }
    }

    fun submitReportToAuthority(agency: String, grievanceNotes: String) {
        viewModelScope.launch {
            // Dormant / Future Authority Integration state as specified in design doc
            _uiState.update {
                it.copy(
                    isReportDialogOpen = false,
                    isReportSubmitted = true,
                    syncMessage = "Violation report queued for $agency"
                )
            }
        }
    }

    fun clearCurrentScan() {
        _uiState.update {
            it.copy(
                currentAnalysis = null,
                currentBitmap = null,
                errorMessage = null,
                geminiAnalysisText = null,
                isSaved = false,
                syncMessage = null,
                isReportSubmitted = false
            )
        }
    }

    fun onLiveTextRecognized(text: String) {
        if (text.isBlank()) return
        val upper = text.uppercase()
        val spotted = mutableListOf<String>()
        val keywords = listOf("MRP", "EXP", "MFG", "FSSAI", "BATCH", "NET QTY", "BEST BEFORE", "WEIGHT", "SCHEDULE H")
        for (kw in keywords) {
            if (upper.contains(kw)) {
                spotted.add(kw)
            }
        }
        val words = text.split("\\s+".toRegex()).size
        _uiState.update {
            it.copy(
                liveDetectedWordCount = words,
                liveKeywordsSpotted = spotted
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        ocrHelper.close()
        textRecognitionHelper.close()
    }
}
