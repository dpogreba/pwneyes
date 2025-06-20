package com.antbear.pwneyes.navigation

import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.ui.AppBarConfiguration
import com.antbear.pwneyes.R

/**
 * Centralized navigation management for the application.
 * Handles drawer setup, navigation actions, and back stack management.
 */
class NavigationManager(
    private val navController: NavController,
    private val drawerLayout: DrawerLayout,
    private val activity: AppCompatActivity,
    private val toolbar: Toolbar
) {
    private val TAG = "NavigationManager"
    
    // Top-level destinations in the navigation hierarchy
    private val topLevelDestinations = setOf(
        R.id.homeFragment,
        R.id.addConnectionFragment,
        R.id.nav_settings
    )
    
    /**
     * Sets up the AppBarConfiguration for the navigation controller
     */
    fun setupAppBarConfiguration(): AppBarConfiguration {
        return AppBarConfiguration(topLevelDestinations, drawerLayout)
    }
    
    /**
     * Sets up the drawer toggle button
     */
    fun setupDrawerToggle(): ActionBarDrawerToggle {
        val toggle = ActionBarDrawerToggle(
            activity,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        return toggle
    }
    
    /**
     * Handles navigation item selection from the drawer
     * @return true if the item was handled, false otherwise
     */
    fun handleNavigationItemSelected(menuItem: MenuItem): Boolean {
        try {
            // Close drawer first to improve perceived responsiveness
            drawerLayout.closeDrawers()
            
            // Log the navigation attempt
            Log.d(TAG, "Navigation item selected: ${menuItem.title}")
            
            // Handle navigation based on the selected item's ID
            return when (menuItem.itemId) {
                R.id.homeFragment -> {
                    navigateToHome()
                    true
                }
                R.id.addConnectionFragment -> {
                    navigateToDestination(R.id.addConnectionFragment)
                    true
                }
                R.id.nav_settings -> {
                    navigateToDestination(R.id.nav_settings)
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to selected item", e)
            Toast.makeText(activity, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }
    
    /**
     * Navigates to the home fragment, clearing back stack
     */
    private fun navigateToHome() {
        // Force navigation to home even if we're already there
        navController.popBackStack(R.id.homeFragment, false)
        navController.navigate(R.id.homeFragment)
    }
    
    /**
     * Navigates to a specific destination
     */
    private fun navigateToDestination(destinationId: Int) {
        try {
            navController.navigate(destinationId)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to destination: $destinationId", e)
            Toast.makeText(
                activity,
                "Navigation error: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * Navigates to a destination with arguments
     */
    fun navigateWithArgs(actionId: Int, args: androidx.navigation.NavArgs) {
        try {
            navController.navigate(actionId, args.toBundle())
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating with args", e)
            Toast.makeText(
                activity,
                "Navigation error: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * Checks if the current destination is the home fragment
     */
    fun isAtHomeDestination(): Boolean {
        return navController.currentDestination?.id == R.id.homeFragment
    }
    
    /**
     * Performs navigation back in the navigation graph
     * @return true if navigation was handled, false otherwise
     */
    fun navigateBack(): Boolean {
        return try {
            navController.navigateUp()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating back", e)
            false
        }
    }
}
