package xyz.ivaniskandar.javi

import android.content.Intent
import android.service.quicksettings.Tile.STATE_ACTIVE
import android.service.quicksettings.Tile.STATE_INACTIVE
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat

class JaviQSTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        with(qsTile) {
            state = if (JaviService.isStarted) {
                STATE_ACTIVE
            } else {
                STATE_INACTIVE
            }
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        with(qsTile) {
            when (state) {
                STATE_ACTIVE -> {
                    state = STATE_INACTIVE
                    switchService(false)
                }
                STATE_INACTIVE -> {
                    state = STATE_ACTIVE
                    switchService(true)
                }
            }
            updateTile()
        }
    }

    private fun switchService(start: Boolean) {
        if (start) {
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