package com.sample.trdtse.m00859088.sample_music_app.healthkit.auth

import android.app.Activity
import android.net.Uri
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitAuthMethod
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitConfig

interface HealthKitAuthorizer {
    val method: HealthKitAuthMethod

    fun start(activity: Activity, state: String): Result<Unit>

    fun ownsRedirect(uri: Uri): Boolean = HealthKitConfig.isDeepLink(uri)

    val tokenRedirectUri: String?
}
