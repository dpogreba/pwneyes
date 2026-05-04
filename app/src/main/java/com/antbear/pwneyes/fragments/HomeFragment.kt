package com.antbear.pwneyes.fragments

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.antbear.pwneyes.R
import com.antbear.pwneyes.adapters.ConnectionAdapter
import com.antbear.pwneyes.data.BluetoothConnection
import com.antbear.pwneyes.databinding.DialogAddConnectionBinding
import com.antbear.pwneyes.databinding.FragmentHomeBinding
import com.antbear.pwneyes.viewmodels.HomeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var adapter: ConnectionAdapter

    private var bluetoothAdapter: BluetoothAdapter? = null
    private val scannedDevices = mutableListOf<BluetoothDevice>()

    // -------------------------------------------------------------------------
    // Permission launcher — requests Bluetooth + Location permissions at runtime
    // -------------------------------------------------------------------------
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            startBluetoothDiscovery()
        } else {
            Toast.makeText(requireContext(), R.string.bt_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    // BroadcastReceiver for classic Bluetooth device discovery
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                    device?.let {
                        if (!scannedDevices.contains(it)) scannedDevices.add(it)
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btManager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = btManager.adapter

        setupRecyclerView()
        observeConnections()
        binding.fabAdd.setOnClickListener { showAddConnectionDialog() }
    }

    private fun setupRecyclerView() {
        adapter = ConnectionAdapter(
            onDeleteClick = { connection ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete_connection_confirm_title)
                    .setMessage(
                        getString(R.string.delete_connection_confirm_message, connection.name)
                    )
                    .setPositiveButton(R.string.action_delete) { _, _ ->
                        viewModel.delete(connection)
                        Snackbar.make(binding.root, R.string.connection_deleted, Snackbar.LENGTH_LONG)
                            .setAction(R.string.undo) { viewModel.insert(connection) }
                            .show()
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
            }
        )
        binding.recyclerConnections.adapter = adapter
        binding.recyclerConnections.layoutManager = LinearLayoutManager(requireContext())
    }

    // Switch between empty-state and list based on item count
    private fun observeConnections() {
        viewModel.allConnections.observe(viewLifecycleOwner) { connections ->
            adapter.submitList(connections)
            val hasItems = connections.isNotEmpty()
            binding.emptyState.visibility = if (hasItems) View.GONE else View.VISIBLE
            binding.recyclerConnections.visibility = if (hasItems) View.VISIBLE else View.GONE
        }
    }

    // -------------------------------------------------------------------------
    // Add connection dialog
    // -------------------------------------------------------------------------
    private fun showAddConnectionDialog() {
        val dialogBinding = DialogAddConnectionBinding.inflate(layoutInflater)
        scannedDevices.clear()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_add_connection_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_save, null) // set later to prevent auto-dismiss
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialogBinding.btnScan.setOnClickListener {
            requestBluetoothPermissionsAndScan(dialogBinding)
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.etConnectionName.text?.toString()?.trim() ?: ""
                val mac = dialogBinding.etMacAddress.text?.toString()?.trim()?.uppercase() ?: ""
                val url = dialogBinding.etDeviceUrl.text?.toString()?.trim() ?: ""

                when {
                    name.isEmpty() -> dialogBinding.tilName.error =
                        getString(R.string.hint_connection_name) + " required"

                    !isValidMac(mac) -> dialogBinding.tilMac.error =
                        "Enter a valid MAC address (XX:XX:XX:XX:XX:XX)"

                    else -> {
                        viewModel.insert(BluetoothConnection(name = name, macAddress = mac, deviceUrl = url))
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun isValidMac(mac: String): Boolean =
        mac.matches(Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$"))

    // -------------------------------------------------------------------------
    // Bluetooth discovery
    // -------------------------------------------------------------------------
    private fun requestBluetoothPermissionsAndScan(dialogBinding: DialogAddConnectionBinding) {
        if (bluetoothAdapter == null) {
            Toast.makeText(requireContext(), R.string.bt_not_supported, Toast.LENGTH_SHORT).show()
            return
        }
        if (!bluetoothAdapter!!.isEnabled) {
            Toast.makeText(requireContext(), R.string.bt_not_enabled, Toast.LENGTH_SHORT).show()
            return
        }
        val perms = buildRequiredPermissions()
        val denied = perms.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) !=
                    PackageManager.PERMISSION_GRANTED
        }
        if (denied.isEmpty()) {
            startBluetoothDiscovery()
            showScanResultsInDialog(dialogBinding)
        } else {
            permissionLauncher.launch(denied.toTypedArray())
        }
    }

    private fun buildRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private fun startBluetoothDiscovery() {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        requireContext().registerReceiver(discoveryReceiver, filter)
        bluetoothAdapter?.startDiscovery()
        Toast.makeText(requireContext(), R.string.scanning_title, Toast.LENGTH_SHORT).show()
    }

    private fun showScanResultsInDialog(dialogBinding: DialogAddConnectionBinding) {
        // After a short delay, populate a simple device picker
        binding.root.postDelayed({
            if (!isAdded) return@postDelayed
            bluetoothAdapter?.cancelDiscovery()
            if (scannedDevices.isEmpty()) {
                Toast.makeText(requireContext(), R.string.scanning_empty, Toast.LENGTH_SHORT).show()
                return@postDelayed
            }
            val names = scannedDevices.map { device ->
                val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) device.name ?: "Unknown" else device.address
                "$deviceName\n${device.address}"
            }.toTypedArray()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.scanning_title)
                .setItems(names) { _, index ->
                    val selected = scannedDevices[index]
                    val deviceName = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) selected.name ?: "Device"
                        else selected.name ?: "Device"
                    } catch (e: SecurityException) { "Device" }
                    dialogBinding.etConnectionName.setText(deviceName)
                    dialogBinding.etMacAddress.setText(selected.address)
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        }, 8_000) // 8 second discovery window
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try { requireContext().unregisterReceiver(discoveryReceiver) } catch (_: Exception) {}
        bluetoothAdapter?.cancelDiscovery()
        _binding = null
    }
}
