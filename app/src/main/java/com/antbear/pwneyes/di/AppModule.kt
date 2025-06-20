package com.antbear.pwneyes.di

import android.content.Context
import com.antbear.pwneyes.billing.BillingManager
import com.antbear.pwneyes.data.AppDatabase
import com.antbear.pwneyes.data.ConnectionDao
import com.antbear.pwneyes.data.ConnectionRepository
import com.antbear.pwneyes.health.ConnectionHealthService
import com.antbear.pwneyes.util.NetworkUtils
import android.util.Log

/**
 * Module providing application-wide dependencies.
 * This is a manual dependency provider that replaces Hilt for now.
 * The Hilt annotations have been removed to allow the app to build properly.
 */
object AppModule {
    private const val TAG = "AppModule"

    /**
     * Provides AppDatabase instance
     */
    fun provideAppDatabase(context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    /**
     * Provides ConnectionDao instance
     */
    fun provideConnectionDao(database: AppDatabase): ConnectionDao {
        return database.connectionDao()
    }
    
    /**
     * Provides ConnectionRepository instance
     */
    fun provideConnectionRepository(connectionDao: ConnectionDao): ConnectionRepository {
        return ConnectionRepository(connectionDao)
    }
    
    /**
     * Provides NetworkUtils instance
     */
    fun provideNetworkUtils(context: Context): NetworkUtils {
        return NetworkUtils(context)
    }
    
    /**
     * Provides ConnectionHealthService instance
     */
    fun provideConnectionHealthService(
        context: Context,
        connectionDao: ConnectionDao
    ): ConnectionHealthService {
        return ConnectionHealthService(context, connectionDao)
    }
    
    /**
     * Provides BillingManager instance
     */
    fun provideBillingManager(context: Context): BillingManager {
        // Check if device has Google Play Services first
        val playServicesAvailable = try {
            val status = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context)
            status == com.google.android.gms.common.ConnectionResult.SUCCESS
        } catch (e: Exception) {
            Log.w(TAG, "Error checking Google Play Services availability", e)
            false
        }
        
        return if (playServicesAvailable) {
            Log.d(TAG, "Google Play Services available, initializing BillingManager...")
            BillingManager(context)
        } else {
            Log.w(TAG, "Google Play Services unavailable, creating default BillingManager")
            BillingManager(context) // Return a default implementation that will handle no-op operations
        }
    }
}
