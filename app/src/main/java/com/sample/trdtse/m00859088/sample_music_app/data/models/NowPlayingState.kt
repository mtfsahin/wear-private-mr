package com.sample.trdtse.m00859088.sample_music_app.data.models

import com.sample.trdtse.m00859088.sample_music_app.media.NowPlaying

data class NowPlayingState(
    val hasAccess: Boolean = false,
    val track: NowPlaying? = null,
    val mirrorToWatch: Boolean = true,
    val mirroredCount: Int = 0,
    val mirrorError: String? = null,
)
