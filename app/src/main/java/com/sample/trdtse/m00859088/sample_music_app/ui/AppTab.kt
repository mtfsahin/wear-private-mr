package com.sample.trdtse.m00859088.sample_music_app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(val label: String, val title: String, val icon: ImageVector) {
    WEAR_ENGINE(
        label = "Wear Engine",
        title = "Wear Engine: messages & files",
        icon = Icons.AutoMirrored.Filled.Send,
    ),
    HEALTH_KIT(
        label = "Health Kit",
        title = "Health Kit Cloud: workouts",
        icon = Icons.Default.FavoriteBorder,
    ),
}
