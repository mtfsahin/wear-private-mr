package com.sample.trdtse.m00859088.sample_music_app.data.models

import com.huawei.wearengine.device.Device
import com.sample.trdtse.m00859088.sample_music_app.managers.WatchType

data class DeviceState(
    val devices: List<Device> = emptyList(),
    val selectedDevice: Device? = null,
    val isLoading: Boolean = false,
    val permissionsGranted: Boolean = false,
    val errorMessage: String? = null,
    val watchType: WatchType = WatchType.LITE,
)
