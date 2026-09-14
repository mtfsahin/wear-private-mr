package com.sample.trdtse.m00859088.sample_music_app.healthkit.api

import android.content.Context

class ApiSiteStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var host: String?
        get() = prefs.getString(KEY_HOST, null)
        set(value) {
            prefs.edit().putString(KEY_HOST, value).apply()
        }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "healthkit_site"
        const val KEY_HOST = "api_host"
    }
}
