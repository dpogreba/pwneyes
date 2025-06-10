package com.antbear.pwneyes.ui.settings

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.antbear.pwneyes.databinding.FragmentSettingsBinding
import com.antbear.pwneyes.ui.home.SharedViewModel

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        setupEraseAllButton()
    }

    private fun setupEraseAllButton() {
        binding.buttonEraseAll.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Erase All Connections")
                .setMessage("Are you sure you want to erase all connections? This action cannot be undone.")
                .setPositiveButton("Erase All") { _, _ -> sharedViewModel.deleteAllConnections() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 