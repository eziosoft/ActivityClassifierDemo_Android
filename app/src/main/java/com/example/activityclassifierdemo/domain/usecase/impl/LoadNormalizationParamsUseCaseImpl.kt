package com.example.activityclassifierdemo.domain.usecase.impl

import android.content.Context
import android.util.Log
import com.example.activityclassifierdemo.domain.usecase.LoadNormalizationParamsUseCase
import com.example.activityclassifierdemo.domain.usecase.NormalizationData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LoadNormalizationParamsUseCaseImpl"
private const val NORM_PARAMS_FILE = "normalization_params.json"

/**
 * Loads normalization parameters and feature metadata from normalization_params.json asset file.
 * Includes:
 * - Feature names (sensor axis labels)
 * - Mean values for z-score normalization
 * - Standard deviation values for z-score normalization
 * - Activity class labels mapped to their string names
 */
@Singleton
class LoadNormalizationParamsUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : LoadNormalizationParamsUseCase {
    override suspend operator fun invoke(): NormalizationData? = withContext(Dispatchers.IO) {
        return@withContext try {
            val json = JSONObject(
                context.assets.open(NORM_PARAMS_FILE).bufferedReader().use { it.readText() }
            )

            val featureNames = json.getJSONArray("feature_names")
                .let { array -> (0 until array.length()).map { array.getString(it) } }

            val mean = json.getJSONArray("mean")
                .let { array -> (0 until array.length()).map { array.getDouble(it).toFloat() } }

            val std = json.getJSONArray("std")
                .let { array -> (0 until array.length()).map { array.getDouble(it).toFloat() } }

            val labelsObj = json.getJSONObject("activity_labels")
            val activityLabels = labelsObj.keys().asSequence().associateWith { labelsObj.getString(it) }

            NormalizationData(featureNames, mean, std, activityLabels)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load normalization parameters", e)
            null
        }
    }
}

