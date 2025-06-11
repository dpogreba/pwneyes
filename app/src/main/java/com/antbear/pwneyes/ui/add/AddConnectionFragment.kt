package com.antbear.pwneyes.ui.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.antbear.pwneyes.databinding.FragmentAddConnectionBinding
import com.antbear.pwneyes.data.Connection
import com.antbear.pwneyes.ui.home.SharedViewModel

class AddConnectionFragment : Fragment() {

    private var _binding: FragmentAddConnectionBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    
    // Variables to store existing connection details for edit mode
    private var isEditMode = false
    private var connectionId: Long = 0
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddConnectionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Check if we're in edit mode by reading arguments
        arguments?.let { args ->
            isEditMode = args.getBoolean("isEditMode", false)
            
            if (isEditMode) {
                // We're in edit mode, so load the connection details
                connectionId = args.getLong("connectionId", 0)
                val name = args.getString("connectionName", "")
                val url = args.getString("connectionUrl", "")
                val username = args.getString("connectionUsername", "")
                val password = args.getString("connectionPassword", "")
                
                // Set the form fields with the connection details
                binding.editTextConnectionName.setText(name)
                binding.editTextConnectionUrl.setText(url)
                binding.editTextUsername.setText(username)
                binding.editTextPassword.setText(password)
                
                // Update the UI to indicate edit mode
                binding.buttonSave.text = "Update"
                binding.textViewTitle.text = "Edit Connection"
            }
        }
        
        setupSaveButton()
    }

    private fun setupSaveButton() {
        binding.buttonSave.setOnClickListener {
            val connection = validateAndCreateConnection()
            if (connection != null) {
                if (isEditMode) {
                    // Update existing connection
                    sharedViewModel.updateConnection(connection)
                    Toast.makeText(requireContext(), "Connection updated", Toast.LENGTH_SHORT).show()
                } else {
                    // Add new connection
                    sharedViewModel.addConnection(connection)
                    Toast.makeText(requireContext(), "Connection saved", Toast.LENGTH_SHORT).show()
                }
                findNavController().navigateUp()
            }
        }
    }

    private fun validateAndCreateConnection(): Connection? {
        val name = binding.editTextConnectionName.text.toString().trim()
        var url = binding.editTextConnectionUrl.text.toString().trim()
        val username = binding.editTextUsername.text.toString().trim()
        val password = binding.editTextPassword.text.toString()

        if (name.isEmpty()) {
            binding.editTextConnectionName.error = "Name is required"
            return null
        }

        if (url.isEmpty()) {
            binding.editTextConnectionUrl.error = "URL is required"
            return null
        }

        // Clean up URL - remove any duplicate http:// or https://
        url = when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            else -> "http://$url"
        }

        return Connection(
            id = if (isEditMode) connectionId else 0, // Use existing ID when editing
            name = name,
            url = url,
            username = username,
            password = password,
            isConnected = false
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
