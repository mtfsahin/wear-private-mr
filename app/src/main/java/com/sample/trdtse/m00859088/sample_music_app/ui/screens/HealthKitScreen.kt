package com.sample.trdtse.m00859088.sample_music_app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sample.trdtse.m00859088.sample_music_app.data.models.WorkoutRecord
import com.sample.trdtse.m00859088.sample_music_app.ui.findActivity
import com.sample.trdtse.m00859088.sample_music_app.ui.screens.components.HealthKitCard
import com.sample.trdtse.m00859088.sample_music_app.ui.screens.components.LogCard
import com.sample.trdtse.m00859088.sample_music_app.ui.screens.components.WorkoutDetailDialog
import com.sample.trdtse.m00859088.sample_music_app.viewmodels.MainViewModel

@Composable
fun HealthKitScreen(
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var toastMessage by remember { mutableStateOf("") }
    var detailWorkout by remember { mutableStateOf<WorkoutRecord?>(null) }

    LaunchedEffect(toastMessage) {
        if (toastMessage.isNotBlank()) {
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
            toastMessage = ""
        }
    }

    LazyColumn(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) {
        item {
            HealthKitCard(
                healthKitState = uiState.healthKitState,
                onConnectClicked = {
                    val activity = context.findActivity()
                    if (activity == null) {
                        toastMessage = "Cannot start authorization from this screen"
                    } else {
                        viewModel.connectHealthKit(activity)
                    }
                },
                onFetchWorkoutsClicked = viewModel::fetchRecentWorkouts,
                onDisconnectClicked = {
                    viewModel.disconnectHealthKit()
                    toastMessage = "Disconnected Huawei Health"
                },
                onAuthMethodSelected = viewModel::selectAuthMethod,
                onLookbackSelected = viewModel::selectLookbackDays,
                onWorkoutClicked = { detailWorkout = it },
            )
        }

        item {
            LogCard(
                logMessages = uiState.logMessages,
                onClearLogs = viewModel::clearLogs,
            )
        }
    }

    detailWorkout?.let { workout ->
        WorkoutDetailDialog(workout = workout, onDismiss = { detailWorkout = null })
    }
}
