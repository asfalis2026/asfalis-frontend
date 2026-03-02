package com.yourname.womensafety.data.network

import android.util.Log
import com.yourname.womensafety.data.local.TokenManager
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.net.URI

/**
 * Manages a Socket.IO connection to the Flask backend's /location namespace.
 * Handles real-time location sharing and tracking updates.
 */
class LocationSocketManager(
    private val tokenManager: TokenManager,
    private val wsUrl: String = ApiConstants.WS_URL
) {
    companion object {
        private const val TAG = "LocationSocketManager"
        private const val NAMESPACE = "/location"
    }

    private var socket: Socket? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError

    /** Connect to the /location namespace with the current JWT token as a query param. */
    fun connect() {
        val token = runBlocking { tokenManager.getAccessToken() } ?: run {
            Log.w(TAG, "No access token — cannot connect to location socket")
            _locationError.value = "Not authenticated"
            return
        }

        try {
            val options = IO.Options.builder()
                .setQuery("token=$token")
                .setTransports(arrayOf("websocket"))
                .setReconnection(true)
                .setReconnectionAttempts(5)
                .setReconnectionDelay(2000)
                .build()

            socket = IO.socket(URI.create("$wsUrl$NAMESPACE"), options).apply {
                on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "Socket connected")
                    _isConnected.value = true
                    _locationError.value = null
                }
                on(Socket.EVENT_DISCONNECT) { args ->
                    Log.d(TAG, "Socket disconnected: ${args.firstOrNull()}")
                    _isConnected.value = false
                }
                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    val reason = (args.firstOrNull() as? Exception)?.message ?: "Connection error"
                    Log.e(TAG, "Socket connect error: $reason")
                    _isConnected.value = false
                    _locationError.value = reason
                }
                on("location_update") { args ->
                    // Incoming location update from server (e.g., a contact sharing their location)
                    val data = args.firstOrNull() as? JSONObject ?: return@on
                    Log.d(TAG, "location_update: $data")
                }
                connect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create socket: ${e.message}", e)
            _locationError.value = e.message
        }
    }

    /**
     * Send a real-time location update to the server.
     *
     * @param lat Latitude
     * @param lng Longitude
     * @param accuracy GPS accuracy in metres
     */
    fun sendLocationUpdate(lat: Double, lng: Double, accuracy: Float? = null) {
        if (socket?.connected() != true) {
            Log.w(TAG, "Socket not connected, cannot send location update")
            return
        }
        val payload = JSONObject().apply {
            put("lat", lat)
            put("lng", lng)
            accuracy?.let { put("accuracy", it) }
        }
        socket?.emit("location_update", payload)
    }

    /**
     * Join a tracking session to share live location with a contact.
     *
     * @param contactId The contact's user ID to share location with.
     */
    fun joinTracking(contactId: String) {
        if (socket?.connected() != true) {
            Log.w(TAG, "Socket not connected, cannot join tracking")
            return
        }
        val payload = JSONObject().apply { put("contact_id", contactId) }
        socket?.emit("join_tracking", payload)
        Log.d(TAG, "Joined tracking with contact: $contactId")
    }

    /**
     * Leave a tracking session with a contact.
     *
     * @param contactId The contact's user ID to stop sharing with.
     */
    fun leaveTracking(contactId: String) {
        if (socket?.connected() != true) return
        val payload = JSONObject().apply { put("contact_id", contactId) }
        socket?.emit("leave_tracking", payload)
        Log.d(TAG, "Left tracking with contact: $contactId")
    }

    /**
     * Register a listener for incoming location updates from contacts who are sharing with you.
     *
     * @param listener Callback with the JSON payload from the server.
     */
    fun onIncomingLocation(listener: (JSONObject) -> Unit) {
        socket?.on("location_update") { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@on
            listener(data)
        }
    }

    /** Disconnect the socket cleanly. */
    fun disconnect() {
        socket?.apply {
            off()
            disconnect()
        }
        socket = null
        _isConnected.value = false
        Log.d(TAG, "Socket disconnected cleanly")
    }
}
