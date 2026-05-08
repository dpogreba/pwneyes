package com.antbear.pwneyes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.antbear.pwneyes.databinding.ActivityMainBinding
import com.antbear.pwneyes.fragments.EditConnectionsFragment
import com.antbear.pwneyes.fragments.HomeFragment
import com.antbear.pwneyes.fragments.SettingsFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupDrawer()
        setupBuyCoffeeButton()

        // Default to Home on first launch
        if (savedInstanceState == null) {
            navigateTo(HomeFragment(), R.string.nav_home, R.id.nav_home)
        }

        checkForUpdates()
    }

    // -------------------------------------------------------------------------
    // Toolbar
    // -------------------------------------------------------------------------
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    // -------------------------------------------------------------------------
    // Navigation drawer
    // -------------------------------------------------------------------------
    private fun setupDrawer() {
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.nav_open_drawer,
            R.string.nav_close_drawer
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)
        // Check Home as the default selected item
        binding.navView.setCheckedItem(R.id.nav_home)
    }

    private fun setupBuyCoffeeButton() {
        binding.navFooter.btnBuyCoffee.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.buymeacoffee.com/ltldrk")))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    // -------------------------------------------------------------------------
    // Update checker — queries GitHub Releases API on a daemon thread.
    // Note: true silent/automatic updates are not possible on Android without
    // an app store.  This notifies the user and opens the download page;
    // the final "Install" tap is always theirs.
    // -------------------------------------------------------------------------
    private fun checkForUpdates() {
        Thread {
            val latest = UpdateChecker.latestVersionIfNewer(BuildConfig.VERSION_NAME)
            if (latest != null) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) showUpdateDialog(latest)
                }
            }
        }.also { it.isDaemon = true }.start()
    }

    private fun showUpdateDialog(version: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.update_dialog_title))
            .setMessage(getString(R.string.update_dialog_message, version))
            .setPositiveButton(R.string.update_dialog_download) { _, _ ->
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(UpdateChecker.RELEASES_PAGE))
                )
            }
            .setNegativeButton(R.string.update_dialog_later, null)
            .show()
    }

    // -------------------------------------------------------------------------
    // NavigationView item selection
    // -------------------------------------------------------------------------
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home ->
                navigateTo(HomeFragment(), R.string.nav_home, R.id.nav_home)
            R.id.nav_edit_connections ->
                navigateTo(EditConnectionsFragment(), R.string.nav_edit_connections, R.id.nav_edit_connections)
            R.id.nav_settings ->
                navigateTo(SettingsFragment(), R.string.nav_settings, R.id.nav_settings)
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun navigateTo(fragment: Fragment, titleRes: Int, navItemId: Int) {
        supportActionBar?.setTitle(titleRes)
        binding.navView.setCheckedItem(navItemId)
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    // -------------------------------------------------------------------------
    // Back button — close drawer first if open
    // -------------------------------------------------------------------------
    @Deprecated("Using onBackPressedDispatcher pattern for API 33+")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
