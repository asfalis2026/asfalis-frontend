package com.yourname.womensafety.data.network.dto

import com.google.gson.annotations.SerializedName

data class ToggleProtectionRequest(
    @SerializedName("is_active") val isActive: Boolean
)

data class ProtectionStatus(
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("activated_at") val activatedAt: String? = null,
    @SerializedName("monitoring_duration_minutes") val monitoringDurationMinutes: Int? = null,
    @SerializedName("bracelet_connected") val braceletConnected: Boolean = false
)

data class SensorDataRequest(
    @SerializedName("sensor_type") val sensorType: String,
    @SerializedName("data") val data: List<SensorReading>,
    @SerializedName("sensitivity") val sensitivity: String
)

data class SensorReading(
    @SerializedName("x") val x: Float,
    @SerializedName("y") val y: Float,
    @SerializedName("z") val z: Float,
    @SerializedName("timestamp") val timestamp: Long
)

/** Response from POST /protection/sensor-data */
data class SensorAnalysisResult(
    @SerializedName("alert_triggered") val alertTriggered: Boolean,
    @SerializedName("alert_id") val alertId: String? = null,
    @SerializedName("confidence") val confidence: Float? = null,
    @SerializedName("trigger_reason") val triggerReason: String? = null,
    @SerializedName("countdown_seconds") val countdownSeconds: Int? = null
)

/** For POST /protection/predict — ML danger prediction via raw [x,y,z] window. */
data class SensorWindowRequest(
    /** List of [x, y, z] triplets. Send 300 readings for best accuracy. */
    @SerializedName("window") val window: List<List<Float>>,
    /** "accelerometer" or "gyroscope" */
    @SerializedName("sensor_type") val sensorType: String = "accelerometer",
    /** Optional GPS location for context */
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)

/** Response from POST /protection/predict */
data class PredictionResult(
    @SerializedName("prediction") val prediction: Int = 0,
    @SerializedName("confidence") val confidence: Float = 0f,
    @SerializedName("sensor_type") val sensorType: String? = null,
    @SerializedName("sos_sent") val sosSent: Boolean = false,
    /** Present when sos_sent = true. Persist immediately — needed for countdown and feedback. */
    @SerializedName("alert_id") val alertId: String? = null,
    @SerializedName("trigger_reason") val triggerReason: String? = null,
    @SerializedName("countdown_seconds") val countdownSeconds: Int? = null,
    @SerializedName("message") val message: String? = null,
    /** Present when rate-limited. Seconds to wait before Auto SOS can trigger again. */
    @SerializedName("retry_after_seconds") val retryAfterSeconds: Int? = null
)

/** POST /api/protection/feedback/<alert_id> */
data class FeedbackRequest(
    @SerializedName("is_false_alarm") val isFalseAlarm: Boolean
)

/**
 * POST /protection/collect — Labeled training window.
 * Sends the 17 pre-computed statistical features to align with backend streaming architecture.
 */
data class SensorTrainingRequest(
    @SerializedName("sensor_type") val sensorType: String,
    @SerializedName("label") val label: Int,
    @SerializedName("window") val window: List<List<Float>>?,

    @SerializedName("x_mean") val xMean: Float,
    @SerializedName("x_std") val xStd: Float,
    @SerializedName("x_max") val xMax: Float,
    @SerializedName("x_min") val xMin: Float,
    @SerializedName("x_sum_sq") val xSumSq: Float,

    @SerializedName("y_mean") val yMean: Float,
    @SerializedName("y_std") val yStd: Float,
    @SerializedName("y_max") val yMax: Float,
    @SerializedName("y_min") val yMin: Float,
    @SerializedName("y_sum_sq") val ySumSq: Float,

    @SerializedName("z_mean") val zMean: Float,
    @SerializedName("z_std") val zStd: Float,
    @SerializedName("z_max") val zMax: Float,
    @SerializedName("z_min") val zMin: Float,
    @SerializedName("z_sum_sq") val zSumSq: Float,

    @SerializedName("is_accelerometer") val isAccelerometer: Float,
    @SerializedName("is_gyroscope") val isGyroscope: Float
)
