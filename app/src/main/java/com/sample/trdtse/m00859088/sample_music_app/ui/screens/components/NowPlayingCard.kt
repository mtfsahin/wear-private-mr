package com.sample.trdtse.m00859088.sample_music_app.ui.screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sample.trdtse.m00859088.sample_music_app.data.models.NowPlayingState
import com.sample.trdtse.m00859088.sample_music_app.media.BundledTrack

@Composable
fun NowPlayingCard(
    state: NowPlayingState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onRefresh: () -> Unit,
    onGrantAccess: () -> Unit,
    onMirrorChanged: (Boolean) -> Unit,
    onSendBundledTrack: (BundledTrack) -> Unit,
    onPickAndSendMp3: () -> Unit,
    isDeviceSelected: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Now playing",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (!state.hasAccess) {
                Text(
                    text = "Reading another app's player needs notification access. Android " +
                        "hands the media sessions only to an app that holds it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onGrantAccess, modifier = Modifier.fillMaxWidth()) {
                    Text("Open notification access settings")
                }
                return@Column
            }

            TrackLines(state)
            Spacer(Modifier.height(10.dp))
            Controls(
                isPlaying = state.track?.isPlaying == true,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeekBy = onSeekBy,
            )

            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            MirrorRow(state, isDeviceSelected, onMirrorChanged)

            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Send an MP3 over Wear Engine",
                style = MaterialTheme.typography.labelMedium,
            )
            BundledTrack.entries.forEach { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = track.label, style = MaterialTheme.typography.bodySmall)
                    TextButton(
                        onClick = { onSendBundledTrack(track) },
                        enabled = isDeviceSelected,
                    ) {
                        Text("Send")
                    }
                }
            }
            OutlinedButton(
                onClick = onPickAndSendMp3,
                enabled = isDeviceSelected,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Pick another file from the phone")
            }
            if (!isDeviceSelected) {
                Text(
                    text = "Select a device first to send a file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TrackLines(state: NowPlayingState) {
    val track = state.track
    if (track == null) {
        Text(
            text = "No player is running. Start something in any music app and it shows " +
                "up here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Text(
        text = track.title.ifBlank { "Unknown track" },
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = listOf(track.artist, track.album)
            .filter { it.isNotBlank() }
            .joinToString(" — ")
            .ifBlank { "No artist information" },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = "${track.appLabel} · ${if (track.isPlaying) "playing" else "paused"}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (track.hasDuration) {
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = {
                (track.positionMillis.toFloat() / track.durationMillis).coerceIn(0f, 1f)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(track.positionMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatTime(track.durationMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Controls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekBy: (Long) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous track",
            )
        }
        SeekButton(label = "−15s") { onSeekBy(-SEEK_STEP_MILLIS) }

        Surface(
            onClick = onPlayPause,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isPlaying) {
                    PauseBars(MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }

        SeekButton(label = "+15s") { onSeekBy(SEEK_STEP_MILLIS) }
        IconButton(onClick = onNext) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next track",
            )
        }
    }
}

@Composable
private fun SeekButton(label: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledTonalIconButtonColors(),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun PauseBars(color: androidx.compose.ui.graphics.Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(2) {
            Surface(
                color = color,
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier
                    .width(5.dp)
                    .height(18.dp),
            ) {}
        }
    }
}

@Composable
private fun MirrorRow(
    state: NowPlayingState,
    isDeviceSelected: Boolean,
    onMirrorChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Send changes to the watch",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = when {
                    !isDeviceSelected ->
                        "No device selected: each change is written to the log instead."
                    state.mirrorError != null -> "Last tick failed: ${state.mirrorError}"
                    state.mirrorToWatch -> "${state.mirroredCount} message(s) sent"
                    else -> "Sends title, artist and time on track change, play/pause and seek."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (state.mirrorError != null && state.mirrorToWatch) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Switch(
            checked = state.mirrorToWatch,
            onCheckedChange = onMirrorChanged,
        )
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private const val SEEK_STEP_MILLIS = 15_000L
