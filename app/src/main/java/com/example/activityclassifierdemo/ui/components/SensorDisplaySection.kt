package com.example.activityclassifierdemo.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.activityclassifierdemo.domain.usecase.InferenceResult
import com.example.activityclassifierdemo.ui.Labels
import com.example.activityclassifierdemo.ui.SensorState
import com.example.activityclassifierdemo.ui.SimpleLineChart
import com.example.activityclassifierdemo.ui.SingleLineChart
import com.example.activityclassifierdemo.ui.theme.ActivityClassifierDemoTheme
import com.example.activityclassifierdemo.ui.theme.GraphAccMag
import com.example.activityclassifierdemo.ui.theme.GraphGyroMag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SensorDisplaySection(
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
            lineColor = GraphAccMag,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Gyroscope magnitude chart
        ChartLabel(text = "Rotation Magnitude (rad²/s²)")
        SingleLineChart(
            data = sensorState.gyroMagnitudeData,
            lineColor = GraphGyroMag,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(8.dp))
        ChartLegend()
    }
}

@Preview
@Composable
private fun SensorDisplaySectionTrainingModePreview() {
    ActivityClassifierDemoTheme {
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
            currentActivity = null
        )
        val mockSensorStateFlow = MutableStateFlow(mockSensorState)

        SensorDisplaySection(
            sensorStateFlow = mockSensorStateFlow,
            isInferenceMode = false,
            movementLabels = listOf(Labels("STANDING", 0), Labels("WALKING", 1), Labels("JUMPING", 2)),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun SensorDisplaySectionInferenceModePreview() {
    ActivityClassifierDemoTheme {
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
        val mockSensorStateFlow = MutableStateFlow(mockSensorState)

        SensorDisplaySection(
            sensorStateFlow = mockSensorStateFlow,
            isInferenceMode = true,
            movementLabels = listOf(Labels("STANDING", 0), Labels("WALKING", 1), Labels("JUMPING", 2)),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

