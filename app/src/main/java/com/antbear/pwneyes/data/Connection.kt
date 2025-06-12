package com.antbear.pwneyes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connections")
data class Connection(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var name: String = "",
    var url: String = "",
    var username: String = "",
    var password: String = "",
    var isConnected: Boolean = false,
    
    // Health monitoring fields
    var healthStatus: HealthStatus = HealthStatus.UNKNOWN,
    var lastChecked: Long = 0, // Timestamp of last health check
    var lastSeen: Long = 0     // Timestamp when last seen online
)

/**
 * Represents the health status of a connection
 */
enum class HealthStatus {
    UNKNOWN,    // Status has not been checked yet
    ONLINE,     // Connection is healthy and responding
    OFFLINE,    // Connection is not responding
    UNSTABLE    // Connection is responding but with issues
}
