package com.antbear.pwneyes.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ConnectionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun addConnection(connection: Connection) {
        val connections = getAllConnections().toMutableList()
        connections.add(connection)
        saveConnections(connections)
    }

    fun getAllConnections(): List<Connection> {
        val json = prefs.getString(CONNECTIONS_KEY, "") ?: ""
        return if (json.isNotEmpty()) {
            val type = object : TypeToken<List<Connection>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun clearConnections() {
        prefs.edit().remove(CONNECTIONS_KEY).apply()
    }

    private fun saveConnections(connections: List<Connection>) {
        prefs.edit()
            .putString(CONNECTIONS_KEY, Gson().toJson(connections))
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "PwnEyesPrefs"
        private const val CONNECTIONS_KEY = "connections"
    }
} 