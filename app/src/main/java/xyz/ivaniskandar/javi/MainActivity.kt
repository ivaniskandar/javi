package xyz.ivaniskandar.javi

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hypnoticSwitch.setOnCheckedChangeListener { button, b ->
            if (!button.isPressed) {
                return@setOnCheckedChangeListener
            }
            if (b) {
                caffeineSwitch.isChecked = false
                ContextCompat.startForegroundService(
                        this,
                        Intent(this, JaviService::class.java).apply {
                            action = ACTION_START_SERVICE
                            putExtra(EXTRA_SERVICE_TYPE, EXTRA_HYPNOTIC)
                        }
                )
            } else {
                startService(
                        Intent(this, JaviService::class.java).apply {
                            action = ACTION_STOP_SERVICE
                        }
                )
            }
        }
        caffeineSwitch.setOnCheckedChangeListener { button, b ->
            if (!button.isPressed) {
                return@setOnCheckedChangeListener
            }
            if (b) {
                hypnoticSwitch.isChecked = false
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, JaviService::class.java).apply {
                        action = ACTION_START_SERVICE
                        putExtra(EXTRA_SERVICE_TYPE, EXTRA_CAFFEINE)
                    }
                )
            } else {
                startService(
                    Intent(this, JaviService::class.java).apply {
                        action = ACTION_STOP_SERVICE
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hypnoticSwitch.isChecked = JaviService.isHypnoticActive
        caffeineSwitch.isChecked = JaviService.isCaffeineActive

    }
}
