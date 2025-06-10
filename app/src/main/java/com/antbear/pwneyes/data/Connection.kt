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
    var isConnected: Boolean = false
)
