package com.sample.trdtse.m00859088.sample_music_app.healthkit.token

import org.json.JSONObject

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresInSeconds: Long,
) {
    companion object {
        fun from(json: JSONObject): TokenResponse = TokenResponse(
            accessToken = json.getString("access_token"),
            refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() },
            expiresInSeconds = json.optLong("expires_in", DEFAULT_EXPIRY_SECONDS),
        )

        private const val DEFAULT_EXPIRY_SECONDS = 3600L
    }
}

data class TokenInfo(
    val scopes: Set<String>,
    val expiresInSeconds: Long,
    val openId: String?,
    val unionId: String?,
    val clientId: String?,
) {
    companion object {
        fun from(json: JSONObject): TokenInfo = TokenInfo(
            scopes = json.optString("scope")
                .split(' ', ',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet(),
            expiresInSeconds = json.optLong("expire_in", 0L),
            openId = json.optString("open_id").takeIf { it.isNotBlank() },
            unionId = json.optString("union_id").takeIf { it.isNotBlank() },
            clientId = json.optString("client_id").takeIf { it.isNotBlank() },
        )
    }
}
