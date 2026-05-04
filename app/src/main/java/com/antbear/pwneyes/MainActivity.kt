package com.antbear.pwneyes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import coil.load
import com.antbear.pwneyes.databinding.ActivityMainBinding
import com.antbear.pwneyes.fragments.EditConnectionsFragment
import com.antbear.pwneyes.fragments.HomeFragment
import com.antbear.pwneyes.fragments.SettingsFragment
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
        // Load the official BMC badge from their CDN; falls back to ic_coffee if offline
        binding.imgBuyCoffee.load("https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png") {
            placeholder(R.drawable.ic_coffee)
            error(R.drawable.ic_coffee)
        }
        binding.imgBuyCoffee.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.buymeacoffee.com/ltldrk")))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
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
