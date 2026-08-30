package com.example.un.data

import androidx.lifecycle.*
import com.example.un.data.local.LieuEntity
import kotlinx.coroutines.launch

class LieuViewModel(private val repository: AppRepository) : ViewModel() {

    val allLieux: LiveData<List<LieuEntity>> = repository.allLieux.asLiveData()

    fun getLieu(id: String): LiveData<LieuEntity?> = repository.getLieuFlow(id).asLiveData()

    fun saveLieu(lieu: LieuEntity) = viewModelScope.launch {
        repository.saveLieu(lieu)
    }

    fun deleteLieu(id: String) = viewModelScope.launch {
        repository.deleteLieu(id)
    }
}

class LieuViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LieuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LieuViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
