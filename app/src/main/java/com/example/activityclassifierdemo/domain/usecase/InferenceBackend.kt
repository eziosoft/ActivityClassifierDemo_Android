package com.example.activityclassifierdemo.domain.usecase

/** Selects which inference runtime is used by [ActivityClassificationUseCase]. */
enum class InferenceBackend {
    ONNX,
    TFLITE
}

