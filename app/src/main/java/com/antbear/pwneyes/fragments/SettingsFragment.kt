package com.antbear.pwneyes.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.antbear.pwneyes.BuildConfig
import com.antbear.pwneyes.R
import com.antbear.pwneyes.databinding.FragmentSettingsBinding
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

    class PreferencesFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            setupThemePref()
            setupVersionPref()
            setupDefaultPortPref()
            setupGithubPref()
            setupWhatsNewPref()
        }

        private fun setupThemePref() {
            findPreference<ListPreference>("pref_theme")?.setOnPreferenceChangeListener { _, value ->
                val mode = when (value as String) {
                    "light"  -> AppCompatDelegate.MODE_NIGHT_NO
                    "dark"   -> AppCompatDelegate.MODE_NIGHT_YES
                    else     -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(mode)
                true
            }
        }

        private fun setupVersionPref() {
            findPreference<Preference>("pref_version")?.summary = BuildConfig.VERSION_NAME
        }

        private fun setupDefaultPortPref() {
            findPreference<EditTextPreference>("pref_default_port")
                ?.setOnBindEditTextListener { editText ->
                    editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    editText.setSelection(editText.text?.length ?: 0)
                }
        }

        private fun setupGithubPref() {
            findPreference<Preference>("pref_github")?.setOnPreferenceClickListener {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/dpogreba/pwneyes"))
                )
                true
            }
        }

        private fun setupWhatsNewPref() {
            findPreference<Preference>("pref_whats_new")?.setOnPreferenceClickListener {
                val content = HtmlCompat.fromHtml(
                    getString(R.string.whats_new_content),
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.pref_whats_new_title)
                    .setMessage(content)
                    .setPositiveButton(R.string.btn_cancel, null)
                    .show()
                true
            }
        }
    }
}
