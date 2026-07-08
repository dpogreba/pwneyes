package com.antbear.pwneyes.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.antbear.pwneyes.R

enum class NotifyKind { HANDSHAKE, POT }

object Notifications {

    const val CHANNEL_ID = "pwneyes_alerts"

    /** Idempotent — safe to call every worker run; no Application subclass needed. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.notify_channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                )
            }
        }
    }

    fun notify(context: Context, connectionId: Long, connName: String, kind: NotifyKind) {
        val (titleRes, textRes, offset) = when (kind) {
            NotifyKind.HANDSHAKE ->
                Triple(R.string.notify_handshake_title, R.string.notify_handshake_text, 0)
            NotifyKind.POT ->
                Triple(R.string.notify_pot_title, R.string.notify_pot_text, 1)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_device)
            .setContentTitle(context.getString(titleRes))
            .setContentText(context.getString(textRes, connName))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Stable id per (connection, kind): a re-fire replaces rather than stacks.
        val notifId = connectionId.toInt() * 2 + offset
        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied — silently no-op; the worker still records state.
        }
    }
}
