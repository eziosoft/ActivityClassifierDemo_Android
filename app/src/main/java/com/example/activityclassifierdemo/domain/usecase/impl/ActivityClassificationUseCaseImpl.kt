package com.example.activityclassifierdemo.domain.usecase.impl

import com.example.activityclassifierdemo.domain.usecase.ActivityClassificationUseCase
import com.example.activityclassifierdemo.domain.usecase.InferenceResult
import com.example.activityclassifierdemo.domain.usecase.InferenceUseCase
import com.example.activityclassifierdemo.domain.usecase.SensorWindowBufferUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val WINDOW_SIZE = 128

/**
 * Collects 128-sample windows from [SensorWindowBufferUseCase] and runs
 * activity classification inference on every sensor update (~50Hz).
 * Works with both ONNX and TFLite implementations via [InferenceUseCase] interface.
 */
@Singleton
class ActivityClassificationUseCaseImpl @Inject constructor(
    private val sensorWindowBufferUseCase: SensorWindowBufferUseCase,
    private val inferenceUseCase: InferenceUseCase
) : ActivityClassificationUseCase {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _currentActivity = MutableStateFlow<InferenceResult?>(null)
    override val currentActivity: StateFlow<InferenceResult?> = _currentActivity.asStateFlow()

    init {
        scope.launch {
            sensorWindowBufferUseCase.sensorWindowData.collect { windowData ->
                if (windowData.size >= WINDOW_SIZE) {
                    val result = inferenceUseCase.runInference(
                        windowData.takeLast(WINDOW_SIZE).toTypedArray()
                    )
                    if (result != null) _currentActivity.value = result
                }
            }
        }
    }
}

