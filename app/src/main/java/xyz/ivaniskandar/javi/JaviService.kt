package xyz.ivaniskandar.javi

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SCREEN_OFF
import android.content.Intent.ACTION_SCREEN_ON
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class JaviService : Service() {
    companion object {
        var isStarted = false
        private var receiverRegistered = false
    }

    private val wakeLock by lazy {
        ContextCompat.getSystemService(this, PowerManager::class.java)!!
                .newWakeLock(PROXIMITY_SCREEN_OFF_WAKE_LOCK, javaClass.name)
    }

    private val clickPendingIntent by lazy {
        val intent = Intent(this, JaviService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        PendingIntent.getService(this, FOREGROUND_SERVICE_ID, intent, 0)
    }

    private val screenActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_SCREEN_ON -> switchWakelock(true)
                ACTION_SCREEN_OFF -> switchWakelock(false)
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_START_SERVICE -> start()
            ACTION_STOP_SERVICE -> stop()
            else -> throw Exception("wtf")
        }
        return START_STICKY
    }

    private fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.getSystemService(this, NotificationManager::class.java)!!.run {
                val channel = NotificationChannel(
                    CHANNEL_GENERAL,
                    getString(R.string.notification_general_title),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                channel.setSound(null, null)
                createNotificationChannel(channel)
            }
        }

        startForeground(
                FOREGROUND_SERVICE_ID,
                NotificationCompat.Builder(this, CHANNEL_GENERAL)
                        .setShowWhen(false)
                        .setSmallIcon(R.drawable.ic_javi)
                        .setColor(getColor(R.color.themeColor))
                        .setContentTitle(getString(R.string.service_notif_title))
                        .setContentText(getString(R.string.service_notif_text))
                        .setContentIntent(clickPendingIntent)
                        .build()
        )
        switchWakelock(true)

        val filter = IntentFilter().apply {
            addAction(ACTION_SCREEN_OFF)
            addAction(ACTION_SCREEN_ON)
        }
        registerReceiver(screenActionReceiver, filter)
        receiverRegistered = true

        isStarted = true
    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
    }

    @SuppressLint("WakelockTimeout")
    private fun switchWakelock(acquire: Boolean) {
        if (acquire) {
            if (!wakeLock.isHeld) {
                wakeLock.acquire()
            }
        } else {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        switchWakelock(false)
        if (receiverRegistered) {
            unregisterReceiver(screenActionReceiver)
            receiverRegistered = false
        }
        isStarted = false
    }

    override fun onBind(intent: Intent): IBinder? = null
}