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
 *   If the model's output probability >= 0.6 (threshold from model_metadata.json),
 *   the app calls POST /sos/trigger (`trigger_type: auto_fall` or `auto_shake`).
 *   This starts the 10-second countdown on the backend and surfaces the UI.
 *
 * Stage 4 — Training data sync (post-alert):
 *   After the alert resolves (sent OR cancelled) the raw 300-point window is pushed to
 *   POST /protection/collect with label=1 (danger, if sent) or label=0 (safe, if cancelled).
 *   The backend extracts the 39 CSV features and stores the window for model retraining.
 *
 * Sensitivity → magnitude pre-filter threshold mapping (m/s²):
 *   high   → 14.0  (avg danger motion: ~0.43 g)
 *   medium → 18.0  (avg danger fall:   ~0.82 g)
 *   low    → 25.0  (reserve for very high impacts)
 */
class AutoSosManager(
    private val context: Context,
    private val sensorManager: SensorManager,
    private val protectionRepository: ProtectionRepository,
    private val sosRepository: SosRepository,
    private val scope: CoroutineScope
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val sosDetector = SOSDetector(context.assets)

    companion object {
        private const val TAG = "AutoSosManager"
        /**
         * Accumulate 300 readings per inference window (~6s @ 50Hz).
         * The LightGBM ONNX model is trained on exactly 300-point windows.
         */
        private const val WINDOW_SIZE = 300
        /**
         * Minimum high-magnitude hits inside any 300-reading window before inference runs.
         * Set to 1: even a single vigorous shake reading should open the gate.
         * The ONNX model (threshold=0.60) is the primary safety filter.
         */
        private const val MAGNITUDE_HIT_THRESHOLD = 1
        /**
         * Run inference every N new readings using a SLIDING WINDOW over the latest 300.
         * This prevents waiting 6 full seconds between attempts — we check every ~1s.
         */
        private const val INFERENCE_STRIDE = 50
        /** 10-minute cooldown after any ML-triggered SOS to prevent duplicate alerts. */
        private const val COOLDOWN_MS = 600_000L
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

    /** Number of readings received since the last inference attempt (for sliding window). */
    private var readingsSinceLastInference = 0

    private var isCooldownActive   = false
    private var isWindowBeingSent  = false
    private var cooldownJob: Job?  = null

    @Volatile private var isArmed = false

    // ── Sensor listener ──────────────────────────────────────────────────────

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Convert m/s² to g (Gravity units) because the ONNX model was trained on g's
            val xG = x / SensorManager.STANDARD_GRAVITY
            val yG = y / SensorManager.STANDARD_GRAVITY
            val zG = z / SensorManager.STANDARD_GRAVITY

            // Maintain both buffer variants in lock-step
            val triplet  = listOf(xG, yG, zG)
            val reading  = SensorReading(xG, yG, zG, System.currentTimeMillis())

            if (rollingBuffer.size >= WINDOW_SIZE) {
                rollingBuffer.removeFirst()
                rawReadingBuffer.removeFirst()
            }
            rollingBuffer.addLast(triplet)
            rawReadingBuffer.addLast(reading)
            readingsSinceLastInference++

            // Magnitude pre-filter (uses original m/s² to match threshold of 12f/15f/20f)
            val magnitude = sqrt(x * x + y * y + z * z)
            if (magnitude > 10f) {
                Log.v(TAG, "Mag=%.2f threshold=%.2f hits=$magnitudeHitCount armed=$isArmed cooldown=$isCooldownActive".format(
                    magnitude, magnitudeThreshold))
            }
            if (magnitude > magnitudeThreshold) {
                magnitudeHitCount++
                Log.d(TAG, "Magnitude HIT #$magnitudeHitCount: %.2f > threshold %.2f".format(
                    magnitude, magnitudeThreshold))
            }

            // SLIDING WINDOW: run inference every INFERENCE_STRIDE new readings,
            // as long as we have a full 300-point window available.
            // This gives us ~1s response time instead of waiting 6s for a full non-overlapping window.
            if (rollingBuffer.size >= WINDOW_SIZE
                && readingsSinceLastInference >= INFERENCE_STRIDE
                && magnitudeHitCount >= MAGNITUDE_HIT_THRESHOLD
                && !isCooldownActive
                && !isWindowBeingSent
                && isArmed
            ) {
                readingsSinceLastInference = 0
                Log.w(TAG, "Sliding window gate passed ($magnitudeHitCount hits) — running ONNX inference")
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
        // Thresholds are well above resting gravity (~9.8 m/s²) but catch deliberate shaking:
        //   high   → 12.0 m/s²  (slight drop/shake ~1.2g)
        //   medium → 15.0 m/s²  (moderate shake ~1.5g)
        //   low    → 20.0 m/s²  (strong impact only ~2g)
        magnitudeThreshold = when (sensitivity.lowercase()) {
            "high" -> 12f
            "low"  -> 20f
            else   -> 15f
        }

        val sensorKind = if (sensorType == "gyroscope")
            Sensor.TYPE_GYROSCOPE else Sensor.TYPE_ACCELEROMETER
        val sensor = sensorManager.getDefaultSensor(sensorKind)
        if (sensor == null) {
            Log.w(TAG, "Sensor not available: $sensorType")
            return
        }
        sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_GAME)
        Log.d(TAG, "Started ($sensorType, sensitivity=$sensitivity, threshold=$magnitudeThreshold m/s², windowSize=$WINDOW_SIZE, stride=$INFERENCE_STRIDE, hitThreshold=$MAGNITUDE_HIT_THRESHOLD, dangerThreshold=${SOSDetector.DANGER_THRESHOLD})")
    }

    /** Stop monitoring sensors and clear all state. */
    fun stop() {
        isArmed = false
        sensorManager.unregisterListener(sensorListener)
        rollingBuffer.clear()
        rawReadingBuffer.clear()
        magnitudeHitCount  = 0
        readingsSinceLastInference = 0
        isCooldownActive   = false
        isWindowBeingSent  = false
        cooldownJob?.cancel()
        sosDetector.close()
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

        scope.launch {
            if (!isArmed) {
                Log.w(TAG, "runLocalInference: system DISARMED — aborting")
                isWindowBeingSent = false
                return@launch
            }

            try {
                // Stage 2 — Local TFLite inference (17 features)
                val features    = FeatureExtractor.extract(snapshot, activeSensorType)
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
                        _cooldownStarted.emit(Unit)
                        Log.w(TAG, "SOS triggered — alertId=$alertId type=$triggerType")

                        // Fire bracelet haptic immediately when ML confirms danger —
                        // BEFORE the UI screen appears so the user feels the warning instantly.
                        IotEventBus.sendCommand(IotCommand.TriggerFeedback)

                        _dangerDetected.emit(DangerEvent(alertId, triggerType))

                        // Stage 4 — Sync DANGER window to backend for retraining.
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
                // Do NOT clear the buffer after sliding-window inference —
                // keep accumulating so the next stride overlaps with recent readings.
                // Only reset the magnitude hit counter so the gate re-arms.
                magnitudeHitCount = 0
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
