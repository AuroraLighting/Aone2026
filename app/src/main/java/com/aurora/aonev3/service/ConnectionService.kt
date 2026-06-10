package com.aurora.aonev3.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aurora.aonev3.R
import com.aurora.aonev3.ui.activities.MainActivity

/**
 * Foreground service that keeps the WebSocket connection alive while the app is open.
 *
 * Android will kill background processes to save battery. Running as a foreground
 * service prevents this. A persistent notification is required by Android for any
 * foreground service (API 26+).
 *
 * Lifecycle:
 *   - Started by MainActivity.onResume()
 *   - Stopped by MainActivity.onDestroy()
 *   - Holds a PARTIAL_WAKE_LOCK so CPU stays awake for reconnect logic when screen is off
 */
class ConnectionService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(connected = false))
        acquireWakeLock()
        Log.d(TAG, "ConnectionService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE_CONNECTED -> {
                val connected = intent.getBooleanExtra(EXTRA_CONNECTED, false)
                updateNotification(connected)
            }
        }
        // START_STICKY: if OS kills this service, restart it automatically
        return START_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        Log.d(TAG, "ConnectionService destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hub connection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps your Aurora A-One hub connected"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(connected: Boolean): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (connected) "Connected to hub" else "Connecting to hub\u2026"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aurora A-One")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_cloud)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(connected: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(connected))
    }

    // ── Wake lock ─────────────────────────────────────────────────────────────

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AuroraAOne::ConnectionWakeLock"
        ).also { it.acquire() }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "ConnectionService"
        private const val CHANNEL_ID = "aone_connection"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_UPDATE_CONNECTED = "com.aurora.aonev3.UPDATE_CONNECTED"
        const val EXTRA_CONNECTED = "connected"

        fun start(context: Context) {
            val intent = Intent(context, ConnectionService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ConnectionService::class.java))
        }

        fun updateStatus(context: Context, connected: Boolean) {
            val intent = Intent(context, ConnectionService::class.java).apply {
                action = ACTION_UPDATE_CONNECTED
                putExtra(EXTRA_CONNECTED, connected)
            }
            context.startService(intent)
        }
    }
}
