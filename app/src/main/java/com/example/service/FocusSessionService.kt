package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class FocusSessionService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null
    private lateinit var sessionManager: FocusSessionManager
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        sessionManager = FocusSessionManager.getInstance(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SESSION_USER -> {
                sessionManager.interruptSession()
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_STOP -> {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START, null -> {
                startForegroundNotification()
                startTicker()
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val notification = buildNotification("Session de concentration en cours...")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                val state = sessionManager.currentSessionState.value
                if (state == null || !sessionManager.isSessionActiveSync()) {
                    sessionManager.completeSession()
                    ServiceCompat.stopForeground(this@FocusSessionService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }

                val remainingMillis = maxOf(0L, state.endTimeMillis - System.currentTimeMillis())
                if (remainingMillis <= 0) {
                    sessionManager.completeSession()
                    ServiceCompat.stopForeground(this@FocusSessionService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }

                val minutes = (remainingMillis / 1000) / 60
                val seconds = (remainingMillis / 1000) % 60
                val timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                val appsCount = state.allowedPackages.size
                val strictLabel = if (state.isStrictMode) " [Strict]" else ""

                val contentText = "$timeStr restant • $appsCount apps autorisées$strictLabel"
                val notification = buildNotification(contentText, state.objective)
                notificationManager.notify(NOTIFICATION_ID, notification)

                delay(1000L)
            }
        }
    }

    private fun buildNotification(contentText: String, objective: String = "Focus Lock"): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FocusSessionService::class.java).apply {
            action = ACTION_STOP_SESSION_USER
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Lock : $objective")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Arrêter la session",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sessions Focus Lock",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Affiche le compte à rebours et l'état de votre session de concentration"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_STOP_SESSION_USER = "com.example.service.ACTION_STOP_SESSION_USER"
        private const val CHANNEL_ID = "focus_lock_session_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
