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

data class SensorAnalysisResult(
    @SerializedName("alert_triggered") val alertTriggered: Boolean,
    @SerializedName("alert_id") val alertId: String? = null,
    @SerializedName("confidence") val confidence: Float? = null
)

/** For POST /protection/predict — ML danger prediction. */
data class SensorWindowRequest(
    @SerializedName("window") val window: List<List<Float>>,
    /** "accelerometer" (default) or "gyroscope" — used server-side for one-hot feature encoding. */
    @SerializedName("sensor_type") val sensorType: String = "accelerometer",
    @SerializedName("location") val location: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)

data class PredictionResult(
    @SerializedName("prediction") val prediction: Int = 0,
    @SerializedName("confidence") val confidence: Float = 0f,
    @SerializedName("sensor_type") val sensorType: String? = null,
    @SerializedName("sos_sent") val sosSent: Boolean,
    /** Present when sos_sent = true. Store immediately — needed for countdown and feedback. */
    @SerializedName("alert_id") val alertId: String? = null,
    @SerializedName("message") val message: String? = null,
    /** Present when rate-limited. Seconds to wait before Auto SOS can trigger again. */
    @SerializedName("retry_after_seconds") val retryAfterSeconds: Int? = null
)

/** POST /api/protection/feedback/<alert_id> */
data class FeedbackRequest(
    @SerializedName("is_false_alarm") val isFalseAlarm: Boolean
)

data class SensorTrainingRequest(
    @SerializedName("sensor_type") val sensorType: String,
    @SerializedName("data") val data: List<SensorReading>,
    @SerializedName("label") val label: Int
)
