package com.lagradost.cloudstream3.utils

import android.content.Context
import androidx.preference.PreferenceManager

object AppLockHelper {
    const val MASTER_PASSCODE = "178757"
    const val KEY_APP_UNLOCKED = "app_master_passcode_unlocked"
    const val KEY_REQUIRE_ON_STARTUP = "app_master_passcode_always_require"

    // In-memory session unlock state
    @Volatile
    var isSessionUnlocked: Boolean = false

    /**
     * Returns true if the app is currently unlocked for this device / session.
     */
    fun isAppUnlocked(context: Context): Boolean {
        if (isSessionUnlocked) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val alwaysRequire = prefs.getBoolean(KEY_REQUIRE_ON_STARTUP, false)
            if (!alwaysRequire) return true
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val alwaysRequire = prefs.getBoolean(KEY_REQUIRE_ON_STARTUP, false)
        if (alwaysRequire) {
            return isSessionUnlocked
        }

        return prefs.getBoolean(KEY_APP_UNLOCKED, false)
    }

    /**
     * Verifies the input against master passcode 178757.
     */
    fun checkPasscode(input: String): Boolean {
        return input.trim() == MASTER_PASSCODE
    }

    /**
     * Unlocks the app. If rememberDevice is true, persists the authorization
     * so authorized users are not repeatedly prompted.
     */
    fun unlockApp(context: Context, rememberDevice: Boolean) {
        isSessionUnlocked = true
        if (rememberDevice) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().putBoolean(KEY_APP_UNLOCKED, true).apply()
        }
    }

    /**
     * Relocks the app across session and persistent storage.
     */
    fun relockApp(context: Context) {
        isSessionUnlocked = false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean(KEY_APP_UNLOCKED, false).apply()
    }
}
