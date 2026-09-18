package com.sample.trdtse.m00859088.sample_music_app.ui.screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sample.trdtse.m00859088.sample_music_app.data.models.WorkoutRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun WorkoutDetailDialog(workout: WorkoutRecord, onDismiss: () -> Unit) {
    val format = remember {
        SimpleDateFormat("d MMM yyyy, HH:mm:ss", Locale.getDefault())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text(workout.activityTypeName) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                DetailRow("Activity type", "${workout.activityType}")
                DetailRow("Name", workout.name.ifBlank { "—" })
                DetailRow("Start", format.format(Date(workout.startTime)))
                DetailRow("End", format.format(Date(workout.endTime)))
                DetailRow("Active time", formatDuration(workout.activeTimeMillis))
                DetailRow("Wall time", formatDuration(workout.endTime - workout.startTime))
                DetailRow("Time zone", workout.timeZone.ifBlank { "—" })
                DetailRow("Source type", workout.sourceType?.toString() ?: "—")
                DetailRow("Record id", workout.id.ifBlank { "—" })

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Summary from the cloud",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (workout.summary.isEmpty()) {
                    Text(
                        text = "This workout carried no summary values.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    workout.summary.forEach { (key, value) ->
                        DetailRow(workout.summaryLabel(key), value.ifBlank { "—" }, key)
                    }
                }
            }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String, subLabel: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (subLabel != null) {
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0L) return "—"
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hours > 0) {
        "%d h %02d min %02d s".format(hours, minutes, seconds)
    } else {
        "%d min %02d s".format(minutes, seconds)
    }
}
