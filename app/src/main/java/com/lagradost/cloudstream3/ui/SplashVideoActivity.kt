package com.lagradost.cloudstream3.ui

import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.CycleInterpolator
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.account.AccountSelectActivity
import com.lagradost.cloudstream3.utils.AppLockHelper
import com.lagradost.cloudstream3.utils.DataStoreHelper.accounts
import com.lagradost.cloudstream3.utils.StartupSoundPlayer

class SplashVideoActivity : FragmentActivity() {

    private var hasNavigated = false
    private var isLockScreenVisible = false
    private var isVerifying = false
    private val enteredPin = StringBuilder()

    private var videoView: VideoView? = null
    private var fallbackPlayer: MediaPlayer? = null
    private var pinDots: List<View> = emptyList()
    private var lockErrorText: TextView? = null
    private var rememberCheckbox: CheckBox? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_video)

        // If explicitly requested to show lock directly (e.g. redirected from MainActivity)
        if (intent.getBooleanExtra("SHOW_LOCK_DIRECTLY", false) && !AppLockHelper.isAppUnlocked(this)) {
            showLockScreen()
            return
        }

        val settingsManager = PreferenceManager.getDefaultSharedPreferences(this)
        val playStartupVideo = settingsManager.getBoolean(StartupSoundPlayer.PLAY_STARTUP_VIDEO_KEY, true)
        if (!playStartupVideo) {
            onMediaFinished()
            return
        }
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
                    onMediaFinished()
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
                    fallbackImage.postDelayed({ onMediaFinished() }, 5500)
                    true
                }
            }
        } catch (e: Exception) {
            fallbackImage.visibility = View.VISIBLE
            fallbackImage.postDelayed({ onMediaFinished() }, 3000)
        }

        findViewById<View>(R.id.splash_root)?.apply {
            setOnClickListener {
                if (!isLockScreenVisible) onMediaFinished()
            }
            // Absolute safety timeout: never stay stuck on splash video screen
            postDelayed({
                if (!isLockScreenVisible && !hasNavigated) onMediaFinished()
            }, 6000)
        }
    }

    private fun onMediaFinished() {
        if (hasNavigated) return

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

        StartupSoundPlayer.stopSound()

        if (AppLockHelper.isAppUnlocked(this)) {
            proceedToApp()
        } else {
            showLockScreen()
        }
    }

    private fun showLockScreen() {
        isLockScreenVisible = true

        try {
            videoView?.visibility = View.GONE
            findViewById<View>(R.id.splash_fallback_image)?.visibility = View.GONE
            findViewById<View>(R.id.splash_skip_hint)?.visibility = View.GONE
        } catch (_: Exception) {}

        val lockContainer = findViewById<View>(R.id.splash_lock_container)
        lockContainer?.visibility = View.VISIBLE

        lockErrorText = findViewById(R.id.lock_error_text)
        rememberCheckbox = findViewById(R.id.chk_remember_device)

        pinDots = listOfNotNull(
            findViewById(R.id.pin_dot_1),
            findViewById(R.id.pin_dot_2),
            findViewById(R.id.pin_dot_3),
            findViewById(R.id.pin_dot_4),
            findViewById(R.id.pin_dot_5),
            findViewById(R.id.pin_dot_6)
        )

        // Setup keypad buttons
        val keyMap = mapOf(
            R.id.key_0 to '0',
            R.id.key_1 to '1',
            R.id.key_2 to '2',
            R.id.key_3 to '3',
            R.id.key_4 to '4',
            R.id.key_5 to '5',
            R.id.key_6 to '6',
            R.id.key_7 to '7',
            R.id.key_8 to '8',
            R.id.key_9 to '9'
        )

        for ((resId, char) in keyMap) {
            findViewById<Button>(resId)?.setOnClickListener {
                appendDigit(char)
            }
        }

        findViewById<Button>(R.id.key_del)?.setOnClickListener {
            deleteLastDigit()
        }

        findViewById<Button>(R.id.key_ok)?.setOnClickListener {
            verifyPin()
        }

        // Set initial TV remote focus on Key 1
        findViewById<Button>(R.id.key_1)?.post {
            findViewById<Button>(R.id.key_1)?.requestFocus()
        }
    }

    private fun appendDigit(digit: Char) {
        if (isVerifying || enteredPin.length >= 6) return

        val index = enteredPin.length
        enteredPin.append(digit)
        if (index < pinDots.size) {
            pinDots[index].setBackgroundResource(R.drawable.bg_pin_dot_filled)
        }
        lockErrorText?.visibility = View.INVISIBLE

        if (enteredPin.length == 6) {
            verifyPin()
        }
    }

    private fun deleteLastDigit() {
        if (isVerifying || enteredPin.isEmpty()) return

        val index = enteredPin.length - 1
        enteredPin.deleteCharAt(index)
        if (index < pinDots.size) {
            pinDots[index].setBackgroundResource(R.drawable.bg_pin_dot_empty)
        }
        lockErrorText?.visibility = View.INVISIBLE
    }

    private fun verifyPin() {
        if (isVerifying) return
        isVerifying = true

        val input = enteredPin.toString()
        if (AppLockHelper.checkPasscode(input)) {
            // Success
            for (dot in pinDots) {
                dot.setBackgroundResource(R.drawable.bg_pin_dot_success)
            }
            lockErrorText?.setTextColor(Color.parseColor("#10B981"))
            lockErrorText?.text = "Passcode Accepted. Unlocking..."
            lockErrorText?.visibility = View.VISIBLE

            val remember = rememberCheckbox?.isChecked ?: true
            AppLockHelper.unlockApp(this, remember)

            mainHandler.postDelayed({
                proceedToApp()
            }, 350)
        } else {
            // Incorrect passcode
            val shake = TranslateAnimation(0f, 15f, 0f, 0f).apply {
                duration = 400
                interpolator = CycleInterpolator(3f)
            }
            findViewById<View>(R.id.pin_dots_container)?.startAnimation(shake)

            lockErrorText?.setTextColor(Color.parseColor("#EF4444"))
            lockErrorText?.text = "Incorrect Passcode. Access Denied."
            lockErrorText?.visibility = View.VISIBLE

            mainHandler.postDelayed({
                enteredPin.setLength(0)
                for (dot in pinDots) {
                    dot.setBackgroundResource(R.drawable.bg_pin_dot_empty)
                }
                isVerifying = false
            }, 600)
        }
    }

    private fun proceedToApp() {
        if (hasNavigated) return
        hasNavigated = true

        StartupSoundPlayer.stopSound()

        val settingsManager = PreferenceManager.getDefaultSharedPreferences(this)
        val skipStartup = settingsManager.getBoolean(
            getString(R.string.skip_startup_account_select_key),
            false
        ) || accounts.count() <= 1

        val targetActivity = if (skipStartup) {
            MainActivity::class.java
        } else {
            AccountSelectActivity::class.java
        }

        val targetIntent = Intent(this, targetActivity).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = intent.data
            intent.extras?.let { putExtras(it) }
        }
        startActivity(targetIntent)
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isLockScreenVisible) {
            when (keyCode) {
                KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> { appendDigit('0'); return true }
                KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> { appendDigit('1'); return true }
                KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> { appendDigit('2'); return true }
                KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> { appendDigit('3'); return true }
                KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> { appendDigit('4'); return true }
                KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> { appendDigit('5'); return true }
                KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_NUMPAD_6 -> { appendDigit('6'); return true }
                KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_NUMPAD_7 -> { appendDigit('7'); return true }
                KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_NUMPAD_8 -> { appendDigit('8'); return true }
                KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_9 -> { appendDigit('9'); return true }
                KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_CLEAR -> { deleteLastDigit(); return true }
                KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> { verifyPin(); return true }
                KeyEvent.KEYCODE_BACK -> {
                    finishAffinity()
                    return true
                }
            }
            return super.onKeyDown(keyCode, event)
        }

        // When video is playing, key press skips video and advances to check lock/app
        onMediaFinished()
        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!isLockScreenVisible && event?.action == MotionEvent.ACTION_DOWN) {
            onMediaFinished()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onBackPressed() {
        if (isLockScreenVisible) {
            finishAffinity()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        StartupSoundPlayer.stopSound()
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
