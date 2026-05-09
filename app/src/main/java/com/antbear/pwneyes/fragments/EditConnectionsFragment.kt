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
import com.antbear.pwneyes.data.Connection
import com.antbear.pwneyes.databinding.DialogAddConnectionBinding
import com.antbear.pwneyes.databinding.FragmentEditConnectionsBinding
import com.antbear.pwneyes.viewmodels.HomeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class EditConnectionsFragment : Fragment() {

    private var _binding: FragmentEditConnectionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var adapter: ConnectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditConnectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeConnections()
        setupDeleteAllButton()
    }

    private fun setupRecyclerView() {
        adapter = ConnectionAdapter(
            onEditClick = { connection -> showEditDialog(connection) },
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
        binding.recyclerEditConnections.adapter = adapter
        binding.recyclerEditConnections.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun showEditDialog(connection: Connection) {
        val dialogBinding = DialogAddConnectionBinding.inflate(layoutInflater)

        // Pre-populate with current values.
        dialogBinding.etConnectionName.setText(connection.name)
        dialogBinding.etIpAddress.setText(connection.ipAddress)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_edit_connection_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_save, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.etConnectionName.text?.toString()?.trim() ?: ""
                val ip   = normalizeIp(dialogBinding.etIpAddress.text?.toString() ?: "")

                when {
                    name.isEmpty() ->
                        dialogBinding.tilName.error = getString(R.string.error_name_required)
                    !isValidIp(ip) ->
                        dialogBinding.tilIp.error = getString(R.string.error_invalid_ip)
                    else -> {
                        dialogBinding.tilName.error = null
                        dialogBinding.tilIp.error   = null
                        viewModel.update(connection.copy(name = name, ipAddress = ip))
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun normalizeIp(raw: String): String =
        raw.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore(":")
            .trim()

    private fun isValidIp(ip: String): Boolean {
        val regex = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")
        val match = regex.matchEntire(ip) ?: return false
        return match.groupValues.drop(1).all { it.toInt() in 0..255 }
    }

    private fun observeConnections() {
        viewModel.allConnections.observe(viewLifecycleOwner) { connections ->
            adapter.submitList(connections)
            val hasItems = connections.isNotEmpty()
            binding.emptyState.visibility = if (hasItems) View.GONE else View.VISIBLE
            binding.recyclerEditConnections.visibility = if (hasItems) View.VISIBLE else View.GONE
            binding.fabDeleteAll.visibility = if (hasItems) View.VISIBLE else View.GONE
        }
    }

    private fun setupDeleteAllButton() {
        binding.fabDeleteAll.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_all_confirm_title)
                .setMessage(R.string.delete_all_confirm_message)
                .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.deleteAll() }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
