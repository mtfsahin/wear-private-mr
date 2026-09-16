package com.sample.trdtse.m00859088.sample_music_app.healthkit.auth

import android.net.Uri

object AuthorizationRedirect {
    fun parseCode(uri: Uri, expectedState: String?): Result<String> {
        val error = uri.getQueryParameter("error") ?: uri.getQueryParameter("errorCode")
        if (!error.isNullOrBlank()) {
            val description = uri.getQueryParameter("error_description")
                ?: uri.getQueryParameter("errorMessage")
            val known = error.toIntOrNull()?.let(LoginFreeStatus::describe)
            val message = known ?: buildString {
                append(error)
                if (!description.isNullOrBlank()) append(": ").append(description)
            }
            return Result.failure(AuthorizationDeniedException(message))
        }

        val code = uri.getQueryParameter("code")
        if (code.isNullOrBlank()) {
            return Result.failure(
                AuthorizationDeniedException("The deep link carried no authorization code.")
            )
        }

        val returnedState = uri.getQueryParameter("state")
        if (!expectedState.isNullOrBlank() && returnedState != expectedState) {
            return Result.failure(
                AuthorizationDeniedException("The state value did not match the request.")
            )
        }

        return Result.success(code)
    }
}

class AuthorizationDeniedException(message: String) : Exception(message)
