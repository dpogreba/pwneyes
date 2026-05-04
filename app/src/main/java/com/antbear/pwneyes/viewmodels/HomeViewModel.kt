package com.antbear.pwneyes.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.antbear.pwneyes.data.BluetoothConnection
import com.antbear.pwneyes.data.ConnectionRepository
import com.antbear.pwneyes.data.PwnEyesDatabase
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ConnectionRepository
    val allConnections: LiveData<List<BluetoothConnection>>

    init {
        val dao = PwnEyesDatabase.getDatabase(application).connectionDao()
        repository = ConnectionRepository(dao)
        allConnections = repository.allConnections
    }

    fun insert(connection: BluetoothConnection) = viewModelScope.launch {
        repository.insert(connection)
    }

    fun update(connection: BluetoothConnection) = viewModelScope.launch {
        repository.update(connection)
    }

    fun delete(connection: BluetoothConnection) = viewModelScope.launch {
        repository.delete(connection)
    }

    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }

    fun setConnectionStatus(id: Long, connected: Boolean) = viewModelScope.launch {
        repository.setConnectionStatus(id, connected)
    }
}
