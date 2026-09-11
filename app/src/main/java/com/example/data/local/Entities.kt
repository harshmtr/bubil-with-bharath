package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "users")
@JsonClass(generateAdapter = true)
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "created_at")
    val created_at: Long = System.currentTimeMillis()
)

@Entity(tableName = "scans")
@JsonClass(generateAdapter = true)
data class ScanEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_id")
    val user_id: String,

    @ColumnInfo(name = "product_name")
    val product_name: String,

    @ColumnInfo(name = "brand")
    val brand: String,

    @ColumnInfo(name = "trust_score")
    val trust_score: Int,

    @ColumnInfo(name = "violations_found")
    val violations_found: String, // JSONB serialized as JSON string

    @ColumnInfo(name = "raw_ocr_text")
    val raw_ocr_text: String,

    @ColumnInfo(name = "scanned_at")
    val scanned_at: Long = System.currentTimeMillis(),

    // Local auxiliary fields for app rich display & sync management
    @ColumnInfo(name = "category")
    val category: String = "GENERAL_PACKAGED",

    @ColumnInfo(name = "image_uri")
    val image_uri: String? = null,

    @ColumnInfo(name = "is_synced")
    val is_synced: Boolean = false
)
