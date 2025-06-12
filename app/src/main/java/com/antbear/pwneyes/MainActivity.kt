package com.antbear.pwneyes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.bumptech.glide.Glide
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
            
            // Set up custom navigation item selection listener
            binding.navView.setNavigationItemSelectedListener { menuItem ->
                try {
                    // Close drawer first to improve perceived responsiveness
                    binding.drawerLayout.closeDrawers()
                    
                    // Log the navigation attempt
                    Log.d(TAG, "Navigation item selected: ${menuItem.title}")
                    
                    // Handle navigation based on the selected item's ID
                    when (menuItem.itemId) {
                        R.id.homeFragment -> {
                            // Force navigation to home even if we're already there
                            navController.popBackStack(R.id.homeFragment, false)
                            navController.navigate(R.id.homeFragment)
                            true
                        }
                        R.id.addConnectionFragment -> {
                            navController.navigate(R.id.addConnectionFragment)
                            true
                        }
                        R.id.nav_settings -> {
                            navController.navigate(R.id.nav_settings)
                            true
                        }
                        else -> false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to selected item", e)
                    Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
                    false
                }
            }

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
            
            // Make sure we start at the home fragment
            if (savedInstanceState == null) {
                navController.navigate(R.id.homeFragment)
            }
            
            // Set up Buy Me Coffee button in the navigation drawer footer
            setupBuyMeCoffeeButton()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up navigation", e)
            Toast.makeText(this, "Error initializing application", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        try {
            menuInflater.inflate(R.menu.main, menu)
            
            // Log billing status for debugging
            Log.d(TAG, "Creating options menu. billingManager is ${if (billingManager != null) "not null" else "null"}")
            Log.d(TAG, "Premium status is $isPremium")
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
    
    private fun setupBuyMeCoffeeButton() {
        try {
            // Get the parent view that contains the NavigationView
            val parent = binding.navView.parent as ViewGroup
            
            // Get index of NavigationView in its parent
            val navViewIndex = parent.indexOfChild(binding.navView)
            
            // Create a new container to hold both the NavigationView and the footer
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = binding.navView.layoutParams
            }
            
            // Remove NavigationView from its parent
            parent.removeView(binding.navView)
            
            // Change NavigationView layout params to have weight 1 (will expand to fill space)
            binding.navView.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                0, // Height will be determined by weight
                1f  // Weight of 1 makes it expand
            )
            
            // Add NavigationView to the container
            container.addView(binding.navView)
            
            // Inflate the Buy Me Coffee footer layout
            val footerView = layoutInflater.inflate(R.layout.nav_footer_buy_me_coffee, container, false)
            
            // Add the footer view to the bottom of the container
            container.addView(footerView)
            
            // Add the container to the parent where NavigationView was
            parent.addView(container, navViewIndex)
            
            // Find the Buy Me Coffee image in the inflated footer view
            val buyMeCoffeeImage = footerView.findViewById<ImageView>(R.id.imgBuyMeCoffee)
            
            // Load the official Buy Me Coffee button image using Glide
            Glide.with(this)
                .load("https://img.buymeacoffee.com/button-api/?text=Buy%20our%20dogs%20a%20treat%21&emoji=☕&slug=ltldrk&button_colour=FFDD00&font_colour=000000&font_family=Cookie&outline_colour=000000&coffee_colour=ffffff")
                .into(buyMeCoffeeImage)
            
            // Set click listener to open the Buy Me Coffee URL in a browser
            buyMeCoffeeImage.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/ltldrk"))
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening Buy Me Coffee link", e)
                    Toast.makeText(this, "Could not open browser", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up Buy Me Coffee button", e)
        }
    }
}
