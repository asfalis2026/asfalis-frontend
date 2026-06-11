package com.yourname.womensafety.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.data.IotCommand
import com.yourname.womensafety.data.IotEventBus
import com.yourname.womensafety.data.IotSosTracker
import com.yourname.womensafety.data.repository.NetworkResult
import com.yourname.womensafety.data.repository.ProtectionRepository
import com.yourname.womensafety.data.repository.SettingsRepository
import com.yourname.womensafety.data.repository.SosRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.yourname.womensafety.utils.trNonComposable

data class SosUiState(
    val alertId: String? = null,
    /** True while the initial POST /sos/trigger is in-flight. Countdown waits until false. */
    val isTriggering: Boolean = false,
    val isSending: Boolean = false,
    val isSent: Boolean = false,
    val isCancelled: Boolean = false,
    /** True while the cancelSos / markUserSafe API call is in-flight. */
    val isCancelling: Boolean = false,
    val errorMessage: String? = null,
    /**
     * True when the trigger has been in-flight for more than [TRIGGER_TIMEOUT_MS].
     * In this state the UI should always unlock the "I'M SAFE" cancel button so
     * the user is never trapped on the SOS screen.
     */
    val isConnectionTimeout: Boolean = false,
    val contacts: List<com.yourname.womensafety.data.network.dto.TrustedContact> = emptyList(),
    val countdownSeconds: Int? = null,
    val secondsRemaining: Float? = null,
    /** Per-contact delivery report from the sendNow API response. Keyed by phone number. */
    val deliveryReport: List<com.yourname.womensafety.data.network.dto.SosDeliveryReport> = emptyList()
)

