package com.example.un.data

import com.example.un.data.local.DatabaseHolder
import com.example.un.data.local.ScanEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SharedDocsViewModel(private val coroutineScope: CoroutineScope) {
    private val scanDao = DatabaseHolder.get().scanDao()

    fun getScans(type: String): Flow<List<ScanEntity>> {
        return scanDao.getScansByType(type)
    }

    fun deleteScan(scan: ScanEntity) {
        coroutineScope.launch(Dispatchers.IO) {
            scanDao.delete(scan)
        }
    }
}
