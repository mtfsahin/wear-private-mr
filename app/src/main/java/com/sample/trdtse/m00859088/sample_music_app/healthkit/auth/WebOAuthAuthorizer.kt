package com.sample.trdtse.m00859088.sample_music_app.healthkit.auth

import android.app.Activity
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitAuthMethod
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitConfig
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitScopes

class WebOAuthAuthorizer : HealthKitAuthorizer {
    override val method = HealthKitAuthMethod.WEB_OAUTH

    override val tokenRedirectUri: String get() = HealthKitConfig.redirectUri

    override fun ownsRedirect(uri: Uri): Boolean =
        HealthKitConfig.isRedirectUri(uri) || HealthKitConfig.isDeepLink(uri)

    override fun start(activity: Activity, state: String): Result<Unit> = runCatching {
        val url = Uri.parse(HealthKitConfig.AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", HealthKitConfig.clientId)
            .appendQueryParameter("redirect_uri", HealthKitConfig.redirectUri)
            .appendQueryParameter("scope", HealthKitScopes.DEFAULT.joinToString(" "))
            .appendQueryParameter("state", state)
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("display", "touch")
            .build()

        if (HealthKitConfig.redirectIsOwnDeepLink) {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(activity, url)
        } else {
            activity.startActivityForResult(
                WebAuthActivity.intent(activity, url.toString()),
                WebAuthActivity.REQUEST_CODE,
            )
        }
    }
}
