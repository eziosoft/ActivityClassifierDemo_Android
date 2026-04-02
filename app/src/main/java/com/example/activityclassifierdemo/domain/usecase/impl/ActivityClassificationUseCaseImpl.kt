package com.example.activityclassifierdemo.domain.usecase.impl

import com.example.activityclassifierdemo.domain.usecase.ActivityClassificationUseCase
import com.example.activityclassifierdemo.domain.usecase.InferenceResult
import com.example.activityclassifierdemo.domain.usecase.InferenceUseCase
import com.example.activityclassifierdemo.domain.usecase.SensorWindowBufferUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.LinkedList
import javax.inject.Inject
import javax.inject.Singleton

private const val DATA_WINDOW_SIZE = 128
private const val AVERAGE_UPDATE_INTERVAL_MS = 1000L

/**
 * Collects 128-sample windows from [SensorWindowBufferUseCase] and runs
 * activity classification inference on every sensor update (~50Hz).
 * Works with both ONNX and TFLite implementations via [InferenceUseCase] interface.
 * Tracks average inference time and updates it every second.
 */
@Singleton
class ActivityClassificationUseCaseImpl @Inject constructor(
    private val sensorWindowBufferUseCase: SensorWindowBufferUseCase,
    private val inferenceUseCase: InferenceUseCase
) : ActivityClassificationUseCase {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _currentActivity = MutableStateFlow<InferenceResult?>(null)
    override val currentActivity: StateFlow<InferenceResult?> = _currentActivity.asStateFlow()
    
    // Track inference times for averaging (keep last 50 measurements)
    private val inferenceTimes = LinkedList<Long>()
    private val MAX_SAMPLES = 50

    private var lastDisplayedAverageTime = 0L
    
    init {
        // Inference collection coroutine
        scope.launch {
            sensorWindowBufferUseCase.sensorWindowData.collect { windowData ->
                if (windowData.size >= DATA_WINDOW_SIZE) {
                    val startTime = System.currentTimeMillis()
                    val result = inferenceUseCase.runInference(
                        windowData.takeLast(DATA_WINDOW_SIZE).toTypedArray()
                    )
                    val inferenceTimeMs = System.currentTimeMillis() - startTime
                    
                    // Add to history and maintain max size
                    synchronized(inferenceTimes) {
                        inferenceTimes.add(inferenceTimeMs)
                        if (inferenceTimes.size > MAX_SAMPLES) {
                            inferenceTimes.removeFirst()
                        }
                    }
                    
                    // Update activity result with last displayed average time
                    if (result != null) {
                        _currentActivity.value = result.copy(inferenceTimeMs = lastDisplayedAverageTime)
                    }
                }
            }
        }
        
        // Average update coroutine - updates displayed time every second
        scope.launch {
            while (true) {
                delay(AVERAGE_UPDATE_INTERVAL_MS)
                
                val averageTime = synchronized(inferenceTimes) {
                    if (inferenceTimes.isEmpty()) 0L else inferenceTimes.average().toLong()
                }
                
                lastDisplayedAverageTime = averageTime
                
                // Update current activity with new average time
                _currentActivity.value?.let { current ->
                    _currentActivity.value = current.copy(inferenceTimeMs = averageTime)
                }
            }
        }
    }
}

