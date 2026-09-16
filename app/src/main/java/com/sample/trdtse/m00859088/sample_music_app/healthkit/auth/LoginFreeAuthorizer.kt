package com.sample.trdtse.m00859088.sample_music_app.healthkit.auth

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.sample.trdtse.m00859088.sample_music_app.healthkit.AuthorizationOutcome
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitAuthMethod
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitConfig
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitScopes
import com.huawei.hms.hihealthlite.HuaweiHiHealthLite

class LoginFreeAuthorizer : HealthKitAuthorizer {
    override val method = HealthKitAuthMethod.LOGIN_FREE_SDK

    override val tokenRedirectUri: String?
        get() = if (HealthKitConfig.SEND_REDIRECT_URI_FOR_LOGIN_FREE) {
            HealthKitConfig.LOGIN_FREE_CALLBACK_URL
        } else {
            null
        }

    override fun start(activity: Activity, state: String): Result<Unit> = runCatching {
        LoginFreeStatus.missingRequirement(activity)?.let { throw IllegalStateException(it) }

        val controller = HuaweiHiHealthLite.getLoginFreeAuthController(activity)
        val scopes = HealthKitScopes.DEFAULT.toTypedArray()

        val intent = controller.requestAuthorizationWithPrivacyIntent(
            scopes,
            state,
            HealthKitConfig.loginFreeAppId,
            HealthKitConfig.deepLinkUrl,
            HealthKitConfig.schemeSecret,
        )

        activity.startActivityForResult(intent, REQUEST_CODE)
    }.recoverCatching { error ->
        Log.e(TAG, "Login-free authorization could not start", error)
        throw when (error) {
            is LinkageError -> IllegalStateException(
                "Login-free SDK cannot run here: ${error.javaClass.simpleName} " +
                    "${error.message}. It needs HMS Core and the Huawei Health app; use " +
                    "Web OAuth on a non-Huawei phone.",
                error,
            )

            else -> error
        }
    }

    companion object {
        const val REQUEST_CODE = 2000

        private const val TAG = "LoginFreeAuthorizer"

        fun readOutcome(
            requestCode: Int,
            resultCode: Int,
            data: Intent?,
        ): AuthorizationOutcome? {
            if (requestCode != REQUEST_CODE) return null

            val deepLinkResult = data?.getIntExtra(EXTRA_RESULT, 0) ?: 0
            if (deepLinkResult != 0) {
                val message = data?.getStringExtra(EXTRA_MESSAGE).orEmpty()
                val description = LoginFreeStatus.describe(deepLinkResult)
                return AuthorizationOutcome.Failed(
                    if (message.isBlank()) description else "$description ($message)"
                )
            }

            if (resultCode == Activity.RESULT_CANCELED) {
                return AuthorizationOutcome.Failed(
                    "Huawei Health closed the authorization without returning a code. " +
                        "When its page showed \"redirect_uri not registered\" (error 1101, " +
                        "sub_error 20023), the cause is not the Scheme URL: the web step " +
                        "of the login-free flow redirects to a Huawei-hosted callback " +
                        "that has to be on the app's callback list in AppGallery Connect, " +
                        "next to your own. The documented value is " +
                        "${HealthKitConfig.LOGIN_FREE_CALLBACK_URL} . Also check that " +
                        "login-free authorization is approved for this app and region."
                )
            }

            return null
        }

        private const val EXTRA_RESULT = "DEEPLINK_RESULT"
        private const val EXTRA_MESSAGE = "DEEPLINK_MSG"
    }
}
