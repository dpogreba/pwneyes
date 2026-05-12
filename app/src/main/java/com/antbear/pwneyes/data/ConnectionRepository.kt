package com.antbear.pwneyes.data

import androidx.lifecycle.LiveData

class ConnectionRepository(private val dao: ConnectionDao) {

    val allConnections: LiveData<List<Connection>> = dao.getAllConnections()

    suspend fun insert(connection: Connection): Long = dao.insert(connection)

    suspend fun update(connection: Connection) = dao.update(connection)

    suspend fun delete(connection: Connection) = dao.delete(connection)

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun setConnectionStatus(id: Long, connected: Boolean) =
        dao.setConnectionStatus(id, connected, System.currentTimeMillis())

    suspend fun updateSortOrders(connections: List<Connection>) {
        connections.forEachIndexed { index, connection ->
            dao.updateSortOrder(connection.id, index)
        }
    }
}
