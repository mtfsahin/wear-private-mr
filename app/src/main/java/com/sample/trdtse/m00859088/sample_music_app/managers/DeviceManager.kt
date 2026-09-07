package com.sample.trdtse.m00859088.sample_music_app.managers

import android.content.Context
import com.huawei.hmf.tasks.OnFailureListener
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.device.DeviceClient

class DeviceManager(context: Context) {

    private val deviceClient: DeviceClient = HiWear.getDeviceClient(context)

    fun getBondedDevices(
        success: OnSuccessListener<MutableList<Device?>?>,
        failure: OnFailureListener?,
    ) {
        deviceClient.bondedDevices
            .addOnSuccessListener(success)
            .addOnFailureListener(failure)
    }
}