class SosViewModel(
    private val sosRepository: SosRepository,
    private val protectionRepository: ProtectionRepository = AppServiceLocator.protectionRepository,
    private val settingsRepository: SettingsRepository = AppServiceLocator.settingsRepository,
    private val contactsRepository: com.yourname.womensafety.data.repository.ContactsRepository = AppServiceLocator.contactsRepository
) : ViewModel() {

    companion object {
        /**
         * After this many ms of isTriggering=true, unlock the cancel button.
         * Must be > BaseRepository max retry window (3 retries × ~30s = ~90s).
         */
        private const val TRIGGER_TIMEOUT_MS = 95_000L

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SosViewModel(AppServiceLocator.sosRepository) as T
            }
        }
    }

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            val cached = contactsRepository.getCachedContacts()
            if (cached != null) {
                _uiState.value = _uiState.value.copy(contacts = cached.sortedByDescending { it.isPrimary }.take(3))
            } else {
                val result = contactsRepository.getContacts()
                if (result is NetworkResult.Success) {
                    _uiState.value = _uiState.value.copy(contacts = result.data.sortedByDescending { it.isPrimary }.take(3))
                }
            }
        }
    }

    /** Reference to the in-flight triggerSos coroutine — used by abortTrigger(). */
    private var triggerJob: Job? = null

    /** Reference to the 8-second timeout watchdog coroutine. */
    private var timeoutJob: Job? = null

    /** Reference to the polling job for countdown status. */
    private var pollJob: Job? = null

    fun triggerSos(triggerType: String = "manual") {
        Log.d("SosViewModel", "triggerSos called: type=$triggerType")
        // Cancel any previously in-flight trigger before starting a new one
        triggerJob?.cancel()
        timeoutJob?.cancel()

        _uiState.value = _uiState.value.copy(isTriggering = true)

        // Watchdog: if trigger hasn't resolved within the retry window, unlock cancel
        timeoutJob = viewModelScope.launch {
            delay(TRIGGER_TIMEOUT_MS)
            if (_uiState.value.isTriggering) {
                Log.w("SosViewModel", "SOS trigger timed out after ${TRIGGER_TIMEOUT_MS}ms — unlocking cancel")
                _uiState.value = _uiState.value.copy(isConnectionTimeout = true)
            }
        }

        triggerJob = viewModelScope.launch {
            // Fetch location first — survives UI recomposition / navigation
            var lat = 0.0
            var lng = 0.0
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(AppServiceLocator.application)
                val cts = CancellationTokenSource()
                val location = suspendCancellableCoroutine { cont ->
                    fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                        .addOnSuccessListener { loc -> cont.resume(loc) }
                        .addOnFailureListener { cont.resume(null) }
                    cont.invokeOnCancellation { cts.cancel() }
                }
                lat = location?.latitude ?: 0.0
                lng = location?.longitude ?: 0.0
            } catch (e: Exception) {
                Log.w("SosViewModel", "Location fetch failed: ${e.message}")
            }

            Log.d("SosViewModel", "Location fetched: lat=$lat, lng=$lng")

            // Read the custom SOS message from cached settings so the backend
            // receives it directly in the trigger payload — no DB lookup needed.
            val cachedSosMessage = settingsRepository.getCachedSettings()?.sosMessage
                ?.takeIf { it.isNotBlank() }

            when (val result = sosRepository.triggerSos(triggerType, lat, lng, sosMessage = cachedSosMessage)) {
                is NetworkResult.Success -> {
                    Log.d("SosViewModel", "triggerSos success: alertId=${result.data.alertId}")
                    timeoutJob?.cancel()
                    val serverStatus = result.data.status.lowercase()
                    val alreadySent = serverStatus == "sent" || serverStatus == "dispatched"
                    IotSosTracker.onUiAlertCreated(result.data.alertId)
                    if (alreadySent) IotSosTracker.onAlertDispatched(result.data.alertId)
                    _uiState.value = _uiState.value.copy(
                        alertId = result.data.alertId,
                        isTriggering = false,
                        isConnectionTimeout = false,
                        isSent = alreadySent,
                        countdownSeconds = result.data.countdownSeconds,
                        secondsRemaining = result.data.countdownSeconds?.toFloat()
                    )
                    if (!alreadySent && result.data.alertId != null) {
                        startPolling(result.data.alertId)
                    }
                    // Hardware team patch: vibrate & blink the bracelet immediately
                    // on every successful SOS trigger (manual, IoT button, auto-fall, proximity)
                    IotEventBus.sendCommand(IotCommand.TriggerFeedback)
                }
                is NetworkResult.Error -> {
                    Log.e("SosViewModel", "triggerSos error: ${result.message}, code: ${result.code}")
                    timeoutJob?.cancel()

                    val userMessage = when (result.code) {
                        "NO_CONTACTS"        -> "⚠️ No emergency contacts found. Please add a verified contact first.".trNonComposable()
                        "INTERNAL_ERROR"     -> "Server error. Please tap Retry.".trNonComposable()
                        "UNAUTHORIZED"       -> "Session expired. Please log in again.".trNonComposable()
                        "NETWORK_ERROR"      -> "No internet connection. Please check your network and try again.".trNonComposable()
                        "SERVER_UNAVAILABLE" -> "Server unavailable. Please tap Retry.".trNonComposable()
                        else                 -> "SOS failed. Please tap Retry.".trNonComposable()
                    }

                    _uiState.value = _uiState.value.copy(
                        isTriggering = false,
                        isConnectionTimeout = false,
                        errorMessage = userMessage
                    )
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    /**
     * Cancels the in-flight triggerSos request and resets state.
     * Called when the user taps Back or I'M SAFE before the trigger has resolved.
     */
    fun abortTrigger() {
        Log.d("SosViewModel", "abortTrigger called — cancelling in-flight trigger")
        triggerJob?.cancel()
        timeoutJob?.cancel()
        pollJob?.cancel()
        triggerJob = null
        timeoutJob = null
        pollJob = null
        // Mark as cancelled so the screen navigates away cleanly
        IotSosTracker.onAlertResolved()
        _uiState.value = _uiState.value.copy(isCancelled = true)
    }

    fun sendNow() {
        val alertId = _uiState.value.alertId ?: run {
            Log.e("SosViewModel", "sendNow called but alertId is null")
            return
        }
        Log.d("SosViewModel", "sendNow called with alertId=$alertId")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            when (val result = sosRepository.sendSosNow(alertId)) {
                is NetworkResult.Success -> {
                    Log.d("SosViewModel", "sendNow success — SOS dispatched")
                    
                    val context = com.yourname.womensafety.data.AppServiceLocator.application
                    val nm = context.getSystemService(android.app.NotificationManager::class.java)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val channel = android.app.NotificationChannel(
                            "sos_delivery", "SOS Delivery Reports", android.app.NotificationManager.IMPORTANCE_HIGH
                        )
                        nm?.createNotificationChannel(channel)
                    }

                    var notifTitle = "SOS Dispatched"
                    var notifText = "Your SOS alert has been delivered to your contacts."
                    
                    val report = result.data.deliveryReport
                    if (report != null && report.any { !it.delivered }) {
                        val failedCount = report.count { !it.delivered }
                        val totalCount = report.size
                        notifTitle = "SOS Partially Dispatched"
                        notifText = "Sent to ${totalCount - failedCount} contact(s), but $failedCount failed (Rate Limit)."
                        
                        // Show a more detailed toast for individual contact status
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            val msg = report.filter { !it.delivered }.joinToString("\n") { "${it.phone}: ${it.status}" }
                            android.widget.Toast.makeText(context, "Delivery failures:\n$msg", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }

                    val notification = androidx.core.app.NotificationCompat.Builder(context, "sos_delivery")
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setContentTitle(notifTitle)
                        .setContentText(notifText)
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build()
                        
                    nm?.notify(System.currentTimeMillis().toInt(), notification)
                    IotSosTracker.onAlertDispatched(alertId)
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        isSent = true,
                        deliveryReport = report ?: emptyList()
                    )
                }
                is NetworkResult.Error -> {
                    when (result.code) {
                        "ALREADY_CANCELLED" -> {
                            Log.d("SosViewModel", "sendNow: alert already cancelled — treating as cancel")
                            IotSosTracker.onAlertResolved()
                            _uiState.value = _uiState.value.copy(isSending = false, isCancelled = true)
                        }
                        "ALREADY_DISPATCHED", "ALREADY_SENT" -> {
                            Log.d("SosViewModel", "sendNow: alert already dispatched — marking sent")
                            IotSosTracker.onAlertDispatched(alertId)
                            _uiState.value = _uiState.value.copy(isSending = false, isSent = true)
                        }
                        else -> {
                            Log.e("SosViewModel", "sendNow error: [${result.code}] ${result.message}")
                            _uiState.value = _uiState.value.copy(
                                isSending = false,
                                errorMessage = "Failed to dispatch SOS — tap Retry".trNonComposable()
                            )
                        }
                    }
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    /**
     * Use this for Auto SOS alerts where the backend already created the alert
     * via POST /protection/predict. Skips the triggerSos() call.
     */
    fun initWithExistingAlert(alertId: String) {
        Log.d("SosViewModel", "initWithExistingAlert: alertId=$alertId")
        IotSosTracker.onUiAlertCreated(alertId)
        _uiState.value = _uiState.value.copy(alertId = alertId)
        startPolling(alertId)
    }

    private fun startPolling(alertId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(2000L)
                when (val res = sosRepository.getSosCountdown(alertId)) {
                    is NetworkResult.Success -> {
                        val status = res.data.status.lowercase()
                        val alreadySent = status == "sent" || status == "dispatched"
                        val cancelled = status == "cancelled" || status == "failed"
                        
                        _uiState.value = _uiState.value.copy(
                            isSent = alreadySent || _uiState.value.isSent,
                            isCancelled = cancelled || _uiState.value.isCancelled,
                            countdownSeconds = res.data.countdownSeconds ?: _uiState.value.countdownSeconds,
                            secondsRemaining = res.data.secondsRemaining ?: _uiState.value.secondsRemaining
                        )
                        
                        if (alreadySent || cancelled || !res.data.isActive) {
                            if (alreadySent) IotSosTracker.onAlertDispatched(alertId)
                            break
                        }
                    }
                    else -> {
                        // ignore network errors on poll
                    }
                }
            }
        }
    }

    /**
     * Submit true/false alarm feedback after an Auto SOS alert resolves.
     * Must be called once per auto-triggered alert to re-label ML training data.
     */
    fun submitFeedback(alertId: String, isFalseAlarm: Boolean) {
        Log.d("SosViewModel", "submitFeedback: alertId=$alertId, isFalseAlarm=$isFalseAlarm")
        viewModelScope.launch {
            protectionRepository.submitFeedback(alertId, isFalseAlarm)
            // Feedback is best-effort — we don't surface errors to the user
        }
    }

    fun cancelSos() {
        val alertId = _uiState.value.alertId

        // If trigger is still in-flight (no alertId yet), abort it
        if (alertId == null) {
            if (_uiState.value.isTriggering) {
                abortTrigger()
                return
            }
            // No alertId and not triggering — just mark cancelled locally
            IotSosTracker.onAlertResolved()
            _uiState.value = _uiState.value.copy(isCancelled = true)
            return
        }

        // Guard: don't fire a second request if one is already in-flight.
        if (_uiState.value.isCancelling) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCancelling = true, errorMessage = null)
            val wasSent = _uiState.value.isSent

            // If not sent yet, cancel pending alert (false alarm without notifying contacts).
            // If already sent, mark user safe (notify contacts that user is safe).
            val result = if (wasSent) {
                sosRepository.markUserSafe(alertId)
            } else {
                sosRepository.cancelSos(alertId)
            }

            when (result) {
                is NetworkResult.Success -> {
                    IotSosTracker.onAlertResolved()
                    _uiState.value = _uiState.value.copy(isCancelling = false, isCancelled = true)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isCancelling = false,
                        errorMessage = if (wasSent) {
                            "Failed to notify contacts you're safe"
                        } else {
                            "Failed to cancel SOS — tap to retry"
                        }
                    )
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        triggerJob?.cancel()
        timeoutJob?.cancel()
        pollJob?.cancel()
    }
}
