package com.sample.trdtse.m00859088.sample_music_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitClient
import com.sample.trdtse.m00859088.sample_music_app.managers.AuthManager
import com.sample.trdtse.m00859088.sample_music_app.managers.DeviceManager
import com.sample.trdtse.m00859088.sample_music_app.managers.P2pManager
import com.sample.trdtse.m00859088.sample_music_app.managers.WatchTypeStore
import com.sample.trdtse.m00859088.sample_music_app.media.NowPlayingController

class MainViewModelFactory(
    private val authManager: AuthManager,
    private val deviceManager: DeviceManager,
    private val p2pManager: P2pManager,
    private val healthKitClient: HealthKitClient,
    private val nowPlayingController: NowPlayingController,
    private val watchTypeStore: WatchTypeStore,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MainViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(
            authManager = authManager,
            deviceManager = deviceManager,
            p2pManager = p2pManager,
            healthKitClient = healthKitClient,
            nowPlayingController = nowPlayingController,
            watchTypeStore = watchTypeStore,
        ) as T
    }
}
