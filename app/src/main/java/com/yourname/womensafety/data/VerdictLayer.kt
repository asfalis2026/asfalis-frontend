package com.yourname.womensafety.data

import android.util.Log

/**
 * VerdictLayer — Implements the "Intelligent Verdict / Pattern Recognition Layer"
 * from the backend's Updated_ml_flow.md spec.
 *
 * Architecture:
 *   - 10 windows (6 seconds each) = 1 sample = ~1 minute of monitoring
 *   - Each window: ONNX model gives per-window P(DANGER)
 *   - VerdictLayer accumulates window results in a circular buffer
 *   - Verdict = DANGER only when ≥ DANGER_VOTE_THRESHOLD windows score >= threshold
 *   - Eliminates false positives from single-spike sensor noise / short accidental hits
 *
 * Per backend spec:
 *   10 Windows = 1 Sample → LightGBM Prediction → Pattern Recognition → Safe/Danger
 */
class VerdictLayer {

    companion object {
        private const val TAG = "VerdictLayer"

        /** Number of windows that form one "sample" per ML flow spec (10 × 6s = 1 min). */
        const val SAMPLE_WINDOW_COUNT = 10

        /**
         * Minimum number of DANGER-classified windows within a sample to fire the SOS.
         * 6/10 = 60% of windows must classify as DANGER to trigger.
         * Lower = more sensitive, higher = fewer false positives.
         */
        const val DANGER_VOTE_THRESHOLD = 6
    }

    /** Verdict returned after each window is added. */
    enum class VerdictResult {
        /** Not enough windows accumulated yet to make a decision. */
        PENDING,
        /** Enough windows classified as SAFE — continue monitoring. */
        SAFE,
        /** DANGER threshold exceeded — trigger SOS immediately. */
        DANGER
    }

    /** Circular buffer holding probabilities from the last [SAMPLE_WINDOW_COUNT] windows. */
    private val windowProbabilities = ArrayDeque<Float>(SAMPLE_WINDOW_COUNT + 1)

    /** How many windows have been accumulated so far in the current sample. */
    val windowCount: Int get() = windowProbabilities.size

    /**
     * Record a per-window ONNX probability and return the current verdict.
     *
     * @param probability  P(DANGER) from [SOSDetector.predictDanger], in [0.0, 1.0].
     * @param dangerThreshold  Per-window threshold above which a window is counted as DANGER
     *                         (use [SOSDetector.DANGER_THRESHOLD]).
     * @return [VerdictResult.PENDING] while still accumulating windows,
     *         [VerdictResult.DANGER] when ≥ [DANGER_VOTE_THRESHOLD] windows exceeded threshold,
     *         [VerdictResult.SAFE] once a full sample is collected but DANGER votes insufficient.
     */
    fun addWindowResult(probability: Float, dangerThreshold: Float): VerdictResult {
        windowProbabilities.addLast(probability)
        if (windowProbabilities.size > SAMPLE_WINDOW_COUNT) {
            windowProbabilities.removeFirst()
        }

        val dangerVotes = windowProbabilities.count { it >= dangerThreshold }

        Log.d(
            TAG,
            "Window ${windowProbabilities.size}/$SAMPLE_WINDOW_COUNT — " +
                "P(DANGER)=%.4f dangerVotes=$dangerVotes (need $DANGER_VOTE_THRESHOLD to trigger)".format(probability)
        )

        return when {
            windowProbabilities.size < SAMPLE_WINDOW_COUNT -> {
                // Still collecting — not enough data for a reliable verdict
                VerdictResult.PENDING
            }
            dangerVotes >= DANGER_VOTE_THRESHOLD -> {
                Log.w(
                    TAG,
                    "🚨 DANGER VERDICT — $dangerVotes/$SAMPLE_WINDOW_COUNT windows exceeded " +
                        "P(DANGER)≥$dangerThreshold — triggering SOS"
                )
                VerdictResult.DANGER
            }
            else -> {
                Log.d(
                    TAG,
                    "SAFE verdict — $dangerVotes/$SAMPLE_WINDOW_COUNT danger windows " +
                        "(need $DANGER_VOTE_THRESHOLD). Continuing monitoring."
                )
                VerdictResult.SAFE
            }
        }
    }

    /**
     * Reset the accumulation buffer.
     * Call this after an SOS alert resolves (sent or cancelled) and after [stop].
     */
    fun reset() {
        windowProbabilities.clear()
        Log.d(TAG, "Verdict layer reset — accumulation restarted")
    }
}
