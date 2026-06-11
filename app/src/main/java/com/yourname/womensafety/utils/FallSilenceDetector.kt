package com.yourname.womensafety.utils

import android.util.Log
import kotlin.math.sqrt

/**
 * Detects the "Fall + Silence" emergency scenario — a rule-based override
 * that works independently of the ML model and VerdictLayer.
 *
 * ## What it detects
 * 1. A hard impact spike: accelerometer magnitude exceeds [IMPACT_THRESHOLD] (≥ 2.5g).
 * 2. Followed by 10 seconds of complete stillness: magnitude stays within the
 *    gravity-only band [SILENCE_MAG_MIN, SILENCE_MAG_MAX] (≈ 0.85–1.15g).
 *
 * ## Why this exists
 * The ML VerdictLayer waits for 10 windows (≈ 60 seconds) before deciding.
 * That is too slow when a person is knocked unconscious — their phone falls,
 * hits the ground, and lies completely still. This detector fires in ~10.5 seconds.
 *
 * ## Thresholds calibrated from hard_free_fall.csv
 * - Resting magnitude   ≈ 1.0g  (phone held still in hand)
 * - Impact spike        > 2.5g  (phone hitting ground)
 * - Post-impact silence ≈ 1.0g  (phone lying flat on floor, gravity only)
 *
 * ## Thread safety
 * [onAccelerometerReading] is called from the sensor callback thread.
 * All state is confined to that thread — no locking needed.
 */
class FallSilenceDetector {

    companion object {
        private const val TAG = "FallSilenceDetector"

        // ── Impact detection ─────────────────────────────────────────────────
        /** Minimum magnitude (g) to qualify as a hard impact. Calibrated from hard_free_fall.csv. */
        const val IMPACT_THRESHOLD = 2.5f

        // ── Silence detection ────────────────────────────────────────────────
        /**
         * "Silent" band — phone is still with only gravity acting on it (≈ 1g).
         * Slightly relaxed so a tilted-but-still phone still qualifies.
         */
        const val SILENCE_MAG_MIN  = 0.85f
        const val SILENCE_MAG_MAX  = 1.15f

        /**
         * Duration of unbroken stillness required after impact before SOS fires.
         * 10 seconds matches the recorded "1 fall + 10 second wait" scenario.
         */
        const val SILENCE_DURATION_MS = 10_000L

        /**
         * Brief settling window immediately after impact during which sensor
         * oscillations from the bounce/roll are ignored before silence is measured.
         */
        private const val POST_IMPACT_SETTLE_MS = 600L

        /**
         * If magnitude exceeds this threshold while watching for silence the
         * event is NOT a fall-then-unconscious scenario (user is moving) — reset.
         */
        private const val MOVEMENT_RESET_THRESHOLD = 1.5f
    }

    private enum class State {
        /** Watching for an impact spike. */
        MONITORING,
        /** Impact detected — now watching for 10 seconds of silence. */
        SILENCE_WATCH,
        /** SOS has been fired. Waiting for [reset] before re-arming. */
        TRIGGERED
    }

    private var state             = State.MONITORING
    private var silenceStartAt    = 0L   // epoch-ms when clean silence began
    private var settleUntil       = 0L   // ignore readings until this epoch-ms

    /**
     * Feed every accelerometer reading (values already converted to g-units).
     *
     * @param xG  X-axis in g  (event.values[0] / 9.80665)
     * @param yG  Y-axis in g  (event.values[1] / 9.80665)
     * @param zG  Z-axis in g  (event.values[2] / 9.80665)
     * @return **true exactly once** when the fall+silence condition is met — trigger SOS.
     *         Returns false on every other call.
     */
    fun onAccelerometerReading(xG: Float, yG: Float, zG: Float): Boolean {
        val now = System.currentTimeMillis()
        val mag = sqrt((xG * xG + yG * yG + zG * zG).toDouble()).toFloat()

        return when (state) {

            // ── MONITORING: look for a hard impact spike ─────────────────────
            State.MONITORING -> {
                if (mag >= IMPACT_THRESHOLD) {
                    Log.w(TAG, "Impact detected! mag=%.2fg ≥ %.1fg — starting silence watch".format(mag, IMPACT_THRESHOLD))
                    settleUntil    = now + POST_IMPACT_SETTLE_MS
                    silenceStartAt = now + POST_IMPACT_SETTLE_MS
                    state          = State.SILENCE_WATCH
                }
                false
            }

            // ── SILENCE_WATCH: check for 10s of complete stillness ───────────
            State.SILENCE_WATCH -> {
                // Still within the post-impact settling window — skip evaluation
                if (now < settleUntil) return false

                when {
                    // Significant movement → person is OK / picking up phone → reset
                    mag > MOVEMENT_RESET_THRESHOLD -> {
                        Log.d(TAG, "Movement detected (%.2fg) — resetting fall+silence detector".format(mag))
                        state = State.MONITORING
                        false
                    }

                    // Magnitude within stillness band → accumulate silence time
                    mag in SILENCE_MAG_MIN..SILENCE_MAG_MAX -> {
                        val silenceMs = now - silenceStartAt
                        Log.v(TAG, "Silence: %.2fg — %.1fs / %.1fs".format(
                            mag, silenceMs / 1000f, SILENCE_DURATION_MS / 1000f))

                        if (silenceMs >= SILENCE_DURATION_MS) {
                            Log.w(TAG, "Fall + 10s silence confirmed — FIRING Fall+Silence SOS")
                            state = State.TRIGGERED
                            true   // ← SOS trigger signal
                        } else {
                            false
                        }
                    }

                    // Slight disturbance (0.3g range) — don't reset but restart timer
                    // e.g. phone still settling on an uneven surface
                    else -> {
                        Log.v(TAG, "Slight settling (%.2fg) — restarting silence timer".format(mag))
                        silenceStartAt = now + POST_IMPACT_SETTLE_MS
                        settleUntil    = silenceStartAt
                        false
                    }
                }
            }

            // ── TRIGGERED: already fired, wait for reset ─────────────────────
            State.TRIGGERED -> false
        }
    }

    /**
     * Re-arms the detector after an SOS has been dispatched or cancelled.
     * Call this in the same place you call [AutoSosManager]'s cooldown/reset logic.
     */
    fun reset() {
        state          = State.MONITORING
        silenceStartAt = 0L
        settleUntil    = 0L
        Log.d(TAG, "FallSilenceDetector reset — re-armed")
    }
}
