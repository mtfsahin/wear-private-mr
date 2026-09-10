package com.sample.trdtse.m00859088.sample_music_app.media

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent

data class NowPlaying(
    val packageName: String,
    val appLabel: String,
    val title: String,
    val artist: String,
    val album: String,
    val isPlaying: Boolean,
    val positionMillis: Long,
    val durationMillis: Long,
) {
    val hasDuration: Boolean get() = durationMillis > 0
}

class NowPlayingController(context: Context) {

    private val context = context.applicationContext

    private val sessionManager =
        this.context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    private val audioManager =
        this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val listenerComponent =
        ComponentName(this.context, MediaNotificationListener::class.java)

    private val mainHandler = Handler(Looper.getMainLooper())

    private var onChanged: (() -> Unit)? = null

    private var attached: MediaController? = null

    private var preferredPackage: String? = null

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { onChanged?.invoke() }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            onChanged?.invoke()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            onChanged?.invoke()
        }

        override fun onSessionDestroyed() {
            detach()
            onChanged?.invoke()
        }
    }

    fun hasAccess(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ).orEmpty()
        return enabled.split(":").any { it.contains(context.packageName) }
    }

    fun start(onSessionsChanged: () -> Unit) {
        onChanged = onSessionsChanged
        runCatching {
            sessionManager.addOnActiveSessionsChangedListener(
                sessionsListener,
                listenerComponent,
                mainHandler,
            )
        }.onFailure { Log.w(TAG, "Could not watch media sessions", it) }
    }

    fun stop() {
        runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsListener) }
        detach()
        onChanged = null
    }

    private fun activeController(): MediaController? {
        if (!hasAccess()) return null
        val controllers = runCatching {
            sessionManager.getActiveSessions(listenerComponent)
        }.getOrElse {
            Log.w(TAG, "Could not read media sessions", it)
            emptyList()
        }

        val chosen = controllers.firstOrNull { it.playbackState.isActive() }
            ?: controllers.firstOrNull { it.packageName == preferredPackage }
            ?: controllers.firstOrNull()

        if (chosen?.playbackState.isActive()) preferredPackage = chosen?.packageName
        attach(chosen)
        return chosen
    }

    private fun attach(controller: MediaController?) {
        if (controller?.sessionToken == attached?.sessionToken) return
        detach()
        controller?.registerCallback(controllerCallback, mainHandler)
        attached = controller
    }

    private fun detach() {
        attached?.let { runCatching { it.unregisterCallback(controllerCallback) } }
        attached = null
    }

    fun read(): NowPlaying? {
        val controller = activeController() ?: return null
        val metadata = controller.metadata
        val state = controller.playbackState

        return NowPlaying(
            packageName = controller.packageName,
            appLabel = appLabel(controller.packageName),
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
            album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
            isPlaying = state.isActive(),
            positionMillis = state.currentPosition(),
            durationMillis = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
        )
    }

    fun play() {
        val controller = activeController()
        controller?.transportControls?.play()
        if (controller == null || controller.playbackState.isGone()) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
        }
    }

    fun pause() {
        val controller = activeController()
        if (controller == null) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
        } else {
            controller.transportControls.pause()
        }
    }

    fun playPause() {
        if (activeController()?.playbackState.isActive()) pause() else play()
    }

    fun next() {
        val controller = activeController()
        if (controller == null) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
        } else {
            controller.transportControls.skipToNext()
        }
    }

    fun previous() {
        val controller = activeController()
        if (controller == null) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        } else {
            controller.transportControls.skipToPrevious()
        }
    }

    fun seekBy(deltaMillis: Long) {
        val controller = activeController() ?: return
        val state = controller.playbackState ?: return
        val duration = controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val target = (state.currentPosition() + deltaMillis).coerceAtLeast(0L)
        controller.transportControls.seekTo(
            if (duration > 0) target.coerceAtMost(duration) else target
        )
    }

    fun seekTo(positionMillis: Long) {
        activeController()?.transportControls?.seekTo(positionMillis)
    }

    private fun sendMediaKey(keyCode: Int) {
        Log.i(TAG, "No usable media session, sending media key $keyCode")
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun PlaybackState?.isActive(): Boolean = when (this?.state) {
        PlaybackState.STATE_PLAYING,
        PlaybackState.STATE_BUFFERING,
        PlaybackState.STATE_CONNECTING,
        PlaybackState.STATE_FAST_FORWARDING,
        PlaybackState.STATE_REWINDING,
        PlaybackState.STATE_SKIPPING_TO_NEXT,
        PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
        PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> true
        else -> false
    }

    private fun PlaybackState?.isGone(): Boolean = when (this?.state) {
        null,
        PlaybackState.STATE_NONE,
        PlaybackState.STATE_STOPPED,
        PlaybackState.STATE_ERROR -> true
        else -> false
    }

    private fun PlaybackState?.currentPosition(): Long {
        if (this == null) return 0L
        if (state != PlaybackState.STATE_PLAYING || lastPositionUpdateTime <= 0L) return position
        val elapsed = SystemClock.elapsedRealtime() - lastPositionUpdateTime
        return (position + elapsed * playbackSpeed).toLong().coerceAtLeast(0L)
    }

    private fun appLabel(packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    private companion object {
        const val TAG = "NowPlaying"
    }
}
