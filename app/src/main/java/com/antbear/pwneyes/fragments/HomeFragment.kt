package com.antbear.pwneyes.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeConnections()
        binding.fabAdd.setOnClickListener { showAddConnectionDialog() }
    }

    // -------------------------------------------------------------------------
    // RecyclerView
    // -------------------------------------------------------------------------
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

    /** Switch between empty-state and list based on item count. */
    private fun observeConnections() {
        viewModel.allConnections.observe(viewLifecycleOwner) { connections ->
            adapter.submitList(connections)
            val hasItems = connections.isNotEmpty()
            binding.emptyState.visibility          = if (hasItems) View.GONE else View.VISIBLE
            binding.recyclerConnections.visibility = if (hasItems) View.VISIBLE else View.GONE
        }
    }

    // -------------------------------------------------------------------------
    // Add connection dialog — IP-based, port 8080 is always assumed
    // -------------------------------------------------------------------------
    private fun showAddConnectionDialog() {
        val dialogBinding = DialogAddConnectionBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_add_connection_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_save, null)   // set below to prevent auto-dismiss
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.etConnectionName.text?.toString()?.trim() ?: ""
                // Strip any http(s):// prefix or :PORT suffix the user may have typed/pasted.
                val ip   = normalizeIp(dialogBinding.etIpAddress.text?.toString() ?: "")

                when {
                    name.isEmpty() ->
                        dialogBinding.tilName.error = getString(R.string.error_name_required)

                    !isValidIp(ip) ->
                        dialogBinding.tilIp.error = getString(R.string.error_invalid_ip)

                    else -> {
                        dialogBinding.tilName.error = null
                        dialogBinding.tilIp.error   = null
                        viewModel.insert(BluetoothConnection(name = name, ipAddress = ip))
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    /**
     * Strips any scheme (http:// / https://) and port (:NNNN) so the user
     * can type just the IP *or* paste a full URL — either works.
     *
     * Examples:
     *   "192.168.44.44"             → "192.168.44.44"
     *   "192.168.44.44:8080"        → "192.168.44.44"
     *   "http://192.168.44.44:8080" → "192.168.44.44"
     *   "http://192.168.44.44"      → "192.168.44.44"
     */
    private fun normalizeIp(raw: String): String =
        raw.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore(":")   // drop :PORT if present
            .trim()

    /**
     * Returns true if [ip] is a valid IPv4 address (each octet 0–255).
     * Always call this on the result of [normalizeIp].
     */
    private fun isValidIp(ip: String): Boolean {
        val regex = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")
        val match = regex.matchEntire(ip) ?: return false
        return match.groupValues.drop(1).all { it.toInt() in 0..255 }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
