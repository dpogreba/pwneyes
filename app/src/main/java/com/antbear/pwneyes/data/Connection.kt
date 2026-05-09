package com.antbear.pwneyes.data

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
    val lastConnectedMs: Long = 0L
) {
    @get:Ignore
    val url: String get() = "http://$ipAddress:8080"
}
