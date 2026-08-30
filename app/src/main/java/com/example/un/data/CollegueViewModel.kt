package com.example.un.data

import androidx.lifecycle.*
import com.example.un.data.local.CollegueEntity
import kotlinx.coroutines.launch

class CollegueViewModel(private val repository: AppRepository) : ViewModel() {

    val allCollegues: LiveData<List<CollegueEntity>> = repository.allCollegues.asLiveData()

    fun saveCollegue(collegue: CollegueEntity) = viewModelScope.launch {
        repository.saveCollegue(collegue)
    }
}

class CollegueViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CollegueViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CollegueViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
