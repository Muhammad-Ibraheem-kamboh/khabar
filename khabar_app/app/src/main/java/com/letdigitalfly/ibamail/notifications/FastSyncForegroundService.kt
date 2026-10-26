package com.letdigitalfly.ibamail.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.letdigitalfly.ibamail.KhabarApp
import com.letdigitalfly.ibamail.R
import com.letdigitalfly.ibamail.config.AppConfig
import com.letdigitalfly.ibamail.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FastSyncForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var syncJob: Job? = null

    companion object {
        const val NOTIFICATION_ID = 4040

        fun start(context: Context) {
            val intent = Intent(context, FastSyncForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FastSyncForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildTodayBoardNotification("Khabar Fast Mode Active", "Syncing every 90 seconds")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startSyncLoop()
        return START_STICKY
    }

    private fun startSyncLoop() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            val app = applicationContext as KhabarApp
            while (isActive) {
                try {
                    app.container.mailRepository.syncAll()

                    // Update persistent notification with counts
                    val unreadImportant = app.container.mailRepository.unreadImportantCountFlow.first()
                    val nearestReminder = app.container.reminderRepository.nearestUpcomingReminderFlow.first()
                    val reminderText = if (nearestReminder != null) " • Next: ${nearestReminder.title}" else ""

                    val content = "Today: $unreadImportant important mails$reminderText"
                    val updatedNotification = buildTodayBoardNotification("Khabar Today Board", content)
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFICATION_ID, updatedNotification)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                delay(AppConfig.FAST_MODE_INTERVAL_SECONDS * 1000)
            }
        }
    }

    private fun buildTodayBoardNotification(title: String, content: String) =
        NotificationCompat.Builder(this, NotificationChannelsManager.CHANNEL_FAST_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    override fun onDestroy() {
        syncJob?.cancel()
        super.onDestroy()
    }
}
