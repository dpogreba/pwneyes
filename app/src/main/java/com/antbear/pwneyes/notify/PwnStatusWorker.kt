package com.antbear.pwneyes.notify

import android.content.Context
import android.util.Base64
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.antbear.pwneyes.data.PwnEyesDatabase
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

data class PwnCounts(val handshakes: Int, val cracked: Int, val potMtime: Long)

/**
 * Pure decision: which notifications to fire given stored vs fresh counts.
 * [stored] == null means no baseline yet (first run) → never notify.
 * Notifies only on a strict increase, so a reset/wipe (count decreases) silently
 * re-baselines instead of firing.
 */
fun decideNotifications(
    stored: PwnCounts?,
    fresh: PwnCounts,
    notifyHandshake: Boolean,
    notifyPot: Boolean
): Set<NotifyKind> {
    if (stored == null) return emptySet()
    val out = mutableSetOf<NotifyKind>()
    if (notifyHandshake && fresh.handshakes > stored.handshakes) out += NotifyKind.HANDSHAKE
    if (notifyPot && (fresh.cracked > stored.cracked || fresh.potMtime > stored.potMtime)) {
        out += NotifyKind.POT
    }
    return out
}

/**
 * Periodically polls every saved connection's `/plugins/pwneyes/status` JSON endpoint on
 * the LAN and raises a local notification when handshakes / cracked keys increase.
 * Per-device failures (unreachable, plugin absent → 404, auth fail → 401) are skipped,
 * never fatal.
 */
class PwnStatusWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val notifyHandshake = prefs.getBoolean("pref_notify_handshake", false)
        val notifyPot = prefs.getBoolean("pref_notify_pot", false)
        if (!notifyHandshake && !notifyPot) return Result.success()

        Notifications.ensureChannel(applicationContext)
        val timeoutMs = prefs.getString("pref_timeout", "4000")?.toIntOrNull() ?: 4_000
        val state = NotifyState(applicationContext)

        val connections = PwnEyesDatabase.getDatabase(applicationContext)
            .connectionDao().getAllSync()

        for (c in connections) {
            val fresh = fetchStatus(c.url, c.username, c.password, timeoutMs) ?: continue

            val stored = if (state.hasBaseline(c.id))
                PwnCounts(state.handshakes(c.id), state.cracked(c.id), state.potMtime(c.id))
            else null

            for (kind in decideNotifications(stored, fresh, notifyHandshake, notifyPot)) {
                Notifications.notify(applicationContext, c.id, c.name, kind)
            }
            state.store(c.id, fresh.handshakes, fresh.cracked, fresh.potMtime)
        }
        return Result.success()
    }

    private fun fetchStatus(baseUrl: String, user: String?, pass: String?, timeoutMs: Int): PwnCounts? {
        return try {
            val conn = URL("$baseUrl/plugins/pwneyes/status").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            if (user != null) {
                val cred = Base64.encodeToString(
                    "$user:${pass ?: ""}".toByteArray(), Base64.NO_WRAP
                )
                conn.setRequestProperty("Authorization", "Basic $cred")
            }
            if (conn.responseCode != 200) {
                conn.disconnect()
                return null
            }
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val json = JSONObject(body)
            PwnCounts(
                handshakes = json.optInt("handshakes", 0),
                cracked = json.optInt("cracked", 0),
                potMtime = json.optLong("potfile_mtime", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val WORK_NAME = "pwn_status"

        /** Idempotent (KEEP) — safe to call on every launch. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PwnStatusWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
