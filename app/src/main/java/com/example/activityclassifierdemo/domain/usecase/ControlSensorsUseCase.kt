package com.example.activityclassifierdemo.domain.usecase

/** Use case for controlling sensor recording. */
interface ControlSensorsUseCase {
    fun startSensors()
    fun stopSensors()
}
