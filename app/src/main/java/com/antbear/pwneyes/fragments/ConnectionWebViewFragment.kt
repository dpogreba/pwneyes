package com.antbear.pwneyes.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.antbear.pwneyes.R
import com.antbear.pwneyes.databinding.FragmentConnectionWebviewBinding
import com.antbear.pwneyes.viewmodels.HomeViewModel
import java.net.HttpURLConnection
import java.net.URL

/**
 * Displays a single Pwnagotchi web UI in a full-screen WebView.
 *
 * On creation it runs a background HTTP HEAD check against [ARG_URL] to
 * determine reachability, then:
 *  - Connected  → hides the status bar so the web UI fills the screen
 *  - Unreachable → shows a red banner with a Retry button
 *
 * The retry button repeats both the connectivity check and the WebView
 * reload so the user never needs to leave the app.
 */
class ConnectionWebViewFragment : Fragment() {

    private var _binding: FragmentConnectionWebviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()

    private var connectionId: Long = 0
    private var connectionUrl: String = ""
    private var connectionName: String = ""

    private val autoRefreshHandler = Handler(Looper.getMainLooper())
    private var autoRefreshRunnable: Runnable? = null

    companion object {
        private const val ARG_ID   = "connection_id"
        private const val ARG_URL  = "connection_url"
        private const val ARG_NAME = "connection_name"

        fun newInstance(id: Long, url: String, name: String) =
            ConnectionWebViewFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ID, id)
                    putString(ARG_URL, url)
                    putString(ARG_NAME, name)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectionWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        connectionId   = arguments?.getLong(ARG_ID)      ?: return
        connectionUrl  = arguments?.getString(ARG_URL)   ?: return
        connectionName = arguments?.getString(ARG_NAME)  ?: ""

        setupWebView(connectionUrl)
        checkAndLoad(connectionId, connectionUrl)

        binding.swipeRefresh.setColorSchemeColors(
            requireContext().getColor(R.color.blue_500)
        )
        binding.swipeRefresh.setOnRefreshListener {
            binding.progressBar.visibility = View.VISIBLE
            binding.webView.reload()
            checkAndLoad(connectionId, connectionUrl)
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_webview, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_open_browser -> {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(connectionUrl)))
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner)
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------
    override fun onResume() {
        super.onResume()
        scheduleAutoRefresh()
    }

    override fun onPause() {
        super.onPause()
        cancelAutoRefresh()
    }

    override fun onDestroyView() {
        cancelAutoRefresh()
        binding.webView.stopLoading()
        binding.webView.destroy()
        super.onDestroyView()
        _binding = null
    }

    // -------------------------------------------------------------------------
    // Auto-refresh
    // -------------------------------------------------------------------------
    private fun scheduleAutoRefresh() {
        val intervalMs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getString("pref_auto_refresh", "0")?.toLongOrNull() ?: 0L
        if (intervalMs <= 0L) return
        autoRefreshRunnable = Runnable {
            if (_binding != null) {
                binding.swipeRefresh.isRefreshing = true
                binding.webView.reload()
                checkAndLoad(connectionId, connectionUrl)
                scheduleAutoRefresh()
            }
        }
        autoRefreshHandler.postDelayed(autoRefreshRunnable!!, intervalMs)
    }

    private fun cancelAutoRefresh() {
        autoRefreshRunnable?.let { autoRefreshHandler.removeCallbacks(it) }
        autoRefreshRunnable = null
    }

    // -------------------------------------------------------------------------
    // WebView
    // -------------------------------------------------------------------------
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(url: String) {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled    = true   // Pwnagotchi's web UI requires JS
                domStorageEnabled    = true
                loadWithOverviewMode = true
                useWideViewPort      = true
                builtInZoomControls  = true
                displayZoomControls  = false
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    _binding?.progressBar?.visibility = View.GONE
                    _binding?.swipeRefresh?.isRefreshing = false
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (request.isForMainFrame) {
                        _binding?.progressBar?.visibility = View.GONE
                        _binding?.swipeRefresh?.isRefreshing = false
                        showStatus(connected = false)
                    }
                }
            }
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.webView.loadUrl(url)
    }

    // -------------------------------------------------------------------------
    // Connectivity check
    // -------------------------------------------------------------------------
    /**
     * Runs an HTTP HEAD probe on [url] on a background thread.
     * Updates the Room DB status and refreshes the status banner.
     * Also wired to the Retry button so the user can re-trigger manually.
     */
    private fun checkAndLoad(id: Long, url: String) {
        showStatus(checking = true)

        Thread {
            val reachable = try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "HEAD"
                val timeoutMs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("pref_timeout", "4000")?.toIntOrNull() ?: 4_000
                conn.connectTimeout = timeoutMs
                conn.readTimeout    = timeoutMs
                val code = conn.responseCode
                conn.disconnect()
                code < 400
            } catch (_: Exception) { false }

            // Persist status to Room so the tab dot updates via LiveData.
            viewModel.setConnectionStatus(id, reachable)

            if (isAdded && _binding != null) {
                requireActivity().runOnUiThread {
                    if (_binding != null) showStatus(connected = reachable)
                }
            }
        }.also { it.isDaemon = true }.start()

        binding.btnRetry.setOnClickListener {
            cancelAutoRefresh()
            binding.progressBar.visibility = View.VISIBLE
            binding.webView.reload()
            checkAndLoad(connectionId, connectionUrl)
        }
    }

    // -------------------------------------------------------------------------
    // Status banner
    // -------------------------------------------------------------------------
    private fun showStatus(checking: Boolean = false, connected: Boolean = false) {
        when {
            checking -> {
                binding.statusBar.setBackgroundColor(
                    requireContext().getColor(R.color.status_checking)
                )
                binding.tvStatus.text        = getString(R.string.status_checking)
                binding.btnRetry.visibility  = View.GONE
                binding.statusBar.visibility = View.VISIBLE
            }
            connected -> {
                // Success — hide the banner entirely; the web UI says it all.
                binding.swipeRefresh.isRefreshing = false
                binding.statusBar.visibility = View.GONE
            }
            else -> {
                binding.swipeRefresh.isRefreshing = false
                binding.statusBar.setBackgroundColor(
                    requireContext().getColor(R.color.status_disconnected)
                )
                binding.tvStatus.text = if (connectionName.isNotBlank())
                    getString(R.string.status_unreachable_named, connectionName)
                else
                    getString(R.string.status_unreachable)
                binding.btnRetry.visibility  = View.VISIBLE
                binding.statusBar.visibility = View.VISIBLE
            }
        }
    }
}
