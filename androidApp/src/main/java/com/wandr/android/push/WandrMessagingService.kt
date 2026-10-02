package com.wandr.android.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives FCM token refreshes and messages while the app is in the foreground. Messages that arrive in the
 * background are displayed by FCM itself (English fallback text); tapping them delivers the data extras to MainActivity.
 */
class WandrMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        PushNotifier.onToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        PushNotifier.show(
            context = applicationContext,
            data = message.data,
            fallbackTitle = message.notification?.title,
            fallbackBody = message.notification?.body
        )
    }
}
