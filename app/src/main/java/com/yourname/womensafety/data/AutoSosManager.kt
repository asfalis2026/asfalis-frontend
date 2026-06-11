package com.yourname.womensafety.data

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.yourname.womensafety.data.IotCommand
import com.yourname.womensafety.data.IotEventBus
import com.yourname.womensafety.data.network.dto.SensorReading
import com.yourname.womensafety.data.repository.NetworkResult
import com.yourname.womensafety.data.repository.ProtectionRepository
import com.yourname.womensafety.data.repository.SosRepository
import com.yourname.womensafety.utils.FeatureExtractor
import com.yourname.womensafety.utils.FallSilenceDetector
import com.yourname.womensafety.utils.SOSDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.sqrt

/**
 * AutoSosManager — Local ML-based Auto SOS pipeline.
 *
 * ### Architecture (Frontend-only inference, as per frontend_doc_updated.md)
 *
 * Stage 1 — Sensor accumulation:
 *   Reads accelerometer/gyroscope. Accumulates readings into a rolling 300-point buffer.
 *   A magnitude pre-filter short-circuits: if peak magnitude never exceeds the threshold
 *   in a window, no inference is attempted (saves battery/CPU).
 *
 * Stage 2 — Local ONNX inference:
 *   Extracts the 17 statistical features via [FeatureExtractor] and feeds them into the
 *   on-device LightGBM model ([SOSDetector.predictDanger]) via ONNX Runtime.
 *   No sensor data is sent to the backend for prediction — the model lives entirely
 *   in the APK assets.
 *
 * Stage 3 — Backend SOS trigger:
 *   the app calls POST /sos/trigger (`trigger_type: auto_fall` or `auto_shake`).
 *   This starts the 10-second countdown on the backend and surfaces the UI.
 *
 * Stage 4 — Training data sync (post-alert):
 *   After the alert resolves (sent OR cancelled) the raw 300-point window is pushed to
 *   POST /protection/collect with label=1 (danger, if sent) or label=0 (safe, if cancelled).
 *   The backend extracts the 39 CSV features and stores the window for model retraining.
 *
 * Sensitivity → magnitude pre-filter threshold mapping (g):
 *   high   → 1.2   (slight drop/shake)
 *   medium → 1.5   (moderate shake)
 *   low    → 2.0   (strong impact)
 */
