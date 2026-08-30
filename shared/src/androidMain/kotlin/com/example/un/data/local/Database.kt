package com.example.un.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

fun getDatabaseBuilder(ctx: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = ctx.applicationContext
    val dbFile = appContext.getDatabasePath("montemps_database")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    ).addMigrations(MIGRATION_7_8, MIGRATION_8_9)
}

private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE work_days ADD COLUMN isAllSup INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE lieux ADD COLUMN adresse TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE lieux ADD COLUMN tel TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE lieux ADD COLUMN latitude REAL")
        db.execSQL("ALTER TABLE lieux ADD COLUMN longitude REAL")
    }
}
