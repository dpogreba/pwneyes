package com.antbear.pwneyes.data

import androidx.lifecycle.LiveData

class ConnectionRepository(private val dao: ConnectionDao) {

    val allConnections: LiveData<List<BluetoothConnection>> = dao.getAllConnections()

    suspend fun insert(connection: BluetoothConnection): Long = dao.insert(connection)

    suspend fun update(connection: BluetoothConnection) = dao.update(connection)

    suspend fun delete(connection: BluetoothConnection) = dao.delete(connection)

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun setConnectionStatus(id: Long, connected: Boolean) =
        dao.setConnectionStatus(id, connected, System.currentTimeMillis())

    suspend fun updateRssi(id: Long, rssi: Int) = dao.updateRssi(id, rssi)
}
