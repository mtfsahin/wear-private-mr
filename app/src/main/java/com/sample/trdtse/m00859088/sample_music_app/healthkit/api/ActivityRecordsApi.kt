package com.sample.trdtse.m00859088.sample_music_app.healthkit.api

import android.net.Uri
import android.util.Log
import com.sample.trdtse.m00859088.sample_music_app.data.models.WorkoutRecord
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitConfig
import com.sample.trdtse.m00859088.sample_music_app.healthkit.net.HealthKitHttpException
import com.sample.trdtse.m00859088.sample_music_app.healthkit.net.HttpJson
import com.sample.trdtse.m00859088.sample_music_app.healthkit.net.requireSuccess
import org.json.JSONArray
import org.json.JSONObject

data class WorkoutQueryResult(
    val records: List<WorkoutRecord>,
    val privacyStatus: Int?,
) {
    val privacyGranted: Boolean get() = privacyStatus == null || privacyStatus == 1
}

class ActivityRecordsApi(private val siteStore: ApiSiteStore? = null) {
    private val http = HttpJson()

    suspend fun fetchWorkouts(
        accessToken: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        activityTypes: List<Int> = emptyList(),
        sourceTypes: List<Int> = HealthKitConfig.ALL_SOURCE_TYPES,
    ): Result<WorkoutQueryResult> = runCatching {
        val firstHost = siteStore?.host ?: HealthKitConfig.defaultApiHost

        try {
            return@runCatching readFrom(
                firstHost, accessToken, startTimeMillis, endTimeMillis, activityTypes, sourceTypes
            )
        } catch (e: HealthKitHttpException) {
            if (!e.isSiteCross) throw e
            siteStore?.clear()

            val redirect = e.redirectHost
            if (redirect != null && !redirect.equals(firstHost, ignoreCase = true)) {
                Log.i(TAG, "Site cross from $firstHost, following Location to $redirect")
                return@runCatching readFrom(
                    redirect, accessToken, startTimeMillis, endTimeMillis, activityTypes,
                    sourceTypes,
                )
            }

            for (host in HealthKitConfig.FALLBACK_API_HOSTS) {
                if (host.equals(firstHost, ignoreCase = true)) continue
                try {
                    return@runCatching readFrom(
                        host, accessToken, startTimeMillis, endTimeMillis, activityTypes,
                        sourceTypes,
                    )
                } catch (retry: HealthKitHttpException) {
                    if (!retry.isSiteCross) throw retry
                }
            }
            throw e
        }
    }

    private suspend fun readFrom(
        host: String,
        accessToken: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        activityTypes: List<Int>,
        sourceTypes: List<Int>,
    ): WorkoutQueryResult {
        val records = mutableListOf<WorkoutRecord>()
        var privacyStatus: Int? = null
        var cursor: String? = null
        var page = 0

        do {
            val url = buildUrl(
                host, startTimeMillis, endTimeMillis, activityTypes, sourceTypes, cursor
            )
            val response = http.getJson(url, accessToken).requireSuccess(url)
            privacyStatus = response.header("x-health-app-privacy")?.toIntOrNull() ?: privacyStatus

            val json = response.json()
            records += readRecords(json)
            cursor = nextCursor(json)
            page++
        } while (cursor != null && page < MAX_PAGES)

        siteStore?.host = host
        return WorkoutQueryResult(records.sortedByDescending { it.startTime }, privacyStatus)
    }

    private fun buildUrl(
        host: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        activityTypes: List<Int>,
        sourceTypes: List<Int>,
        cursor: String?,
    ): String {
        val builder = Uri.parse(HealthKitConfig.activityRecordsUrl(host)).buildUpon()
        if (cursor != null) {
            builder.appendQueryParameter("cursor", cursor)
        } else {
            builder.appendQueryParameter("startTime", startTimeMillis.toString())
            builder.appendQueryParameter("endTime", endTimeMillis.toString())
            activityTypes.forEach { builder.appendQueryParameter("activityType", it.toString()) }
            sourceTypes.forEach { builder.appendQueryParameter("sourceType", it.toString()) }
        }
        return builder.build().toString()
    }

    private fun readRecords(json: JSONObject): List<WorkoutRecord> {
        val array = json.optJSONArray("activityRecord") ?: JSONArray()
        return (0 until array.length()).map { parseRecord(array.getJSONObject(it)) }
    }

    private fun nextCursor(json: JSONObject): String? {
        if (!json.optBoolean("hasMoreData", false)) return null
        return json.optString("cursor").takeIf { it.isNotBlank() }
    }

    private fun parseRecord(obj: JSONObject): WorkoutRecord {
        val summary = mutableMapOf<String, String>()
        val entries = obj.optJSONObject("activitySummary")?.optJSONArray("dataSummary")
        if (entries != null) {
            for (i in 0 until entries.length()) {
                val entry = entries.getJSONObject(i)
                val key = entry.optString("dataTypeName").takeIf { it.isNotBlank() } ?: continue
                summary[key] = readValue(entry.optJSONArray("value"))
            }
        }

        return WorkoutRecord(
            id = obj.optString("id"),
            name = obj.optString("name"),
            startTime = obj.optLong("startTime"),
            endTime = obj.optLong("endTime"),
            activityType = obj.optInt("activityType"),
            activeTimeMillis = obj.optLong("activeTime"),
            timeZone = obj.optString("timeZone"),
            sourceType = if (obj.has("sourceType")) obj.optInt("sourceType") else null,
            summary = summary,
        )
    }

    private fun readValue(values: JSONArray?): String {
        if (values == null || values.length() == 0) return ""
        return (0 until values.length()).mapNotNull { index ->
            val value = values.optJSONObject(index) ?: return@mapNotNull null
            VALUE_FIELDS.firstNotNullOfOrNull { field ->
                if (value.has(field)) formatNumber(value.opt(field)) else null
            }
        }.joinToString(", ")
    }

    private fun formatNumber(value: Any?): String = when (value) {
        is Double -> if (value % 1.0 == 0.0) "%.0f".format(value) else "%.2f".format(value)
        else -> value?.toString().orEmpty()
    }

    private companion object {
        const val MAX_PAGES = 5
        val VALUE_FIELDS = listOf("floatValue", "integerValue", "longValue", "stringValue")
        const val TAG = "ActivityRecordsApi"
    }
}
