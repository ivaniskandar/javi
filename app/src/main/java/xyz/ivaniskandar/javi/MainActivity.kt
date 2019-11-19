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

        mainSwitch.isChecked = JaviService.isStarted
        mainSwitch.setOnCheckedChangeListener { _, b ->
            if (b) {
                ContextCompat.startForegroundService(
                        this,
                        Intent(this, JaviService::class.java).apply {
                            action = ACTION_START_SERVICE
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
        mainSwitch.isChecked = JaviService.isStarted
    }
}
