package com.example.un.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.ConstructedBy
import androidx.room.RoomDatabaseConstructor

@Database(entities = [ClientEntity::class, ScanEntity::class, WorkDayEntity::class, LieuEntity::class, CollegueEntity::class], version = 9, exportSchema = false)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun scanDao(): ScanDao
    abstract fun workDayDao(): WorkDayDao
    abstract fun lieuDao(): LieuDao
    abstract fun collegueDao(): CollegueDao
}

expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
