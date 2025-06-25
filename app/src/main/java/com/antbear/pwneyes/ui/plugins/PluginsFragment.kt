package com.antbear.pwneyes.ui.plugins

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.antbear.pwneyes.R
import com.antbear.pwneyes.databinding.FragmentPluginsBinding
import com.antbear.pwneyes.ui.viewer.ViewerViewModel

class PluginsFragment : Fragment() {
    private val TAG = "PluginsFragment"
    
    private var _binding: FragmentPluginsBinding? = null
    private val binding get() = _binding!!
    
    // Use ViewModel for state preservation
    private val viewModel: ViewerViewModel by viewModels()
    
    // Safe Args to get connection details
    private val args: PluginsFragmentArgs by navArgs()
    
    // Plugin state tracking
    private val pluginStates = mutableMapOf<String, Boolean>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPluginsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupTabs()
        loadPluginStates()
        setupPluginControls()
    }
    
    private fun setupToolbar() {
        // Set connection name in toolbar
        val title = "${args.connectionName} - Plugins"
        binding.connectionTitle.text = title
        
        // Set URL in indicator
        binding.urlIndicator.text = "${args.connectionBaseUrl}/plugins"
        
        // Handle back button clicks
        binding.backArrow.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupTabs() {
        // Set click listeners for all tabs
        binding.homeTab.setOnClickListener { navigateToTab("home") }
        binding.inboxTab.setOnClickListener { navigateToTab("inbox") }
        binding.newTab.setOnClickListener { navigateToTab("inbox/new") }
        binding.profileTab.setOnClickListener { navigateToTab("inbox/profile") }
        binding.peersTab.setOnClickListener { navigateToTab("inbox/peers") }
        
        // Note: No need to handle plugins tab click as we're already on it
        // Just highlight it as selected
        binding.pluginsTab.isSelected = true
    }
    
    private fun navigateToTab(tabPath: String) {
        try {
            // For now, since we only have the plugins tab implemented as a native view,
            // we'll fall back to the WebView approach for other tabs
            val action = PluginsFragmentDirections.actionPluginsToTabDetail(
                url = "${args.connectionBaseUrl}/$tabPath",
                tabName = tabPath.split("/").last().capitalize(),
                tabSelector = tabPath.replace("/", "_"),
                connectionName = args.connectionName,
                connectionBaseUrl = args.connectionBaseUrl,
                username = args.username,
                password = args.password
            )
            
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to tab: $tabPath", e)
            Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadPluginStates() {
        // For now, we'll use default states (all enabled)
        // In a real implementation, these would be loaded from the device
        pluginStates["aircrackonly"] = true
        pluginStates["auto-tune"] = true
        pluginStates["auto-update"] = true
        pluginStates["banthex"] = true
        pluginStates["bt-tether"] = true
        pluginStates["discohash"] = true
        
        // If we have saved states in ViewModel, use those instead
        viewModel.getPluginStates()?.let { savedStates ->
            pluginStates.putAll(savedStates)
        }
    }
    
    private fun setupPluginControls() {
        // Set up switch states based on loaded data
        binding.aircrackSwitch.isChecked = pluginStates["aircrackonly"] ?: true
        binding.autotuneSwitch.isChecked = pluginStates["auto-tune"] ?: true
        binding.autoupdateSwitch.isChecked = pluginStates["auto-update"] ?: true
        binding.banthexSwitch.isChecked = pluginStates["banthex"] ?: true
        binding.bttetherSwitch.isChecked = pluginStates["bt-tether"] ?: true
        binding.discohashSwitch.isChecked = pluginStates["discohash"] ?: true
        
        // Set up switch change listeners
        setupSwitchListener(binding.aircrackSwitch, "aircrackonly")
        setupSwitchListener(binding.autotuneSwitch, "auto-tune")
        setupSwitchListener(binding.autoupdateSwitch, "auto-update")
        setupSwitchListener(binding.banthexSwitch, "banthex")
        setupSwitchListener(binding.bttetherSwitch, "bt-tether")
        setupSwitchListener(binding.discohashSwitch, "discohash")
        
        // Set up upgrade button click listeners
        binding.aircrackUpgrade.setOnClickListener { handleUpgradeClick("aircrackonly") }
        binding.autotuneUpgrade.setOnClickListener { handleUpgradeClick("auto-tune") }
        binding.autoupdateUpgrade.setOnClickListener { handleUpgradeClick("auto-update") }
        binding.banthexUpgrade.setOnClickListener { handleUpgradeClick("banthex") }
        binding.bttetherUpgrade.setOnClickListener { handleUpgradeClick("bt-tether") }
        binding.discohashUpgrade.setOnClickListener { handleUpgradeClick("discohash") }
    }
    
    private fun setupSwitchListener(switch: Switch, pluginName: String) {
        switch.setOnCheckedChangeListener { _, isChecked ->
            // Update the state in our map
            pluginStates[pluginName] = isChecked
            
            // Update the ViewModel
            viewModel.updatePluginState(pluginName, isChecked)
            
            // In a real implementation, this would send the command to the device
            Toast.makeText(
                requireContext(),
                "$pluginName ${if (isChecked) "enabled" else "disabled"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun handleUpgradeClick(pluginName: String) {
        // In a real implementation, this would trigger an upgrade process
        Toast.makeText(
            requireContext(),
            "Upgrading $pluginName...",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
