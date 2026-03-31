package com.example.activityclassifierdemo.domain

import kotlinx.coroutines.flow.SharedFlow

/**
 * Repository for reading sensor data at 50Hz.
 * Provides a flow of 8-element float arrays: 
 * [acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z, acc_magnitude_sq, gyro_magnitude_sq]
 * where magnitude_sq = x^2 + y^2 + z^2
 */
interface SensorRepository {
    /**
     * Flow emitting sensor data arrays at 50Hz when recording is active.
     * Array format: [acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z, acc_mag_sq, gyro_mag_sq]
     */
    val sensorData: SharedFlow<FloatArray>

    val isRecording: SharedFlow<Boolean>

    fun startRecording()
    fun stopRecording()
}
