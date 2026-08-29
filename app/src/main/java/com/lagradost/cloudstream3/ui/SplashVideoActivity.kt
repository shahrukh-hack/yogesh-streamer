package com.lagradost.cloudstream3.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.VideoView
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.account.AccountSelectActivity
import com.lagradost.cloudstream3.utils.StartupSoundPlayer

class SplashVideoActivity : FragmentActivity() {

    private var hasNavigated = false
    private var videoView: VideoView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_video)

        val settingsManager = PreferenceManager.getDefaultSharedPreferences(this)
        val playStartupSound = settingsManager.getBoolean(StartupSoundPlayer.PLAY_STARTUP_SOUND_KEY, true)

        videoView = findViewById(R.id.splash_videoview)
        val fallbackImage: ImageView = findViewById(R.id.splash_fallback_image)

        val videoUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.om_namah_shivaya)

        try {
            // Check if there is an intro video in raw or fallback to sound + splash logo
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
                    // Fallback to playing sacred sound and navigating
                    if (playStartupSound) {
                        StartupSoundPlayer.playStartupSound(this@SplashVideoActivity)
                    }
                    fallbackImage.visibility = View.VISIBLE
                    fallbackImage.postDelayed({ navigateToApp() }, 3000)
                    true
                }
            }
        } catch (e: Exception) {
            if (playStartupSound) {
                StartupSoundPlayer.playStartupSound(this)
            }
            fallbackImage.visibility = View.VISIBLE
            fallbackImage.postDelayed({ navigateToApp() }, 3000)
        }

        findViewById<View>(R.id.splash_root)?.setOnClickListener {
            navigateToApp()
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

        val targetIntent = Intent(this, AccountSelectActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_FORWARD_RESULT
            data = intent.data
            putExtras(intent)
        }
        startActivity(targetIntent)
        finish()
    }
}
