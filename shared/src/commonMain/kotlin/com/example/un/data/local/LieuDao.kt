package com.example.un.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LieuDao {
    @Query("SELECT * FROM lieux ORDER BY nomLieu COLLATE NOCASE ASC")
    fun getAllLieux(): Flow<List<LieuEntity>>

    @Query("SELECT * FROM lieux")
    suspend fun getAllLieuxList(): List<LieuEntity>

    @Query("SELECT * FROM lieux WHERE id = :id LIMIT 1")
    suspend fun getLieuById(id: String): LieuEntity?

    @Query("SELECT * FROM lieux WHERE id = :id LIMIT 1")
    fun getLieuFlow(id: String): Flow<LieuEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(lieu: LieuEntity)

    @Delete
    suspend fun delete(lieu: LieuEntity)

    @Query("DELETE FROM lieux WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM lieux WHERE isSynced = 0")
    suspend fun getUnsynced(): List<LieuEntity>

    @Query("UPDATE lieux SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
