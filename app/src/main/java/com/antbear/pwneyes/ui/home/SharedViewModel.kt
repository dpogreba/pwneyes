package com.antbear.pwneyes.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.antbear.pwneyes.PwnEyesApplication
import com.antbear.pwneyes.data.Connection
import com.antbear.pwneyes.data.ConnectionRepository
import com.antbear.pwneyes.data.AppDatabase
import kotlinx.coroutines.launch

class SharedViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ConnectionRepository
    val connections: LiveData<List<Connection>>

    init {
        val connectionDao = AppDatabase.getDatabase(application).connectionDao()
        repository = ConnectionRepository(connectionDao)
        connections = repository.allConnections
    }

    fun addConnection(connection: Connection) {
        viewModelScope.launch {
            repository.insert(connection)
        }
    }

    fun toggleConnection(connection: Connection) {
        viewModelScope.launch {
            val updatedConnection = connection.copy(isConnected = !connection.isConnected)
            repository.update(updatedConnection)
        }
    }

    fun deleteConnection(connection: Connection) {
        viewModelScope.launch {
            repository.delete(connection)
        }
    }

    fun deleteAllConnections() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }
} 