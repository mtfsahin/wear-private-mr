package com.sample.trdtse.m00859088.sample_music_app.healthkit.auth

import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitConfig
import org.json.JSONObject

object OAuthPageError {
    fun parse(pageText: String): String? {
        val trimmed = pageText.trim()
        if (!trimmed.startsWith("{") || !trimmed.contains("\"error\"")) return null

        val json = runCatching { JSONObject(trimmed) }.getOrNull() ?: return null
        if (!json.has("error")) return null

        val error = json.optString("error")
        val description = json.optString("error_description")
        val subError = json.optInt("sub_error", 0)

        return buildString {
            append("Huawei refused the request (error $error")
            if (subError != 0) append(", sub_error $subError")
            append(")")
            if (description.isNotBlank()) append(": ").append(description)
            append(". ")
            append(hint(subError))
        }
    }

    private fun hint(subError: Int): String = when (subError) {
        SUB_ERROR_INVALID_REDIRECT_URI ->
            "The redirect_uri sent here is not one of the app's registered ones. Put the " +
                "value registered in AppGallery Connect in healthkit.redirectUri; the " +
                "login-free Scheme URL is a different field and does not work here."

        SUB_ERROR_REDIRECT_URI_NOT_REGISTERED ->
            "The redirect URI is well formed but is not on the app's callback list in " +
                "AppGallery Connect. Add this exact value there, and for the login-free " +
                "SDK add Huawei's own callback URL as well: " +
                HealthKitConfig.LOGIN_FREE_CALLBACK_URL

        else -> "Check the app's OAuth 2.0 settings in AppGallery Connect."
    }

    private const val SUB_ERROR_INVALID_REDIRECT_URI = 20022
    private const val SUB_ERROR_REDIRECT_URI_NOT_REGISTERED = 20023
}
