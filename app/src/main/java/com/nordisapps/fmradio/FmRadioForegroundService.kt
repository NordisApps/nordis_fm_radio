package com.nordisapps.fmradio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class FmRadioForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "fm_radio_playback"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.nordisapps.fmradio.ACTION_STOP"
        const val EXTRA_FREQUENCY = "extra_frequency"
        const val EXTRA_STATION_NAME = "extra_station_name"
    }

    private val radioManager by lazy { FmRadioManager(applicationContext) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            try {
                radioManager.stop()
                Log.d("FMTEST", "RADIO OFF via notification action")
            } catch (e: Exception) {
                Log.e("FMTEST", "Stop failed: ${e.message}", e)
            }
            RadioServiceEvents.onStoppedExternally?.invoke()
            stopSelf()
            return START_NOT_STICKY
        }
        val frequency = intent?.getStringExtra(EXTRA_FREQUENCY) ?: ""
        val stationName = intent?.getStringExtra(EXTRA_STATION_NAME) ?: ""

        startForeground(NOTIFICATION_ID, buildNotification(frequency, stationName))
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            radioManager.stop()
            Log.d("FMTEST", "RADIO OFF via onTaskRemoved (foreground service)")
        } catch (e: Exception) {
            Log.e("FMTEST", "Cleanup failed: ${e.message}", e)
        }
        RadioServiceEvents.onStoppedExternally?.invoke()
        stopSelf()
    }

    private fun buildNotification(frequency: String, stationName: String) = run {
        val stopIntent = Intent(this, FmRadioForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(stationName.ifBlank { "$frequency MHz" })
            .setContentText(if (stationName.isNotBlank()) "$frequency MHz" else "Nordis FM Radio")
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Стоп", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "FM Radio Playback",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}