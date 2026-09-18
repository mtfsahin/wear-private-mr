package com.sample.trdtse.m00859088.sample_music_app.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sample.trdtse.m00859088.sample_music_app.data.models.HealthKitState
import com.sample.trdtse.m00859088.sample_music_app.data.models.WorkoutRecord
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitAuthMethod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HealthKitCard(
    healthKitState: HealthKitState,
    onConnectClicked: () -> Unit,
    onFetchWorkoutsClicked: () -> Unit,
    onDisconnectClicked: () -> Unit,
    onAuthMethodSelected: (HealthKitAuthMethod) -> Unit,
    onLookbackSelected: (Long) -> Unit,
    onWorkoutClicked: (WorkoutRecord) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            HealthKitHeader(
                isAuthorized = healthKitState.isAuthorized,
                isBusy = healthKitState.isAuthorizing || healthKitState.isLoadingWorkouts,
                onRefreshClicked = onFetchWorkoutsClicked,
            )

            Spacer(modifier = Modifier.height(6.dp))

            when {
                healthKitState.isAuthorizing -> {
                    StatusRow("Waiting for Huawei Health sign-in…")
                }

                !healthKitState.isAuthorized -> {
                    Text(
                        text = "Connect a Huawei Health account to read the workouts that " +
                            "have synced to Huawei Health Kit Cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    AuthMethodPicker(
                        state = healthKitState,
                        onAuthMethodSelected = onAuthMethodSelected,
                    )

                    healthKitState.errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onConnectClicked, modifier = Modifier.fillMaxWidth()) {
                        Text("Connect Huawei Health")
                    }
                }

                else -> {
                    RangePicker(
                        state = healthKitState,
                        onLookbackSelected = onLookbackSelected,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onFetchWorkoutsClicked,
                            enabled = !healthKitState.isLoadingWorkouts,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (healthKitState.isLoadingWorkouts) "Fetching…" else "Fetch recent workouts")
                        }
                        OutlinedButton(onClick = onDisconnectClicked) {
                            Text("Disconnect")
                        }
                    }

                    healthKitState.errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    WorkoutList(
                        workouts = healthKitState.workouts,
                        isLoading = healthKitState.isLoadingWorkouts,
                        onWorkoutClicked = onWorkoutClicked,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthMethodPicker(
    state: HealthKitState,
    onAuthMethodSelected: (HealthKitAuthMethod) -> Unit,
) {
    Text(
        text = "Authorization method",
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        HealthKitAuthMethod.entries.forEachIndexed { index, method ->
            SegmentedButton(
                selected = state.authMethod == method,
                onClick = { onAuthMethodSelected(method) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = HealthKitAuthMethod.entries.size
                ),
            ) {
                Text(method.label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    if (state.authMethod == HealthKitAuthMethod.LOGIN_FREE_SDK) {
        Text(
            text = "The Huawei Health app asks for the scopes and for cloud sync.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun HealthKitHeader(isAuthorized: Boolean, isBusy: Boolean, onRefreshClicked: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Huawei Health Kit, workouts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(6.dp))
            }
            ConnectionDot(isAuthorized = isAuthorized)
            if (isAuthorized) {
                IconButton(onClick = onRefreshClicked, enabled = !isBusy) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh workouts",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionDot(isAuthorized: Boolean) {
    val color = if (isAuthorized) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun StatusRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun WorkoutList(
    workouts: List<WorkoutRecord>,
    isLoading: Boolean,
    onWorkoutClicked: (WorkoutRecord) -> Unit,
) {
    if (workouts.isEmpty()) {
        Text(
            text = if (isLoading) "" else "No workouts found in this range yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        workouts.take(MAX_VISIBLE_WORKOUTS).forEach { workout ->
            WorkoutRow(workout, onWorkoutClicked)
        }
        if (workouts.size > MAX_VISIBLE_WORKOUTS) {
            Text(
                text = "+ ${workouts.size - MAX_VISIBLE_WORKOUTS} more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WorkoutRow(workout: WorkoutRecord, onClick: (WorkoutRecord) -> Unit) {
    val dateFormat = remember(workout.startTime) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(workout) }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = workout.activityTypeName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = dateFormat.format(Date(workout.startTime)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${workout.durationMinutes} min",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePicker(state: HealthKitState, onLookbackSelected: (Long) -> Unit) {
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    Text(
        text = "Look back",
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(bottom = 4.dp)
    )

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        LOOKBACK_CHOICES.forEachIndexed { index, choice ->
            SegmentedButton(
                selected = state.lookbackDays == choice.days,
                onClick = { onLookbackSelected(choice.days) },
                enabled = !state.isLoadingWorkouts,
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = LOOKBACK_CHOICES.size
                ),
            ) {
                Text(choice.label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    Text(
        text = "${dateFormat.format(Date(state.rangeStartMillis))} – today",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
}

private data class LookbackChoice(val days: Long, val label: String)

private val LOOKBACK_CHOICES = listOf(
    LookbackChoice(7L, "7 d"),
    LookbackChoice(30L, "30 d"),
    LookbackChoice(90L, "90 d"),
    LookbackChoice(365L, "1 y"),
)
private const val MAX_VISIBLE_WORKOUTS = 8
