package com.example.data.remote

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiAnalyzer {

    companion object {
        private const val TAG = "GeminiAnalyzer"
        private const val MODEL_NAME = "gemini-3.1-pro-preview"
        private const val FALLBACK_MODEL = "gemini-2.5-flash"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Resize if too large to save bandwidth while retaining legibility
        val maxDim = 1280
        val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun analyzeImageWithGemini(
        bitmap: Bitmap,
        rawOcrText: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                Exception("Gemini API key is not configured in Secrets panel or .env. Using offline ML Kit and rule engine.")
            )
        }

        val base64Image = bitmapToBase64(bitmap)
        val promptText = """
            You are an expert Legal Metrology and Consumer Product Safety Inspector.
            Analyze this product label image and the extracted OCR text:
            ---
            Extracted OCR Text:
            $rawOcrText
            ---
            Verify compliance with Indian Legal Metrology (Packaged Commodities) Rules, FSSAI regulations (if Food), and Drugs & Cosmetics Act (if Medicine).
            Identify:
            1. Product Name & Brand
            2. Mandatory declarations found (MRP in ₹, Net Quantity, Expiry Date, Manufacturer Address, Consumer Helpline)
            3. Violations, missing declarations, deceptive packaging, or allergen risks.
            4. Provide an overall Consumer Trust Score (0-100) and concise audit verdict.
        """.trimIndent()

        // Try primary model gemini-3.1-pro-preview, then fallback if needed
        val primaryResult = executeGenerateContent(MODEL_NAME, apiKey, promptText, base64Image)
        if (primaryResult.isSuccess) {
            return@withContext primaryResult
        }

        Log.w(TAG, "Primary model $MODEL_NAME failed, falling back to $FALLBACK_MODEL: ${primaryResult.exceptionOrNull()?.message}")
        executeGenerateContent(FALLBACK_MODEL, apiKey, promptText, base64Image)
    }

    private fun executeGenerateContent(
        model: String,
        apiKey: String,
        promptText: String,
        base64Image: String?
    ): Result<String> {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", promptText))

            if (!base64Image.isNullOrBlank()) {
                val inlineData = JSONObject()
                    .put("mimeType", "image/jpeg")
                    .put("data", base64Image)
                partsArray.put(JSONObject().put("inlineData", inlineData))
            }

            val contentsArray = JSONArray()
            contentsArray.put(JSONObject().put("parts", partsArray))

            val requestJson = JSONObject()
                .put("contents", contentsArray)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(mediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return Result.failure(Exception("Gemini HTTP ${response.code}: $responseBody"))
            }

            val parsed = JSONObject(responseBody)
            val candidates = parsed.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text", "")

            if (!text.isNullOrBlank()) {
                return Result.success(text)
            } else {
                return Result.failure(Exception("Empty response received from Gemini"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini request error", e)
            return Result.failure(e)
        }
    }
}
