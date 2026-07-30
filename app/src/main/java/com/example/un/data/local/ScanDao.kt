package com.example.un.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans WHERE type = :type ORDER BY timestamp DESC")
    fun getScansByType(type: String): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE type = :type AND dateFormatted = :date")
    fun searchByDate(type: String, date: String): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE type = :type AND timestamp BETWEEN :start AND :end")
    suspend fun getScansInRange(type: String, start: Long, end: Long): List<ScanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scan: ScanEntity)

    @Delete
    suspend fun delete(scan: ScanEntity)
}
