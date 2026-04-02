package com.example.activityclassifierdemo.domain.usecase

import kotlinx.coroutines.flow.SharedFlow

/**
 * Use case that buffers the last 128 sensor readings in a sliding window.
 * 
 * This buffered data is used for:
 * - Displaying real-time sensor graphs in the UI
 * - Running activity classification inference
 * 
 * Emits a list of FloatArray where each array is [acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z, acc_mag_sq, gyro_mag_sq]
 */
interface SensorWindowBufferUseCase {
    val sensorWindowData: SharedFlow<List<FloatArray>>
}
