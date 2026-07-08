package com.antbear.pwneyes.fragments

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.antbear.pwneyes.R
import com.antbear.pwneyes.adapters.ConnectionPagerAdapter
import com.antbear.pwneyes.data.Connection
import com.antbear.pwneyes.databinding.DialogAddConnectionBinding
import com.antbear.pwneyes.databinding.FragmentHomeBinding
import com.antbear.pwneyes.viewmodels.HomeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var pagerAdapter: ConnectionPagerAdapter
    private var tabMediator: TabLayoutMediator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPager()
        observeConnections()
        binding.fabAdd.setOnClickListener { showAddConnectionDialog() }
    }

    // -------------------------------------------------------------------------
    // ViewPager2 + TabLayout
    // -------------------------------------------------------------------------
    private fun setupPager() {
        pagerAdapter = ConnectionPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
    }

    /**
     * Called every time the connection list changes (add / delete / status update).
     * Re-attaches the mediator so tab labels and dot colors reflect the latest state.
     */
    private fun bindTabs(connections: List<Connection>) {
        // Detach old mediator before swapping data to avoid stale callbacks.
        tabMediator?.detach()

        pagerAdapter.submitList(connections)

        tabMediator = TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val c = pagerAdapter.getConnection(position)

            // Dot color: green when connected, red when last check failed, grey if never checked.
            val dotColor = when {
                c.isConnected -> ContextCompat.getColor(requireContext(), R.color.status_connected)
                c.lastConnectedMs > 0 -> ContextCompat.getColor(requireContext(), R.color.status_disconnected)
                else -> ContextCompat.getColor(requireContext(), R.color.status_unknown)
            }

            // "● Name" — only the bullet gets a custom color; the name follows the theme.
            val label = SpannableString("● ${c.name}")
            label.setSpan(ForegroundColorSpan(dotColor), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            tab.text = label
        }.also { it.attach() }
    }

    private fun observeConnections() {
        viewModel.allConnections.observe(viewLifecycleOwner) { connections ->
            val hasItems = connections.isNotEmpty()
            binding.emptyState.visibility     = if (hasItems) View.GONE  else View.VISIBLE
            binding.pagerContainer.visibility = if (hasItems) View.VISIBLE else View.GONE
            if (hasItems) bindTabs(connections)
        }
    }

    // -------------------------------------------------------------------------
    // Add connection dialog
    // -------------------------------------------------------------------------
    private fun showAddConnectionDialog() {
        val dialogBinding = DialogAddConnectionBinding.inflate(layoutInflater)

        val defaultPort = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getString("pref_default_port", "8080") ?: "8080"
        dialogBinding.etPort.setText(defaultPort)

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

                    !isValidPort(dialogBinding.etPort.text?.toString() ?: "") ->
                        dialogBinding.tilPort.error = getString(R.string.error_invalid_port)

                    else -> {
                        val port = dialogBinding.etPort.text?.toString()?.toIntOrNull() ?: 8080
                        dialogBinding.tilName.error = null
                        dialogBinding.tilIp.error   = null
                        dialogBinding.tilPort.error = null
                        viewModel.insert(
                            Connection(
                                name = name,
                                ipAddress = ip,
                                port = port,
                                username = dialogBinding.etUsername.text?.toString()?.trim()?.ifBlank { null },
                                password = dialogBinding.etPassword.text?.toString()?.ifBlank { null }
                            )
                        )
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
     */
    private fun normalizeIp(raw: String): String =
        raw.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore(":")
            .trim()

    /**
     * Returns true if [ip] is a valid IPv4 address (each octet 0–255).
     * Always call on the result of [normalizeIp].
     */
    private fun isValidIp(ip: String): Boolean {
        val regex = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")
        val match = regex.matchEntire(ip) ?: return false
        return match.groupValues.drop(1).all { it.toInt() in 0..255 }
    }

    private fun isValidPort(raw: String): Boolean {
        val port = raw.trim().toIntOrNull() ?: return false
        return port in 1..65535
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------
    override fun onDestroyView() {
        tabMediator?.detach()
        tabMediator = null
        super.onDestroyView()
        _binding = null
    }
}
