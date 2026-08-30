package com.example.un.data

import com.example.un.data.local.DatabaseHolder
import com.example.un.data.local.ClientEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

class ClientViewModel(private val coroutineScope: CoroutineScope) {
    private val clientDao = DatabaseHolder.get().clientDao()

    val clients: Flow<List<ClientEntity>> = clientDao.getAllVisibleClients()

    fun updateClient(client: ClientEntity) {
        coroutineScope.launch(Dispatchers.IO) {
            clientDao.insertOrUpdate(client)
        }
    }
}
