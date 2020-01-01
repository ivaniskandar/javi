package xyz.ivaniskandar.javi

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.content.Intent.ACTION_SCREEN_OFF
import android.content.Intent.ACTION_SCREEN_ON
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_PROXIMITY
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import android.os.*
import android.os.PowerManager.FULL_WAKE_LOCK
import android.os.PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK
import android.provider.Settings
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class JaviService : Service(), SensorEventListener {
    companion object {
        var isHypnoticActive = false
        var isCaffeineActive = false

        private var isHypnoticReceiverRegistered = false
        private var isCaffeineReceiverRegistered = false
    }

    private val vibrator by lazy { ContextCompat.getSystemService(this, Vibrator::class.java)!! }
    private val sensorManager by lazy { ContextCompat.getSystemService(this, SensorManager::class.java)!! }
    private var proximity: Sensor? = null

    private val hypnoticWakeLock by lazy {
        ContextCompat.getSystemService(this, PowerManager::class.java)!!
                .newWakeLock(PROXIMITY_SCREEN_OFF_WAKE_LOCK, javaClass.name)
    }

    @Suppress("DEPRECATION")
    private val caffeineWakeLock by lazy {
        ContextCompat.getSystemService(this, PowerManager::class.java)!!
            .newWakeLock(FULL_WAKE_LOCK, javaClass.name)
    }

    private val clickPendingIntent by lazy {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_GENERAL)
        }
        PendingIntent.getActivity(this, FOREGROUND_SERVICE_ID, intent, 0)
    }

    private val hypnoticScreenActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_SCREEN_ON -> switchProximityListener(true)
                ACTION_SCREEN_OFF -> switchProximityListener(false)
            }
        }
    }
    private val caffeineScreenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            stop()
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_START_SERVICE -> {
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

                when (intent.getStringExtra(EXTRA_SERVICE_TYPE)) {
                    EXTRA_CAFFEINE -> switchCaffeineMode(true)
                    EXTRA_HYPNOTIC -> switchHypnoticMode(true)
                    else -> throw Exception("wtf")
                }
            }
            ACTION_STOP_SERVICE -> stop()
            else -> throw Exception("wtf")
        }
        return START_STICKY
    }

    private fun switchHypnoticMode(active: Boolean) {
        if (active) {
            if (isCaffeineActive) {
                switchCaffeineMode(false)
            }
            val filter = IntentFilter().apply {
                addAction(ACTION_SCREEN_OFF)
                addAction(ACTION_SCREEN_ON)
                priority = 999
            }
            registerReceiver(hypnoticScreenActionReceiver, filter)
            isHypnoticReceiverRegistered = true

            proximity = sensorManager.getDefaultSensor(TYPE_PROXIMITY)
            switchProximityListener(true)
        } else {
            if (isHypnoticReceiverRegistered) {
                unregisterReceiver(hypnoticScreenActionReceiver)
                isHypnoticReceiverRegistered = false
            }
            switchProximityListener(false)
        }
        isHypnoticActive = active

        // Update tile state
        TileService.requestListeningState(
            this,
            ComponentName(this, HypnoticQSTileService::class.java)
        )
    }

    private fun switchCaffeineMode(active: Boolean) {
        if (active) {
            if (isHypnoticActive) {
                switchHypnoticMode(false)
            }
            val filter = IntentFilter().apply {
                addAction(ACTION_SCREEN_OFF)
                priority = 999
            }
            registerReceiver(caffeineScreenOffReceiver, filter)
            isCaffeineReceiverRegistered = true

            switchCaffeineWakeLock(true)
        } else {
            if (isCaffeineReceiverRegistered) {
                unregisterReceiver(caffeineScreenOffReceiver)
                isCaffeineReceiverRegistered = false
            }
            switchCaffeineWakeLock(false)
        }
        isCaffeineActive = active

        // Update tile state
        TileService.requestListeningState(
            this,
            ComponentName(this, CaffeineQSTileService::class.java)
        )
    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
    }

    @SuppressLint("WakelockTimeout")
    private fun switchCaffeineWakelock(acquire: Boolean) {
        if (acquire) {
            if (!hypnoticWakeLock.isHeld) {
                hypnoticWakeLock.acquire()
            }
        } else {
            if (hypnoticWakeLock.isHeld) {
                hypnoticWakeLock.release()
            }
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun switchCaffeineWakeLock(acquire: Boolean) {
        if (acquire) {
            if (!caffeineWakeLock.isHeld) {
                caffeineWakeLock.acquire()
            }
        } else {
            if (caffeineWakeLock.isHeld) {
                caffeineWakeLock.release()
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
        switchCaffeineMode(false)
        switchHypnoticMode(false)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val distance = event.values[0]
        if (distance == 0f) {
            vibrator.vibrate(VibrationEffect.createOneShot(100L, VibrationEffect.DEFAULT_AMPLITUDE))
            switchCaffeineWakelock(true)
        } else {
            switchCaffeineWakelock(false)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do nothing
    }

    override fun onBind(intent: Intent): IBinder? = null
}