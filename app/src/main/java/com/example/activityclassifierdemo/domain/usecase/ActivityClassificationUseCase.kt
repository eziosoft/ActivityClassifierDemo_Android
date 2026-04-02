package com.example.activityclassifierdemo.domain.usecase

import kotlinx.coroutines.flow.StateFlow

/** Orchestrates activity classification from continuous sensor data streams. */
interface ActivityClassificationUseCase {
    val currentActivity: StateFlow<InferenceResult?>
}


