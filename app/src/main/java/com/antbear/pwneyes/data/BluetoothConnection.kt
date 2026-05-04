package com.antbear.pwneyes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connections")
data class BluetoothConnection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val macAddress: String,
    val deviceUrl: String = "",
    val isConnected: Boolean = false,
    val lastConnectedMs: Long = 0L,
    val rssi: Int = 0
)
