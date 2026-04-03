package com.example.activityclassifierdemo.presentation.ui.screens

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.activityclassifierdemo.domain.usecase.ActivityClassificationUseCase
import com.example.activityclassifierdemo.domain.usecase.ControlSensorsUseCase
import com.example.activityclassifierdemo.domain.usecase.InferenceResult
import com.example.activityclassifierdemo.domain.usecase.LoadNormalizationParamsUseCase
import com.example.activityclassifierdemo.domain.usecase.SensorWindowBufferUseCase
import com.example.activityclassifierdemo.domain.usecase.TrainingDataRecorderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Labels(val label: String, val id: Int)

/**
 * Fast state — updated at ~50Hz by sensor + inference pipeline.
 * Collected only by [SensorDisplaySection] to avoid recomposing the whole screen.
 */
@Stable
data class SensorState(
    val accelerometerData: List<FloatArray> = emptyList(),
    val gyroscopeData: List<FloatArray> = emptyList(),
    val accMagnitudeData: List<FloatArray> = emptyList(),
    val gyroMagnitudeData: List<FloatArray> = emptyList(),
    val currentActivity: InferenceResult? = null
)

/**
 * Slow state — updated only on user actions (mode toggle, recording, export).
 * Collected by [SensorScreen] so the slow parts never recompose due to sensor updates.
 */
@Stable
data class UiState(
    val isInferenceMode: Boolean = true,
    val isRecordingTraining: Boolean = false,
    val currentRecordingLabelId: Int? = null,
    val recordedSamplesCount: Int = 0,
    val labelCounts: Map<Int, Int> = emptyMap(),
    val exportedCsvData: String? = null,
    val movementLabels: List<Labels> = emptyList()
)

sealed class ScreenEvent {
    object StartRecording : ScreenEvent()
    object StopRecording : ScreenEvent()
    data class ToggleInferenceMode(val enabled: Boolean) : ScreenEvent()
    data class StartTrainingRecording(val labelId: Int) : ScreenEvent()
    object StopTrainingRecording : ScreenEvent()
    object ExportTrainingData : ScreenEvent()
    object ClearTrainingData : ScreenEvent()
    object DismissExportDialog : ScreenEvent()
}

@HiltViewModel
class SensorViewModel @Inject constructor(
    private val controlSensorsUseCase: ControlSensorsUseCase,
    private val sensorWindowBufferUseCase: SensorWindowBufferUseCase,
    private val trainingDataRecorderUseCase: TrainingDataRecorderUseCase,
    private val activityClassificationUseCase: ActivityClassificationUseCase,
    private val loadNormalizationParamsUseCase: LoadNormalizationParamsUseCase
) : ViewModel() {

    private val _sensorState = MutableStateFlow(SensorState())
    val sensorState: StateFlow<SensorState> = _sensorState.asStateFlow()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Load movement labels from normalization params
        viewModelScope.launch {
            val normData = loadNormalizationParamsUseCase()
            if (normData != null) {
                _uiState.value = _uiState.value.copy(
                    movementLabels = normData.activityLabels.entries
                        .sortedBy { it.key }
                        .map { Labels(it.value, it.key.toInt()) }
                )
            }
        }

        // Fast path — sensor data + inference at ~50Hz
        viewModelScope.launch {
            sensorWindowBufferUseCase.sensorWindowData.collect { data ->
                _sensorState.value = _sensorState.value.copy(
                    accelerometerData = data.map { it.sliceArray(0..2) },
                    gyroscopeData = data.map { it.sliceArray(3..5) },
                    accMagnitudeData = data.map { floatArrayOf(it[6]) },
                    gyroMagnitudeData = data.map { floatArrayOf(it[7]) }
                )
            }
        }

        viewModelScope.launch {
            activityClassificationUseCase.currentActivity.collect { activity ->
                _sensorState.value = _sensorState.value.copy(currentActivity = activity)
            }
        }

        // Slow path — recording state changes on user action
        viewModelScope.launch {
            trainingDataRecorderUseCase.isRecording.collect { isRecording ->
                _uiState.value = _uiState.value.copy(isRecordingTraining = isRecording)
            }
        }

        viewModelScope.launch {
            trainingDataRecorderUseCase.recordedCount.collect { count ->
                _uiState.value = _uiState.value.copy(
                    recordedSamplesCount = count,
                    labelCounts = trainingDataRecorderUseCase.getCountByLabel()
                )
            }
        }
    }

    fun onEvent(event: ScreenEvent) {
        when (event) {
            is ScreenEvent.StartRecording -> controlSensorsUseCase.startSensors()
            is ScreenEvent.StopRecording -> controlSensorsUseCase.stopSensors()
            is ScreenEvent.ToggleInferenceMode ->
                _uiState.value =
                    _uiState.value.copy(isInferenceMode = event.enabled)

            is ScreenEvent.StopTrainingRecording -> {
                trainingDataRecorderUseCase.stopRecording()
                _uiState.value = _uiState.value.copy(currentRecordingLabelId = null)
            }

            is ScreenEvent.StartTrainingRecording -> {
                trainingDataRecorderUseCase.startRecording(event.labelId)
                _uiState.value = _uiState.value.copy(currentRecordingLabelId = event.labelId)
            }

            is ScreenEvent.ExportTrainingData -> _uiState.value = _uiState.value.copy(
                exportedCsvData = trainingDataRecorderUseCase.exportToCSV()
            )

            is ScreenEvent.ClearTrainingData -> {
                trainingDataRecorderUseCase.clearData()
                _uiState.value =
                    _uiState.value.copy(recordedSamplesCount = 0, labelCounts = emptyMap())
            }

            is ScreenEvent.DismissExportDialog ->
                _uiState.value =
                    _uiState.value.copy(exportedCsvData = null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        trainingDataRecorderUseCase.cleanup()
    }
}
