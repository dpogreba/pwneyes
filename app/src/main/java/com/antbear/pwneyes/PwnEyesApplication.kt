package com.antbear.pwneyes

import android.app.Application
import com.antbear.pwneyes.data.AppDatabase
import com.antbear.pwneyes.data.ConnectionRepository

class PwnEyesApplication : Application() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ConnectionRepository(database.connectionDao()) }
} 