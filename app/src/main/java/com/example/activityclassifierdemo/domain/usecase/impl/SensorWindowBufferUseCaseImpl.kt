package com.example.activityclassifierdemo.domain.usecase.impl

import com.example.activityclassifierdemo.domain.SensorRepository
import com.example.activityclassifierdemo.domain.usecase.SensorWindowBufferUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val BUFFER_SIZE = 128

/**
 * Use case that buffers the last 128 sensor readings in a sliding window.
 * 
 * This buffered data is used for:
 * - Displaying real-time sensor graphs in the UI
 * - Running activity classification inference
 * 
 * Emits a list of FloatArray where each array is [acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z, acc_mag_sq, gyro_mag_sq]
 */
@Singleton
class SensorWindowBufferUseCaseImpl @Inject constructor(
    private val sensorRepository: SensorRepository
) : SensorWindowBufferUseCase {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val buffer = ArrayDeque<FloatArray>(BUFFER_SIZE)

    private val _sensorWindow = MutableSharedFlow<List<FloatArray>>(
        replay = 1,
        extraBufferCapacity = 1
    )
    override val sensorWindowData: SharedFlow<List<FloatArray>> = _sensorWindow.asSharedFlow()

    init {
        scope.launch {
            sensorRepository.sensorData.collect { sensorData ->
                addToBuffer(sensorData)
                _sensorWindow.tryEmit(buffer.toList())
            }
        }
    }

    private fun addToBuffer(data: FloatArray) {
        if (buffer.size >= BUFFER_SIZE) {
            buffer.removeFirst()
        }
        buffer.addLast(data)
    }
}

