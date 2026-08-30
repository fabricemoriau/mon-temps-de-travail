package com.example.un.data.local

object DatabaseHolder {
    private var database: AppDatabase? = null
    
    fun init(db: AppDatabase) {
        database = db
    }
    
    fun get(): AppDatabase = database ?: throw IllegalStateException("Database not initialized")
}
