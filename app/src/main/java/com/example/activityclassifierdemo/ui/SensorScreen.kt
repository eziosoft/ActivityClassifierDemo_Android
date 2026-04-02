package com.example.activityclassifierdemo.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.activityclassifierdemo.domain.usecase.InferenceResult
import com.example.activityclassifierdemo.ui.components.BottomModeNavigation
import com.example.activityclassifierdemo.ui.components.ExportDialog
import com.example.activityclassifierdemo.ui.components.SensorDisplaySection
import com.example.activityclassifierdemo.ui.components.TrainingRecordingCard
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val onToggle = remember {
                { enabled: Boolean -> onEvent(ScreenEvent.ToggleInferenceMode(enabled)) }
            }
            BottomModeNavigation(isInferenceMode = uiState.isInferenceMode, onModeChanged = onToggle)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

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
