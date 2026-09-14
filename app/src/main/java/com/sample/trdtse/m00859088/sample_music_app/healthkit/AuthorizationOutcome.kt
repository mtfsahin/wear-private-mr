package com.sample.trdtse.m00859088.sample_music_app.healthkit

import android.net.Uri

sealed interface AuthorizationOutcome {
    data class Redirect(val uri: Uri) : AuthorizationOutcome

    data class Failed(val message: String) : AuthorizationOutcome
}
