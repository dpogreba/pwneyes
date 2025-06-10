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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddConnectionBinding.inflate(inflater, container, false)
        setupSaveButton()
        return binding.root
    }

    private fun setupSaveButton() {
        binding.buttonSave.setOnClickListener {
            val connection = validateAndCreateConnection()
            if (connection != null) {
                sharedViewModel.addConnection(connection)
                Toast.makeText(requireContext(), "Connection saved", Toast.LENGTH_SHORT).show()
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
            id = 0, // Room will auto-generate
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