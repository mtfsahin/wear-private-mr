package com.sample.trdtse.m00859088.sample_music_app.data.models

import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitAuthMethod
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitConfig
import java.util.concurrent.TimeUnit

data class HealthKitState(
    val authMethod: HealthKitAuthMethod = HealthKitConfig.defaultAuthMethod,
    val isAuthorized: Boolean = false,
    val isAuthorizing: Boolean = false,
    val isLoadingWorkouts: Boolean = false,
    val workouts: List<WorkoutRecord> = emptyList(),
    val lastSyncedAtMillis: Long? = null,
    val errorMessage: String? = null,
    val lookbackDays: Long = HealthKitConfig.DEFAULT_LOOKBACK_DAYS,
) {
    val rangeStartMillis: Long
        get() = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(lookbackDays)
}
