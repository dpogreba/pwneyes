package com.antbear.pwneyes

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.antbear.pwneyes.billing.BillingManager
import com.antbear.pwneyes.databinding.ActivityMainBinding
import com.antbear.pwneyes.navigation.NavigationManager
import com.antbear.pwneyes.util.NetworkUtils
import kotlinx.coroutines.launch

// TODO: Uncomment when Hilt is properly configured
// import dagger.hilt.android.AndroidEntryPoint
// @AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private val TAG = "MainActivity"
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var navigationManager: NavigationManager
    
    // Dependencies - now manually obtained from Application class
    private var billingManager: BillingManager? = null
    private var networkUtils: NetworkUtils? = null
    
    // Track premium status
    private var isPremium = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Get dependencies from application instance
            billingManager = (application as PwnEyesApplication).billingManager
            networkUtils = (application as PwnEyesApplication).networkUtils
            
            // Setup billing and observe premium status changes
            setupBilling()
            
            // Inflate layout using ViewBinding
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Set up UI components
            setupNavigation(savedInstanceState)
            
            // Check network connectivity
            checkNetworkConnectivity()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error initializing application", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupBilling() {
        try {
            // Observe premium status changes if billing manager is available
            billingManager?.let { manager ->
                manager.premiumStatus.observe(this) { premium ->
                    isPremium = premium
                    invalidateOptionsMenu() // Refresh the options menu
                    Log.d(TAG, "Premium status updated: $isPremium")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing billing manager", e)
            // Default to not premium if there's an error
            isPremium = false
        }
    }
    
    private fun checkNetworkConnectivity() {
        lifecycleScope.launch {
            try {
                networkUtils?.let { utils ->
                    // Use old API until we implement Coroutines fully
                    utils.checkInternetAsync { hasInternet ->
                        if (!hasInternet) {
                            Toast.makeText(
                                this@MainActivity, 
                                "No internet connection detected. Some features may not work properly.", 
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking network connectivity", e)
            }
        }
    }

    private fun setupNavigation(savedInstanceState: Bundle?) {
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

            // Initialize NavigationManager
            navigationManager = NavigationManager(
                navController = navController,
                drawerLayout = binding.drawerLayout,
                activity = this,
                toolbar = binding.toolbar
            )
            
            // Configure AppBarConfiguration with top-level destinations
            appBarConfiguration = navigationManager.setupAppBarConfiguration()

            // Link the ActionBar with the NavController
            setupActionBarWithNavController(navController, appBarConfiguration)
            
            // Setup navigation drawer
            toggle = navigationManager.setupDrawerToggle()
            
            // Set up custom navigation item selection listener
            binding.navView.setNavigationItemSelectedListener { menuItem ->
                navigationManager.handleNavigationItemSelected(menuItem)
            }
            
            // Make sure we start at the home fragment
            if (savedInstanceState == null) {
                navController.navigate(R.id.homeFragment)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up navigation", e)
            Toast.makeText(this, "Error initializing navigation", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Return false to hide the menu (three dots in top-right corner)
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return try {
            when (item.itemId) {
                R.id.action_settings -> {
                    navController.navigate(R.id.nav_settings)
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
    
    override fun onDestroy() {
        try {
            // Remove premium status observer to prevent memory leaks
            billingManager?.premiumStatus?.removeObservers(this)
            
            // Release any other resources
            binding.drawerLayout.removeDrawerListener(toggle)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
        
        super.onDestroy()
    }
}
