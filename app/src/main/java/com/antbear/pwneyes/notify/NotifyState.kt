package com.antbear.pwneyes.notify

import android.content.Context

/**
 * Per-connection last-seen counters used for change detection by [PwnStatusWorker].
 * Pure worker bookkeeping — deliberately kept out of Room (no schema reason to store it).
 * Keyed by connection id in a dedicated SharedPreferences file.
 */
class NotifyState(context: Context) {

    private val prefs =
        context.getSharedPreferences("pwneyes_notify_state", Context.MODE_PRIVATE)

    /** True once a baseline has been recorded — used to suppress first-run notifications. */
    fun hasBaseline(id: Long): Boolean = prefs.contains("hs_$id")

    fun handshakes(id: Long): Int = prefs.getInt("hs_$id", 0)
    fun cracked(id: Long): Int = prefs.getInt("cracked_$id", 0)
    fun potMtime(id: Long): Long = prefs.getLong("potmtime_$id", 0L)

    fun store(id: Long, handshakes: Int, cracked: Int, potMtime: Long) {
        prefs.edit()
            .putInt("hs_$id", handshakes)
            .putInt("cracked_$id", cracked)
            .putLong("potmtime_$id", potMtime)
            .apply()
    }

    /** Drop a deleted connection's baseline so a re-added id can't inherit stale counts. */
    fun clear(id: Long) {
        prefs.edit()
            .remove("hs_$id")
            .remove("cracked_$id")
            .remove("potmtime_$id")
            .apply()
    }
}
