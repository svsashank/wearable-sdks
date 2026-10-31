package org.smriti.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * Keeps Bluetooth alive when the app is in the background (Android kills
 * background BLE otherwise; the ring vendor requires this too).
 *
 * Milestone 2 fills in the loop: every few minutes ask the voice source for new
 * recordings → download → upload → delete from the band once the note exists;
 * and pull health history from the health source.
 */
class WearableService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0
        ServiceCompat.startForeground(this, 1, notification(), type)
        return START_STICKY
    }

    private fun notification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel("wearables", "Wearables", NotificationManager.IMPORTANCE_LOW))
        }
        return NotificationCompat.Builder(this, "wearables")
            .setContentTitle("Smriti").setContentText("Connected to your wearables")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setOngoing(true).build()
    }
}
