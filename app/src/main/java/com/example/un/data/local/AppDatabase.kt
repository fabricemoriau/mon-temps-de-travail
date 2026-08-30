package com.example.un.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ClientEntity::class, ScanEntity::class, WorkDayEntity::class, LieuEntity::class, CollegueEntity::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun scanDao(): ScanDao
    abstract fun workDayDao(): WorkDayDao
    abstract fun lieuDao(): LieuDao
    abstract fun collegueDao(): CollegueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Ajout de la colonne isAllSup à la table work_days
                database.execSQL("ALTER TABLE work_days ADD COLUMN isAllSup INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Ajout des colonnes adresse, tel, latitude, longitude à la table lieux
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
}
