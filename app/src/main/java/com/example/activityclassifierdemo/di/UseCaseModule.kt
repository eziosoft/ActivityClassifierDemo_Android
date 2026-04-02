package com.example.activityclassifierdemo.di

import com.example.activityclassifierdemo.domain.usecase.ActivityClassificationUseCase
import com.example.activityclassifierdemo.domain.usecase.ControlSensorsUseCase
import com.example.activityclassifierdemo.domain.usecase.LoadNormalizationParamsUseCase
import com.example.activityclassifierdemo.domain.usecase.SensorWindowBufferUseCase
import com.example.activityclassifierdemo.domain.usecase.TrainingDataRecorderUseCase
import com.example.activityclassifierdemo.domain.usecase.impl.ActivityClassificationUseCaseImpl
import com.example.activityclassifierdemo.domain.usecase.impl.ControlSensorsUseCaseImpl
import com.example.activityclassifierdemo.domain.usecase.impl.LoadNormalizationParamsUseCaseImpl
import com.example.activityclassifierdemo.domain.usecase.impl.SensorWindowBufferUseCaseImpl
import com.example.activityclassifierdemo.domain.usecase.impl.TrainingDataRecorderUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt dependency injection module for binding use case interfaces to their implementations.
 * Provides clean abstraction layer for all business logic use cases.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {

    @Binds
    @Singleton
    abstract fun bindActivityClassificationUseCase(
        impl: ActivityClassificationUseCaseImpl
    ): ActivityClassificationUseCase

    @Binds
    @Singleton
    abstract fun bindControlSensorsUseCase(
        impl: ControlSensorsUseCaseImpl
    ): ControlSensorsUseCase

    @Binds
    @Singleton
    abstract fun bindSensorWindowBufferUseCase(
        impl: SensorWindowBufferUseCaseImpl
    ): SensorWindowBufferUseCase

    @Binds
    @Singleton
    abstract fun bindTrainingDataRecorderUseCase(
        impl: TrainingDataRecorderUseCaseImpl
    ): TrainingDataRecorderUseCase

    @Binds
    @Singleton
    abstract fun bindLoadNormalizationParamsUseCase(
        impl: LoadNormalizationParamsUseCaseImpl
    ): LoadNormalizationParamsUseCase
}
