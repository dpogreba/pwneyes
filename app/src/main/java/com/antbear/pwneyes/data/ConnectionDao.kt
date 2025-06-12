package com.antbear.pwneyes.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ConnectionDao {
    @Query("SELECT * FROM connections ORDER BY name ASC")
    fun getAllConnections(): LiveData<List<Connection>>
    
    @Query("SELECT * FROM connections ORDER BY name ASC")
    suspend fun getAllConnectionsSync(): List<Connection>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(connection: Connection)

    @Delete
    suspend fun delete(connection: Connection)

    @Update
    suspend fun update(connection: Connection)

    @Query("DELETE FROM connections")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM connections WHERE id = :id")
    suspend fun getConnectionById(id: Long): Connection?
}
