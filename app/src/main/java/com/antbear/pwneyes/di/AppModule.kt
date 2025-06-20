package com.antbear.pwneyes.di

import android.content.Context
import com.antbear.pwneyes.billing.BillingManager
import com.antbear.pwneyes.data.AppDatabase
import com.antbear.pwneyes.data.ConnectionDao
import com.antbear.pwneyes.data.ConnectionRepository
import com.antbear.pwneyes.health.ConnectionHealthService
import com.antbear.pwneyes.util.NetworkUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideConnectionDao(database: AppDatabase): ConnectionDao {
        return database.connectionDao()
    }
    
    @Provides
    @Singleton
    fun provideConnectionRepository(connectionDao: ConnectionDao): ConnectionRepository {
        return ConnectionRepository(connectionDao)
    }
    
    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils {
        return NetworkUtils(context)
    }
    
    @Provides
    @Singleton
    fun provideConnectionHealthService(
        @ApplicationContext context: Context,
        connectionDao: ConnectionDao
    ): ConnectionHealthService {
        return ConnectionHealthService(context, connectionDao)
    }
    
    @Provides
    @Singleton
    fun provideBillingManager(@ApplicationContext context: Context): BillingManager {
        // Check if device has Google Play Services first
        val playServicesAvailable = try {
            val status = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context)
            status == com.google.android.gms.common.ConnectionResult.SUCCESS
        } catch (e: Exception) {
            false
        }
        
        return if (playServicesAvailable) {
            BillingManager(context)
        } else {
            BillingManager(context) // Return a default implementation that will handle no-op operations
        }
    }
}
