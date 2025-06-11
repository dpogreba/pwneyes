package com.antbear.pwneyes

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatDelegate
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.antbear.pwneyes.billing.BillingManager
import com.antbear.pwneyes.databinding.ActivityMainBinding
import com.antbear.pwneyes.util.AdsManager

class MainActivity : AppCompatActivity() {
    
    private val TAG = "MainActivity"
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var toggle: ActionBarDrawerToggle
    private var billingManager: BillingManager? = null
    private var isPremium = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Get the billing manager from the application
            billingManager = (application as PwnEyesApplication).billingManager
            
            // Observe premium status changes if billing manager is available
            billingManager?.let { manager ->
                manager.premiumStatus.observe(this, Observer { premium ->
                    isPremium = premium
                    invalidateOptionsMenu() // Refresh the options menu
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing billing manager in MainActivity", e)
            // Default to not premium if there's an error
            isPremium = false
        }

        // Inflate layout using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            // Set up Toolbar as the ActionBar
            setSupportActionBar(binding.toolbar)

            // Set up NavHostFragment and NavController
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
                    as? NavHostFragment 
            
            if (navHostFragment == null) {
                Log.e(TAG, "NavHostFragment not found in activity_main.xml")
                Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show()
                return
            }
            
            navController = navHostFragment.navController

            // Configure AppBarConfiguration with top-level destinations and the DrawerLayout
            appBarConfiguration = AppBarConfiguration(
                setOf(R.id.homeFragment, R.id.addConnectionFragment, R.id.nav_settings),
                binding.drawerLayout
            )

            // Link the ActionBar and NavigationView with the NavController
            setupActionBarWithNavController(navController, appBarConfiguration)
            binding.navView.setupWithNavController(navController)

            // Set up the ActionBarDrawerToggle to display the default hamburger icon on the top left
            toggle = ActionBarDrawerToggle(
                this,
                binding.drawerLayout,
                binding.toolbar,
                R.string.navigation_drawer_open,   // Ensure these strings exist in res/values/strings.xml
                R.string.navigation_drawer_close
            )
            binding.drawerLayout.addDrawerListener(toggle)
            toggle.syncState()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up navigation", e)
            Toast.makeText(this, "Error initializing application", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        try {
            menuInflater.inflate(R.menu.main, menu)
            
            // Add debug logging
            Log.d(TAG, "Creating options menu. billingManager is ${if (billingManager != null) "not null" else "null"}")
            Log.d(TAG, "Premium status is $isPremium")
            
            // Display a Toast with billing status (will help for debugging)
            Toast.makeText(this, 
                "Billing: ${if (billingManager != null) "Available" else "Not Available"} | Premium: $isPremium", 
                Toast.LENGTH_LONG).show()
            
            // Adjust menu based on premium status
            val removeAdsItem = menu.findItem(R.id.action_remove_ads)
            val restorePurchasesItem = menu.findItem(R.id.action_restore_purchases)
            
            removeAdsItem?.isVisible = !isPremium && billingManager != null
            restorePurchasesItem?.isVisible = billingManager != null
            
            Log.d(TAG, "Menu items visibility - Remove Ads: ${removeAdsItem?.isVisible}, Restore: ${restorePurchasesItem?.isVisible}")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating options menu", e)
        }
        
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return try {
            when (item.itemId) {
                R.id.action_settings -> {
                    navController.navigate(R.id.nav_settings)
                    true
                }
                R.id.action_remove_ads -> {
                    // Launch the purchase flow
                    billingManager?.launchPurchaseFlow(this) ?: run {
                        Toast.makeText(this, "Billing not available", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.action_restore_purchases -> {
                    // Restore purchases
                    billingManager?.restorePurchases() ?: run {
                        Toast.makeText(this, "Billing not available", Toast.LENGTH_SHORT).show()
                    }
                    Toast.makeText(this, "Restoring purchases...", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling menu item selection", e)
            super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return try {
            NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating up", e)
            super.onSupportNavigateUp()
        }
    }
}
