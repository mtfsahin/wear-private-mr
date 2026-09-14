package com.sample.trdtse.m00859088.sample_music_app.healthkit

import android.net.Uri
import com.sample.trdtse.m00859088.sample_music_app.BuildConfig

enum class HealthKitAuthMethod(val label: String) {
    WEB_OAUTH("Web OAuth"),
    LOGIN_FREE_SDK("Login-free SDK"),
}

enum class HistoryWindow(val scope: String?, val days: Long) {
    NONE(null, 0L),
    WEEK("https://www.huawei.com/healthkit/historydata.open.week", 7L),
    MONTH("https://www.huawei.com/healthkit/historydata.open.month", 31L),
    YEAR("https://www.huawei.com/healthkit/historydata.open.year", 365L),
}

object HealthKitConfig {
    val clientId: String = BuildConfig.HEALTH_KIT_CLIENT_ID
    val clientSecret: String = BuildConfig.HEALTH_KIT_CLIENT_SECRET
    val deepLinkUrl: String = BuildConfig.HEALTH_KIT_DEEP_LINK
    val schemeSecret: String = BuildConfig.HEALTH_KIT_SCHEME_SECRET

    val redirectUri: String =
        BuildConfig.HEALTH_KIT_REDIRECT_URI.takeIf { it.isNotBlank() } ?: deepLinkUrl

    val loginFreeAppId: String =
        BuildConfig.HEALTH_KIT_LOGIN_FREE_APP_ID.takeIf { it.isNotBlank() } ?: clientId

    val redirectIsOwnDeepLink: Boolean
        get() = Uri.parse(redirectUri).scheme.equals(
            Uri.parse(deepLinkUrl).scheme,
            ignoreCase = true,
        )

    val defaultAuthMethod: HealthKitAuthMethod =
        runCatching { HealthKitAuthMethod.valueOf(BuildConfig.HEALTH_KIT_AUTH_METHOD) }
            .getOrDefault(HealthKitAuthMethod.WEB_OAUTH)

    val historyWindow: HistoryWindow =
        runCatching { HistoryWindow.valueOf(BuildConfig.HEALTH_KIT_HISTORY_WINDOW) }
            .getOrDefault(HistoryWindow.NONE)

    const val LOGIN_FREE_CALLBACK_URL =
        "https://h5hosting.dbankcdn.com/cch5/healthkit/oauth-h5/oauth-callback.html"
    const val SEND_REDIRECT_URI_FOR_LOGIN_FREE = true

    const val AUTHORIZE_URL = "https://oauth-login.cloud.huawei.com/oauth2/v3/authorize"
    const val TOKEN_URL = "https://oauth-login.cloud.huawei.com/oauth2/v3/token"

    const val TOKEN_INFO_URL =
        "https://oauth-api.cloud.huawei.com/rest.php?nsp_fmt=JSON&nsp_svc=huawei.oauth2.user.getTokenInfo"

    const val ACTIVITY_RECORDS_PATH = "healthkit/v2/activityRecords"

    val defaultApiHost: String =
        BuildConfig.HEALTH_KIT_API_HOST.takeIf { it.isNotBlank() } ?: "health-api.cloud.huawei.com"

    val FALLBACK_API_HOSTS: List<String> = listOf(
        "health-api.cloud.huawei.com",
        "health-api.cloud.huawei.eu",
        "health-api.cloud.huawei.asia",
        "health-api.cloud.huawei.ru",
        "health-api.cloud.huawei.com.cn",
    )

    const val SITE_CROSS_ERROR_CODE = 121001

    const val REFRESH_TOKEN_RESULT_CODE = 1203
    val REFRESH_TOKEN_EXPIRED_SUB_ERRORS: Set<Int> = setOf(11205, 31204)

    fun activityRecordsUrl(host: String): String = "https://$host/$ACTIVITY_RECORDS_PATH"

    val ALL_SOURCE_TYPES: List<Int> = listOf(0, 1, 2, 3, 4, 5)

    const val DEFAULT_LOOKBACK_DAYS = 7L

    const val MAX_QUERY_RANGE_DAYS = 365L

    private fun requiredValues(method: HealthKitAuthMethod): Map<String, String> =
        when (method) {
            HealthKitAuthMethod.WEB_OAUTH -> mapOf(
                "healthkit.clientId" to clientId,
                "healthkit.clientSecret" to clientSecret,
                "healthkit.redirectUri" to redirectUri,
            )

            HealthKitAuthMethod.LOGIN_FREE_SDK -> mapOf(
                "healthkit.clientId" to clientId,
                "healthkit.clientSecret" to clientSecret,
                "healthkit.loginFreeAppId" to loginFreeAppId,
                "healthkit.deepLink" to deepLinkUrl,
                "healthkit.schemeSecret" to schemeSecret,
            )
        }

    fun isConfigured(method: HealthKitAuthMethod): Boolean =
        requiredValues(method).values.none { it.isBlank() }

    fun missingConfigMessage(method: HealthKitAuthMethod): String {
        val missing = requiredValues(method).filterValues { it.isBlank() }.keys
        return "Add ${missing.joinToString(", ")} to local.properties (see local.properties.example)."
    }

    fun isDeepLink(uri: Uri?): Boolean = matches(uri, deepLinkUrl)

    fun isRedirectUri(uri: Uri?): Boolean = matches(uri, redirectUri)

    private fun matches(uri: Uri?, target: String): Boolean {
        if (uri == null || target.isBlank()) return false
        val expected = Uri.parse(target)
        return uri.scheme.equals(expected.scheme, ignoreCase = true) &&
            uri.host.equals(expected.host, ignoreCase = true) &&
            uri.path.orEmpty().trimEnd('/') == expected.path.orEmpty().trimEnd('/')
    }
}

object HealthKitScopes {
    const val OPENID = "openid"
    const val ACTIVITY_RECORD_READ = "https://www.huawei.com/healthkit/activityrecord.read"
    const val ACTIVITY_READ = "https://www.huawei.com/healthkit/activity.read"

    private val BASE = listOf(OPENID, ACTIVITY_RECORD_READ, ACTIVITY_READ)

    val DEFAULT: List<String>
        get() = BASE + listOfNotNull(HealthKitConfig.historyWindow.scope)
}
