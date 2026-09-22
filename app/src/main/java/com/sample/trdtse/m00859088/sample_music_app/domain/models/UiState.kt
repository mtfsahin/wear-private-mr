package com.sample.trdtse.m00859088.sample_music_app.domain.models

import androidx.compose.runtime.Immutable
import com.sample.trdtse.m00859088.sample_music_app.data.models.DeviceState
import com.sample.trdtse.m00859088.sample_music_app.data.models.MessageState
import com.sample.trdtse.m00859088.sample_music_app.data.models.NowPlayingState
import com.sample.trdtse.m00859088.sample_music_app.data.models.FileState
import com.sample.trdtse.m00859088.sample_music_app.data.models.HealthKitState

@Immutable
data class UiState(
    val deviceState: DeviceState = DeviceState(),
    val messageState: MessageState = MessageState(),
    val fileState: FileState = FileState(),
    val healthKitState: HealthKitState = HealthKitState(),
    val nowPlayingState: NowPlayingState = NowPlayingState(),
    val receivedMessages: List<String> = emptyList(),
    val logMessages: List<String> = emptyList()
)