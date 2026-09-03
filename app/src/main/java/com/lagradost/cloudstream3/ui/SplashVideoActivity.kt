package com.lagradost.cloudstream3.ui

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.VideoView
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.account.AccountSelectActivity
import com.lagradost.cloudstream3.utils.StartupSoundPlayer

class SplashVideoActivity : FragmentActivity() {

    private var hasNavigated = false
    private var videoView: VideoView? = null
    private var fallbackPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_video)

        val settingsManager = PreferenceManager.getDefaultSharedPreferences(this)
        val playStartupSound = settingsManager.getBoolean(StartupSoundPlayer.PLAY_STARTUP_SOUND_KEY, true)

        videoView = findViewById(R.id.splash_videoview)
        val fallbackImage: ImageView = findViewById(R.id.splash_fallback_image)

        val videoUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.intro_video)

        try {
            videoView?.apply {
                setVideoURI(videoUri)
                setOnPreparedListener { mp ->
                    mp.isLooping = false
                    start()
                }
                setOnCompletionListener {
                    navigateToApp()
                }
                setOnErrorListener { _, _, _ ->
                    // Fallback to static banner + audio
                    if (playStartupSound) {
                        try {
                            fallbackPlayer = MediaPlayer.create(this@SplashVideoActivity, R.raw.om_namah_shivaya)?.apply {
                                setOnCompletionListener { mp ->
                                    try { mp.release() } catch (_: Exception) {}
                                }
                                start()
                            }
                        } catch (_: Exception) {}
                    }
                    fallbackImage.visibility = View.VISIBLE
                    fallbackImage.postDelayed({ navigateToApp() }, 5500)
                    true
                }
            }
        } catch (e: Exception) {
            fallbackImage.visibility = View.VISIBLE
            fallbackImage.postDelayed({ navigateToApp() }, 3000)
        }

        findViewById<View>(R.id.splash_root)?.apply {
            setOnClickListener { navigateToApp() }
            // Absolute safety timeout: never stay stuck on splash video screen
            postDelayed({ navigateToApp() }, 5500)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        navigateToApp()
        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            navigateToApp()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun navigateToApp() {
        if (hasNavigated) return
        hasNavigated = true

        try {
            videoView?.stopPlayback()
        } catch (_: Exception) {}

        try {
            fallbackPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            fallbackPlayer = null
        } catch (_: Exception) {}

        val targetIntent = Intent(this, AccountSelectActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_FORWARD_RESULT
            data = intent.data
            putExtras(intent)
        }
        startActivity(targetIntent)
        finish()
    }

    override fun onDestroy() {
        try {
            videoView?.stopPlayback()
        } catch (_: Exception) {}
        try {
            fallbackPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            fallbackPlayer = null
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
