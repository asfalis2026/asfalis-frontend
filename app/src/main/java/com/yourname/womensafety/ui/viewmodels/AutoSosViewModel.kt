package com.yourname.womensafety.ui.viewmodels

import android.app.Application
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Intent
import com.yourname.womensafety.data.AutoSosManager
import com.yourname.womensafety.service.SafetyForegroundService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * AutoSosViewModel — orchestrates the Auto SOS pipeline.
 *
 * ## Architecture change (Foreground Service)
 * The [com.yourname.womensafety.data.AutoSosManager] previously lived here,
 * which meant the sensor engine was destroyed whenever the Dashboard Composable
 * left the backstack (tab navigation) or the screen was locked.
 *
 * Now [AutoSosManager] lives inside [SafetyForegroundService] which:
 *   - Survives tab navigation (UI lifecycle changes)
 *   - Keeps sensors alive with a WakeLock when the screen is locked
 *
 * This ViewModel's only job is:
 *   1. Send Intents to start/stop [SafetyForegroundService]
 *   2. Collect events from [SafetyForegroundService]'s companion-object StateFlows
 *      and re-expose them to the UI
 */
class AutoSosViewModel(private val app: Application) : AndroidViewModel(app) {

    /** True while sensor monitoring is active (mirrors service running state). */
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    val bufferProgress: StateFlow<Float> = SafetyForegroundService.bufferProgress
    val modelStatus = SafetyForegroundService.modelStatus
    val currentWindowIndex: StateFlow<Int> = SafetyForegroundService.currentWindowIndex

    // Track current params to detect restarts needed by sensitivity changes
    private var currentSensitivity = ""
    private var currentSensorType  = ""

    /**
     * Emits a [AutoSosManager.DangerEvent] whenever the ML model predicts danger.
     * Collect this in the UI to navigate to the SOS countdown screen.
     */
    private val _dangerDetected = MutableSharedFlow<AutoSosManager.DangerEvent>(extraBufferCapacity = 1)
    val dangerDetected: SharedFlow<AutoSosManager.DangerEvent> = _dangerDetected

    /**
     * Emits Unit when the post-trigger cooldown begins.
     * Collect this in the UI to show a Toast informing the user.
     */
    private val _cooldownStarted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val cooldownStarted: SharedFlow<Unit> = _cooldownStarted

    init {
        // Forward global service events into this ViewModel's own SharedFlows for the UI
        viewModelScope.launch {
            SafetyForegroundService.dangerDetected.collect { event ->
                _dangerDetected.emit(event)
            }
        }
        viewModelScope.launch {
            SafetyForegroundService.cooldownStarted.collect {
                _cooldownStarted.emit(Unit)
            }
        }
        // Mirror the service's running state so the Dashboard dot indicator stays correct
        viewModelScope.launch {
            SafetyForegroundService.isRunning.collect { running ->
                _isActive.value = running
            }
        }
    }

    /**
     * Enable or disable sensor monitoring.
     * Starts or stops [SafetyForegroundService] via an explicit Intent.
     *
     * @param active      Whether to start or stop monitoring.
     * @param sensitivity "low" | "medium" | "high" — determines magnitude threshold.
     * @param sensorType  "accelerometer" | "gyroscope"
     */
    fun setActive(active: Boolean, sensitivity: String = "medium", sensorType: String = "accelerometer") {
        val paramsChanged = sensitivity != currentSensitivity || sensorType != currentSensorType
        if (_isActive.value == active && !paramsChanged) return

        currentSensitivity = sensitivity
        currentSensorType  = sensorType

        val intent = Intent(app, SafetyForegroundService::class.java)
        if (active) {
            intent.action = SafetyForegroundService.ACTION_START
            intent.putExtra(SafetyForegroundService.EXTRA_SENSITIVITY, sensitivity)
            intent.putExtra(SafetyForegroundService.EXTRA_SENSOR_TYPE, sensorType)
            // startForegroundService ensures the service can call startForeground() within 5 seconds
            ContextCompat.startForegroundService(app, intent)
        } else {
            intent.action = SafetyForegroundService.ACTION_STOP
            app.startService(intent)
        }
    }

    /**
     * Call this after the SOS alert has been resolved (cancelled or dispatched).
     * The service's internal cooldown handles reset; nothing extra needed here
     * unless the service exposes a notifySosResolved API in the future.
     */
    fun onAlertResolved() {
        // The AutoSosManager inside the service handles cooldown internally.
        // No action needed here currently.
    }

    override fun onCleared() {
        super.onCleared()
        // Do NOT stop the service here — it must survive ViewModel lifecycle changes
        // (e.g. configuration change, tab navigation). The service is stopped only
        // when the user explicitly disarms protection via setActive(false).
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    }
}
