package com.antbear.pwneyes

import android.os.Bundle
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

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var billingManager: BillingManager
    private var isPremium = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the billing manager from the application
        billingManager = (application as PwnEyesApplication).billingManager
        
        // Observe premium status changes
        billingManager.premiumStatus.observe(this, Observer { premium ->
            isPremium = premium
            invalidateOptionsMenu() // Refresh the options menu
        })

        // Inflate layout using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up Toolbar as the ActionBar
        setSupportActionBar(binding.toolbar)

        // Set up NavHostFragment and NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
                as? NavHostFragment ?: throw IllegalStateException("NavHostFragment not found in activity_main.xml.")
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
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        
        // Adjust menu based on premium status
        menu.findItem(R.id.action_remove_ads).isVisible = !isPremium
        menu.findItem(R.id.action_restore_purchases).isVisible = true
        
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                navController.navigate(R.id.nav_settings)
                true
            }
            R.id.action_remove_ads -> {
                // Launch the purchase flow
                billingManager.launchPurchaseFlow(this)
                true
            }
            R.id.action_restore_purchases -> {
                // Restore purchases
                billingManager.restorePurchases()
                Toast.makeText(this, "Restoring purchases...", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}
