package com.sample.trdtse.m00859088.sample_music_app.healthkit.net

import android.util.Log
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

internal class HttpResponse(
    val status: Int,
    val body: String,
    private val headers: Map<String, String>,
) {
    fun header(name: String): String? = headers[name.lowercase()]

    val isSuccess: Boolean get() = status in 200..299

    fun json(): JSONObject = JSONObject(body)
}

internal class HttpJson(
    private val connectTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
    private val readTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
) {
    suspend fun postForm(url: String, form: Map<String, String>): JSONObject {
        val response = post(url, form).requireSuccess(url)
        val json = response.json()
        if (carriesError(json)) throw HealthKitHttpException(response, url)
        return json
    }

    private fun carriesError(json: JSONObject): Boolean {
        if (json.isNull("error")) return false
        val asObject = json.optJSONObject("error")
        if (asObject != null) return asObject.optInt("code", 0) != 0
        val text = json.optString("error")
        return json.optInt("error", 0) != 0 || (text.isNotBlank() && text.toIntOrNull() == null)
    }

    suspend fun post(url: String, form: Map<String, String>): HttpResponse =
        withContext(Dispatchers.IO) {
            val body = form.entries.joinToString("&") { (k, v) -> "${encode(k)}=${encode(v)}" }
            val connection = open(url, "POST", followRedirects = true).apply {
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            connection.use {
                it.outputStream.use { out -> out.write(body.toByteArray(Charsets.UTF_8)) }
                it.read()
            }
        }

    suspend fun getJson(url: String, accessToken: String): HttpResponse =
        withContext(Dispatchers.IO) {
            val traceId = UUID.randomUUID().toString()
            val connection = open(url, "GET", followRedirects = false).apply {
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("x-client-id", HealthKitConfig.clientId)
                setRequestProperty("x-caller-trace-id", traceId)
            }
            connection.use { it.read(traceId) }
        }

    private fun open(
        url: String,
        method: String,
        followRedirects: Boolean,
    ): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            instanceFollowRedirects = followRedirects
        }

    private fun HttpURLConnection.read(traceId: String? = null): HttpResponse {
        val status = responseCode
        val stream = if (status in 200..299) inputStream else errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

        val headers = headerFields
            .filterKeys { it != null }
            .mapKeys { (key, _) -> key.lowercase() }
            .mapValues { (_, values) -> values.firstOrNull().orEmpty() }

        if (traceId != null) {
            Log.i(
                TAG,
                "$requestMethod $url -> $status" +
                    ", x-health-app-privacy=" + (headers["x-health-app-privacy"] ?: "absent") +
                    ", trace=" + traceId +
                    ", bytes=" + text.length,
            )
        }

        return HttpResponse(status, text, headers)
    }

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T =
        try {
            block(this)
        } finally {
            disconnect()
        }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 15_000
        const val TAG = "HealthKitHttp"
    }
}

internal fun HttpResponse.requireSuccess(url: String): HttpResponse {
    if (!isSuccess) throw HealthKitHttpException(this, url)
    return this
}

internal class HealthKitHttpException(
    private val response: HttpResponse,
    url: String,
) : Exception("HTTP ${response.status} from $url: ${response.body.take(500)}${hintFor(response)}") {
    val statusCode: Int get() = response.status
    val body: String get() = response.body

    val errorCode: Int? = parse(response.body) { json ->
        json.optJSONObject("error")?.optInt("code", 0)?.takeIf { it != 0 }
            ?: json.optInt("code", 0).takeIf { it != 0 }
    }

    val resultCode: Int? = parse(response.body) { json ->
        json.optInt("error", 0).takeIf { it != 0 } ?: json.optInt("code", 0).takeIf { it != 0 }
    }

    val subError: Int? = parse(response.body) { it.optInt("sub_error").takeIf { v -> v != 0 } }

    val isSiteCross: Boolean
        get() = errorCode == HealthKitConfig.SITE_CROSS_ERROR_CODE ||
            (statusCode == 403 && response.body.contains("site cross", ignoreCase = true))

    val redirectHost: String?
        get() = response.header("Location")
            ?.takeIf { it.isNotBlank() }
            ?.let { raw ->
                val absolute = if (raw.contains("://")) raw else "https://$raw"
                runCatching { URL(absolute).host }.getOrNull()
            }
            ?.takeIf { it.isNotBlank() }

    val isInvalidCredentials: Boolean
        get() = statusCode == 401 ||
            response.body.contains("Invalid Credentials", ignoreCase = true)

    val isInsufficientScope: Boolean
        get() = statusCode == 403 &&
            response.body.contains("insufficient authentication scopes", ignoreCase = true)

    val isQueryTimeOutOfRange: Boolean
        get() = statusCode == 403 &&
            response.body.contains("query time is out of range", ignoreCase = true)

    val isInvalidArgument: Boolean
        get() = statusCode == 400 &&
            response.body.contains("invalidArgument", ignoreCase = true)

    val isRefreshTokenExpired: Boolean
        get() = resultCode == HealthKitConfig.REFRESH_TOKEN_RESULT_CODE &&
            HealthKitConfig.REFRESH_TOKEN_EXPIRED_SUB_ERRORS.contains(subError ?: -1)

    private fun <T> parse(body: String, read: (JSONObject) -> T?): T? =
        runCatching { read(JSONObject(body)) }.getOrNull()
}

private fun hintFor(response: HttpResponse): String {
    val body = response.body
    return when {
        body.contains("query time is out of range", ignoreCase = true) ->
            " — Health Kit Cloud only serves data created after the user authorized the " +
                "app. Start the window at the authorization time, or ask for one of the " +
                "historydata.open scopes."

        body.contains("insufficient authentication scopes", ignoreCase = true) ->
            " — the access token does not carry the scope this call needs. Check the scope " +
                "list on the token and the scopes approved for this App ID."

        body.contains("site cross", ignoreCase = true) ->
            " — wrong regional site. The Location header names the site to use."

        body.contains("Invalid Credentials", ignoreCase = true) ->
            " — the access token is not valid any more. Refresh it, and authorize again if " +
                "the refresh fails."

        body.contains("client_id not match", ignoreCase = true) ->
            " — the authorization code belongs to a different App ID than the client_id " +
                "sent here. The login-free SDK issues the code for the Android App ID, so " +
                "the token call has to use healthkit.loginFreeAppId with that app own " +
                "secret."

        body.contains("invalid code", ignoreCase = true) ->
            " — the authorization code was already used or has expired. Each code works " +
                "once, for a few minutes."

        else -> ""
    }
}
