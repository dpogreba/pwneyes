package com.antbear.pwneyes.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.antbear.pwneyes.R
import com.antbear.pwneyes.databinding.FragmentHomeBinding
import com.antbear.pwneyes.data.Connection
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.util.Base64
import android.webkit.HttpAuthHandler
import android.webkit.WebResourceError
import com.antbear.pwneyes.ui.home.HomeFragmentDirections

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SharedViewModel by activityViewModels()
    private lateinit var connectionsAdapter: ConnectionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeConnections()
    }

    private fun setupRecyclerView() {
        connectionsAdapter = ConnectionsAdapter(
            onConnectClicked = { connection ->
                viewModel.toggleConnection(connection)
            },
            onEditClicked = { connection ->
                navigateToEditConnection(connection)
            },
            onDeleteClicked = { connection ->
                viewModel.deleteConnection(connection)
            }
        )

        binding.recyclerViewConnections.apply {
            adapter = connectionsAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    private fun navigateToEditConnection(connection: Connection) {
        // Create a NavDirections object using the generated class
        val action = HomeFragmentDirections.actionHomeFragmentToAddConnectionFragment(
            connectionId = connection.id,
            connectionName = connection.name,
            connectionUrl = connection.url,
            isEditMode = true
        )
        
        // Navigate to the AddConnectionFragment with the connection details
        findNavController().navigate(action)
    }

    private fun observeConnections() {
        viewModel.connections.observe(viewLifecycleOwner) { connections ->
            connectionsAdapter.updateConnections(connections)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
