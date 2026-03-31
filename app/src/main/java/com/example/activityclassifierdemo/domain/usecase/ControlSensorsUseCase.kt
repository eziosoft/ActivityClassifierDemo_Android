package com.example.activityclassifierdemo.domain.usecase

import com.example.activityclassifierdemo.domain.SensorRepository
import javax.inject.Inject

/**
 * Use case for controlling sensor recording.
 */
class ControlSensorsUseCase @Inject constructor(
    private val sensorRepository: SensorRepository
) {

    fun startSensors() {
        sensorRepository.startRecording()
    }

    fun stopSensors() {
        sensorRepository.stopRecording()
    }
}
