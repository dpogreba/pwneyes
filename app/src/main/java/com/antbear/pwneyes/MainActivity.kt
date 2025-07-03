package com.antbear.pwneyes

import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.antbear.pwneyes.billing.BillingManager
import com.antbear.pwneyes.databinding.ActivityMainBinding
import com.antbear.pwneyes.navigation.NavigationManager
import com.antbear.pwneyes.util.BluetoothUtils
import com.antbear.pwneyes.util.NetworkUtils
import com.antbear.pwneyes.util.VersionManager
import com.google.android.material.navigation.NavigationView
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
    private var bluetoothUtils: BluetoothUtils? = null
    
    // Version management
    private var versionManager: VersionManager? = null
    
    // Track premium status
    private var isPremium = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Enable edge-to-edge display for Android 15 compatibility
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // Get dependencies from application instance
            val app = application as PwnEyesApplication
            billingManager = app.billingManager
            networkUtils = app.networkUtils
            versionManager = app.versionManager
            
            // Initialize BluetoothUtils
            bluetoothUtils = BluetoothUtils(this)
            
            // Setup billing and observe premium status changes
            setupBilling()
            
            // Inflate layout using ViewBinding
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Setup window insets handling to fix status bar overlap
            setupWindowInsets()

            // Set up UI components
            setupNavigation(savedInstanceState)
            
            // Check network connectivity
            checkNetworkConnectivity()
            
            // Check Bluetooth tethering status
            checkBluetoothTethering()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error initializing application", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Setup window insets handling to prevent status bar overlap
     * This fixes the issue where "Home" text appears under the status bar
     */
    private fun setupWindowInsets() {
        try {
            Log.d(TAG, "Setting up window insets for status bar handling")
            
            // Apply window insets to the main content area
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                
                Log.d(TAG, "Window insets - top: ${insets.top}, bottom: ${insets.bottom}, left: ${insets.left}, right: ${insets.right}")
                
                // Apply top padding to account for status bar
                // The AppBarLayout will handle the status bar area
                view.setPadding(
                    insets.left,
                    0, // Don't add top padding to root - let AppBarLayout handle it
                    insets.right,
                    0  // Don't add bottom padding to root - let individual components handle it
                )
                
                // Apply insets to the AppBarLayout to push it below the status bar
                binding.appBarMain.setPadding(
                    0,
                    insets.top, // This pushes the toolbar below the status bar
                    0,
                    0
                )
                
                // Apply bottom insets to the ad container to account for navigation bar
                binding.adContainer.setPadding(
                    binding.adContainer.paddingLeft,
                    binding.adContainer.paddingTop,
                    binding.adContainer.paddingRight,
                    binding.adContainer.paddingBottom + insets.bottom
                )
                
                // Return the insets to allow other views to handle them
                windowInsets
            }
            
            Log.d(TAG, "Window insets setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up window insets", e)
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
    
    /**
     * Check network connectivity and show appropriate messages to the user
     * Using the improved NetworkUtils methods
     */
    private fun checkNetworkConnectivity() {
        try {
            // First check if network is even available
            val isNetworkAvailable = networkUtils?.isNetworkAvailable() ?: false
            if (!isNetworkAvailable) {
                showNetworkErrorMessage("No network connection. Please connect to WiFi or mobile data.")
                return
            }
            
            // Use the coroutine-based approach for actual internet connectivity check
            networkUtils?.let { utils ->
                lifecycleScope.launch {
                    try {
                        utils.checkInternetWithCoroutines(lifecycleScope) { hasInternet ->
                            if (!hasInternet) {
                                // Get a more detailed error message
                                val errorMessage = utils.getNetworkErrorMessage()
                                showNetworkErrorMessage(errorMessage)
                                
                                // Try to recover connectivity automatically
                                utils.attemptConnectivityRecovery()
                            } else {
                                Log.d(TAG, "Internet connectivity confirmed")
                                // Log detailed network info for debugging
                                Log.d(TAG, "Network info: ${utils.getNetworkInfoForLogging()}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking internet connectivity", e)
                        showNetworkErrorMessage("Error checking network status. Please try again.")
                    }
                }
            } ?: run {
                Log.e(TAG, "NetworkUtils not available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkNetworkConnectivity", e)
            showNetworkErrorMessage("Network check failed. Please check your connection.")
        }
    }
    
    /**
     * Show a network error message to the user with proper error handling
     */
    private fun showNetworkErrorMessage(message: String) {
        try {
            // Always show messages on the main thread
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
            } else {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                }
            }
            // Log the message as well
            Log.w(TAG, "Network error: $message")
        } catch (e: Exception) {
            // Last resort error handling
            Log.e(TAG, "Error showing network message", e)
        }
    }

    private fun setupNavigation(savedInstanceState: Bundle?) {
        Log.d(TAG, "🔍 setupNavigation called")
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
            
            // Add debug logging for navigation issues
            navController.addOnDestinationChangedListener { _, destination, _ ->
                Log.d(TAG, "🔍 Navigation destination changed: ${destination.label} (id: ${destination.id})")
            }
            
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
            
            // Set up the "See What's New" menu item visibility
            updateWhatsNewMenuItemVisibility()
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
    
    /**
     * Updates the visibility of the "See What's New" menu item based on app update status
     */
    private fun updateWhatsNewMenuItemVisibility() {
        try {
            Log.d(TAG, "Updating 'What's New' menu item visibility")
            
            // Get the navigation view
            val navigationView = binding.navView
            
            // Get the menu
            val menu = navigationView.menu
            
            // Find the "What's New" menu item
            val whatsNewItem = menu.findItem(R.id.nav_whats_new)
            
            // Check if the app was updated and the what's new dialog hasn't been seen yet
            val shouldShow = versionManager?.shouldShowWhatsNewButton() ?: false
            
            // Update visibility
            whatsNewItem?.isVisible = shouldShow
            
            Log.d(TAG, "What's New menu item visibility set to: $shouldShow")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating What's New menu item visibility", e)
        }
    }
    
    /**
     * Called when invalidateOptionsMenu() is called
     */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        updateWhatsNewMenuItemVisibility()
        return super.onPrepareOptionsMenu(menu)
    }
    
    override fun onDestroy() {
        try {
            // Remove premium status observer to prevent memory leaks
            billingManager?.premiumStatus?.removeObservers(this)

            // Release any other resources
            binding.drawerLayout.removeDrawerListener(toggle)
            
            // Stop network monitoring to free up resources
            networkUtils?.stopNetworkMonitoring()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }

        super.onDestroy()
    }
    
    /**
     * Called when the app is resumed.
     * Good opportunity to check network status again.
     */
    /**
     * Check if Bluetooth tethering is enabled and show a warning dialog if it's not
     */
    private fun checkBluetoothTethering() {
        try {
            // Show dialog if needed (only if the setting is enabled and tethering is disabled)
            bluetoothUtils?.showBluetoothTetheringDialogIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Bluetooth tethering", e)
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        try {
            // Check network connectivity again when app is resumed
            checkNetworkConnectivity()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume", e)
        }
    }
}
