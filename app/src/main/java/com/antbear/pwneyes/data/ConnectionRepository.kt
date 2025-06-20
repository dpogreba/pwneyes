package com.antbear.pwneyes.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

open class ConnectionRepository(private val connectionDao: ConnectionDao?) {
    private val TAG = "ConnectionRepository"
    
    open val allConnections: LiveData<List<Connection>> = connectionDao?.getAllConnections() 
        ?: MutableLiveData<List<Connection>>(emptyList())

    open suspend fun insert(connection: Connection) {
        try {
            connectionDao?.insert(connection) ?: run {
                Log.e(TAG, "Cannot insert connection, DAO is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting connection", e)
        }
    }

    open suspend fun delete(connection: Connection) {
        try {
            connectionDao?.delete(connection) ?: run {
                Log.e(TAG, "Cannot delete connection, DAO is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting connection", e)
        }
    }

    open suspend fun update(connection: Connection) {
        try {
            connectionDao?.update(connection) ?: run {
                Log.e(TAG, "Cannot update connection, DAO is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating connection", e)
        }
    }

    open suspend fun deleteAll() {
        try {
            connectionDao?.deleteAll() ?: run {
                Log.e(TAG, "Cannot delete all connections, DAO is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all connections", e)
        }
    }
}
