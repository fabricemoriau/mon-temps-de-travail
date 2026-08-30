package com.example.un.data

import com.example.un.data.local.DatabaseHolder
import com.example.un.data.local.LieuEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class LieuViewModel(private val coroutineScope: CoroutineScope) {
    private val lieuDao = DatabaseHolder.get().lieuDao()

    val lieux: Flow<List<LieuEntity>> = lieuDao.getAllLieux()

    fun addLieu(lieu: LieuEntity) {
        coroutineScope.launch(Dispatchers.IO) {
            lieuDao.insertOrUpdate(lieu)
        }
    }

    fun deleteLieu(id: String) {
        coroutineScope.launch(Dispatchers.IO) {
            lieuDao.deleteById(id)
        }
    }
}
