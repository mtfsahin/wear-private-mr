package com.sample.trdtse.m00859088.sample_music_app.healthkit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.sample.trdtse.m00859088.sample_music_app.data.models.WorkoutRecord
import com.sample.trdtse.m00859088.sample_music_app.healthkit.api.ActivityRecordsApi
import com.sample.trdtse.m00859088.sample_music_app.healthkit.api.ApiSiteStore
import com.sample.trdtse.m00859088.sample_music_app.healthkit.api.WorkoutQueryResult
import com.sample.trdtse.m00859088.sample_music_app.healthkit.auth.AuthMethodStore
import com.sample.trdtse.m00859088.sample_music_app.healthkit.auth.AuthorizationRedirect
import com.sample.trdtse.m00859088.sample_music_app.healthkit.auth.HealthKitAuthorizer
import com.sample.trdtse.m00859088.sample_music_app.healthkit.auth.LoginFreeAuthorizer
import com.sample.trdtse.m00859088.sample_music_app.healthkit.auth.WebAuthActivity
import com.sample.trdtse.m00859088.sample_music_app.healthkit.auth.WebOAuthAuthorizer
import com.sample.trdtse.m00859088.sample_music_app.healthkit.net.HealthKitHttpException
import com.sample.trdtse.m00859088.sample_music_app.healthkit.token.HealthKitTokenClient
import com.sample.trdtse.m00859088.sample_music_app.healthkit.token.TokenStore
import java.util.UUID
import java.util.concurrent.TimeUnit

data class WorkoutFetch(
    val records: List<WorkoutRecord>,
    val notice: String? = null,
)

