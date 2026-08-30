package com.example.un.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

object DatabaseHelper {
    private var INSTANCE: AppDatabase? = null

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE work_days ADD COLUMN isAllSup INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE lieux ADD COLUMN adresse TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE lieux ADD COLUMN tel TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE lieux ADD COLUMN latitude REAL")
            database.execSQL("ALTER TABLE lieux ADD COLUMN longitude REAL")
        }
    }

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "montemps_database"
            )
            .addMigrations(MIGRATION_7_8, MIGRATION_8_9)
            .build()
            INSTANCE = instance
            instance
        }
    }
}
