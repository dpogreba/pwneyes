package com.antbear.pwneyes.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var isDragging = false

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
            },
            onStartDrag = { viewHolder -> itemTouchHelper.startDrag(viewHolder) }
        )
        binding.recyclerEditConnections.adapter = adapter
        binding.recyclerEditConnections.layoutManager = LinearLayoutManager(requireContext())

        val dragCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            // Drag is initiated explicitly from the handle (see onStartDrag), so a
            // long-press on the card body — including on the Edit/Delete buttons —
            // must NOT start a drag.
            override fun isLongPressDragEnabled() = false

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                adapter.moveItem(from, to)
                return true
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) isDragging = true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                isDragging = false
                // Persist the authoritative dragged order (currentList may lag the async
                // differ). The Room UPDATE re-emits allConnections, resyncing the adapter.
                viewModel.updateSortOrders(adapter.endDrag())
            }
        }
        itemTouchHelper = ItemTouchHelper(dragCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerEditConnections)
    }

    private fun showEditDialog(connection: Connection) {
        val dialogBinding = DialogAddConnectionBinding.inflate(layoutInflater)

        // Pre-populate with current values.
        dialogBinding.etConnectionName.setText(connection.name)
        dialogBinding.etIpAddress.setText(connection.ipAddress)
        dialogBinding.etPort.setText(connection.port.toString())

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
                    !isValidPort(dialogBinding.etPort.text?.toString() ?: "") ->
                        dialogBinding.tilPort.error = getString(R.string.error_invalid_port)
                    else -> {
                        val port = dialogBinding.etPort.text?.toString()?.toIntOrNull() ?: 8080
                        dialogBinding.tilName.error = null
                        dialogBinding.tilIp.error   = null
                        dialogBinding.tilPort.error = null
                        viewModel.update(connection.copy(name = name, ipAddress = ip, port = port))
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

    private fun isValidPort(raw: String): Boolean {
        val port = raw.trim().toIntOrNull() ?: return false
        return port in 1..65535
    }

    private fun observeConnections() {
        viewModel.allConnections.observe(viewLifecycleOwner) { connections ->
            // Don't let a background status re-emit clobber an in-progress drag; clearView
            // persists the final order, whose Room write re-emits and resyncs us.
            if (!isDragging) adapter.submitList(connections)
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
