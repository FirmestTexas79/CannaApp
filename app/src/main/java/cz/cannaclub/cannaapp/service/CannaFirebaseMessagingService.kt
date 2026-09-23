package cz.cannaclub.cannaapp.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import cz.cannaclub.cannaapp.MainActivity
import cz.cannaclub.cannaapp.R
import cz.cannaclub.cannaapp.preferences.UserPreferences
import cz.cannaclub.cannaapp.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CannaFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID   = "canna_points"
        const val CHANNEL_NAME = "Body a odměny"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ─────────────────────────────────────────────────────
    // Appka v popředí → notifikaci musíme zobrazit sami.
    // Appka na pozadí → zobrazí ji systém z "notification" části zprávy.
    // ─────────────────────────────────────────────────────
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "CannaClub"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: ""

        showNotification(title, body)
    }

    // ─────────────────────────────────────────────────────
    // FCM občas token vymění. Když je někdo přihlášený,
    // uložíme nový token hned — jinak by server posílal na starý.
    // ─────────────────────────────────────────────────────
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val userId = UserPreferences(applicationContext).getSavedUserId()
        if (userId.isNotBlank()) {
            scope.launch { UserRepository().saveFcmToken(userId, token) }
        }
    }

    private fun showNotification(title: String, body: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Malá ikona musí být jednobarevná silueta — barevná adaptivní ikona
        // (mipmap/ic_launcher) se v liště zobrazí jako bílý čtverec.
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(this, R.color.notification_color))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
