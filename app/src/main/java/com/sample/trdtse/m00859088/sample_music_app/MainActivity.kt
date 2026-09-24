package com.sample.trdtse.m00859088.sample_music_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitClient
import com.sample.trdtse.m00859088.sample_music_app.managers.AuthManager
import com.sample.trdtse.m00859088.sample_music_app.managers.DeviceManager
import com.sample.trdtse.m00859088.sample_music_app.managers.P2pManager
import com.sample.trdtse.m00859088.sample_music_app.managers.WatchTypeStore
import com.sample.trdtse.m00859088.sample_music_app.media.NowPlayingController
import com.sample.trdtse.m00859088.sample_music_app.ui.WearEngineApp
import com.sample.trdtse.m00859088.sample_music_app.ui.theme.WearEngineAndroidLiteFileMessageSenderTheme
import com.sample.trdtse.m00859088.sample_music_app.viewmodels.MainViewModel
import com.sample.trdtse.m00859088.sample_music_app.viewmodels.MainViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var p2pManager: P2pManager
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        p2pManager = P2pManager(this)

        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(
                authManager = AuthManager(this),
                deviceManager = DeviceManager(this),
                p2pManager = p2pManager,
                healthKitClient = HealthKitClient(this),
                nowPlayingController = NowPlayingController(this),
                watchTypeStore = WatchTypeStore(this),
            ),
        )[MainViewModel::class.java]

        setContent {
            WearEngineAndroidLiteFileMessageSenderTheme {
                WearEngineApp(viewModel = viewModel)
            }
        }

        intent?.data?.let(viewModel::handleHealthKitRedirect)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let(viewModel::handleHealthKitRedirect)
    }

    @Deprecated("Kept because the login-free SDK reports its outcome this way.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.handleAuthorizationActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        viewModel.sendAppDestroy(if (isFinishing) "user closed the app" else "activity destroyed")
        p2pManager.unregisterReceiver()
        super.onDestroy()
    }
}
