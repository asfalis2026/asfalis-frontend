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
     * Submit a raw sensor window to the ML model.
     * Only call this when the device-side magnitude threshold has been exceeded.
     *
     * @param window  List of [x, y, z] readings (min 3, recommended 40). Raw values only.
     * @param sensorType "accelerometer" (default) or "gyroscope"
     * @param location Optional human-readable address sent in WhatsApp alert
     */
    suspend fun predict(
        window: List<List<Float>>,
        sensorType: String = "accelerometer",
        location: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): NetworkResult<PredictionResult> {
        return safeApiCall {
            protectionApi.predict(SensorWindowRequest(window, sensorType, location, latitude, longitude))
        }
    }

    /**
     * Submit feedback after an Auto SOS resolves.
     * Must always be called once per auto-triggered alert to re-label training data.
     *
     * @param alertId     The alert_id received from predict()
     * @param isFalseAlarm  true = false alarm (cancel / I'm safe), false = real danger (sent)
     */
    suspend fun submitFeedback(alertId: String, isFalseAlarm: Boolean): NetworkResult<Unit> {
        return safeApiCall {
            protectionApi.submitFeedback(alertId, FeedbackRequest(isFalseAlarm))
        }
    }
}
