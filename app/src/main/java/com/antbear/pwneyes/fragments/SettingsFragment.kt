package com.antbear.pwneyes.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.antbear.pwneyes.BuildConfig
import com.antbear.pwneyes.R
import com.antbear.pwneyes.databinding.FragmentSettingsBinding
import com.antbear.pwneyes.viewmodels.HomeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        childFragmentManager
            .beginTransaction()
            .replace(R.id.settings_fragment_container, PreferencesFragment())
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // -------------------------------------------------------------------------
    // Inner PreferenceFragment
    // -------------------------------------------------------------------------
    class PreferencesFragment : PreferenceFragmentCompat() {

        private val viewModel: HomeViewModel by activityViewModels()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            setupThemePref()
            setupVersionPref()
            setupEraseDataPref()
            setupBuyCoffeePref()
        }

        private fun setupThemePref() {
            findPreference<ListPreference>("pref_theme")?.setOnPreferenceChangeListener { _, value ->
                val mode = when (value as String) {
                    "light" -> AppCompatDelegate.MODE_NIGHT_NO
                    "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(mode)
                true
            }
        }

        private fun setupVersionPref() {
            findPreference<Preference>("pref_version")?.summary = BuildConfig.VERSION_NAME
        }

        private fun setupEraseDataPref() {
            findPreference<Preference>("pref_erase_data")?.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete_all_confirm_title)
                    .setMessage(R.string.delete_all_confirm_message)
                    .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.deleteAll() }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
                true
            }
        }

        private fun setupBuyCoffeePref() {
            findPreference<Preference>("pref_buy_coffee")?.setOnPreferenceClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.buymeacoffee.com/ltldrk")))
                true
            }
        }
    }
}
