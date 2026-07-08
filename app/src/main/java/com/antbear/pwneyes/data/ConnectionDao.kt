package com.antbear.pwneyes.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ConnectionDao {

    @Query("SELECT * FROM connections ORDER BY sortOrder ASC, name ASC")
    fun getAllConnections(): LiveData<List<Connection>>

    /** Blocking read for background workers (must not run on the main thread). */
    @Query("SELECT * FROM connections")
    fun getAllSync(): List<Connection>

    @Query("SELECT * FROM connections WHERE id = :id")
    suspend fun getById(id: Long): Connection?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connection: Connection): Long

    @Update
    suspend fun update(connection: Connection)

    @Delete
    suspend fun delete(connection: Connection)

    @Query("DELETE FROM connections")
    suspend fun deleteAll()

    @Query("UPDATE connections SET isConnected = :connected, lastConnectedMs = :timestampMs WHERE id = :id")
    suspend fun setConnectionStatus(id: Long, connected: Boolean, timestampMs: Long)

    @Query("UPDATE connections SET sortOrder = :order WHERE id = :id")
    suspend fun updateSortOrder(id: Long, order: Int)

    /** Persist a whole reorder in one transaction → a single table invalidation / LiveData emit. */
    @Transaction
    suspend fun applySortOrders(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> updateSortOrder(id, index) }
    }
}
