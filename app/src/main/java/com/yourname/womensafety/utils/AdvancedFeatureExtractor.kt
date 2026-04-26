package com.yourname.womensafety.utils

import com.yourname.womensafety.data.network.dto.SensorReading
import com.yourname.womensafety.data.network.dto.SensorTrainingRequest
import kotlin.math.sqrt

/**
 * AdvancedFeatureExtractor — computes the 17 statistical features exactly
 * as expected by the backend's streaming architecture.
 */
object AdvancedFeatureExtractor {

    fun extractToTrainingRequest(
        window: List<SensorReading>,
        sensorType: String,
        datasetName: String?,
        dangerLabel: Int,
        motionDescription: String?
    ): SensorTrainingRequest {
        
        if (window.isEmpty()) {
            return fallbackEmptyRequest(sensorType, dangerLabel)
        }

        val xs = window.map { it.x }.toFloatArray()
        val ys = window.map { it.y }.toFloatArray()
        val zs = window.map { it.z }.toFloatArray()

        return SensorTrainingRequest(
            sensorType = sensorType,
            label = dangerLabel,
            window = window.map { listOf(it.x, it.y, it.z) },

            xMean = mean(xs), xStd = std(xs), xMax = xs.maxOrNull() ?: 0f, xMin = xs.minOrNull() ?: 0f, xSumSq = sumSq(xs),
            yMean = mean(ys), yStd = std(ys), yMax = ys.maxOrNull() ?: 0f, yMin = ys.minOrNull() ?: 0f, ySumSq = sumSq(ys),
            zMean = mean(zs), zStd = std(zs), zMax = zs.maxOrNull() ?: 0f, zMin = zs.minOrNull() ?: 0f, zSumSq = sumSq(zs),

            isAccelerometer = if (sensorType.equals("accelerometer", ignoreCase = true)) 1f else 0f,
            isGyroscope = if (sensorType.equals("gyroscope", ignoreCase = true)) 1f else 0f
        )
    }

    private fun mean(arr: FloatArray): Float {
        if (arr.isEmpty()) return 0f
        return arr.average().toFloat()
    }

    private fun std(arr: FloatArray): Float {
        if (arr.size < 2) return 0f
        val m = mean(arr)
        val variance = arr.map { (it - m) * (it - m) }.average().toFloat()
        return sqrt(variance)
    }

    private fun sumSq(arr: FloatArray): Float {
        return arr.map { (it * it).toDouble() }.sum().toFloat()
    }

    private fun fallbackEmptyRequest(sensorType: String, dangerLabel: Int): SensorTrainingRequest {
        return SensorTrainingRequest(
            sensorType = sensorType,
            label = dangerLabel,
            window = emptyList(),
            xMean = 0f, xStd = 0f, xMax = 0f, xMin = 0f, xSumSq = 0f,
            yMean = 0f, yStd = 0f, yMax = 0f, yMin = 0f, ySumSq = 0f,
            zMean = 0f, zStd = 0f, zMax = 0f, zMin = 0f, zSumSq = 0f,
            isAccelerometer = if (sensorType.equals("accelerometer", ignoreCase = true)) 1f else 0f,
            isGyroscope = if (sensorType.equals("gyroscope", ignoreCase = true)) 1f else 0f
        )
    }
}
