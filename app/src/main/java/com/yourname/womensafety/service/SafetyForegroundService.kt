package com.yourname.womensafety.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.yourname.womensafety.MainActivity
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.data.AutoSosManager
import com.yourname.womensafety.data.repository.NetworkResult
import com.yourname.womensafety.utils.ModelStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * SafetyForegroundService — hosts the Auto SOS sensor engine as a persistent
 * Android Foreground Service so that monitoring survives:
 *
 *   • Tab navigation (ViewModel / Composable lifecycle changes)
 *   • Screen lock / phone-idle / Doze mode
 *
 * ## Locked-Screen SOS (Android 10+ fix)
 * When the screen is off, the Compose UI is paused and cannot run the countdown.
 * This service maintains its own 10-second service-side countdown independently.
 * If the screen is locked when danger is detected:
 *   1. A live countdown notification is shown (updates every second).
 *   2. At T=0 the service calls POST /api/sos/{alertId}/send directly.
 *   3. When the user unlocks the phone, the UI detects the already-sent state and
 *      shows the "SOS Dispatched" confirmation screen.
 *
 * If the screen is ON, the Compose UI handles the countdown (existing behavior),
 * and the service-side countdown is cancelled when the UI collects the event.
 */
class SafetyForegroundService : Service() {

    // ── Companion — global state observed by AutoSosViewModel from anywhere ──

    companion object {
        private const val TAG = "SafetyFgService"

        const val ACTION_START       = "com.yourname.womensafety.ACTION_START_SAFETY"
        const val ACTION_STOP        = "com.yourname.womensafety.ACTION_STOP_SAFETY"
        /** Sent by the UI when it takes over the countdown so the service cancels its own. */
        const val ACTION_UI_TOOK_OVER = "com.yourname.womensafety.ACTION_UI_TOOK_OVER"
        const val EXTRA_ALERT_ID     = "alert_id"
        const val EXTRA_SENSITIVITY  = "sensitivity"
        const val EXTRA_SENSOR_TYPE  = "sensor_type"

        private const val NOTIFICATION_ID         = 1001
        private const val COUNTDOWN_NOTIFICATION_ID = 1004
        private const val CHANNEL_ID              = "asfalis_safety_channel"
        private const val COUNTDOWN_CHANNEL_ID    = "asfalis_countdown_channel"
        private const val SOS_ALERT_CHANNEL_ID    = "sos_alert_channel"

        /**
         * Emits every time the ML model or the Fall+Silence rule detects danger.
         * Collected in [AutoSosViewModel] to navigate to the SOS countdown screen.
         */
        val dangerDetected: MutableSharedFlow<AutoSosManager.DangerEvent> =
            MutableSharedFlow(extraBufferCapacity = 1)

        /**
         * Emits Unit when the post-trigger cooldown begins.
         * Collected in [AutoSosViewModel] to show a Toast.
         */
        val cooldownStarted: MutableSharedFlow<Unit> =
            MutableSharedFlow(extraBufferCapacity = 1)

        /** True while the service is running (sensors active). */
        val isRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)

        val bufferProgress: MutableStateFlow<Float> = MutableStateFlow(0f)
        val modelStatus: MutableStateFlow<ModelStatus> = MutableStateFlow(ModelStatus.READY)
        val currentWindowIndex: MutableStateFlow<Int> = MutableStateFlow(0)

