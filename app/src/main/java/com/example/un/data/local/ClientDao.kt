package com.example.un.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {

    @Query("SELECT * FROM clients ORDER BY isDeleted ASC, nom COLLATE NOCASE ASC, prenom COLLATE NOCASE ASC")
    fun getAllVisibleClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun getClientById(id: String): ClientEntity?

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    fun getClientFlow(id: String): Flow<ClientEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(client: ClientEntity)

    @Query("UPDATE clients SET isDeleted = 1, updatedAt = :timestamp, isSynced = 0 WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long)

    @Query("SELECT * FROM clients WHERE isSynced = 0")
    suspend fun getUnsyncedClients(): List<ClientEntity>

    @Query("SELECT * FROM clients")
    suspend fun getAllClientsList(): List<ClientEntity>

    @Query("UPDATE clients SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
