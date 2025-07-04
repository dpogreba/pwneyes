package com.antbear.pwneyes.navigation

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.ui.AppBarConfiguration
import com.antbear.pwneyes.PwnEyesApplication
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
                R.id.nav_whats_new -> {
                    showWhatsNewDialog()
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
            // Get the Bundle from NavArgs using reflection
            val bundleMethod = args.javaClass.getMethod("toBundle")
            val bundle = bundleMethod.invoke(args) as Bundle
            
            navController.navigate(actionId, bundle)
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
    
    /**
     * Shows the "What's New" dialog and hides the menu item afterward
     */
    private fun showWhatsNewDialog() {
        try {
            // Get the application instance
            val application = activity.application as PwnEyesApplication
            
            // Show the release notes dialog
            val releaseNotesManager = application.releaseNotesManager
            val shown = releaseNotesManager?.showWhatsNewDialog() ?: false
            
            if (shown) {
                Log.d(TAG, "What's New dialog shown successfully")
                
                // Update menu to hide the What's New item
                activity.invalidateOptionsMenu()
            } else {
                Log.w(TAG, "Failed to show What's New dialog")
                Toast.makeText(
                    activity,
                    "Could not display release notes at this time",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing What's New dialog", e)
            Toast.makeText(
                activity,
                "Error displaying release notes: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
