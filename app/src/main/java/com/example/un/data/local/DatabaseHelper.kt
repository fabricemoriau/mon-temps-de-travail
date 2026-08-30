package com.example.un.data.local

import android.content.Context

object DatabaseHelper {
    fun getDatabase(context: Context): AppDatabase {
        return DatabaseHolder.get()
    }
}
