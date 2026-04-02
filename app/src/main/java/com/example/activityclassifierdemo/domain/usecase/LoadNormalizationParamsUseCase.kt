package com.example.activityclassifierdemo.domain.usecase

/** Normalization parameters and feature data loaded from normalization_params.json. */
data class NormalizationData(
    val featureNames: List<String>,
    val mean: List<Float>,
    val std: List<Float>,
    val activityLabels: Map<String, String>
)

/**
 * Loads normalization parameters and feature metadata from normalization_params.json asset file.
 * Includes:
 * - Feature names (sensor axis labels)
 * - Mean values for z-score normalization
 * - Standard deviation values for z-score normalization
 * - Activity class labels mapped to their string names
 */
interface LoadNormalizationParamsUseCase {
    suspend operator fun invoke(): NormalizationData?
}




