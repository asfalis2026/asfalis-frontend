package com.yourname.womensafety.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Sealed hierarchy representing outcomes of a wearable button press,
 * resolved by [IotWearableManager] after calling the appropriate SOS endpoint.
 */
sealed class IotAction {
    /** Single-tap: SOS triggered via POST /api/sos/trigger. Navigate to the SOS screen. */
    data class Triggered(val alertId: String) : IotAction()

    /**
     * Double-tap: active SOS cancelled (POST /api/sos/cancel) or user marked safe
     * (POST /api/sos/safe).  Show a Toast/Snackbar; dismiss the SOS screen if visible.
     */
    data class Cancelled(val alertId: String?, val message: String) : IotAction()

    /** SPP socket connected successfully — UI can show "Bracelet Connected". */
    data object Connected : IotAction()

    /**
     * An established SPP socket dropped (device powered off, out of range, etc.).
     * The background service is still retrying — UI should show a "reconnecting" state.
     * Not posted when the user intentionally disconnects via [IotViewModel.disconnect].
     */
    data object Disconnected : IotAction()

    /**
     * First SPP connection attempt failed (device off or out of range).
     * UI should revert to disconnected state and show [reason].
     */
    data class ConnectionFailed(val reason: String) : IotAction()

    /**
     * Emitted every [IotWearableManager.PROXIMITY_POLL_INTERVAL_MS] while connected.
     * [distanceMetres] is the RSSI-based estimate; null readings are not emitted.
     * Collected by [IotViewModel] to surface the distance in the UI.
     * An SOS is auto-triggered by [IotWearableManager] when the threshold is breached
     * for [PROXIMITY_BREACH_COUNT] consecutive polls — this event is informational only.
     */
    data class ProximityUpdate(val distanceMetres: Float) : IotAction()
}

/**
 * Commands dispatched from the App to the Wearable (hardware team patch).
 */
sealed class IotCommand {
    /** Tells the wearable to trigger its motor and LED (haptic feedback). */
    data object TriggerFeedback : IotCommand()
}

/**
 * App-wide event bus that lets [IotWearableService] notify the UI layer
 * about IoT button-press outcomes without a direct reference to any Activity
 * or Composable.
 *
 * Collect [events] in [AppNavGraph] to drive navigation and feedback.
 * Collect [commands] in [IotWearableManager] to send commands to the wearable.
 */
object IotEventBus {

    private val _events = MutableSharedFlow<IotAction>(extraBufferCapacity = 1)
    private val _commands = MutableSharedFlow<IotCommand>(extraBufferCapacity = 4)

    /** Observe this flow in AppNavGraph. */
    val events: SharedFlow<IotAction> = _events

    /** Observe this flow in IotWearableManager to receive commands to send to the wearable. */
    val commands: SharedFlow<IotCommand> = _commands

    /** Thread-safe. Can be called from any coroutine / background thread. */
    fun post(action: IotAction) {
        _events.tryEmit(action)
    }

    /** Thread-safe. Send commands from the UI/ViewModels to the connected wearable. */
    fun sendCommand(command: IotCommand) {
        _commands.tryEmit(command)
    }
}
