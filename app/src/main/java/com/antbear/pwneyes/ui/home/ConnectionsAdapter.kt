package com.antbear.pwneyes.ui.home

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.antbear.pwneyes.databinding.ItemConnectionBinding
import com.antbear.pwneyes.data.Connection
import java.net.URLEncoder
import android.util.Log
import android.webkit.CookieManager
import androidx.recyclerview.widget.DiffUtil
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Context
import kotlinx.coroutines.*
import com.antbear.pwneyes.ui.webview.WebViewManager
import java.net.URI
import android.app.AlertDialog
import android.widget.EditText

class ConnectionsAdapter(
    private val onConnectClicked: (Connection) -> Unit,
    private val onEditClicked: (Connection) -> Unit,
    private val onDeleteClicked: (Connection) -> Unit
) : RecyclerView.Adapter<ConnectionsAdapter.ConnectionViewHolder>() {

    private var connections: List<Connection> = emptyList()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun updateConnections(newConnections: List<Connection>) {
        val oldConnections = connections
        connections = newConnections
        calculateDiff(oldConnections, newConnections)
    }

    private fun calculateDiff(oldList: List<Connection>, newList: List<Connection>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = oldList.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) = 
                oldList[oldPos].id == newList[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) = 
                oldList[oldPos] == newList[newPos]
        }
        
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ConnectionViewHolder(
        private val binding: ItemConnectionBinding,
        private val onConnectClick: (Connection) -> Unit,
        private val onEditClick: (Connection) -> Unit,
        private val onDeleteClick: (Connection) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var webViewManager: WebViewManager? = null

        private fun setupWebView(connection: Connection) {
            // Create WebViewManager if not already created
            if (webViewManager == null) {
                webViewManager = WebViewManager(
                    binding.webView,
                    binding.loadingProgress,
                    binding.debugInfo
                )
                
                // Set up callback for loading failures
                webViewManager?.onLoadingFailed = {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val currentConnection = connections[position]
                        if (currentConnection.isConnected) {
                            onConnectClick(currentConnection)
                        }
                    }
                }
            }
        }

        fun bind(connection: Connection) {
            binding.textViewName.text = connection.name
            binding.buttonConnect.text = if (connection.isConnected) "Disconnect" else "Connect"
            binding.buttonConnect.setOnClickListener { onConnectClick(connection) }
            binding.buttonEdit.setOnClickListener { onEditClick(connection) }
            binding.buttonDelete.setOnClickListener { onDeleteClick(connection) }

            // Always set up WebView first
            setupWebView(connection)

            if (connection.isConnected) {
                binding.webViewContainer.visibility = View.VISIBLE
                binding.webView.visibility = View.VISIBLE
                webViewManager?.loadUrl(connection.url.trim())
            } else {
                binding.webViewContainer.visibility = View.GONE
                binding.webView.visibility = View.GONE
                webViewManager?.stopLoading()
            }
        }

        fun cleanup() {
            webViewManager?.cleanup()
            webViewManager = null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionViewHolder {
        val binding = ItemConnectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConnectionViewHolder(binding, onConnectClicked, onEditClicked, onDeleteClicked)
    }

    override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int) {
        holder.bind(connections[position])
    }

    override fun getItemCount() = connections.size

    override fun onViewRecycled(holder: ConnectionViewHolder) {
        super.onViewRecycled(holder)
        holder.cleanup()
    }
}
