package com.sample.trdtse.m00859088.sample_music_app.healthkit.auth

import android.content.Context
import com.huawei.hms.hihealthlite.LoginFreeSDKStatusCodes

object LoginFreeStatus {
    const val HUAWEI_HEALTH_PACKAGE = "com.huawei.health"
    const val HMS_CORE_PACKAGE = "com.huawei.hwid"
    const val HMS_CERT_FINGERPRINT_MISMATCH = 6003

    fun describe(code: Int): String = when (code) {
        LoginFreeSDKStatusCodes.APP_MCP_CHECK_FAIL ->
            "$code APP_MCP_CHECK_FAIL: Huawei Health rejected the app. The login-free " +
                "permission is not approved for this app ID or region."

        LoginFreeSDKStatusCodes.APP_SIGNATURE_OR_DEEPLINKURL_AUTH_FAIL ->
            "$code APP_SIGNATURE_OR_DEEPLINKURL_AUTH_FAIL: the Scheme Secret or the " +
                "Scheme URL does not match AppGallery Connect. Check healthkit.deepLink " +
                "and healthkit.schemeSecret."

        LoginFreeSDKStatusCodes.APP_SCOPES_CHECK_FAIL ->
            "$code APP_SCOPES_CHECK_FAIL: a requested scope is not approved for this app."

        LoginFreeSDKStatusCodes.APP_CANCEL_AUTH_OR_AUTH_FAIL ->
            "$code APP_CANCEL_AUTH_OR_AUTH_FAIL: the user cancelled, or Huawei Health " +
                "refused the authorization."

        LoginFreeSDKStatusCodes.APP_ANY_PARMA_EMPTY ->
            "$code APP_ANY_PARMA_EMPTY: one of scopes, state, clientId, deepLinkUrl or " +
                "signature was empty."

        LoginFreeSDKStatusCodes.H5_APPID_OR_SCOPES_WRONG ->
            "$code H5_APPID_OR_SCOPES_WRONG: the app ID or a scope is wrong on the web page."

        LoginFreeSDKStatusCodes.H5_APPID_OR_SCOPES_EMPTY ->
            "$code H5_APPID_OR_SCOPES_EMPTY: the app ID or the scope list was empty."

        LoginFreeSDKStatusCodes.H5_CANCEL_AUTH ->
            "$code H5_CANCEL_AUTH: the user closed the web authorization page."

        LoginFreeSDKStatusCodes.HMS_CANCEL_AUTH ->
            "$code HMS_CANCEL_AUTH: the user cancelled the HUAWEI ID screen."

        LoginFreeSDKStatusCodes.HMS_APPID_WRONG ->
            "$code HMS_APPID_WRONG: HUAWEI ID does not recognise this app ID."

        LoginFreeSDKStatusCodes.HMS_SCOPE_LIST_EMPTY ->
            "$code HMS_SCOPE_LIST_EMPTY: no scopes were sent to the HUAWEI ID screen."

        LoginFreeSDKStatusCodes.HEALTHKIT_ACCOUNT_AUTH_FAIL ->
            "$code HEALTHKIT_ACCOUNT_AUTH_FAIL: the HUAWEI ID could not be authorized."

        LoginFreeSDKStatusCodes.HEALTHKIT_SDK_TO_APP_FAIL ->
            "$code HEALTHKIT_SDK_TO_APP_FAIL: the SDK could not open Huawei Health. " +
                "Version 16.1.4.310 or later is required for solution 2."

        LoginFreeSDKStatusCodes.HEALTHKIT_SDK_TO_H5_FAIL ->
            "$code HEALTHKIT_SDK_TO_H5_FAIL: the SDK could not open the web page."

        LoginFreeSDKStatusCodes.HEALTHKIT_DEEPLINK_THIRDSERVICE_FAIL ->
            "$code HEALTHKIT_DEEPLINK_THIRDSERVICE_FAIL: the deep link back to this app " +
                "could not be opened. The Scheme URL in AppGallery Connect and the intent " +
                "filter in AndroidManifest.xml are not the same."

        HMS_CERT_FINGERPRINT_MISMATCH ->
            "$code: HMS Core refused the app because the signing certificate of this " +
                "build is not the SHA-256 fingerprint registered for the App ID in " +
                "AppGallery Connect. Sign the APK with the registered keystore, or add " +
                "the fingerprint of this keystore to the app there."

        else -> "$code: unmapped login-free status code."
    }

    fun missingRequirement(context: Context): String? {
        if (isInstalled(context, HUAWEI_HEALTH_PACKAGE)) return null

        if (isInstalled(context, HMS_CORE_PACKAGE)) return null

        return "Neither the Huawei Health app ($HUAWEI_HEALTH_PACKAGE) nor HMS Core " +
            "($HMS_CORE_PACKAGE) is installed. The login-free SDK needs one of them; " +
            "Web OAuth works without either."
    }

    private fun isInstalled(context: Context, packageName: String): Boolean = runCatching {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    }.getOrDefault(false)
}
