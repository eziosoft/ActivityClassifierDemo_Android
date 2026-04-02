package com.example.activityclassifierdemo.domain.usecase.impl

import com.example.activityclassifierdemo.domain.SensorRepository
import com.example.activityclassifierdemo.domain.usecase.LabeledSensorData
import com.example.activityclassifierdemo.domain.usecase.TrainingDataRecorderUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Records labeled sensor data and exports it to CSV. */
@Singleton
class TrainingDataRecorderUseCaseImpl @Inject constructor(
    private val sensorRepository: SensorRepository
) : TrainingDataRecorderUseCase {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var recordingJob: Job? = null

    private val recordedData = mutableListOf<LabeledSensorData>()
    private var currentLabelId: Int? = null

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordedCount = MutableStateFlow(0)
    override val recordedCount: StateFlow<Int> = _recordedCount.asStateFlow()

    override fun startRecording(labelId: Int) {
        if (_isRecording.value) return
        currentLabelId = labelId
        _isRecording.value = true

        recordingJob = scope.launch {
            sensorRepository.sensorData.collect { sensorData ->
                if (_isRecording.value && currentLabelId != null) {
                    val sample = LabeledSensorData(
                        timestamp = System.currentTimeMillis(),
                        accX = sensorData[0],
                        accY = sensorData[1],
                        accZ = sensorData[2],
                        gyroX = sensorData[3],
                        gyroY = sensorData[4],
                        gyroZ = sensorData[5],
                        accMagSq = sensorData[6],
                        gyroMagSq = sensorData[7],
                        labelId = currentLabelId!!
                    )
                    synchronized(recordedData) {
                        recordedData.add(sample)
                        _recordedCount.value = recordedData.size
                    }
                }
            }
        }
    }

    override fun stopRecording() {
        _isRecording.value = false
        currentLabelId = null
        recordingJob?.cancel()
        recordingJob = null
    }

    /**
     * Exports all recorded data as a CSV string.
     * Header: timestamp,acc_x,acc_y,acc_z,gyro_x,gyro_y,gyro_z,acc_mag_sq,gyro_mag_sq,label_id
     */
    override fun exportToCSV(): String {
        val dataCopy = synchronized(recordedData) {
            if (recordedData.isEmpty()) return "No data recorded"
            recordedData.toList()
        }

        return buildString {
            append("timestamp,acc_x,acc_y,acc_z,gyro_x,gyro_y,gyro_z,acc_mag_sq,gyro_mag_sq,label_id\n")
            dataCopy.forEach { s ->
                append("${s.timestamp},${s.accX},${s.accY},${s.accZ},${s.gyroX},${s.gyroY},${s.gyroZ},${s.accMagSq},${s.gyroMagSq},${s.labelId}\n")
            }
        }
    }

    override fun clearData() {
        synchronized(recordedData) {
            recordedData.clear()
            _recordedCount.value = 0
        }
    }

    override fun getCountByLabel(): Map<Int, Int> {
        val dataCopy = synchronized(recordedData) { recordedData.toList() }
        return dataCopy.groupBy { it.labelId }.mapValues { it.value.size }
    }

    override fun cleanup() {
        scope.cancel()
    }
}

