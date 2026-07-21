package com.antbear.pwneyes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.antbear.pwneyes.databinding.ActivityMainBinding
import com.antbear.pwneyes.databinding.DialogDownloadProgressBinding
import com.antbear.pwneyes.fragments.EditConnectionsFragment
import com.antbear.pwneyes.fragments.HomeFragment
import com.antbear.pwneyes.fragments.SettingsFragment
import com.antbear.pwneyes.notify.PwnStatusWorker
import com.antbear.pwneyes.update.ApkUpdater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle

    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result handled lazily */ }

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

        // Background status poller for handshake / cracked-key notifications (idempotent).
        PwnStatusWorker.schedule(this)
        maybeRequestNotifPermission()
    }

    private fun maybeRequestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
            val update = UpdateChecker.latestUpdateIfNewer(BuildConfig.VERSION_NAME)
            if (update != null) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) showUpdateDialog(update)
                }
            }
        }.also { it.isDaemon = true }.start()
    }

    private fun showUpdateDialog(update: UpdateChecker.UpdateInfo) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.update_dialog_title))
            .setMessage(getString(R.string.update_dialog_message, update.version))
            .setPositiveButton(R.string.update_dialog_download) { _, _ -> startUpdate(update) }
            .setNegativeButton(R.string.update_dialog_later, null)
            .show()
    }

    /**
     * Download-and-install path. Every branch falls back to the releases page so a
     * failure never leaves the user stuck with no way to update.
     */
    private fun startUpdate(update: UpdateChecker.UpdateInfo) {
        val apkUrl = update.apkUrl
        when {
            apkUrl == null -> openReleasesPage()                     // release has no APK asset
            !ApkUpdater.canRequestInstall(this) -> promptInstallPermission()
            else -> downloadAndInstall(apkUrl)
        }
    }

    /** Android 8+: the user grants "install unknown apps" once, in system settings. */
    private fun promptInstallPermission() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.update_install_permission_title)
            .setMessage(R.string.update_install_permission_message)
            .setPositiveButton(R.string.update_open_settings) { _, _ ->
                runCatching { startActivity(ApkUpdater.installPermissionIntent(this)) }
                    .onFailure { openReleasesPage() }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun downloadAndInstall(apkUrl: String) {
        val progressBinding = DialogDownloadProgressBinding.inflate(layoutInflater)
        val progressDialog = MaterialAlertDialogBuilder(this)
            .setView(progressBinding.root)
            .setCancelable(false)
            .create()
        progressDialog.show()

        // Resolve the context on the main thread; the worker touches nothing
        // lifecycle-bound (see the probe-thread crash this app already fixed once).
        val ctx = applicationContext

        Thread {
            val file = ApkUpdater.downloadApk(ctx, apkUrl) { pct ->
                runOnUiThread { progressBinding.progressDownload.progress = pct }
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                progressDialog.dismiss()
                when {
                    file == null ->
                        updateFailed(R.string.update_download_failed)
                    !ApkUpdater.signatureMatchesInstalledApp(ctx, file) -> {
                        file.delete()
                        updateFailed(R.string.update_signature_mismatch)
                    }
                    else ->
                        runCatching { startActivity(ApkUpdater.installIntent(ctx, file)) }
                            .onFailure { updateFailed(R.string.update_download_failed) }
                }
            }
        }.also { it.isDaemon = true }.start()
    }

    private fun updateFailed(messageRes: Int) {
        MaterialAlertDialogBuilder(this)
            .setMessage(messageRes)
            .setPositiveButton(R.string.update_open_browser) { _, _ -> openReleasesPage() }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun openReleasesPage() {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateChecker.RELEASES_PAGE)))
        }
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
