package com.sample.trdtse.m00859088.sample_music_app.healthkit.auth

import android.content.Context
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitAuthMethod
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitConfig

class AuthMethodStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var method: HealthKitAuthMethod
        get() = read(KEY_METHOD, HealthKitConfig.defaultAuthMethod, HealthKitAuthMethod::valueOf)
        set(value) = prefs.edit().putString(KEY_METHOD, value.name).apply()

    private fun <T> read(key: String, fallback: T, parse: (String) -> T): T {
        val stored = prefs.getString(key, null) ?: return fallback
        return runCatching { parse(stored) }.getOrDefault(fallback)
    }

    private companion object {
        const val PREFS_NAME = "healthkit_auth_method"
        const val KEY_METHOD = "auth_method"
    }
}
