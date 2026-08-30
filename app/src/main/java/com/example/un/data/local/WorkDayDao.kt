package com.example.un.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDayDao {
    @Query("SELECT * FROM work_days ORDER BY timestamp DESC")
    fun getAllWorkDays(): Flow<List<WorkDayEntity>>

    @Query("SELECT * FROM work_days")
    suspend fun getAllWorkDaysList(): List<WorkDayEntity>

    @Query("SELECT * FROM work_days WHERE dateId = :id LIMIT 1")
    suspend fun getWorkDayById(id: String): WorkDayEntity?

    @Query("SELECT * FROM work_days WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    suspend fun getWorkDaysInRange(start: Long, end: Long): List<WorkDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workDay: WorkDayEntity)

    @Delete
    suspend fun delete(workDay: WorkDayEntity)
}
