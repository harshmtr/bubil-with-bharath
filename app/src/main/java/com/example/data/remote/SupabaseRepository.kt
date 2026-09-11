package com.example.data.remote

import android.content.Context
import android.util.Log
import org.json.JSONObject
import com.example.data.local.ScanDao
import com.example.data.local.ScanEntity
import com.example.domain.models.ViolationItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class SupabaseRepository(
    private val context: Context,
    private val scanDao: ScanDao
) {
    private val prefs = context.getSharedPreferences("supabase_config", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "SupabaseRepo"
        const val PREF_SUPABASE_URL = "supabase_url"
        const val PREF_SUPABASE_ANON_KEY = "supabase_anon_key"
        const val PREF_USER_ID = "user_id"
        const val PREF_USER_EMAIL = "user_email"

        // Default placeholder project coordinates (can be updated by user in Profile Screen)
        const val DEFAULT_SUPABASE_URL = "https://your-project.supabase.co"
        const val DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy_anon_key"
        const val DEFAULT_USER_ID = "a1b2c3d4-e5f6-4a5b-8c9d-0123456789ab"
        const val DEFAULT_EMAIL = "consumer@safetyscanner.gov.in"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val violationsListAdapter = moshi.adapter<List<ViolationItem>>(
        Types.newParameterizedType(List::class.java, ViolationItem::class.java)
    )

    fun getSupabaseUrl(): String = prefs.getString(PREF_SUPABASE_URL, DEFAULT_SUPABASE_URL) ?: DEFAULT_SUPABASE_URL
    fun getAnonKey(): String = prefs.getString(PREF_SUPABASE_ANON_KEY, DEFAULT_ANON_KEY) ?: DEFAULT_ANON_KEY
    fun getUserId(): String = prefs.getString(PREF_USER_ID, DEFAULT_USER_ID) ?: DEFAULT_USER_ID
    fun getUserEmail(): String = prefs.getString(PREF_USER_EMAIL, DEFAULT_EMAIL) ?: DEFAULT_EMAIL

    fun saveConfig(url: String, key: String, email: String, userId: String) {
        prefs.edit()
            .putString(PREF_SUPABASE_URL, url.trim().removeSuffix("/"))
            .putString(PREF_SUPABASE_ANON_KEY, key.trim())
            .putString(PREF_USER_EMAIL, email.trim())
            .putString(PREF_USER_ID, userId.trim())
            .apply()
    }

    private fun formatIsoTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val url = getSupabaseUrl()
        val key = getAnonKey()

        if (url.contains("your-project.supabase.co") || key.contains("dummy")) {
            return@withContext Result.failure(Exception("Supabase credentials not configured yet. You can configure them in Profile."))
        }

        try {
            val request = Request.Builder()
                .url("$url/rest/v1/scans?limit=1")
                .header("apikey", key)
                .header("Authorization", "Bearer $key")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success("Connected successfully to Supabase PostgreSQL!")
            } else {
                Result.failure(Exception("Supabase returned HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection error", e)
            Result.failure(e)
        }
    }

    /**
     * Inserts scan into Supabase PostgreSQL `scans` table according to schema:
     * id, user_id, product_name, brand, trust_score, violations_found (jsonb), raw_ocr_text, scanned_at
     */
    suspend fun syncScanToRemote(scan: ScanEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val url = getSupabaseUrl()
        val key = getAnonKey()

        if (url.contains("your-project.supabase.co") || key.contains("dummy")) {
            Log.i(TAG, "Supabase credentials are placeholder. Cached locally in Room.")
            return@withContext Result.success(Unit)
        }

        try {
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()

            // Prepare JSON payload strictly matching schema:
            // violations_found is JSONB, so we send the raw JSON array string unescaped
            val violationsJson = if (scan.violations_found.isNotBlank()) scan.violations_found else "[]"
            val isoDate = formatIsoTimestamp(scan.scanned_at)

            // Escape strings for JSON
            val escapedProd = JSONObject.quote(scan.product_name)
            val escapedBrand = JSONObject.quote(scan.brand)
            val escapedOcr = JSONObject.quote(scan.raw_ocr_text)

            val payloadJson = """
                [
                  {
                    "id": "${scan.id}",
                    "user_id": "${scan.user_id}",
                    "product_name": $escapedProd,
                    "brand": $escapedBrand,
                    "trust_score": ${scan.trust_score},
                    "violations_found": $violationsJson,
                    "raw_ocr_text": $escapedOcr,
                    "scanned_at": "$isoDate"
                  }
                ]
            """.trimIndent()

            val request = Request.Builder()
                .url("$url/rest/v1/scans")
                .header("apikey", key)
                .header("Authorization", "Bearer $key")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(payloadJson.toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                scanDao.markScanSynced(scan.id)
                Result.success(Unit)
            } else {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "Supabase insert failed: ${response.code} $errorBody")
                Result.failure(Exception("Supabase sync failed: ${response.code} $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
            Result.failure(e)
        }
    }
}
