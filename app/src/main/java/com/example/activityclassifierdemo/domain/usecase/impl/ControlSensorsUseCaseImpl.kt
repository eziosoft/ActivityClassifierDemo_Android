package com.example.activityclassifierdemo.domain.usecase.impl

import com.example.activityclassifierdemo.domain.SensorRepository
import com.example.activityclassifierdemo.domain.usecase.ControlSensorsUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for controlling sensor recording.
 */
@Singleton
class ControlSensorsUseCaseImpl @Inject constructor(
    private val sensorRepository: SensorRepository
) : ControlSensorsUseCase {

    override fun startSensors() {
        sensorRepository.startRecording()
    }

    override fun stopSensors() {
        sensorRepository.stopRecording()
    }
}

