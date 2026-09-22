package com.sample.trdtse.m00859088.sample_music_app.viewmodels

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huawei.wearengine.device.Device
import com.sample.trdtse.m00859088.sample_music_app.data.models.DeviceState
import com.sample.trdtse.m00859088.sample_music_app.data.models.FileState
import com.sample.trdtse.m00859088.sample_music_app.data.models.HealthKitState
import com.sample.trdtse.m00859088.sample_music_app.data.models.MessageState
import com.sample.trdtse.m00859088.sample_music_app.domain.models.UiState
import com.sample.trdtse.m00859088.sample_music_app.healthkit.AuthorizationOutcome
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitAuthMethod
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitClient
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitConfig
import com.sample.trdtse.m00859088.sample_music_app.managers.AuthManager
import com.sample.trdtse.m00859088.sample_music_app.media.BundledTrack
import com.sample.trdtse.m00859088.sample_music_app.media.NowPlaying
import com.sample.trdtse.m00859088.sample_music_app.media.NowPlayingController
import com.sample.trdtse.m00859088.sample_music_app.media.WatchCommand
import com.sample.trdtse.m00859088.sample_music_app.media.WatchMediaMessages
import com.sample.trdtse.m00859088.sample_music_app.managers.DeviceManager
import com.sample.trdtse.m00859088.sample_music_app.managers.P2pManager
import com.sample.trdtse.m00859088.sample_music_app.managers.WatchType
import com.sample.trdtse.m00859088.sample_music_app.managers.WatchTypeStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import kotlin.math.abs

