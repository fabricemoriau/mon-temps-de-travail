package com.example.un.data

import androidx.lifecycle.*
import com.example.un.data.local.ClientEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ClientViewModel(private val repository: ClientRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    
    // La liste des clients est filtrée automatiquement en temps réel
    val filteredClients: LiveData<List<ClientEntity>> = combine(
        repository.allClients,
        _searchQuery
    ) { clients, query ->
        if (query.isEmpty()) {
            clients
        } else {
            clients.filter {
                it.nom.contains(query, ignoreCase = true) ||
                it.prenom.contains(query, ignoreCase = true) ||
                it.tel.contains(query, ignoreCase = true) ||
                it.adresse.contains(query, ignoreCase = true)
            }
        }
    }.asLiveData()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveClient(client: ClientEntity) = viewModelScope.launch {
        repository.saveClient(client)
    }

    fun deleteClient(id: String) = viewModelScope.launch {
        repository.softDeleteClient(id)
    }
}

class ClientViewModelFactory(private val repository: ClientRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClientViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClientViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