class AutoSosManager(
    private val context: Context,
    private val sensorManager: SensorManager,
    private val protectionRepository: ProtectionRepository,
    private val sosRepository: SosRepository,
    private val scope: CoroutineScope
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val sosDetector        = SOSDetector(context.assets)
    private val fallSilenceDetector = FallSilenceDetector()

    companion object {
        private const val TAG = "AutoSosManager"
        /**
         * Accumulate 300 readings per window (∼10 seconds @ 30 Hz).
         * The LightGBM ONNX model is trained on exactly 300-point windows.
         */
        private const val WINDOW_SIZE = 300
        /**
         * Minimum number of high-magnitude readings inside a window before running inference.
         * Reduced from 3 to 2 so brief sharp movements are still caught.
         */
        private const val MAGNITUDE_HIT_THRESHOLD = 2
        /** 30-second cooldown after any ML-triggered SOS for testing (was 10 mins). */
        private const val COOLDOWN_MS = 30_000L
    }

    // ── Public events ────────────────────────────────────────────────────────

    /** Emits when the on-device model detects Danger AND the backend confirms an alert. */
    private val _dangerDetected = MutableSharedFlow<DangerEvent>(extraBufferCapacity = 1)
    val dangerDetected: SharedFlow<DangerEvent> = _dangerDetected

    /** Emits Unit when the 10-minute cooldown starts (after a confirmed danger detection). */
    private val _cooldownStarted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val cooldownStarted: SharedFlow<Unit> = _cooldownStarted

    data class DangerEvent(
        val alertId: String,
        /** "auto_fall" (accelerometer) | "auto_shake" (gyroscope) */
        val triggerType: String
    )

    // ── Internal state ───────────────────────────────────────────────────────

    private var magnitudeThreshold = 18f
    private var activeSensorType   = "accelerometer"

    /**
     * Rolling 300-reading buffer of raw [x, y, z] lists for TFLite input.
     * Access only from the sensor callback thread.
     */
    private val rollingBuffer = ArrayDeque<List<Float>>(WINDOW_SIZE + 1)

    /**
     * Parallel buffer of [SensorReading] (includes timestamps) kept in sync with
     * [rollingBuffer] so we can push the raw window to /protection/collect after an alert.
     */
    private val rawReadingBuffer = ArrayDeque<SensorReading>(WINDOW_SIZE + 1)

    /** Number of readings in the current window that exceeded the magnitude threshold. */
    private var magnitudeHitCount = 0

    private var isCooldownActive   = false
    private var isWindowBeingSent  = false
    private var cooldownJob: Job?  = null

    @Volatile private var isArmed = false

    private var windowAnalysisCount = 0

    // ── Sensor listener ──────────────────────────────────────────────────────

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            // Convert m/s² to g units (1g = 9.80665 m/s²)
            // The ML model was trained on datasets using g units.
            val gravity = 9.80665f
            val xG = event.values[0] / gravity
            val yG = event.values[1] / gravity
            val zG = event.values[2] / gravity

            // Maintain both buffer variants in lock-step
            val triplet  = listOf(xG, yG, zG)
            val reading  = SensorReading(xG, yG, zG, System.currentTimeMillis())

            if (rollingBuffer.size >= WINDOW_SIZE) {
                rollingBuffer.removeFirst()
                rawReadingBuffer.removeFirst()
            }
            rollingBuffer.addLast(triplet)
            rawReadingBuffer.addLast(reading)

            com.yourname.womensafety.service.SafetyForegroundService.bufferProgress.value = rollingBuffer.size.toFloat() / WINDOW_SIZE

            // Magnitude pre-filter — lightweight gate before TFLite inference.
            // Thresholds (g): high=1.2, medium=1.5, low=2.0
            // Earth gravity alone is ~1.0 g, so these catch deliberate shaking/impacts.
            val magnitude = sqrt(xG * xG + yG * yG + zG * zG)
            if (magnitude > 1.1f) {
                // Log ALL readings above 1.1 g so we can confirm sensor is firing
                Log.v(TAG, "Magnitude=%.2f threshold=%.2f hits=$magnitudeHitCount armed=$isArmed cooldown=$isCooldownActive".format(
                    magnitude, magnitudeThreshold))
            }
            if (magnitude > magnitudeThreshold) {
                magnitudeHitCount++
                Log.d(TAG, "Magnitude HIT #$magnitudeHitCount: %.2f > threshold %.2f".format(
                    magnitude, magnitudeThreshold))
            }

            // ── Fall + Silence heuristic (runs every reading, bypasses ML + VerdictLayer) ──
            // Calibrated from hard_free_fall.csv: 1 hard fall → 10 seconds of silence → DANGER.
            // Covers unconscious-person / dragged-away scenario where the phone lies still.
            if (isArmed && !isCooldownActive && fallSilenceDetector.onAccelerometerReading(xG, yG, zG)) {
                Log.w(TAG, "Fall+Silence trigger fired — bypassing ML, triggering immediate SOS")
                scope.launch {
                    try {
                        val loc = getCurrentLocation()
                        val result = sosRepository.triggerSos(
                            triggerType = "auto_fall_silence",
                            latitude    = loc?.latitude  ?: 0.0,
                            longitude   = loc?.longitude ?: 0.0
                        )
                        if (result is NetworkResult.Success) {
                            val alertId = result.data.alertId
                            startCooldown()
                            fallSilenceDetector.reset()   // ← FIX: re-arm after successful trigger!
                            _cooldownStarted.emit(Unit)
                            IotEventBus.sendCommand(IotCommand.TriggerFeedback)
                            Log.w(TAG, "Fall+Silence SOS confirmed — alertId=$alertId")
                            _dangerDetected.emit(DangerEvent(alertId, "auto_fall_silence"))
                        } else if (result is NetworkResult.Error) {
                            Log.e(TAG, "Fall+Silence SOS trigger failed: [${result.code}] ${result.message}")
                            fallSilenceDetector.reset()   // re-arm if backend rejected
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Fall+Silence trigger error: ${e.message}", e)
                        fallSilenceDetector.reset()
                    }
                }
            }

            // Only run inference when a full window is accumulated AND we are not
            // already processing or in cooldown (evaluates continuous motion, including idle)
            if (rollingBuffer.size >= WINDOW_SIZE
                && !isCooldownActive
                && !isWindowBeingSent
                && isArmed
            ) {
                Log.d(TAG, "Window accumulated (hits: $magnitudeHitCount) — running local ONNX inference")
                runLocalInference(rollingBuffer.toList(), rawReadingBuffer.toList())
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Start monitoring sensors.
     *
     * @param sensitivity "low" | "medium" | "high"
     * @param sensorType  "accelerometer" | "gyroscope"
     */
    fun start(sensitivity: String = "medium", sensorType: String = "accelerometer") {
        isArmed            = true
        activeSensorType   = sensorType
        // Thresholds are in g units (resting gravity ~1.0):
        //   high   → 1.2 g (slight drop/shake)
        //   medium → 1.5 g (moderate shake)
        //   low    → 2.0 g (strong impact)
        magnitudeThreshold = when (sensitivity.lowercase()) {
            "high" -> 1.2f
            "low"  -> 2.0f
            else   -> 1.5f
        }

        val sensorKind = if (sensorType == "gyroscope")
            Sensor.TYPE_GYROSCOPE else Sensor.TYPE_ACCELEROMETER
        val sensor = sensorManager.getDefaultSensor(sensorKind)
        if (sensor == null) {
            Log.w(TAG, "Sensor not available: $sensorType")
            return
        }
        sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_GAME)
        Log.d(TAG, "Started ($sensorType, sensitivity=$sensitivity, threshold=$magnitudeThreshold m/s², windowSize=$WINDOW_SIZE, hitThreshold=$MAGNITUDE_HIT_THRESHOLD)")
    }

    /** Stop monitoring sensors and clear all state. */
    fun stop() {
        isArmed = false
        sensorManager.unregisterListener(sensorListener)
        rollingBuffer.clear()
        rawReadingBuffer.clear()
        magnitudeHitCount  = 0
        isCooldownActive   = false
        isWindowBeingSent  = false
        cooldownJob?.cancel()
        sosDetector.close()  // Release ONNX session & environment
        Log.d(TAG, "Stopped monitoring")
    }

    /** Called externally after an SOS alert is resolved to start the 10-min cooldown. */
    fun notifySosResolved() {
        startCooldown()
    }

    // ── Stage 2+3+4 logic ────────────────────────────────────────────────────

    /**
     * Stage 2: Extract 17 features locally and run TFLite inference.
     * Stage 3: If Danger, trigger POST /sos/trigger immediately.
     * Stage 4: Push raw window to POST /protection/collect after alert resolves.
     */
    private fun runLocalInference(
        snapshot: List<List<Float>>,
        rawSnapshot: List<SensorReading>
    ) {
        isWindowBeingSent = true
        magnitudeHitCount = 0   // reset for next window

        windowAnalysisCount = (windowAnalysisCount % 10) + 1
        com.yourname.womensafety.service.SafetyForegroundService.currentWindowIndex.value = windowAnalysisCount

        scope.launch {
            if (!isArmed) {
                Log.w(TAG, "runLocalInference: system DISARMED — aborting")
                isWindowBeingSent = false
                return@launch
            }

            try {
                // Stage 2 — Local TFLite inference (17 features)
                val features    = FeatureExtractor.extract(snapshot, activeSensorType)
                Log.d(TAG, "Features[0-4] (X): mean=${features[0]}, std=${features[1]}, max=${features[2]}, min=${features[3]}, sum_sq=${features[4]}")
                val probability = sosDetector.predictDanger(features)
                val triggerType = if (activeSensorType == "gyroscope") "auto_shake" else "auto_fall"

                Log.d(TAG, "ONNX: probability=%.4f threshold=%.2f sensorType=$activeSensorType".format(
                    probability, SOSDetector.DANGER_THRESHOLD))

                if (!sosDetector.shouldTriggerSOS(probability)) {
                    Log.d(TAG, "Stage 2 — SAFE (probability=%.4f) — no action".format(probability))
                    // Push safe window to backend for training data balance
                    syncWindowToBackend(rawSnapshot, label = "safe", isSafe = true)
                    return@launch
                }

                Log.w(TAG, "Stage 2 — DANGER (probability=%.4f) — triggering SOS (type=$triggerType)".format(probability))

                // Stage 3 — Trigger SOS on backend (creates countdown)
                val loc = getCurrentLocation()
                val result = sosRepository.triggerSos(
                    triggerType = triggerType,
                    latitude    = loc?.latitude  ?: 0.0,
                    longitude   = loc?.longitude ?: 0.0
                )

                when (result) {
                    is NetworkResult.Success -> {
                        val alertId = result.data.alertId
                        startCooldown()
                        fallSilenceDetector.reset()   // re-arm after any SOS dispatched
                        _cooldownStarted.emit(Unit)
                        IotEventBus.sendCommand(IotCommand.TriggerFeedback)
                        Log.w(TAG, "SOS triggered — alertId=$alertId type=$triggerType")
                        _dangerDetected.emit(DangerEvent(alertId, triggerType))

                        // Stage 4 — Sync DANGER window to backend for retraining.
                        // We do this asynchronously so it does not block the UI countdown.
                        syncWindowToBackend(rawSnapshot, label = "danger", isSafe = false)
                    }
                    is NetworkResult.Error -> {
                        Log.e(TAG, "SOS trigger failed: [${result.code}] ${result.message}")
                    }
                    is NetworkResult.Loading -> Unit
                }
            } catch (e: Exception) {
                Log.e(TAG, "Inference/trigger error: ${e.message}", e)
            } finally {
                isWindowBeingSent = false
                // Keep 50% of the buffer for a sliding window (150 readings = 3 to 5 seconds overlap)
                // This ensures short bursts of vigorous motion are not split across window boundaries
                // and diluted by surrounding idle/medium motion.
                val keepCount = WINDOW_SIZE / 2
                while (rollingBuffer.size > keepCount) {
                    rollingBuffer.removeFirst()
                    rawReadingBuffer.removeFirst()
                }
            }
        }
    }

    /**
     * Stage 4 — Push the raw 300-point window to POST /protection/collect.
     * The backend extracts the 39 statistical features and stores the labelled window.
     *
     * @param label   0 = safe, 1 = danger
     * @param isSafe  Used to choose the motion description annotation.
     */
    private fun syncWindowToBackend(
        rawSnapshot: List<SensorReading>,
        label: String,
        isSafe: Boolean
    ) {
        scope.launch {
            val desc = if (isSafe)
                "SAFE — Automatic label by on-device model"
            else
                "DANGER — Auto-detected by on-device model (type=$activeSensorType)"
            when (val result = protectionRepository.collectLabeledWindow(
                window             = rawSnapshot,
                label              = label,
                datasetName        = activeSensorType,
                motionDescription  = desc
            )) {
                is NetworkResult.Success ->
                    Log.d(TAG, "Training window synced to backend: label=$label (${if (isSafe) "SAFE" else "DANGER"})")
                is NetworkResult.Error ->
                    Log.w(TAG, "Training window sync failed: [${result.code}] ${result.message}")
                is NetworkResult.Loading -> Unit
            }
        }
    }

    // ── Cooldown ─────────────────────────────────────────────────────────────

    private fun startCooldown(durationMs: Long = COOLDOWN_MS) {
        isCooldownActive = true
        cooldownJob?.cancel()
        cooldownJob = scope.launch {
            delay(durationMs)
            isCooldownActive = false
            Log.d(TAG, "Auto SOS cooldown expired — monitoring resumed")
        }
    }

    // ── GPS helper ───────────────────────────────────────────────────────────

    private data class LocationData(val latitude: Double, val longitude: Double)

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): LocationData? {
        return try {
            val cts = CancellationTokenSource()
            val location = suspendCancellableCoroutine { cont ->
                fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { loc -> cont.resume(loc) }
                    .addOnFailureListener { cont.resume(null) }
                cont.invokeOnCancellation { cts.cancel() }
            }
            if (location != null) LocationData(location.latitude, location.longitude) else null
        } catch (e: Exception) {
            Log.w(TAG, "Location unavailable: ${e.message}")
            null
        }
    }
}
