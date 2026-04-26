package com.yourname.womensafety.data.repository

import com.yourname.womensafety.data.network.api.ProtectionApiService
import com.yourname.womensafety.data.network.dto.*

class ProtectionRepository(
    private val protectionApi: ProtectionApiService
) : BaseRepository() {

    suspend fun getProtectionStatus(): NetworkResult<ProtectionStatus> {
        return safeApiCall { protectionApi.getProtectionStatus() }
    }

    suspend fun toggleProtection(isActive: Boolean): NetworkResult<ProtectionStatus> {
        return safeApiCall { protectionApi.toggleProtection(ToggleProtectionRequest(isActive)) }
    }

    /**
     * Submit a raw sensor window to the backend ML model for prediction.
     * Send 300 readings for best accuracy. GPS coordinates are optional but preferred.
     *
     * @param window     List of [x, y, z] triplets — raw accelerometer/gyroscope values.
     * @param sensorType "accelerometer" (default) or "gyroscope"
     * @param latitude   Optional GPS latitude
     * @param longitude  Optional GPS longitude
     */
    suspend fun predict(
        window: List<List<Float>>,
        sensorType: String = "accelerometer",
        latitude: Double? = null,
        longitude: Double? = null
    ): NetworkResult<PredictionResult> {
        return safeApiCall {
            protectionApi.predict(SensorWindowRequest(window, sensorType, latitude, longitude))
        }
    }

    /**
     * Collect a labeled training window so the backend can retrain the model.
     * Extracts exactly 17 statistical features locally to align with backend storage schema.
     *
     * @param window           Raw [SensorReading] list (300 readings @ 50 Hz = 6 seconds).
     * @param label            "safe" or "danger"
     * @param datasetName      Used to infer "accelerometer" vs "gyroscope"
     * @param motionDescription Optional human description.
     */
    suspend fun collectLabeledWindow(
        window: List<SensorReading>,
        label: String,
        datasetName: String? = null,
        motionDescription: String? = null
    ): NetworkResult<Unit> {
        val dangerLabel = if (label.lowercase() == "danger") 1 else 0
        val sensorTypeStr = datasetName ?: "accelerometer"
        
        // 1. Convert to simple List<List<Float>> for FeatureExtractor
        val windowFloats = window.map { listOf(it.x, it.y, it.z) }
        
        // 2. Extract 17 features
        val f = com.yourname.womensafety.utils.FeatureExtractor.extract(windowFloats, sensorTypeStr)
        
        // 3. Map to DTO
        val trainingRequest = SensorTrainingRequest(
            sensorType = sensorTypeStr,
            label = dangerLabel,
            window = windowFloats,
            xMean = f[0], xStd = f[1], xMax = f[2], xMin = f[3], xSumSq = f[4],
            yMean = f[5], yStd = f[6], yMax = f[7], yMin = f[8], ySumSq = f[9],
            zMean = f[10], zStd = f[11], zMax = f[12], zMin = f[13], zSumSq = f[14],
            isAccelerometer = f[15], isGyroscope = f[16]
        )

        return safeApiCall {
            protectionApi.collectData(trainingRequest)
        }
    }

    /**
     * Submit feedback after an Auto SOS resolves.
     * Must always be called once per auto-triggered alert to re-label training data.
     *
     * @param alertId      The alert_id received from predict()
     * @param isFalseAlarm true = false alarm (cancelled / I'm safe), false = genuine danger
     */
    suspend fun submitFeedback(alertId: String, isFalseAlarm: Boolean): NetworkResult<Unit> {
        return safeApiCall {
            protectionApi.submitFeedback(alertId, FeedbackRequest(isFalseAlarm))
        }
    }

    /**
     * Trigger background ML model retraining.
     * Requires at least one SAFE and one DANGER window to have been collected first.
     */
    suspend fun trainModel(): NetworkResult<Unit> {
        return safeApiCall { protectionApi.trainModel() }
    }
}
