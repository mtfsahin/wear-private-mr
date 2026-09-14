package com.sample.trdtse.m00859088.sample_music_app.healthkit.token

import android.content.Context

class TokenStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        private set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        private set(value) = prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    var expiresAtMillis: Long
        get() = prefs.getLong(KEY_EXPIRES_AT, 0L)
        private set(value) = prefs.edit().putLong(KEY_EXPIRES_AT, value).apply()

    var authorizedAtMillis: Long
        get() = prefs.getLong(KEY_AUTHORIZED_AT, 0L)
        private set(value) = prefs.edit().putLong(KEY_AUTHORIZED_AT, value).apply()

    var grantedScopes: Set<String>
        get() = prefs.getStringSet(KEY_SCOPES, emptySet()).orEmpty()
        set(value) = prefs.edit().putStringSet(KEY_SCOPES, value).apply()

    val hasTokens: Boolean
        get() = !refreshToken.isNullOrBlank() || !accessToken.isNullOrBlank()

    fun hasValidAccessToken(nowMillis: Long = System.currentTimeMillis()): Boolean =
        !accessToken.isNullOrBlank() && nowMillis < expiresAtMillis - EXPIRY_MARGIN_MS

    fun save(tokens: TokenResponse) {
        accessToken = tokens.accessToken
        expiresAtMillis = System.currentTimeMillis() + tokens.expiresInSeconds * 1000
        tokens.refreshToken?.takeIf { it.isNotBlank() }?.let { refreshToken = it }
    }

    fun markAuthorizedNow(nowMillis: Long = System.currentTimeMillis()) {
        if (authorizedAtMillis <= 0L) authorizedAtMillis = nowMillis
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "healthkit_tokens"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_AUTHORIZED_AT = "authorized_at"
        const val KEY_SCOPES = "granted_scopes"
        const val EXPIRY_MARGIN_MS = 60_000L
    }
}
