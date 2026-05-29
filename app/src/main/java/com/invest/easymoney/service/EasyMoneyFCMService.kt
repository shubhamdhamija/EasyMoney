package com.invest.easymoney.service

import android.app.NotificationManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.invest.easymoney.R
import com.invest.easymoney.util.Constants

class EasyMoneyFCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed: $token")
        // TODO: Send token to your backend server for targeted push notifications
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "FCM message received from: ${message.from}")
        val title = message.notification?.title ?: message.data["title"] ?: "EasyMoney Alert"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        if (body.isNotEmpty()) showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val TAG = "EasyMoneyFCM"
    }
}
