package com.example.activityclassifierdemo.domain.usecase

import kotlinx.coroutines.flow.StateFlow

/** A labeled sensor sample for training data collection. */
data class LabeledSensorData(
    val timestamp: Long,
    val accX: Float,
    val accY: Float,
    val accZ: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
    val accMagSq: Float,
    val gyroMagSq: Float,
    val labelId: Int
)

/** Records labeled sensor data and exports it to CSV. */
interface TrainingDataRecorderUseCase {
    val isRecording: StateFlow<Boolean>
    val recordedCount: StateFlow<Int>

    fun startRecording(labelId: Int)
    fun stopRecording()
    fun exportToCSV(): String
    fun clearData()
    fun getCountByLabel(): Map<Int, Int>
    fun cleanup()
}
