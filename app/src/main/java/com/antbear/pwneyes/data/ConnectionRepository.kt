package com.antbear.pwneyes.data

import androidx.lifecycle.LiveData

class ConnectionRepository(private val connectionDao: ConnectionDao) {
    val allConnections: LiveData<List<Connection>> = connectionDao.getAllConnections()

    suspend fun insert(connection: Connection) {
        connectionDao.insert(connection)
    }

    suspend fun delete(connection: Connection) {
        connectionDao.delete(connection)
    }

    suspend fun update(connection: Connection) {
        connectionDao.update(connection)
    }

    suspend fun deleteAll() {
        connectionDao.deleteAll()
    }
} 