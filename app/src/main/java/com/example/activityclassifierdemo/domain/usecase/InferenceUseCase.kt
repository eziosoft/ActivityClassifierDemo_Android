package com.example.activityclassifierdemo.domain.usecase
import kotlinx.coroutines.flow.StateFlow
/** Common contract for ONNX and TFLite inference implementations. */
interface InferenceUseCase {
    val isModelLoaded: StateFlow<Boolean>
    suspend fun runInference(window: Array<FloatArray>): InferenceResult?
}
