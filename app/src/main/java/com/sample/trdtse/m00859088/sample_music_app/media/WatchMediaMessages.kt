package com.sample.trdtse.m00859088.sample_music_app.media

import org.json.JSONObject

enum class WatchCommand { PLAY, PAUSE, TOGGLE, NEXT, PREVIOUS }

object WatchMediaMessages {

    const val TYPE_NOW_PLAYING = "now_playing"
    const val TYPE_APP_DESTROY = "app_destroy"

    fun nowPlaying(track: NowPlaying?): String =
        JSONObject().apply {
            put("type", TYPE_NOW_PLAYING)
            put("title", track?.title.orEmpty())
            put("artist", track?.artist.orEmpty())
            put("playing", track?.isPlaying == true)
            put("current", (track?.positionMillis ?: 0L) / 1000)
            put("total", (track?.durationMillis ?: 0L) / 1000)
        }.toString()

    fun appDestroy(reason: String, sentAtMillis: Long = System.currentTimeMillis()): String =
        JSONObject().apply {
            put("type", TYPE_APP_DESTROY)
            put("sent_at", sentAtMillis)
            put("reason", reason)
        }.toString()

    fun parseCommand(raw: String): WatchCommand? {
        val text = raw.trim()
        val word = if (text.startsWith("{")) {
            runCatching { JSONObject(text).optString("command") }.getOrNull()
        } else {
            text
        }
        return when (word?.trim()?.lowercase()) {
            "play", "start", "resume" -> WatchCommand.PLAY
            "pause", "stop" -> WatchCommand.PAUSE
            "toggle", "playpause", "play_pause" -> WatchCommand.TOGGLE
            "next" -> WatchCommand.NEXT
            "previous", "prev" -> WatchCommand.PREVIOUS
            else -> null
        }
    }
}
