package com.sample.trdtse.m00859088.sample_music_app.ui.screens.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sample.trdtse.m00859088.sample_music_app.data.models.MessageState
import com.sample.trdtse.m00859088.sample_music_app.viewmodels.MainViewModel

@Composable
fun MessageFileSenderCard(
    viewModel: MainViewModel = viewModel(),
    messageState: MessageState,
    onMessageChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onSendPing: () -> Unit,
    isDeviceSelected: Boolean,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "Send Message & File",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            TextField(
                value = messageState.text,
                onValueChange = onMessageChanged,
                label = { Text("Type your message") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                shape = RoundedCornerShape(8.dp),
                enabled = isDeviceSelected && !messageState.isSending,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SendButton(
                    label = "Send Ping",
                    onClick = onSendPing,
                    isSending = messageState.isSending,
                    isEnabled = isDeviceSelected,
                    modifier = Modifier.weight(1f),
                )
                SendButton(
                    label = "Send Message",
                    onClick = onSendMessage,
                    isSending = messageState.isSending,
                    isEnabled = messageState.text.isNotEmpty() && isDeviceSelected,
                    modifier = Modifier.weight(1f),
                )
            }

            SendButton(
                label = "Send File",
                icon = false,
                onClick = {
                    if (uiState.deviceState.selectedDevice == null) {
                        Toast.makeText(context, "Please select a device first", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        viewModel.sendFile(context)
                        Toast.makeText(context, "Sending File...", Toast.LENGTH_SHORT).show()
                    }
                },
                isSending = uiState.fileState.isSending,
                isEnabled = isDeviceSelected,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SendButton(
    label: String,
    onClick: () -> Unit,
    isSending: Boolean,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    icon: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = isEnabled && !isSending,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
    ) {
        if (isSending) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sending...")
        } else {
            if (icon) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(label)
        }
    }
}
