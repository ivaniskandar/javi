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
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_PROXIMITY
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import android.os.*
import android.os.PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class JaviService : Service(), SensorEventListener {
    companion object {
        var isStarted = false
        private var receiverRegistered = false
    }

    private val vibrator by lazy { ContextCompat.getSystemService(this, Vibrator::class.java)!! }
    private val sensorManager by lazy { ContextCompat.getSystemService(this, SensorManager::class.java)!! }
    private var proximity: Sensor? = null

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
                ACTION_SCREEN_ON -> switchProximityListener(true)
                ACTION_SCREEN_OFF -> switchProximityListener(false)
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

        val filter = IntentFilter().apply {
            addAction(ACTION_SCREEN_OFF)
            addAction(ACTION_SCREEN_ON)
        }
        registerReceiver(screenActionReceiver, filter)
        receiverRegistered = true

        proximity = sensorManager.getDefaultSensor(TYPE_PROXIMITY)
        switchProximityListener(true)

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

    private fun switchProximityListener(on: Boolean) {
        if (on) {
            proximity?.run {
                sensorManager.registerListener(this@JaviService, this, SENSOR_DELAY_NORMAL)
            }
        } else {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        switchWakelock(false)
        switchProximityListener(false)
        if (receiverRegistered) {
            unregisterReceiver(screenActionReceiver)
            receiverRegistered = false
        }
        isStarted = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val distance = event.values[0]
        if (distance == 0f) {
            vibrator.vibrate(VibrationEffect.createOneShot(100L, VibrationEffect.DEFAULT_AMPLITUDE))
            switchWakelock(true)
        } else {
            switchWakelock(false)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do nothing
    }

    override fun onBind(intent: Intent): IBinder? = null
}