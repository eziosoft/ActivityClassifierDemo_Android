package com.example.activityclassifierdemo.domain.usecase.impl

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import com.example.activityclassifierdemo.domain.usecase.InferenceResult
import com.example.activityclassifierdemo.domain.usecase.InferenceUseCase
import com.example.activityclassifierdemo.domain.usecase.LoadNormalizationParamsUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.FloatBuffer
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RunInferenceOnnxUseCaseImpl"
private const val MODEL_FILE = "har_model.onnx"
private const val MODEL_DATA_FILE = "har_model.onnx.data"
private const val BATCH_SIZE = 1
private const val WINDOW_SIZE = 128
private const val NUM_FEATURES = 8
private const val INPUT_NAME = "input"


/**
 * Loads the HAR ONNX model and normalization parameters from assets,
 * then runs activity classification inference on 128-sample sensor windows.
 *
 * Uses NNAPI hardware acceleration when available, falls back to CPU.
 */
@Singleton
class RunInferenceOnnxUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val loadNormalizationParamsUseCase: LoadNormalizationParamsUseCase
) : InferenceUseCase {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isModelLoaded = MutableStateFlow(false)
    override val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var activityLabels: Map<String, String> = emptyMap()

    private var meanArray: FloatArray = FloatArray(NUM_FEATURES)
    private var stdArray: FloatArray = FloatArray(NUM_FEATURES)

    init {
        scope.launch { loadModel() }
    }

    private suspend fun loadModel() = withContext(Dispatchers.IO) {
        try {
            // Load normalization and feature data from JSON
            val normData = loadNormalizationParamsUseCase() ?: return@withContext
            meanArray = normData.mean.toFloatArray()
            stdArray = normData.std.toFloatArray()
            activityLabels = normData.activityLabels

            // Extract model files to internal storage
            // ONNX Runtime requires both files on the filesystem (not in APK assets)
            listOf(MODEL_FILE, MODEL_DATA_FILE).forEach { name ->
                context.assets.open(name).use { it.copyTo(File(context.filesDir, name).outputStream()) }
            }

            // Create ONNX session with NNAPI acceleration
            ortEnvironment = OrtEnvironment.getEnvironment()
            val sessionOptions = OrtSession.SessionOptions().apply {
                try {
                    addNnapi()
                    Log.d(TAG, "NNAPI acceleration enabled")
                } catch (e: Exception) {
                    Log.w(TAG, "NNAPI unavailable, using CPU", e)
                }
            }
            ortSession = ortEnvironment?.createSession(
                File(context.filesDir, MODEL_FILE).absolutePath,
                sessionOptions
            )

            _isModelLoaded.value = true
            Log.d(TAG, "Model loaded. Inputs: ${ortSession?.inputNames}, Outputs: ${ortSession?.outputNames}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
        }
    }

    /**
     * Normalizes a 128×8 sensor window using z-score normalization (standardization).
     * Formula: (value - mean) / std for each feature.
     */
    private fun normalizeWindow(window: Array<FloatArray>): FloatBuffer {
        val inputBuffer = FloatBuffer.allocate(WINDOW_SIZE * NUM_FEATURES)
        for (t in 0 until WINDOW_SIZE) {
            for (f in 0 until NUM_FEATURES) {
                inputBuffer.put((window[t][f] - meanArray[f]) / stdArray[f])
            }
        }
        inputBuffer.rewind()
        return inputBuffer
    }

    /**
     * Runs inference on a 128×8 sensor window.
     * Returns null if the model is not yet loaded or an error occurs.
     */
    override suspend fun runInference(window: Array<FloatArray>): InferenceResult? = withContext(Dispatchers.Default) {
        // Guard: Model must be loaded before inference
        val session = ortSession ?: return@withContext null

        // Validate input shape: expect 128 time steps × 8 sensor features
        if (window.size != WINDOW_SIZE || window[0].size != NUM_FEATURES) {
            Log.e(TAG, "Invalid window shape: ${window.size}×${window[0].size}")
            return@withContext null
        }

        try {
            // Step 1: Normalize sensor data using z-score (mean=0, std=1)
            // IMPORTANT: This matches the normalization applied during model training.
            val inputBuffer = normalizeWindow(window)

            // Step 2: Create ONNX tensor with batch shape [1, 128, 8]
            // ONNX Runtime expects a 3D tensor: (batch_size, time_steps, features)
            val inputTensor = OnnxTensor.createTensor(
                ortEnvironment,
                inputBuffer,
                longArrayOf(BATCH_SIZE.toLong(), WINDOW_SIZE.toLong(), NUM_FEATURES.toLong())
            )

            // Step 3: Run the model forward pass
            // Returns output tensor(s) containing raw logits for each activity class
            val output = session.run(mapOf(INPUT_NAME to inputTensor))

            // Step 4: Extract logits from the output tensor
            // Logits are raw, unnormalized model outputs (unbounded range, not probabilities yet).
            // Higher logits indicate higher confidence for that activity class.
            // The model outputs shape [1, num_classes]; we extract the first (and only) batch
            @Suppress("UNCHECKED_CAST")
            val logits: FloatArray = (output[0].value as Array<FloatArray>)[0]

            // Step 5: Convert logits to probabilities using softmax
            // Ensures probabilities sum to 1 and are in [0, 1] range
            val probabilities: FloatArray = softmax(logits)

            // Step 6: Find the activity with the highest probability
            val predictedClass: Int = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0

            // Cleanup: Release ONNX resources
            inputTensor.close()
            output.close()

            // Step 7: Build result with predicted class, name, confidence, and full probabilities
            InferenceResult(
                activityId = predictedClass,
                activityName = activityLabels[predictedClass.toString()] ?: "UNKNOWN",
                confidence = probabilities[predictedClass],
                probabilities = probabilities
            )
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            null
        }
    }

    /**
     * Converts raw logits to normalized probabilities using softmax.
     *
     * Formula: softmax(x_i) = exp(x_i - max(x)) / sum(exp(x_j - max(x)))
     *
     * Why softmax?
     * - Converts unbounded logits to probabilities in range [0, 1]
     * - Ensures all probabilities sum to exactly 1.0
     * - Emphasizes the largest logit (winner-takes-most effect)
     * - Numerically stable by subtracting max before exp (prevents overflow)
     *
     * Example: logits [-1.2, 3.5, 0.8] → probabilities [0.02, 0.95, 0.03]
     */
    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.maxOrNull() ?: 0f
        val exps = logits.map { kotlin.math.exp((it - max).toDouble()).toFloat() }
        val sum = exps.sum()
        return exps.map { it / sum }.toFloatArray()
    }
}