class MainViewModel(
    private val authManager: AuthManager,
    private val deviceManager: DeviceManager,
    private val p2pManager: P2pManager,
    private val healthKitClient: HealthKitClient,
    private val nowPlayingController: NowPlayingController,
    private val watchTypeStore: WatchTypeStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    private data class SentSnapshot(
        val track: NowPlaying?,
        val sentAtMillis: Long,
    )

    private var lastSent: SentSnapshot? = null
    private var sendJob: Job? = null

    init {
        applyWatchType(watchTypeStore.watchType)
        checkPermissionsAndLoadDevices()
        nowPlayingController.start(::refreshNowPlaying)
        refreshNowPlaying()
        startNowPlayingPolling()

        updateHealthKit {
            it.copy(
                isAuthorized = healthKitClient.isAuthorized,
                authMethod = healthKitClient.authMethod,
            )
        }
    }

    private fun updateDevice(transform: (DeviceState) -> DeviceState) {
        _uiState.update { it.copy(deviceState = transform(it.deviceState)) }
    }

    private fun updateMessage(transform: (MessageState) -> MessageState) {
        _uiState.update { it.copy(messageState = transform(it.messageState)) }
    }

    private fun updateFile(transform: (FileState) -> FileState) {
        _uiState.update { it.copy(fileState = transform(it.fileState)) }
    }

    private fun updateHealthKit(transform: (HealthKitState) -> HealthKitState) {
        _uiState.update { it.copy(healthKitState = transform(it.healthKitState)) }
    }

    private fun checkPermissionsAndLoadDevices() {
        authManager.checkPermissions(object : AuthManager.AuthCheckCallback {
            override fun onResult(allPermissionsGranted: Boolean) {
                if (allPermissionsGranted) loadDevices() else requestDevicePermission()
            }

            override fun onError(e: Exception?) {
                requestDevicePermission()
            }
        })
    }

    fun requestDevicePermission(onSuccess: Runnable? = null, onCancel: Runnable? = null) {
        authManager.requestPermission(onSuccess, onCancel)
    }

    fun selectDevice(device: Device) {
        updateDevice { it.copy(selectedDevice = device) }
        registerWatchReceiver(device)
        addLogMessage("Selected device: ${device.name}")
        if (_uiState.value.nowPlayingState.mirrorToWatch) forceSendNowPlaying()
    }

    private fun registerWatchReceiver(device: Device) {
        p2pManager.registerReceiver(device, object : P2pManager.MessageListener {
            override fun onMessageReceived(message: String?) {
                message?.let(::handleWatchMessage)
            }

            override fun onMessageSent(message: String?) = Unit
        })
    }

    fun selectWatchType(type: WatchType) {
        if (type == _uiState.value.deviceState.watchType) return
        watchTypeStore.watchType = type
        applyWatchType(type)
        _uiState.value.deviceState.selectedDevice?.let(::registerWatchReceiver)
    }

    private fun applyWatchType(type: WatchType) {
        p2pManager.setPeer(type)
        updateDevice { it.copy(watchType = type) }
        addLogMessage(
            if (type.isConfigured) "Watch type: ${type.label} (${type.peerPackage})"
            else "Watch type: ${type.label}, peer package or fingerprint is empty in local.properties"
        )
    }

    private fun handleWatchMessage(raw: String) {
        _uiState.update { it.copy(receivedMessages = it.receivedMessages + raw) }
        val command = WatchMediaMessages.parseCommand(raw) ?: return
        viewModelScope.launch {
            addLogMessage("Watch command: ${command.name.lowercase()}")
            when (command) {
                WatchCommand.PLAY -> nowPlayingController.play()
                WatchCommand.PAUSE -> nowPlayingController.pause()
                WatchCommand.TOGGLE -> nowPlayingController.playPause()
                WatchCommand.NEXT -> nowPlayingController.next()
                WatchCommand.PREVIOUS -> nowPlayingController.previous()
            }
            forceSendNowPlaying()
        }
    }

    fun updateMessageText(text: String) {
        updateMessage { it.copy(text = text) }
    }

    fun sendPing() {
        val selectedDevice = _uiState.value.deviceState.selectedDevice
        if (selectedDevice == null) {
            addLogMessage("No device selected for ping")
            return
        }

        val peerPackage = _uiState.value.deviceState.watchType.peerPackage
        p2pManager.pingDevice(selectedDevice, peerPackage, { result ->
            addLogMessage("Send Ping Result: $result")
        }, { error ->
            addLogMessage("Ping failed: ${error.message}")
        })
    }

    fun sendMessage(message: String) {
        val selectedDevice = _uiState.value.deviceState.selectedDevice
        if (selectedDevice == null) {
            addLogMessage("No device selected for message")
            return
        }

        updateMessage { it.copy(isSending = true) }

        p2pManager.sendMessage(selectedDevice, message, {
            updateMessage { it.copy(isSending = false, text = "") }
            addLogMessage("Message sent: $message")
        }, { error ->
            updateMessage { it.copy(isSending = false) }
            Log.d(TAG, "sendMessage failed: ${error.message}", error)
            addLogMessage("Message failed: ${error.message}")
        })
    }

    fun sendFile(context: Context) {
        val selectedDevice = _uiState.value.deviceState.selectedDevice
        if (selectedDevice == null) {
            addLogMessage("Cannot send file: device not selected")
            return
        }

        updateFile { it.copy(isSending = true) }

        p2pManager.sendFile(context, selectedDevice, {
            updateFile { it.copy(isSending = false) }
            addLogMessage("File sent")
        }, { error ->
            updateFile { it.copy(isSending = false) }
            addLogMessage("File failed: ${error.message}")
        })
    }

    fun refreshDevices() {
        loadDevices()
    }

    fun clearLogs() {
        _uiState.update { it.copy(receivedMessages = emptyList(), logMessages = emptyList()) }
    }

    private fun loadDevices() {
        updateDevice { it.copy(isLoading = true) }

        deviceManager.getBondedDevices({ deviceList ->
            val devices = deviceList?.filterNotNull() ?: emptyList()
            updateDevice {
                it.copy(
                    devices = devices,
                    isLoading = false,
                    permissionsGranted = true,
                    errorMessage = null,
                )
            }
            addLogMessage(
                if (devices.isEmpty()) "No bonded devices found"
                else "Found ${devices.size} bonded devices"
            )
        }, { error ->
            val refused = AuthManager.isPermissionRefused(error)
            val text = AuthManager.describeError(error)
            updateDevice {
                it.copy(
                    isLoading = false,
                    permissionsGranted = !refused,
                    errorMessage = text,
                )
            }
            addLogMessage("Device loading failed: $text")
        })
    }

    @SuppressLint("SimpleDateFormat")
    private fun addLogMessage(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())
        _uiState.update { it.copy(logMessages = it.logMessages + "[$timestamp] $message") }
    }

    fun selectAuthMethod(method: HealthKitAuthMethod) {
        healthKitClient.useMethod(method)
        updateHealthKit { it.copy(authMethod = method, errorMessage = null) }
        addLogMessage("Health Kit method: ${method.label}")
    }

    fun connectHealthKit(activity: Activity) {
        updateHealthKit { it.copy(isAuthorizing = true, errorMessage = null) }

        healthKitClient.startAuthorization(activity)
            .onSuccess { addLogMessage("Waiting for Huawei Health authorization") }
            .onFailure { error ->
                setHealthKitError("Could not start authorization: ${error.message}")
            }
    }

    fun handleHealthKitRedirect(uri: Uri) {
        if (!healthKitClient.ownsRedirect(uri)) return

        viewModelScope.launch {
            healthKitClient.completeAuthorization(uri)
                .onSuccess {
                    updateHealthKit {
                        it.copy(isAuthorizing = false, isAuthorized = true, errorMessage = null)
                    }
                    addLogMessage("Huawei Health account connected")
                    fetchRecentWorkouts()
                }
                .onFailure { error ->
                    setHealthKitError("Authorization failed: ${error.message}")
                }
        }
    }

    fun handleAuthorizationActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        healthKitClient.readAuthorizationOutcome(requestCode, resultCode, data)
            ?.let { outcome ->
                when (outcome) {
                    is AuthorizationOutcome.Redirect -> handleHealthKitRedirect(outcome.uri)
                    is AuthorizationOutcome.Failed -> setHealthKitError(outcome.message)
                }
            }
    }

    fun selectLookbackDays(days: Long) {
        val safe = days.coerceIn(1L, HealthKitConfig.MAX_QUERY_RANGE_DAYS)
        updateHealthKit { it.copy(lookbackDays = safe) }
        fetchRecentWorkouts()
    }

    fun fetchRecentWorkouts() {
        updateHealthKit { it.copy(isLoadingWorkouts = true, errorMessage = null) }
        val range = _uiState.value.healthKitState

        viewModelScope.launch {
            healthKitClient.fetchWorkouts(days = range.lookbackDays)
                .onSuccess { fetch ->
                    updateHealthKit {
                        it.copy(
                            isLoadingWorkouts = false,
                            workouts = fetch.records,
                            lastSyncedAtMillis = System.currentTimeMillis(),
                            errorMessage = fetch.notice,
                        )
                    }
                    addLogMessage(
                        fetch.notice ?: "Fetched ${fetch.records.size} workout(s)"
                    )
                }
                .onFailure { error ->
                    setHealthKitError("Fetch failed: ${error.message}")
                }
        }
    }

    fun disconnectHealthKit() {
        healthKitClient.signOut()
        updateHealthKit {
            HealthKitState(
                authMethod = healthKitClient.authMethod,
            )
        }
        addLogMessage("Disconnected Huawei Health account")
    }

    private fun setHealthKitError(message: String) {
        updateHealthKit {
            it.copy(isAuthorizing = false, isLoadingWorkouts = false, errorMessage = message)
        }
        addLogMessage(message)
    }



    fun refreshNowPlaying() {
        val track = nowPlayingController.read()
        _uiState.update {
            it.copy(
                nowPlayingState = it.nowPlayingState.copy(
                    hasAccess = nowPlayingController.hasAccess(),
                    track = track,
                )
            )
        }
        if (_uiState.value.nowPlayingState.mirrorToWatch &&
            sendJob?.isActive != true &&
            hasChangedSinceLastSend(track)
        ) {
            scheduleNowPlayingSend()
        }
    }


    private fun startNowPlayingPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                refreshNowPlaying()
                delay(POLL_INTERVAL_MS)
            }
        }
    }
    fun togglePlayPause() {
        nowPlayingController.playPause()
        refreshNowPlaying()
    }

    fun skipToNext() {
        nowPlayingController.next()
        refreshNowPlaying()
    }

    fun skipToPrevious() {
        nowPlayingController.previous()
        refreshNowPlaying()
    }

    fun seekBy(deltaMillis: Long) {
        nowPlayingController.seekBy(deltaMillis)
        refreshNowPlaying()
    }

    fun seekTo(positionMillis: Long) {
        nowPlayingController.seekTo(positionMillis)
        refreshNowPlaying()
    }

    fun sendAudioFile(context: Context, uri: Uri, displayName: String?) {
        val selectedDevice = _uiState.value.deviceState.selectedDevice
        if (selectedDevice == null) {
            addLogMessage("Cannot send audio: device not selected")
            return
        }

        updateFile { it.copy(isSending = true) }

        viewModelScope.launch {
            val copied = runCatching {
                withContext(Dispatchers.IO) {
                    val name = displayName?.takeIf { it.isNotBlank() } ?: "track.mp3"
                    val target = File(context.cacheDir, name)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("Could not open the picked file")
                    target
                }
            }.getOrElse { error ->
                updateFile { it.copy(isSending = false) }
                addLogMessage("Audio copy failed: ${error.message}")
                return@launch
            }

            addLogMessage("Sending ${copied.name} (${copied.length() / 1024} KB)")
            p2pManager.sendFile(selectedDevice, copied, {
                updateFile { it.copy(isSending = false) }
                addLogMessage("Audio sent: ${copied.name}")
            }, { error ->
                updateFile { it.copy(isSending = false) }
                addLogMessage("Audio failed: ${error.message}")
            })
        }
    }



    fun setMirrorToWatch(enabled: Boolean) {
        _uiState.update {
            it.copy(nowPlayingState = it.nowPlayingState.copy(mirrorToWatch = enabled))
        }
        if (enabled) {
            addLogMessage("Sending track, play/pause and seek changes to the watch")
            forceSendNowPlaying()
        } else {
            sendJob?.cancel()
            addLogMessage("Stopped sending the player to the watch")
        }
    }

    private fun hasChangedSinceLastSend(track: NowPlaying?): Boolean {
        val last = lastSent ?: return true
        val previous = last.track
        if (previous == null || track == null) return previous != track
        if (previous.packageName != track.packageName ||
            previous.title != track.title ||
            previous.artist != track.artist ||
            previous.isPlaying != track.isPlaying
        ) {
            return true
        }
        val expected = if (track.isPlaying) {
            previous.positionMillis + (System.currentTimeMillis() - last.sentAtMillis)
        } else {
            previous.positionMillis
        }
        return abs(track.positionMillis - expected) > SEEK_TOLERANCE_MS
    }

    private fun forceSendNowPlaying() {
        lastSent = null
        scheduleNowPlayingSend()
    }

    private fun scheduleNowPlayingSend() {
        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            delay(SEND_DEBOUNCE_MS)
            val track = nowPlayingController.read()
            lastSent = SentSnapshot(track, System.currentTimeMillis())
            sendNowPlayingToWatch(track)
        }
    }

    private fun sendNowPlayingToWatch(track: NowPlaying?) {
        val payload = WatchMediaMessages.nowPlaying(track)
        val device = _uiState.value.deviceState.selectedDevice ?: run {
            Log.i(TAG, "no device, would send: $payload")
            addLogMessage("No device, would send: $payload")
            return
        }
        p2pManager.sendMessage(device, payload, {
            _uiState.update {
                it.copy(
                    nowPlayingState = it.nowPlayingState.copy(
                        mirroredCount = it.nowPlayingState.mirroredCount + 1,
                        mirrorError = null,
                    )
                )
            }
            addLogMessage("Sent to watch: $payload")
        }, { error ->
            _uiState.update {
                it.copy(
                    nowPlayingState = it.nowPlayingState.copy(mirrorError = error.message)
                )
            }
            addLogMessage("Now playing send failed: ${error.message}")
        })
    }

    fun sendAppDestroy(reason: String) {
        val payload = WatchMediaMessages.appDestroy(reason)
        val device = _uiState.value.deviceState.selectedDevice ?: run {
            Log.i(TAG, "no device, would send: $payload")
            return
        }
        p2pManager.sendMessage(
            device,
            payload,
            { Log.i(TAG, "app_destroy sent to the watch") },
            { error -> Log.w(TAG, "app_destroy could not be sent: ${error.message}") },
        )
    }

    fun sendBundledTrack(context: Context, track: BundledTrack) {
        val device = _uiState.value.deviceState.selectedDevice
        if (device == null) {
            addLogMessage("Cannot send ${track.fileName}: device not selected")
            return
        }

        updateFile { it.copy(isSending = true) }

        viewModelScope.launch {
            val file = runCatching {
                withContext(Dispatchers.IO) {
                    val target = File(context.cacheDir, track.fileName)
                    context.resources.openRawResource(track.rawResId).use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                    target
                }
            }.getOrElse { error ->
                updateFile { it.copy(isSending = false) }
                addLogMessage("Could not unpack ${track.fileName}: ${error.message}")
                return@launch
            }

            addLogMessage("Sending ${file.name} (${file.length() / 1024} KB)")
            p2pManager.sendFile(device, file, {
                updateFile { it.copy(isSending = false) }
                addLogMessage("Sent ${file.name}")
            }, { error ->
                updateFile { it.copy(isSending = false) }
                addLogMessage("${file.name} failed: ${error.message}")
            })
        }
    }
    override fun onCleared() {
        pollJob?.cancel()
        sendJob?.cancel()
        nowPlayingController.stop()
        super.onCleared()
    }
    private companion object {
        const val TAG = "MainViewModel"
        const val SEND_DEBOUNCE_MS = 400L
        const val SEEK_TOLERANCE_MS = 3_000L
        const val POLL_INTERVAL_MS = 1_000L
    }
}
