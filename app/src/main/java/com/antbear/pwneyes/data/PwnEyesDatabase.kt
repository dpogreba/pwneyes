package com.antbear.pwneyes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Connection::class],
    version = 3,
    exportSchema = false
)
abstract class PwnEyesDatabase : RoomDatabase() {

    abstract fun connectionDao(): ConnectionDao

    companion object {
        @Volatile
        private var INSTANCE: PwnEyesDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE connections ADD COLUMN port INTEGER NOT NULL DEFAULT 8080")
                database.execSQL("ALTER TABLE connections ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): PwnEyesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PwnEyesDatabase::class.java,
                    "pwneyes_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
