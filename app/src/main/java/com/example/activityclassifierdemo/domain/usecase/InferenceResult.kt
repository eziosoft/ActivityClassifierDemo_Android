package com.example.activityclassifierdemo.domain.usecase

/** Inference result containing the predicted activity and per-class probabilities. */
data class InferenceResult(
    val activityId: Int,
    val activityName: String,
    val confidence: Float,
    val probabilities: FloatArray,
    val inferenceTimeMs: Long = 0L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as InferenceResult
        return activityId == other.activityId &&
                activityName == other.activityName &&
                confidence == other.confidence &&
                probabilities.contentEquals(other.probabilities) &&
                inferenceTimeMs == other.inferenceTimeMs
    }

    /**
     * Custom hashCode required because [probabilities] is a FloatArray.
     * Data class default uses reference-based hashing for arrays, but we override equals()
     * to use content-based comparison. Must use contentHashCode() to satisfy the
     * equals-hashCode contract: equal objects must have equal hash codes.
     * Without this, the object would behave incorrectly in hash-based collections.
     */
    override fun hashCode(): Int {
        var result = activityId
        result = 31 * result + activityName.hashCode()
        result = 31 * result + confidence.hashCode()
        result = 31 * result + probabilities.contentHashCode()
        return result
    }
}

