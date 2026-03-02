package com.yourname.womensafety

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yourname.womensafety.data.AppServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AsfalisFirebaseService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AsfalisFirebaseService"
        private const val CHANNEL_SOS = "asfalis_sos"
        private const val CHANNEL_LOCATION = "asfalis_location"
        private const val CHANNEL_GENERAL = "asfalis_general"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        // Re-register the token with the backend
        serviceScope.launch {
            try {
                AppServiceLocator.userRepository.updateFcmToken(token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update FCM token: ${e.message}", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM message received from: ${message.from}")

        val data = message.data
        val notification = message.notification

        val type = data["type"] ?: notification?.let { "general" } ?: return
        val title = data["title"] ?: notification?.title ?: "ASFALIS Alert"
        val body = data["body"] ?: notification?.body ?: ""

        when (type) {
            "sos_alert" -> handleSosAlert(title, body, data)
            "location_share" -> handleLocationShare(title, body, data)
            else -> showNotification(CHANNEL_GENERAL, title, body, generateNotificationId())
        }
    }

    private fun handleSosAlert(title: String, body: String, data: Map<String, String>) {
        Log.d(TAG, "SOS alert received: $data")
        createNotificationChannel(
            CHANNEL_SOS,
            "SOS Alerts",
            "Emergency SOS alerts from trusted contacts",
            NotificationManager.IMPORTANCE_HIGH
        )
        // Deep-link intent into the app (dashboard or SOS history)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("deep_link", "sos_history")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, generateNotificationId(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        showNotification(CHANNEL_SOS, title, body, generateNotificationId(), pendingIntent, highPriority = true)
    }

    private fun handleLocationShare(title: String, body: String, data: Map<String, String>) {
        Log.d(TAG, "Location share notification: $data")
        createNotificationChannel(
            CHANNEL_LOCATION,
            "Location Sharing",
            "Notifications when contacts share their live location",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        showNotification(CHANNEL_LOCATION, title, body, generateNotificationId())
    }

    private fun showNotification(
        channelId: String,
        title: String,
        body: String,
        notificationId: Int,
        pendingIntent: PendingIntent? = null,
        highPriority: Boolean = false
    ) {
        createNotificationChannel(channelId, channelId, "", if (highPriority) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(if (highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)

        pendingIntent?.let { builder.setContentIntent(it) }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    private fun createNotificationChannel(
        channelId: String,
        name: String,
        description: String,
        importance: Int
    ) {
        val channel = NotificationChannel(channelId, name, importance).apply {
            this.description = description
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun generateNotificationId(): Int = System.currentTimeMillis().toInt()
}
