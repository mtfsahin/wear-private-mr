package com.sample.trdtse.m00859088.sample_music_app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sample.trdtse.m00859088.sample_music_app.managers.WatchType
import com.sample.trdtse.m00859088.sample_music_app.ui.screens.HealthKitScreen
import com.sample.trdtse.m00859088.sample_music_app.ui.screens.WearEngineScreen
import com.sample.trdtse.m00859088.sample_music_app.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WearEngineApp(
    viewModel: MainViewModel,
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.WEAR_ENGINE) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedTab.title,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        if (selectedTab == AppTab.WEAR_ENGINE) {
                            WatchTypeSwitch(
                                selected = uiState.deviceState.watchType,
                                onSelect = viewModel::selectWatchType,
                            )
                        }
                        IconButton(onClick = { viewModel.clearLogs() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Logs",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            BottomTabBar(
                selected = selectedTab,
                onSelect = { selectedTab = it },
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        when (selectedTab) {
            AppTab.WEAR_ENGINE -> WearEngineScreen(viewModel, contentPadding)
            AppTab.HEALTH_KIT -> HealthKitScreen(viewModel, contentPadding)
        }
    }
}

@Composable
private fun BottomTabBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AppTab.entries.forEach { tab ->
                BottomTab(
                    tab = tab,
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomTab(
    tab: AppTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "tabBackground",
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "tabContent",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = content,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = tab.label,
                color = content,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun WatchTypeSwitch(selected: WatchType, onSelect: (WatchType) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp),
    ) {
        WatchType.entries.forEach { type ->
            val isSelected = type == selected
            val background by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                label = "watchTypeBackground",
            )
            Text(
                text = type.label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(background)
                    .clickable { onSelect(type) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
