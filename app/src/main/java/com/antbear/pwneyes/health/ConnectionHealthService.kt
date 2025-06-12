package com.antbear.pwneyes.health

import android.content.Context
import android.util.Log
import androidx.work.*
import com.antbear.pwneyes.data.Connection
import com.antbear.pwneyes.data.ConnectionDao
import com.antbear.pwneyes.data.HealthStatus
import kotlinx.coroutines.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Service responsible for monitoring connection health.
 * Uses WorkManager for periodic health checks.
 */
class ConnectionHealthService(
    private val context: Context,
    private val connectionDao: ConnectionDao
) {
    private val TAG = "ConnectionHealthService"
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val WORK_NAME = "connection_health_check"
        private const val CONNECTION_TIMEOUT_MS = 5000 // 5 seconds
        private const val READ_TIMEOUT_MS = 5000 // 5 seconds
        
        // Default monitoring intervals
        const val CHECK_INTERVAL_MINUTES = 15L
        const val CHECK_INTERVAL_FLEX_MINUTES = 5L
        
        // Health check worker
        class HealthCheckWorker(
            context: Context,
            params: WorkerParameters
        ) : CoroutineWorker(context, params) {
            override suspend fun doWork(): Result {
                val healthService = ConnectionHealthService(
                    applicationContext,
                    com.antbear.pwneyes.data.AppDatabase.getDatabase(applicationContext).connectionDao()
                )
                
                return try {
                    healthService.checkAllConnections()
                    Result.success()
                } catch (e: Exception) {
                    Log.e("HealthCheckWorker", "Error during health check", e)
                    Result.retry()
                }
            }
        }
    }

    /**
     * Start periodic connection monitoring using WorkManager
     */
    fun startMonitoring(
        intervalMinutes: Long = CHECK_INTERVAL_MINUTES,
        flexMinutes: Long = CHECK_INTERVAL_FLEX_MINUTES
    ) {
        Log.d(TAG, "Starting connection health monitoring")

        // Define constraints for the work
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create periodic work request
        val workRequest = PeriodicWorkRequestBuilder<HealthCheckWorker>(
            intervalMinutes, TimeUnit.MINUTES,
            flexMinutes, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        // Enqueue the work with a unique name
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
    }

    /**
     * Stop connection monitoring
     */
    fun stopMonitoring() {
        Log.d(TAG, "Stopping connection health monitoring")
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Perform an immediate health check of all connections
     */
    suspend fun checkAllConnections() {
        Log.d(TAG, "Checking health of all connections")
        coroutineScope.launch {
            try {
                // Get all connections
                val connections = withContext(Dispatchers.IO) {
                    connectionDao.getAllConnectionsSync()
                }
                
                // Check each connection in parallel
                connections.forEach { connection ->
                    launch {
                        checkConnection(connection)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking connections", e)
            }
        }.join() // Wait for all checks to complete
    }

    /**
     * Check the health of a single connection
     */
    private suspend fun checkConnection(connection: Connection) {
        Log.d(TAG, "Checking health of connection: ${connection.name}")
        
        val now = System.currentTimeMillis()
        var newStatus = connection.healthStatus
        
        try {
            val isHealthy = pingConnection(connection)
            
            newStatus = if (isHealthy) {
                HealthStatus.ONLINE
            } else {
                HealthStatus.OFFLINE
            }
            
            // Update last seen time if online
            val lastSeen = if (newStatus == HealthStatus.ONLINE) now else connection.lastSeen
            
            // Update connection in database
            val updatedConnection = connection.copy(
                healthStatus = newStatus,
                lastChecked = now,
                lastSeen = lastSeen
            )
            
            connectionDao.update(updatedConnection)
            
            // Handle status change notification
            if (connection.healthStatus != newStatus && newStatus != HealthStatus.UNKNOWN) {
                handleStatusChange(connection, newStatus)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking connection: ${connection.name}", e)
            
            // Update connection with error status
            val updatedConnection = connection.copy(
                healthStatus = HealthStatus.OFFLINE,
                lastChecked = now
            )
            
            connectionDao.update(updatedConnection)
        }
    }

    /**
     * Ping a connection to check its health
     * Returns true if connection is healthy, false otherwise
     */
    private suspend fun pingConnection(connection: Connection): Boolean {
        return withContext(Dispatchers.IO) {
            var urlConnection: HttpURLConnection? = null
            
            try {
                val url = connection.url.trim()
                val fullUrl = when {
                    url.startsWith("http://") || url.startsWith("https://") -> url
                    else -> "http://$url"
                }
                
                urlConnection = URL(fullUrl).openConnection() as HttpURLConnection
                urlConnection.connectTimeout = CONNECTION_TIMEOUT_MS
                urlConnection.readTimeout = READ_TIMEOUT_MS
                urlConnection.requestMethod = "HEAD"  // Only get headers, not content
                
                // Connect and get response code
                val responseCode = urlConnection.responseCode
                
                // Any 2xx or 3xx response code is considered successful
                responseCode in 200..399
                
            } catch (e: IOException) {
                Log.e(TAG, "Connection error for ${connection.name}: ${e.message}")
                false
            } finally {
                urlConnection?.disconnect()
            }
        }
    }

    /**
     * Handle a change in connection status
     */
    private fun handleStatusChange(connection: Connection, newStatus: HealthStatus) {
        Log.d(TAG, "Status change for ${connection.name}: ${connection.healthStatus} -> $newStatus")
        
        // TODO: Add notification logic here
        // For now, just log the status change
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        coroutineScope.cancel()
    }
}
