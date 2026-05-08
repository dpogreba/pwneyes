package com.antbear.pwneyes.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "connections")
data class BluetoothConnection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** IPv4 address entered by the user, e.g. "192.168.44.44" */
    val ipAddress: String,
    val isConnected: Boolean = false,
    val lastConnectedMs: Long = 0L
) {
    /** Full URL derived from the IP; port 8080 is always assumed. */
    @get:Ignore
    val url: String get() = "http://$ipAddress:8080"
}
