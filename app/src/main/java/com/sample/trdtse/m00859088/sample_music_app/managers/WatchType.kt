package com.sample.trdtse.m00859088.sample_music_app.managers

import android.content.Context
import com.sample.trdtse.m00859088.sample_music_app.BuildConfig

enum class WatchType(val label: String, val peerPackage: String, val peerFingerprint: String) {
    LITE("Lite", BuildConfig.WEAR_LITE_PEER_PACKAGE, BuildConfig.WEAR_LITE_PEER_FINGERPRINT),
    SMART("Smart", BuildConfig.WEAR_SMART_PEER_PACKAGE, BuildConfig.WEAR_SMART_PEER_FINGERPRINT);

    val isConfigured: Boolean get() = peerPackage.isNotBlank() && peerFingerprint.isNotBlank()
}

class WatchTypeStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var watchType: WatchType
        get() = prefs.getString(KEY_TYPE, null)
            ?.let { runCatching { WatchType.valueOf(it) }.getOrNull() }
            ?: WatchType.LITE
        set(value) = prefs.edit().putString(KEY_TYPE, value.name).apply()

    private companion object {
        const val PREFS_NAME = "wear_engine_watch_type"
        const val KEY_TYPE = "watch_type"
    }
}
