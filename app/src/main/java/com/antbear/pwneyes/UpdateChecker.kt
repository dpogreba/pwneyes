package com.antbear.pwneyes

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight GitHub Releases update checker.
 *
 * Queries the public GitHub API for the latest release tag of this repo and
 * returns the tag string if it represents a version newer than [currentVersion].
 * All failures (no internet, API error, parse error) are swallowed silently so
 * the check never disrupts normal app use.
 *
 * Call [latestUpdateIfNewer] on a background thread; it does blocking I/O.
 */
object UpdateChecker {

    private const val API_URL =
        "https://api.github.com/repos/dpogreba/pwneyes/releases/latest"

    const val RELEASES_PAGE =
        "https://github.com/dpogreba/pwneyes/releases/latest"

    /** A newer release: its version plus the APK asset to download (null if the release has none). */
    data class UpdateInfo(val version: String, val apkUrl: String?)

    /**
     * Returns the latest release if it is strictly newer than [currentVersion],
     * or `null` if there is no update or the check failed.
     *
     * Must be called from a background thread.
     */
    fun latestUpdateIfNewer(currentVersion: String): UpdateInfo? {
        return try {
            val conn = URL(API_URL).openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.connectTimeout = 6_000
            conn.readTimeout  = 6_000
            if (conn.responseCode != 200) return null

            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(body)
            // Tag is typically "v1.2"; strip the leading 'v' for comparison.
            val latest = json.optString("tag_name", "").removePrefix("v")
            if (latest.isEmpty()) return null

            // Strip debug suffix so "1.0-debug" compares equal to "1.0".
            val current = currentVersion.substringBefore("-")
            if (!isNewer(latest, current)) return null

            UpdateInfo(latest, firstApkAssetUrl(json))
        } catch (_: Exception) {
            null
        }
    }

    /**
     * First `.apk` asset download URL in the release, or null.
     * The URL is validated before it is ever handed to the downloader — we only
     * ever fetch an APK from GitHub over HTTPS, never an arbitrary URL that
     * happened to appear in the API response.
     */
    private fun firstApkAssetUrl(release: JSONObject): String? {
        val assets = release.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val url = assets.optJSONObject(i)?.optString("browser_download_url").orEmpty()
            if (url.endsWith(".apk", ignoreCase = true) && isTrustedApkUrl(url)) return url
        }
        return null
    }

    /** HTTPS + a GitHub-owned host. Anything else is refused. */
    fun isTrustedApkUrl(url: String): Boolean = try {
        val parsed = URL(url)
        val host = parsed.host.lowercase()
        parsed.protocol.equals("https", ignoreCase = true) &&
            (host == "github.com" || host.endsWith(".github.com") ||
                host.endsWith(".githubusercontent.com"))
    } catch (_: Exception) {
        false
    }

    // Simple semantic-version comparison: "1.2.0" > "1.1.9" returns true.
    private fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").mapNotNull  { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        val len = maxOf(l.size, c.size)
        for (i in 0 until len) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv > cv) return true
            if (lv < cv) return false
        }
        return false
    }
}
