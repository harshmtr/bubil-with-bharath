package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY scanned_at DESC")
    fun getAllScans(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE id = :id LIMIT 1")
    fun getScanById(id: String): Flow<ScanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScans(scans: List<ScanEntity>)

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun deleteScanById(id: String)

    @Query("UPDATE scans SET is_synced = 1 WHERE id = :id")
    suspend fun markScanSynced(id: String)

    @Query("DELETE FROM scans")
    suspend fun clearAll()
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getUser(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}
