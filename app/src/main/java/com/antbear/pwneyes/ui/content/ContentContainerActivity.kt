package com.antbear.pwneyes.ui.content

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.antbear.pwneyes.R
import com.antbear.pwneyes.databinding.ActivityContentContainerBinding

/**
 * This activity serves as a standalone container for content fragments.
 * It uses a single-fragment layout to display one fragment at a time,
 * ensuring that fragments like Plugins, Profile, etc. are displayed
 * as full-screen experiences independent of the main navigation flow.
 */
class ContentContainerActivity : AppCompatActivity() {
    private val TAG = "ContentContainerActivity"
    private lateinit var binding: ActivityContentContainerBinding
    
    companion object {
        private const val EXTRA_FRAGMENT_ID = "fragment_id"
        private const val EXTRA_ARGS = "fragment_args"
        
        /**
         * Create an intent to launch this activity with specific fragment
         */
        fun createIntent(
            context: Context,
            fragmentId: Int,
            args: Bundle? = null
        ): Intent {
            return Intent(context, ContentContainerActivity::class.java).apply {
                putExtra(EXTRA_FRAGMENT_ID, fragmentId)
                if (args != null) {
                    putExtra(EXTRA_ARGS, args)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityContentContainerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Get fragment ID and arguments from intent
        val fragmentId = intent.getIntExtra(EXTRA_FRAGMENT_ID, -1)
        val fragmentArgs = intent.getBundleExtra(EXTRA_ARGS)
        
        if (fragmentId == -1) {
            Log.e(TAG, "No fragment ID specified")
            finish()
            return
        }
        
        // Set up the NavHostFragment with a runtime-created navigation graph
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.container_nav_host_fragment) as NavHostFragment
        
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.nav_graph)
        
        // Set the start destination to the requested fragment
        graph.setStartDestination(fragmentId)
        
        // Set the graph on the NavController with the fragment arguments
        navHostFragment.navController.setGraph(graph, fragmentArgs)
        
        // Set toolbar title based on fragment
        when (fragmentId) {
            R.id.nav_plugins -> supportActionBar?.setTitle(R.string.plugins_title)
            // Add other fragments here as they are implemented
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        // Handle the back button in the toolbar
        finish()
        return true
    }
}
