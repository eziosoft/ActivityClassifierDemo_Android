package com.example.activityclassifierdemo.di

import com.example.activityclassifierdemo.domain.usecase.InferenceBackend
import com.example.activityclassifierdemo.domain.usecase.InferenceUseCase
import com.example.activityclassifierdemo.domain.usecase.impl.RunInferenceOnnxUseCaseImpl
import com.example.activityclassifierdemo.domain.usecase.impl.RunInferenceTFLiteUseCaseImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the appropriate inference implementation (ONNX or TFLite).
 * Change [SELECTED_BACKEND] to switch between implementations at build time.
 */
@Module
@InstallIn(SingletonComponent::class)
object InferenceModule {

    /**
     * Select which inference backend to use:
     * - [InferenceBackend.ONNX] for ONNX Runtime (har_model.onnx)
     * - [InferenceBackend.TFLITE] for TensorFlow Lite (har_model.tflite)
     */
    private val SELECTED_BACKEND = InferenceBackend.ONNX

    @Provides
    @Singleton
    fun provideInferenceUseCase(
        onnxImpl: RunInferenceOnnxUseCaseImpl,
        tfliteImpl: RunInferenceTFLiteUseCaseImpl
    ): InferenceUseCase = when (SELECTED_BACKEND) {
        InferenceBackend.ONNX -> onnxImpl
        InferenceBackend.TFLITE -> tfliteImpl
    }
}



