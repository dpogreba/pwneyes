package com.antbear.pwneyes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BluetoothConnection::class],
    version = 2,       // bumped: macAddress/deviceUrl/rssi → ipAddress
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
                    // Schema changed from v1 (macAddress) to v2 (ipAddress).
                    // Drop-and-recreate is acceptable for early development.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
