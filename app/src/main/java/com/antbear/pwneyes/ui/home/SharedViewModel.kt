package com.antbear.pwneyes.ui.home

import android.app.Application
import android.util.Log
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
    private val TAG = "SharedViewModel"
    
    // Use custom init function to initialize the private properties safely
    private lateinit var _repository: ConnectionRepository
    private lateinit var _connections: LiveData<List<Connection>>
    
    // Public getters that will be accessed after initialization
    val repository: ConnectionRepository
        get() = _repository
        
    val connections: LiveData<List<Connection>>
        get() = _connections

    init {
        initializeRepositoryAndConnections(application)
    }
    
    private fun initializeRepositoryAndConnections(application: Application) {
        // Get the repository from the application instance instead of creating a new one
        try {
            val app = application as PwnEyesApplication
            _repository = app.repository
            _connections = _repository.allConnections
            Log.d(TAG, "SharedViewModel initialized with application repository")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SharedViewModel", e)
            // Create a fallback repository if needed
            val fallbackDao = try {
                AppDatabase.getDatabase(application).connectionDao()
            } catch (e2: Exception) {
                Log.e(TAG, "Could not get database from application", e2)
                null
            }
            
            if (fallbackDao != null) {
                _repository = ConnectionRepository(fallbackDao)
                _connections = _repository.allConnections
                Log.d(TAG, "SharedViewModel initialized with fallback repository")
            } else {
                // Last resort - create an empty repository that won't crash
                Log.e(TAG, "Using empty repository as last resort")
                val emptyLiveData = MutableLiveData<List<Connection>>(emptyList())
                _repository = object : ConnectionRepository(null) {
                    override val allConnections: LiveData<List<Connection>> = emptyLiveData
                    
                    override suspend fun insert(connection: Connection) {
                        Log.w(TAG, "Insert operation not available in fallback mode")
                    }
                    
                    override suspend fun update(connection: Connection) {
                        Log.w(TAG, "Update operation not available in fallback mode")
                    }
                    
                    override suspend fun delete(connection: Connection) {
                        Log.w(TAG, "Delete operation not available in fallback mode")
                    }
                    
                    override suspend fun deleteAll() {
                        Log.w(TAG, "DeleteAll operation not available in fallback mode")
                    }
                }
                _connections = emptyLiveData
            }
        }
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
    
    fun updateConnection(connection: Connection) {
        viewModelScope.launch {
            repository.update(connection)
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
