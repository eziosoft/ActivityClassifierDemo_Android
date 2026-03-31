package com.example.activityclassifierdemo.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.activityclassifierdemo.domain.usecase.InferenceResult
import com.example.activityclassifierdemo.ui.theme.ActivityClassifierDemoTheme
import java.io.File
import kotlinx.coroutines.flow.StateFlow

// ─────────────────────────────────────────────────────────────────────────────
// Root screen — ViewModel injection and lifecycle setup.
// Delegates to SensorScreenContent for the actual UI.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SensorScreen(viewModel: SensorViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Lifecycle: start/stop sensors
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onEvent(ScreenEvent.StartRecording)
                Lifecycle.Event.ON_PAUSE -> viewModel.onEvent(ScreenEvent.StopRecording)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onEvent(ScreenEvent.StopRecording)
        }
    }

    SensorScreenContent(
        uiState = uiState,
        sensorStateFlow = viewModel.sensorState,
        context = context,
        onEvent = viewModel::onEvent
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Stateless screen content — can be previewed independently.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SensorScreenContent(
    uiState: UiState,
    sensorStateFlow: StateFlow<SensorState>,
    context: Context,
    onEvent: (ScreenEvent) -> Unit
) {
    // Export dialog — only shown when CSV is ready
    uiState.exportedCsvData?.let { csvData ->
        val onDismiss = remember { { onEvent(ScreenEvent.DismissExportDialog) } }
        val onShare = remember(csvData) {
            {
                val file = File(context.cacheDir, "training_data_${System.currentTimeMillis()}.csv")
                file.writeText(csvData)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share CSV"))
                onEvent(ScreenEvent.DismissExportDialog)
            }
        }
        ExportDialog(csvData = csvData, onDismiss = onDismiss, onShare = onShare)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Mode toggle
            val onToggle =
                remember {
                    { enabled: Boolean -> onEvent(ScreenEvent.ToggleInferenceMode(enabled)) }
                }
            ModeToggle(isInferenceMode = uiState.isInferenceMode, onToggle = onToggle)

            Spacer(modifier = Modifier.height(16.dp))

            // Training recorder
            if (!uiState.isInferenceMode) {
                val onStartRecording =
                    remember {
                        { labelId: Int -> onEvent(ScreenEvent.StartTrainingRecording(labelId)) }
                    }
                val onStopRecording = remember { { onEvent(ScreenEvent.StopTrainingRecording) } }
                val onExport = remember { { onEvent(ScreenEvent.ExportTrainingData) } }
                val onClear = remember { { onEvent(ScreenEvent.ClearTrainingData) } }
                TrainingRecordingCard(
                    isRecording = uiState.isRecordingTraining,
                    currentLabelId = uiState.currentRecordingLabelId,
                    recordedCount = uiState.recordedSamplesCount,
                    labelCounts = uiState.labelCounts,
                    movementLabels = uiState.movementLabels,
                    onStartRecording = onStartRecording,
                    onStopRecording = onStopRecording,
                    onExport = onExport,
                    onClear = onClear
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Sensor display
            SensorDisplaySection(
                sensorStateFlow = sensorStateFlow,
                isInferenceMode = uiState.isInferenceMode,
                movementLabels = uiState.movementLabels,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sensor display — collects SensorState independently at ~50Hz.
// Recompositions are contained here and never affect the parent SensorScreen.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SensorDisplaySection(
    sensorStateFlow: StateFlow<SensorState>,
    isInferenceMode: Boolean,
    movementLabels: List<Labels>,
    modifier: Modifier = Modifier
) {
    val sensorState by sensorStateFlow.collectAsState()

    Column(modifier = modifier) {
        // Inference card
        if (isInferenceMode) {
            CurrentActivityCard(
                currentActivity = sensorState.currentActivity,
                movementLabels = movementLabels
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Accelerometer chart
        ChartLabel(text = "Linear Acceleration (m/s²)")
        SimpleLineChart(
            data = sensorState.accelerometerData,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Gyroscope chart
        ChartLabel(text = "Gyroscope (rad/s)")
        SimpleLineChart(
            data = sensorState.gyroscopeData,
            xIndex = 0,
            yIndex = 1,
            zIndex = 2,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Acceleration magnitude chart
        ChartLabel(text = "Acceleration Magnitude (m²/s⁴)")
        SingleLineChart(
            data = sensorState.accMagnitudeData,
            lineColor = Color(0xFFFF6F00),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Gyroscope magnitude chart
        ChartLabel(text = "Rotation Magnitude (rad²/s²)")
        SingleLineChart(
            data = sensorState.gyroMagnitudeData,
            lineColor = Color(0xFF7B1FA2),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(8.dp))
        ChartLegend()
    }
}

// Stateless composables — only recompose when their own params change

@Composable
private fun ModeToggle(isInferenceMode: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isInferenceMode) "Inference Mode" else "Training Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isInferenceMode) "Detecting activity in real-time" else "Recording labeled training data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Checkbox(checked = isInferenceMode, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun CurrentActivityCard(currentActivity: InferenceResult?, movementLabels: List<Labels>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (currentActivity != null) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Current Activity",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (currentActivity != null) {
                Text(
                    text = currentActivity.activityName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(currentActivity.confidence * 100).toInt()}% confidence",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )

                if (currentActivity.probabilities.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    movementLabels.forEachIndexed { index, labelData ->
                        if (index < currentActivity.probabilities.size) {
                            val prob = currentActivity.probabilities[index]
                            val isActive = index == currentActivity.activityId
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = labelData.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.width(80.dp)
                                )
                                LinearProgressIndicator(
                                    progress = { prob },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "${(prob * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.width(40.dp),
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            if (index <
                                movementLabels.size - 1
                            ) {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Waiting for data...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrainingRecordingCard(
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
            Text(
                text = "Training Data Recorder",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Total samples: $recordedCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (labelCounts.isNotEmpty()) {
                Text(
                    text = labelCounts.entries.joinToString(", ") { (id, count) ->
                        "${movementLabels.find { it.id == id }?.label ?: "ID:$id"}: $count"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (isRecording && currentLabelId != null) {
                Text(
                    text = "🔴 Recording: ${movementLabels.find {
                        it.id == currentLabelId
                    }?.label ?: "ID:$currentLabelId"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                movementLabels.chunked(2).forEach { rowLabels ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowLabels.forEach { labelData ->
                            val isCurrentlyRecording = isRecording && currentLabelId == labelData.id
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
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
                        if (rowLabels.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            if (isRecording) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onStopRecording,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Stop Recording") }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onExport,
                    modifier = Modifier.weight(1f),
                    enabled =
                    recordedCount > 0
                ) {
                    Text("Export CSV")
                }
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f),
                    enabled =
                    recordedCount > 0
                ) {
                    Text("Clear Data")
                }
            }
        }
    }
}

@Composable
private fun ExportDialog(csvData: String, onDismiss: () -> Unit, onShare: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Training Data") },
        text = {
            Column {
                Text("CSV data is ready to export.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${csvData.lines().size - 1} samples ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onShare) { Text("Share") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ChartLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun ChartLegend() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        LegendItem(color = Color.Red, label = "X")
        LegendItem(color = Color.Green, label = "Y")
        LegendItem(color = Color.Blue, label = "Z")
        LegendItem(color = Color(0xFFFF6F00), label = "Acc mag")
        LegendItem(color = Color(0xFF7B1FA2), label = "Gyro mag")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Preview — demonstrates SensorScreenContent in isolation

@Preview
@Composable
private fun SensorScreenContentPreview() {
    val mockContext = LocalContext.current

    val mockUiState = UiState(
        isInferenceMode = false,
        isRecordingTraining = false,
        currentRecordingLabelId = null,
        recordedSamplesCount = 0,
        labelCounts = emptyMap(),
        exportedCsvData = null,
        movementLabels = listOf(Labels("STANDING", 0), Labels("WALKING", 1), Labels("JUMPING", 2))
    )

    val mockSensorState = SensorState(
        accelerometerData = listOf(
            floatArrayOf(0.5f, 0.3f, 9.8f),
            floatArrayOf(0.6f, 0.2f, 9.7f),
            floatArrayOf(0.4f, 0.4f, 9.9f)
        ),
        gyroscopeData = listOf(
            floatArrayOf(0.1f, 0.05f, 0.02f),
            floatArrayOf(0.15f, 0.08f, 0.03f),
            floatArrayOf(0.12f, 0.06f, 0.025f)
        ),
        accMagnitudeData = listOf(floatArrayOf(9.88f), floatArrayOf(9.87f), floatArrayOf(9.89f)),
        gyroMagnitudeData = listOf(floatArrayOf(0.12f), floatArrayOf(0.18f), floatArrayOf(0.14f)),
        currentActivity = InferenceResult(
            activityId = 0,
            activityName = "Standing",
            confidence = 0.92f,
            probabilities = floatArrayOf(0.92f, 0.05f, 0.03f)
        )
    )

    val mockSensorStateFlow = kotlinx.coroutines.flow.MutableStateFlow(mockSensorState)

    ActivityClassifierDemoTheme {
        SensorScreenContent(
            uiState = mockUiState,
            sensorStateFlow = mockSensorStateFlow,
            context = mockContext,
            onEvent = {}
        )
    }
}
