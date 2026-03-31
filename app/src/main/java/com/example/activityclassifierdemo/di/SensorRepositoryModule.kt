package com.example.activityclassifierdemo.di

import com.example.activityclassifierdemo.data.SensorRepositoryImpl
import com.example.activityclassifierdemo.domain.SensorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SensorRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSensorRepository(impl: SensorRepositoryImpl): SensorRepository
}