class HealthKitClient(
    context: Context,
    private val tokenStore: TokenStore = TokenStore(context),
    private val activityRecordsApi: ActivityRecordsApi = ActivityRecordsApi(ApiSiteStore(context)),
) {
    private val tokenClient = HealthKitTokenClient()

    private val methodStore = AuthMethodStore(context)

    private var pendingState: String? = null

    var authMethod: HealthKitAuthMethod = methodStore.method
        private set

    private var authorizer: HealthKitAuthorizer = createAuthorizer(authMethod)

    val isAuthorized: Boolean
        get() = tokenStore.hasTokens

    fun useMethod(method: HealthKitAuthMethod) {
        authMethod = method
        methodStore.method = method
        authorizer = createAuthorizer(method)
    }

    fun startAuthorization(activity: Activity): Result<Unit> {
        if (!HealthKitConfig.isConfigured(authMethod)) {
            return Result.failure(
                IllegalStateException(HealthKitConfig.missingConfigMessage(authMethod))
            )
        }
        val state = UUID.randomUUID().toString()
        pendingState = state
        return authorizer.start(activity, state)
    }

    fun ownsRedirect(uri: Uri?): Boolean = uri != null && authorizer.ownsRedirect(uri)

    suspend fun completeAuthorization(uri: Uri): Result<Unit> {
        val code = AuthorizationRedirect.parseCode(uri, pendingState)
            .getOrElse { return Result.failure(it) }
        pendingState = null

        return tokenClient.exchangeCode(code, authorizer.tokenRedirectUri)
            .mapCatching { tokens ->
                tokenStore.save(tokens)
                tokenStore.markAuthorizedNow()
                rememberGrantedScopes(tokens.accessToken)
            }
    }

    fun readAuthorizationOutcome(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ): AuthorizationOutcome? = when (requestCode) {
        WebAuthActivity.REQUEST_CODE -> WebAuthActivity.readOutcome(requestCode, data)

        LoginFreeAuthorizer.REQUEST_CODE ->
            LoginFreeAuthorizer.readOutcome(requestCode, resultCode, data)

        else -> null
    }

    suspend fun fetchWorkouts(
        days: Long = HealthKitConfig.DEFAULT_LOOKBACK_DAYS,
        endTimeMillis: Long = System.currentTimeMillis(),
        activityTypes: List<Int> = emptyList(),
    ): Result<WorkoutFetch> {
        val safeDays = days.coerceIn(1L, HealthKitConfig.MAX_QUERY_RANGE_DAYS)
        val asked = endTimeMillis - TimeUnit.DAYS.toMillis(safeDays)
        val startTime = asked

        val accessToken = accessToken().getOrElse { return Result.failure(it) }

        return read(accessToken, startTime, endTimeMillis, activityTypes)
            .map { outcome ->
                WorkoutFetch(
                    records = outcome.result.records,
                    notice = noticeFor(outcome.result, outcome.startUsedMillis, asked),
                )
            }
    }

    private class Read(val result: WorkoutQueryResult, val startUsedMillis: Long)

    private suspend fun read(
        accessToken: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        activityTypes: List<Int>,
    ): Result<Read> {
        val first = activityRecordsApi.fetchWorkouts(
            accessToken, startTimeMillis, endTimeMillis, activityTypes
        )
        val error = first.exceptionOrNull() as? HealthKitHttpException
            ?: return first.map { Read(it, startTimeMillis) }

        return when {
            error.isInvalidCredentials -> {
                val fresh = refreshAccessToken().getOrElse { return Result.failure(it) }
                activityRecordsApi.fetchWorkouts(
                    fresh, startTimeMillis, endTimeMillis, activityTypes
                ).map { Read(it, startTimeMillis) }
            }

            error.isQueryTimeOutOfRange -> {
                val authorizedAt = tokenStore.authorizedAtMillis
                when {
                    authorizedAt in (startTimeMillis + 1)..endTimeMillis -> {
                        Log.i(TAG, "Query time out of range, window restarted at $authorizedAt")
                        activityRecordsApi.fetchWorkouts(
                            accessToken, authorizedAt, endTimeMillis, activityTypes
                        ).map { Read(it, authorizedAt) }
                    }

                    authorizedAt <= 0L -> Result.failure(
                        IllegalStateException(
                            "Health Kit Cloud serves only data created after this user " +
                                "authorized the app, and this session was connected before " +
                                "the app started recording that moment. Disconnect and " +
                                "connect the account again.",
                            error,
                        )
                    )

                    else -> Result.failure(
                        IllegalStateException(
                            "Health Kit Cloud serves only data created after this user " +
                                "authorized the app. To read older data, apply for a " +
                                "historical data scope on the Health Service Kit card and " +
                                "set healthkit.historyWindow to WEEK, MONTH or YEAR.",
                            error,
                        )
                    )
                }
            }

            error.isInsufficientScope -> Result.failure(
                IllegalStateException(missingScopeMessage(accessToken), error)
            )

            else -> first.map { Read(it, startTimeMillis) }
        }
    }

    private fun noticeFor(
        result: WorkoutQueryResult,
        usedStartMillis: Long,
        askedStartMillis: Long,
    ): String? {
        if (result.records.isNotEmpty()) return null

        return when {
            result.privacyStatus == 2 ->
                "Huawei Health answered x-health-app-privacy 2: this user has not granted " +
                    "the Health Service Kit privacy authorization, so nothing is shared " +
                    "with the app. Open Huawei Health, grant it, and turn on cloud sync."

            result.privacyStatus == 3 ->
                "Huawei Health answered x-health-app-privacy 3: this account is not a " +
                    "Huawei Health user, so the cloud holds no workout data for it."

            usedStartMillis > askedStartMillis ->
                "No workouts in the readable window. The query could only start at the " +
                    "moment this user authorized the app, which is later than the range " +
                    "you asked for. Older data needs a historical data scope."

            else -> null
        }
    }

    suspend fun accessToken(): Result<String> {
        if (tokenStore.hasValidAccessToken()) {
            return Result.success(tokenStore.accessToken.orEmpty())
        }
        return refreshAccessToken()
    }

    private suspend fun refreshAccessToken(): Result<String> {
        val refreshToken = tokenStore.refreshToken
            ?: return Result.failure(IllegalStateException("Connect a Huawei Health account first."))

        return tokenClient.refresh(refreshToken)
            .map { tokens ->
                tokenStore.save(tokens)
                tokens.accessToken
            }
            .recoverCatching { error ->
                if ((error as? HealthKitHttpException)?.isRefreshTokenExpired == true) {
                    Log.i(TAG, "Refresh token expired, clearing the stored session")
                    tokenStore.clear()
                    throw IllegalStateException(
                        "The Huawei sign-in has ended, which happens after a password " +
                            "change, an account change or about six months. Connect the " +
                            "account again.",
                        error,
                    )
                }
                throw error
            }
    }

    private suspend fun rememberGrantedScopes(accessToken: String) {
        tokenClient.tokenInfo(accessToken)
            .onSuccess { info ->
                tokenStore.grantedScopes = info.scopes
                Log.i(TAG, "Granted scopes: ${info.scopes.joinToString(" ")}")
            }
            .onFailure { Log.w(TAG, "Could not read the token scopes", it) }
    }

    private suspend fun missingScopeMessage(accessToken: String): String {
        val granted = tokenClient.tokenInfo(accessToken).getOrNull()?.scopes
            ?: tokenStore.grantedScopes
        val missing = HealthKitScopes.DEFAULT.filterNot { it in granted }

        return if (missing.isEmpty()) {
            "Health Kit Cloud refused the call for lack of scopes even though the token " +
                "carries them. Check that the App ID used for the authorization code is " +
                "the App ID the Health Service Kit scopes were approved for."
        } else {
            "The token is missing ${missing.joinToString(", ")}. Apply for these scopes on " +
                "the Health Service Kit card, then authorize again and select them."
        }
    }

    fun signOut() {
        tokenStore.clear()
        pendingState = null
    }

    private fun createAuthorizer(method: HealthKitAuthMethod): HealthKitAuthorizer =
        when (method) {
            HealthKitAuthMethod.WEB_OAUTH -> WebOAuthAuthorizer()
            HealthKitAuthMethod.LOGIN_FREE_SDK -> LoginFreeAuthorizer()
        }

    private companion object {
        const val TAG = "HealthKitClient"
    }
}
