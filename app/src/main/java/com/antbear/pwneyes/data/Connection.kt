package com.antbear.pwneyes.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "connections")
data class Connection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val ipAddress: String,
    val isConnected: Boolean = false,
    val lastConnectedMs: Long = 0L,
    @ColumnInfo(defaultValue = "8080")
    val port: Int = 8080,
    @ColumnInfo(defaultValue = "0")
    val sortOrder: Int = 0,
    // Optional HTTP Basic Auth for the status poller (null when the device has auth disabled).
    val username: String? = null,
    val password: String? = null
) {
    @get:Ignore
    val url: String get() = "http://$ipAddress:$port"
}
