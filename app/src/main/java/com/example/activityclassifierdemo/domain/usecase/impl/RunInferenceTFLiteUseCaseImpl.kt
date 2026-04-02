package com.example.activityclassifierdemo.domain.usecase.impl

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
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RunInferenceTFLiteUseCaseImpl"
private const val MODEL_FILE = "har_model.tflite"
private const val BATCH_SIZE = 1
private const val WINDOW_SIZE = 128
private const val NUM_FEATURES = 8
private const val NUM_CLASSES = 3

/**
 * Loads the HAR TFLite model from assets and runs activity classification
 * inference on 128-sample sensor windows.
 *
 * Uses 4 CPU threads by default; tries NNAPI acceleration when available.
 * Drop-in replacement for [RunInferenceOnnxUseCaseImpl] — both implement [InferenceUseCase].
 */
@Singleton
class RunInferenceTFLiteUseCaseImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val loadNormalizationParamsUseCase: LoadNormalizationParamsUseCase
) : InferenceUseCase {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isModelLoaded = MutableStateFlow(false)
    override val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private var interpreter: Interpreter? = null
    private var activityLabels: Map<String, String> = emptyMap()
    private var meanArray: FloatArray = FloatArray(NUM_FEATURES)
    private var stdArray: FloatArray = FloatArray(NUM_FEATURES)

    init {
        scope.launch { loadModel() }
    }

    private suspend fun loadModel() = withContext(Dispatchers.IO) {
        try {
            // Load normalization parameters
            val normData = loadNormalizationParamsUseCase() ?: return@withContext
            meanArray = normData.mean.toFloatArray()
            stdArray = normData.std.toFloatArray()
            activityLabels = normData.activityLabels

            // Map model file directly from assets — no need to copy to filesDir
            val modelBuffer = context.assets.openFd(MODEL_FILE).use { fd ->
                FileInputStream(fd.fileDescriptor).channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fd.startOffset,
                    fd.declaredLength
                )
            }

            val options = Interpreter.Options().apply {
                numThreads = 4
                
                // Try NNAPI acceleration first (best for modern Android devices)
                try {
                    val nnApiDelegate = org.tensorflow.lite.nnapi.NnApiDelegate()
                    addDelegate(nnApiDelegate)
                    Log.d(TAG, "✓ NNAPI hardware acceleration enabled")
                } catch (e: Exception) {
                    Log.w(TAG, "✗ NNAPI unavailable, falling back to CPU", e)
                    // NNAPI not available - will use CPU with numThreads
                }
            }

            interpreter = Interpreter(modelBuffer, options)
            _isModelLoaded.value = true
            Log.d(TAG, "TFLite model loaded. Input shape: ${interpreter?.getInputTensor(0)?.shape()?.contentToString()}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load TFLite model", e)
        }
    }

    /**
     * Normalizes the window and runs the TFLite model.
     * Input:  ByteBuffer [1, 128, 8] float32 (z-score normalized)
     * Output: Array[1][3] float32 raw logits → softmax probabilities
     */
    override suspend fun runInference(window: Array<FloatArray>): InferenceResult? =
        withContext(Dispatchers.Default) {
            val tflite = interpreter ?: return@withContext null

            if (window.size != WINDOW_SIZE || window[0].size != NUM_FEATURES) {
                Log.e(TAG, "Invalid window shape: ${window.size}×${window[0].size}")
                return@withContext null
            }

            try {
                // Build normalized input as direct ByteBuffer (required by TFLite)
                val inputBuffer = ByteBuffer
                    .allocateDirect(BATCH_SIZE * WINDOW_SIZE * NUM_FEATURES * Float.SIZE_BYTES)
                    .apply {
                        order(ByteOrder.nativeOrder())
                        for (t in 0 until WINDOW_SIZE) {
                            for (f in 0 until NUM_FEATURES) {
                                putFloat((window[t][f] - meanArray[f]) / stdArray[f])
                            }
                        }
                        rewind()
                    }

                // Output buffer: [1, NUM_CLASSES] — raw logits
                val outputBuffer = Array(BATCH_SIZE) { FloatArray(NUM_CLASSES) }

                tflite.run(inputBuffer, outputBuffer)

                val probabilities = softmax(outputBuffer[0])
                val predictedClass = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0

                InferenceResult(
                    activityId = predictedClass,
                    activityName = activityLabels[predictedClass.toString()] ?: "UNKNOWN",
                    confidence = probabilities[predictedClass],
                    probabilities = probabilities
                )
            } catch (e: Exception) {
                Log.e(TAG, "TFLite inference failed", e)
                null
            }
        }

    /** Numerically stable softmax — same implementation as [RunInferenceOnnxUseCaseImpl]. */
    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.maxOrNull() ?: 0f
        val exps = logits.map { kotlin.math.exp((it - max).toDouble()).toFloat() }
        val sum = exps.sum()
        return exps.map { it / sum }.toFloatArray()
    }
}

