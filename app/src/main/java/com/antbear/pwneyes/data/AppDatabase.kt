package com.antbear.pwneyes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Connection::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun connectionDao(): ConnectionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create a new table with the updated schema
                db.execSQL("""
                    CREATE TABLE connections_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL DEFAULT 'undefined',
                        url TEXT NOT NULL DEFAULT 'undefined',
                        username TEXT NOT NULL DEFAULT 'undefined',
                        password TEXT NOT NULL DEFAULT 'undefined',
                        isConnected INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // Copy data from the old table to the new table
                db.execSQL("""
                    INSERT INTO connections_new (id, name, url, username, password, isConnected)
                    SELECT id, name, url, username, password, 0 FROM connections
                """)

                // Remove the old table
                db.execSQL("DROP TABLE connections")

                // Rename the new table to the correct name
                db.execSQL("ALTER TABLE connections_new RENAME TO connections")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pwneyes_database"
                )
                .addMigrations(MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
