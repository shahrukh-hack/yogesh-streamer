package com.lagradost.cloudstream3.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.R
import java.io.File

object StartupSoundPlayer {
    private const val TAG = "StartupSoundPlayer"
    const val PLAY_STARTUP_SOUND_KEY = "play_startup_sound_key"
    const val CUSTOM_STARTUP_SOUND_KEY = "custom_startup_sound_path_key"

    var hasPlayedThisSession: Boolean = false

    fun playStartupSound(context: Context) {
        if (hasPlayedThisSession) return
        hasPlayedThisSession = true

        val settingsManager = PreferenceManager.getDefaultSharedPreferences(context)
        val isEnabled = settingsManager.getBoolean(PLAY_STARTUP_SOUND_KEY, true)
        if (!isEnabled) return

        try {
            val customPath = settingsManager.getString(CUSTOM_STARTUP_SOUND_KEY, null)
            val player = if (!customPath.isNullOrEmpty() && File(customPath).exists()) {
                MediaPlayer().apply {
                    setDataSource(customPath)
                    prepare()
                }
            } else {
                MediaPlayer.create(context, R.raw.om_namah_shivaya)
            }

            player?.apply {
                setOnCompletionListener { mp ->
                    try {
                        mp.reset()
                        mp.release()
                    } catch (_: Exception) {
                    }
                }
                setOnErrorListener { mp, _, _ ->
                    try {
                        mp.reset()
                        mp.release()
                    } catch (_: Exception) {
                    }
                    true
                }
                start()
                Log.i(TAG, "Playing sacred Om Namah Shivaya startup sound")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play startup sound", e)
        }
    }
}
