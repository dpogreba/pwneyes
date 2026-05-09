package com.antbear.pwneyes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Connection::class],
    version = 2,
    exportSchema = false
)
abstract class PwnEyesDatabase : RoomDatabase() {

    abstract fun connectionDao(): ConnectionDao

    companion object {
        @Volatile
        private var INSTANCE: PwnEyesDatabase? = null

        fun getDatabase(context: Context): PwnEyesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PwnEyesDatabase::class.java,
                    "pwneyes_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
