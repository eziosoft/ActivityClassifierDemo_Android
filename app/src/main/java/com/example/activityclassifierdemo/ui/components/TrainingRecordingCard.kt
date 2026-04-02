package com.example.activityclassifierdemo.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.activityclassifierdemo.ui.Labels
import com.example.activityclassifierdemo.ui.theme.ActivityClassifierDemoTheme

@Composable
fun TrainingRecordingCard(
    isRecording: Boolean,
    currentLabelId: Int?,
    recordedCount: Int,
    labelCounts: Map<Int, Int>,
    movementLabels: List<Labels>,
    onStartRecording: (Int) -> Unit,
    onStopRecording: () -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Data Recorder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$recordedCount samples",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isRecording && currentLabelId != null) {
                    Text(
                        text = "🔴 REC",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (labelCounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = labelCounts.entries.joinToString(" • ") { (id, count) ->
                        "${movementLabels.find { it.id == id }?.label ?: "ID:$id"}: $count"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Recording buttons - single row for all activities
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                movementLabels.forEach { labelData ->
                    val isCurrentlyRecording = isRecording && currentLabelId == labelData.id
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .pointerInput(labelData.id, isRecording) {
                                detectTapGestures(
                                    onPress = {
                                        if (!isRecording) {
                                            onStartRecording(labelData.id)
                                            tryAwaitRelease()
                                            onStopRecording()
                                        }
                                    }
                                )
                            },
                        color = if (isCurrentlyRecording) Color.Red else MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp),
                        contentColor = Color.White
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = labelData.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isCurrentlyRecording) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            if (isRecording) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onStopRecording,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Stop Recording") }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onExport,
                    modifier = Modifier.weight(1f),
                    enabled = recordedCount > 0
                ) {
                    Text("Export")
                }
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f),
                    enabled = recordedCount > 0
                ) {
                    Text("Clear")
                }
            }
        }
    }
}

@Preview
@Composable
private fun TrainingRecordingCardIdlePreview() {
    ActivityClassifierDemoTheme {
        TrainingRecordingCard(
            isRecording = false,
            currentLabelId = null,
            recordedCount = 150,
            labelCounts = mapOf(0 to 50, 1 to 100),
            movementLabels = listOf(
                Labels("STANDING", 0),
                Labels("WALKING", 1),
                Labels("JUMPING", 2)
            ),
            onStartRecording = {},
            onStopRecording = {},
            onExport = {},
            onClear = {}
        )
    }
}

@Preview
@Composable
private fun TrainingRecordingCardRecordingPreview() {
    ActivityClassifierDemoTheme {
        TrainingRecordingCard(
            isRecording = true,
            currentLabelId = 1,
            recordedCount = 150,
            labelCounts = mapOf(0 to 50, 1 to 100),
            movementLabels = listOf(
                Labels("STANDING", 0),
                Labels("WALKING", 1),
                Labels("JUMPING", 2)
            ),
            onStartRecording = {},
            onStopRecording = {},
            onExport = {},
            onClear = {}
        )
    }
}


