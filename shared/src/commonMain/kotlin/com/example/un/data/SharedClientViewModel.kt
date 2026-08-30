package com.example.un.data

import com.example.un.data.local.DatabaseHolder
import com.example.un.data.local.ClientEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

class SharedClientViewModel(private val coroutineScope: CoroutineScope) {
    
    // On utilise lazy pour éviter d'accéder à la DB au moment de la création si elle n'est pas prête
    private val clientDao by lazy { 
        try {
            DatabaseHolder.get().clientDao()
        } catch (e: Exception) {
            null
        }
    }

    val clients: Flow<List<ClientEntity>> = clientDao?.getAllVisibleClients() ?: emptyFlow()

    fun updateClient(client: ClientEntity) {
        coroutineScope.launch(Dispatchers.IO) {
            clientDao?.insertOrUpdate(client)
        }
    }
}
