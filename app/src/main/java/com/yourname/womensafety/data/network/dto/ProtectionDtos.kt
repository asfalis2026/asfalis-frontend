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
    @SerializedName("location") val location: String? = null
)

data class PredictionResult(
    @SerializedName("prediction") val prediction: Int,
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("sos_sent") val sosSent: Boolean
)

data class SensorTrainingRequest(
    @SerializedName("sensor_type") val sensorType: String,
    @SerializedName("data") val data: List<SensorReading>,
    @SerializedName("label") val label: Int
)
