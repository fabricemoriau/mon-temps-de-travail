package com.example.un.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CollegueDao {
    @Query("SELECT * FROM collegues ORDER BY prenom ASC, nom ASC")
    fun getAllCollegues(): Flow<List<CollegueEntity>>

    @Query("SELECT * FROM collegues")
    suspend fun getAllColleguesList(): List<CollegueEntity>

    @Query("SELECT * FROM collegues WHERE id = :id LIMIT 1")
    suspend fun getCollegueById(id: String): CollegueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(collegue: CollegueEntity)

    @Delete
    suspend fun delete(collegue: CollegueEntity)

    @Query("DELETE FROM collegues WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM collegues WHERE isSynced = 0")
    suspend fun getUnsynced(): List<CollegueEntity>

    @Query("UPDATE collegues SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
