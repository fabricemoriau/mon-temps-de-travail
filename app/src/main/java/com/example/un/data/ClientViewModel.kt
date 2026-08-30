package com.example.un.data

import androidx.lifecycle.*
import com.example.un.data.local.ClientEntity
import com.example.un.utils.SearchUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ClientViewModel(private val repository: AppRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    
    val filteredClients: LiveData<List<ClientEntity>> = combine(
        repository.allClients,
        _searchQuery
    ) { clients, query ->
        val visibleClients = clients.filter { !it.isDeleted }
        if (query.isEmpty()) {
            visibleClients
        } else {
            visibleClients.filter { client ->
                SearchUtils.matches(
                    query,
                    client.nom,
                    client.prenom,
                    client.tel,
                    client.adresse,
                    client.notes
                )
            }
        }
    }.asLiveData()

    fun getClient(id: String): LiveData<ClientEntity?> {
        return repository.getClientFlow(id).asLiveData()
    }

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

class ClientViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClientViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClientViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
