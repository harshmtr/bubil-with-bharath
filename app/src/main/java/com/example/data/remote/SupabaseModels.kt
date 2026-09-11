package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseUserDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "email") val email: String,
    @param:Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseScanDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "user_id") val userId: String,
    @param:Json(name = "product_name") val productName: String,
    @param:Json(name = "brand") val brand: String,
    @param:Json(name = "trust_score") val trustScore: Int,
    @param:Json(name = "violations_found") val violationsFound: Any? = null,
    @param:Json(name = "raw_ocr_text") val rawOcrText: String,
    @param:Json(name = "scanned_at") val scannedAt: String? = null
)
