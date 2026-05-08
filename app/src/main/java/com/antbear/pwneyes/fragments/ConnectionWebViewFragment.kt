package com.antbear.pwneyes.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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

        val id  = arguments?.getLong(ARG_ID)    ?: return
        val url = arguments?.getString(ARG_URL) ?: return

        setupWebView(url)
        checkAndLoad(id, url)
    }

    // -------------------------------------------------------------------------
    // WebView
    // -------------------------------------------------------------------------
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(url: String) {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled  = true   // Pwnagotchi's web UI requires JS
                domStorageEnabled  = true
                loadWithOverviewMode = true
                useWideViewPort    = true
                builtInZoomControls = true
                displayZoomControls = false
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    _binding?.progressBar?.visibility = View.GONE
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (request.isForMainFrame) {
                        _binding?.progressBar?.visibility = View.GONE
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
                conn.requestMethod  = "HEAD"
                conn.connectTimeout = 4_000
                conn.readTimeout    = 4_000
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
            binding.progressBar.visibility = View.VISIBLE
            binding.webView.reload()
            checkAndLoad(id, url)
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
                binding.tvStatus.text          = getString(R.string.status_checking)
                binding.btnRetry.visibility    = View.GONE
                binding.statusBar.visibility   = View.VISIBLE
            }
            connected -> {
                // Success — hide the banner entirely; the web UI says it all.
                binding.statusBar.visibility = View.GONE
            }
            else -> {
                binding.statusBar.setBackgroundColor(
                    requireContext().getColor(R.color.status_disconnected)
                )
                binding.tvStatus.text          = getString(R.string.status_unreachable)
                binding.btnRetry.visibility    = View.VISIBLE
                binding.statusBar.visibility   = View.VISIBLE
            }
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------
    override fun onDestroyView() {
        // Properly tear down WebView to prevent memory leaks.
        binding.webView.stopLoading()
        binding.webView.destroy()
        super.onDestroyView()
        _binding = null
    }
}