        /**
         * Tracks which alertIds already have a service-side countdown running.
         * The UI sends ACTION_UI_TOOK_OVER to cancel the service countdown for that alertId.
         */
        private val activeServiceCountdowns = mutableSetOf<String>()
    }

    // ── Instance state ────────────────────────────────────────────────────────

    private var autoSosManager: AutoSosManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private var forwardJobs: Job? = null
    /** Maps alertId → countdown Job so we can cancel it if the UI takes over. */
    private val countdownJobs = mutableMapOf<String, Job>()

    // ── Service lifecycle ────────────────────────────────────────────────────

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val sensitivity = intent.getStringExtra(EXTRA_SENSITIVITY) ?: "medium"
                val sensorType  = intent.getStringExtra(EXTRA_SENSOR_TYPE)  ?: "accelerometer"
                startSafety(sensitivity, sensorType)
            }
            ACTION_STOP -> stopSafety()
            ACTION_UI_TOOK_OVER -> {
                // UI is visible and has taken over the countdown — cancel the service countdown
                val alertId = intent.getStringExtra(EXTRA_ALERT_ID) ?: return START_STICKY
                Log.d(TAG, "UI took over countdown for alertId=$alertId — cancelling service countdown")
                countdownJobs.remove(alertId)?.cancel()
                activeServiceCountdowns.remove(alertId)
                cancelCountdownNotification()
            }
            else -> Log.w(TAG, "Unknown action: ${intent?.action}")
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanUp()
        Log.d(TAG, "Service destroyed")
    }

    // ── Core logic ───────────────────────────────────────────────────────────

    private fun startSafety(sensitivity: String, sensorType: String) {
        if (isRunning.value) {
            Log.d(TAG, "Already running — restarting manager with new params ($sensitivity, $sensorType)")
            autoSosManager?.stop()
            autoSosManager?.start(sensitivity, sensorType)
            return
        }

        Log.d(TAG, "Starting safety monitoring (sensitivity=$sensitivity, sensorType=$sensorType)")

        // 1. Show persistent foreground notification
        createNotificationChannel()
        createSosAlertChannel()
        createCountdownChannel()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, buildNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }

        // 2. Acquire partial wake lock so CPU stays awake with screen off
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "asfalis:safety_wakelock").also {
            it.acquire(12 * 60 * 60 * 1000L)
        }

        // 3. Create and start AutoSosManager
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val manager = AutoSosManager(
            context              = applicationContext,
            sensorManager        = sensorManager,
            protectionRepository = AppServiceLocator.protectionRepository,
            sosRepository        = AppServiceLocator.sosRepository,
            scope                = serviceScope
        )
        autoSosManager = manager
        manager.start(sensitivity, sensorType)

        // 4. Forward events and start service-side countdown
        forwardJobs = serviceScope.launch {
            launch {
                manager.dangerDetected.collect { event ->
                    Log.w(TAG, "DangerEvent: type=${event.triggerType} alertId=${event.alertId}")

                    // Always emit to UI — if screen is on, UI will take over
                    dangerDetected.emit(event)

                    // Fire a heads-up / full-screen notification to wake the screen
                    fireAlertNotification(event)

                    // Start service-side countdown in parallel.
                    // If the UI takes over (screen was on), it sends ACTION_UI_TOOK_OVER
                    // which cancels this job before it reaches T=0.
                    startServiceSideCountdown(event)
                }
            }
            launch {
                manager.cooldownStarted.collect {
                    cooldownStarted.emit(Unit)
                }
            }
        }

        isRunning.value = true
        Log.d(TAG, "Safety monitoring started")
    }

    /**
     * Runs a 10-second countdown entirely inside the service.
     * Updates a live notification every second so the user sees the countdown
     * even on the lock screen.
     * At T=0, calls POST /api/sos/{alertId}/send directly without needing the UI.
     *
     * If the screen was ON and the UI collects the dangerDetected event first,
     * it sends ACTION_UI_TOOK_OVER which cancels this job before T=0.
     */
    @SuppressLint("MissingPermission")
    private fun startServiceSideCountdown(event: AutoSosManager.DangerEvent) {
        val alertId = event.alertId
        if (activeServiceCountdowns.contains(alertId)) return
        activeServiceCountdowns.add(alertId)

        val job = serviceScope.launch {
            // Small delay to give the UI a chance to take over if screen is on
            delay(500L)

            var ticks = 10
            Log.w(TAG, "Service-side countdown started for alertId=$alertId")

            while (ticks > 0) {
                // Update the live countdown notification
                showCountdownNotification(ticks, event)
                delay(1000L)
                ticks--
            }

            // T=0 reached — UI did not take over (screen was locked)
            // Dispatch SOS directly from the service
            Log.w(TAG, "Service countdown T=0 — dispatching SOS for alertId=$alertId (screen was locked)")
            dispatchSosFromService(alertId)
            cancelCountdownNotification()
            activeServiceCountdowns.remove(alertId)
        }

        countdownJobs[alertId] = job
    }

    /** Calls POST /api/sos/{alertId}/send directly from the service when screen is locked. */
    private suspend fun dispatchSosFromService(alertId: String) {
        try {
            val result = AppServiceLocator.sosRepository.sendSosNow(alertId)
            when (result) {
                is NetworkResult.Success -> {
                    Log.w(TAG, "Service-dispatched SOS SUCCESS for alertId=$alertId")
                    val nm = getSystemService(NotificationManager::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val channel = NotificationChannel(
                            "sos_delivery", "SOS Delivery Reports", NotificationManager.IMPORTANCE_HIGH
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
                        
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            val msg = report.filter { !it.delivered }.joinToString("\n") { "${it.phone}: ${it.status}" }
                            android.widget.Toast.makeText(this@SafetyForegroundService, "Delivery failures:\n$msg", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }

                    val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Notification.Builder(this, "sos_delivery")
                    } else {
                        @Suppress("DEPRECATION")
                        Notification.Builder(this).setPriority(Notification.PRIORITY_MAX)
                    }
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setContentTitle(notifTitle)
                        .setContentText(notifText)
                        .setAutoCancel(true)
                        .build()
                        
                    nm?.notify(System.currentTimeMillis().toInt(), notification)
                    com.yourname.womensafety.data.IotSosTracker.onAlertDispatched(alertId)
                }
                is NetworkResult.Error ->
                    Log.e(TAG, "Service-dispatched SOS FAILED: [${result.code}] ${result.message}")
                else -> Unit
            }
        } catch (e: Exception) {
            Log.e(TAG, "Service-dispatched SOS exception: ${e.message}", e)
        }
    }

    /** Shows/updates the live countdown notification (visible on lock screen). */
    private fun showCountdownNotification(ticks: Int, event: AutoSosManager.DangerEvent) {
        val nm = getSystemService(NotificationManager::class.java) ?: return

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "sos_alert")
            putExtra("trigger_type", event.triggerType)
            putExtra("alert_id", event.alertId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 1005, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, COUNTDOWN_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this).setPriority(Notification.PRIORITY_MAX)
        }

        val notification = builder
            .setContentTitle("🚨 SOS ALERT — Sending in $ticks seconds")
            .setContentText("Tap to open app and cancel if you're safe")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)  // Only buzz once, not every second
            .build()

        nm.notify(COUNTDOWN_NOTIFICATION_ID, notification)
    }

    private fun cancelCountdownNotification() {
        getSystemService(NotificationManager::class.java)?.cancel(COUNTDOWN_NOTIFICATION_ID)
    }

    /** Fires the initial full-screen intent notification to wake the screen. */
    private fun fireAlertNotification(event: AutoSosManager.DangerEvent) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "sos_alert")
            putExtra("trigger_type", event.triggerType)
            putExtra("alert_id", event.alertId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 1002, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, SOS_ALERT_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setPriority(Notification.PRIORITY_MAX)
                .setVibrate(longArrayOf(0, 500, 200, 500))
        }

        val notification = builder
            .setContentTitle("⚠️ DANGER DETECTED")
            .setContentText("SOS sending in 10 seconds — tap to cancel if safe")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java)?.notify(1003, notification)
    }

    // ── Channel creation ─────────────────────────────────────────────────────

    /**
     * Creates the low-importance countdown channel.
     * IMPORTANCE_DEFAULT = shows notification but no sound (avoids volume bar flicker).
     */
    private fun createCountdownChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(COUNTDOWN_CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                COUNTDOWN_CHANNEL_ID,
                "SOS Countdown",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Live countdown before SOS is dispatched"
                setSound(null, null)      // No sound — silence, just vibrate once
                enableVibration(false)    // No per-second vibration
                setShowBadge(true)
            }
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Pre-creates the SOS alert channel with sound=null.
     * KEY FIX: Prevents Android from touching the volume slider on friend's device.
     */
    private fun createSosAlertChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(SOS_ALERT_CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                SOS_ALERT_CHANNEL_ID,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full-screen alerts for SOS triggers"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                // sound=null means no ringtone is played → no volume bar shown
                setSound(null, null)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun stopSafety() {
        Log.d(TAG, "Stopping safety monitoring")
        countdownJobs.values.forEach { it.cancel() }
        countdownJobs.clear()
        activeServiceCountdowns.clear()
        cancelCountdownNotification()
        cleanUp()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun cleanUp() {
        autoSosManager?.stop()
        autoSosManager = null
        forwardJobs?.cancel()
        serviceScope.coroutineContext.cancelChildren()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
        isRunning.value = false
    }

    // ── Background (silent) monitoring notification ───────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auto SOS Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification while Auto SOS is monitoring sensors"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Auto SOS Active")
                .setContentText("Monitoring sensors for your safety")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setShowWhen(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Auto SOS Active")
                .setContentText("Monitoring sensors for your safety")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setShowWhen(false)
                .build()
        }
    }
}
