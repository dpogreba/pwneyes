package com.antbear.pwneyes.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing connections. Supports both Room and in-memory storage.
 * Gracefully handles null DAOs by falling back to in-memory storage.
 */
open class ConnectionRepository(private val connectionDao: ConnectionDao?) {
    private val TAG = "ConnectionRepository"
    
    // In-memory storage as fallback when Room isn't available
    private val inMemoryConnections = ConcurrentHashMap<Long, Connection>()
    private val connectionsLiveData = MutableLiveData<List<Connection>>(emptyList())
    
    // Counter for generating IDs for in-memory connections
    private var nextId: Long = 1
    
    // Expose all connections from either Room or in-memory storage
    open val allConnections: LiveData<List<Connection>> = if (connectionDao != null) {
        Log.d(TAG, "Using Room DAO for connection storage")
        connectionDao.getAllConnections()
    } else {
        Log.d(TAG, "Using in-memory fallback for connection storage")
        connectionsLiveData
    }

    open suspend fun insert(connection: Connection) {
        try {
            if (connectionDao != null) {
                connectionDao.insert(connection)
                Log.d(TAG, "Connection inserted using Room: ${connection.name}")
            } else {
                // Use in-memory storage
                if (connection.id == 0L) {
                    connection.id = nextId++
                }
                inMemoryConnections[connection.id] = connection
                updateLiveData()
                Log.d(TAG, "Connection inserted in-memory: ${connection.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting connection: ${e.message}", e)
        }
    }

    open suspend fun delete(connection: Connection) {
        try {
            if (connectionDao != null) {
                connectionDao.delete(connection)
                Log.d(TAG, "Connection deleted using Room: ${connection.name}")
            } else {
                // Use in-memory storage
                inMemoryConnections.remove(connection.id)
                updateLiveData()
                Log.d(TAG, "Connection deleted from in-memory: ${connection.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting connection: ${e.message}", e)
        }
    }

    open suspend fun update(connection: Connection) {
        try {
            if (connectionDao != null) {
                connectionDao.update(connection)
                Log.d(TAG, "Connection updated using Room: ${connection.name}")
            } else {
                // Use in-memory storage
                if (inMemoryConnections.containsKey(connection.id)) {
                    inMemoryConnections[connection.id] = connection
                    updateLiveData()
                    Log.d(TAG, "Connection updated in-memory: ${connection.name}")
                } else {
                    Log.w(TAG, "Connection not found for update: ${connection.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating connection: ${e.message}", e)
        }
    }

    open suspend fun deleteAll() {
        try {
            if (connectionDao != null) {
                connectionDao.deleteAll()
                Log.d(TAG, "All connections deleted using Room")
            } else {
                // Use in-memory storage
                inMemoryConnections.clear()
                updateLiveData()
                Log.d(TAG, "All connections deleted from in-memory")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all connections: ${e.message}", e)
        }
    }
    
    private fun updateLiveData() {
        connectionsLiveData.postValue(inMemoryConnections.values.toList())
    }
}
