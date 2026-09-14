package com.sample.trdtse.m00859088.sample_music_app.healthkit.token

import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitConfig
import com.sample.trdtse.m00859088.sample_music_app.healthkit.net.HttpJson

internal class HealthKitTokenClient(
    private val http: HttpJson = HttpJson(),
) {
    suspend fun exchangeCode(code: String, redirectUri: String?): Result<TokenResponse> =
        runCatching {
            val form = buildMap {
                put("grant_type", "authorization_code")
                put("code", code)
                put("client_id", HealthKitConfig.clientId)
                put("client_secret", HealthKitConfig.clientSecret)
                if (!redirectUri.isNullOrBlank()) put("redirect_uri", redirectUri)
            }
            TokenResponse.from(http.postForm(HealthKitConfig.TOKEN_URL, form))
        }

    suspend fun refresh(refreshToken: String): Result<TokenResponse> = runCatching {
        val form = mapOf(
            "grant_type" to "refresh_token",
            "refresh_token" to refreshToken,
            "client_id" to HealthKitConfig.clientId,
            "client_secret" to HealthKitConfig.clientSecret,
        )
        TokenResponse.from(http.postForm(HealthKitConfig.TOKEN_URL, form))
    }

    suspend fun tokenInfo(accessToken: String): Result<TokenInfo> = runCatching {
        val form = mapOf(
            "open_id" to "OPENID",
            "access_token" to accessToken,
        )
        TokenInfo.from(http.postForm(HealthKitConfig.TOKEN_INFO_URL, form))
    }
}
