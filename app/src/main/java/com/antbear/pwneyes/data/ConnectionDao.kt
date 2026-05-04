package com.antbear.pwneyes.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ConnectionDao {

    @Query("SELECT * FROM connections ORDER BY name ASC")
    fun getAllConnections(): LiveData<List<BluetoothConnection>>

    @Query("SELECT * FROM connections WHERE id = :id")
    suspend fun getById(id: Long): BluetoothConnection?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connection: BluetoothConnection): Long

    @Update
    suspend fun update(connection: BluetoothConnection)

    @Delete
    suspend fun delete(connection: BluetoothConnection)

    @Query("DELETE FROM connections")
    suspend fun deleteAll()

    @Query("UPDATE connections SET isConnected = :connected, lastConnectedMs = :timestampMs WHERE id = :id")
    suspend fun setConnectionStatus(id: Long, connected: Boolean, timestampMs: Long)

    @Query("UPDATE connections SET rssi = :rssi WHERE id = :id")
    suspend fun updateRssi(id: Long, rssi: Int)
}
