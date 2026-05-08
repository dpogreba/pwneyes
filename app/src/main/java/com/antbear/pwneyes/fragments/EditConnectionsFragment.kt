package com.antbear.pwneyes.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.antbear.pwneyes.R
import com.antbear.pwneyes.adapters.ConnectionAdapter
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
