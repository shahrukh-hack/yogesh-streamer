package com.lagradost.cloudstream3.utils

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.R
import java.io.File

object StartupSoundPlayer {
    private const val TAG = "StartupSoundPlayer"
    const val PLAY_STARTUP_SOUND_KEY = "play_startup_sound_key"
    const val PLAY_STARTUP_VIDEO_KEY = "play_startup_video_key"
    const val CUSTOM_STARTUP_SOUND_KEY = "custom_startup_sound_path_key"

    var hasPlayedThisSession: Boolean = false

    @Volatile
    private var activePlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var fadeRunnable: Runnable? = null

    val isPlaying: Boolean
        get() = try {
            activePlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }

    fun stopSound() {
        mainHandler.post {
            fadeRunnable?.let { mainHandler.removeCallbacks(it) }
            fadeRunnable = null
            try {
                activePlayer?.apply {
                    if (isPlaying) {
                        stop()
                    }
                    reset()
                    release()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping startup sound", e)
            } finally {
                activePlayer = null
            }
        }
    }

    fun playStartupSound(context: Context) {
        if (hasPlayedThisSession) return
        hasPlayedThisSession = true

        val settingsManager = PreferenceManager.getDefaultSharedPreferences(context)
        val isEnabled = settingsManager.getBoolean(PLAY_STARTUP_SOUND_KEY, true)
        if (!isEnabled) return

        try {
            stopSound()

            val customPath = settingsManager.getString(CUSTOM_STARTUP_SOUND_KEY, null)
            val player = if (!customPath.isNullOrEmpty() && File(customPath).exists()) {
                MediaPlayer().apply {
                    setDataSource(customPath)
                    prepare()
                }
            } else {
                MediaPlayer.create(context, R.raw.om_namah_shivaya)
            }

            if (player == null) return
            activePlayer = player

            player.apply {
                setOnCompletionListener { mp ->
                    try {
                        mp.reset()
                        mp.release()
                    } catch (_: Exception) {
                    }
                    activePlayer = null
                }
                setOnErrorListener { mp, _, _ ->
                    try {
                        mp.reset()
                        mp.release()
                    } catch (_: Exception) {
                    }
                    activePlayer = null
                    true
                }
                start()
                Log.i(TAG, "Playing sacred startup sound with 5s auto-fade cap")

                fadeRunnable = Runnable {
                    fadeOutAndStop(500)
                }
                mainHandler.postDelayed(fadeRunnable!!, 5000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play startup sound", e)
            activePlayer = null
        }
    }

    private fun fadeOutAndStop(fadeDurationMs: Long = 500) {
        val player = activePlayer ?: return
        val steps = 10
        val stepTime = fadeDurationMs / steps
        var currentStep = steps

        val stepRunnable = object : Runnable {
            override fun run() {
                try {
                    val p = activePlayer
                    if (p != null && p.isPlaying && currentStep > 0) {
                        currentStep--
                        val vol = currentStep.toFloat() / steps.toFloat()
                        p.setVolume(vol, vol)
                        mainHandler.postDelayed(this, stepTime)
                    } else {
                        stopSound()
                    }
                } catch (_: Exception) {
                    stopSound()
                }
            }
        }
        mainHandler.post(stepRunnable)
    }
}
