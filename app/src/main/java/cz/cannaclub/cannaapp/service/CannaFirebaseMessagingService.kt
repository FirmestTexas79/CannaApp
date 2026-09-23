package cz.cannaclub.cannaapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import cz.cannaclub.cannaapp.MainActivity
import cz.cannaclub.cannaapp.R

class CannaFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID   = "canna_points"
        const val CHANNEL_NAME = "Body a odměny"
    }

    // ─────────────────────────────────────────────────────
    // Zavolá se při přijetí notifikace když je appka otevřená
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
    // Zavolá se při obnově FCM tokenu
    // Token se uloží do Firestore aby Cloud Function
    // věděla kam poslat notifikaci
    // ─────────────────────────────────────────────────────
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token se uloží při přihlášení zákazníka
        // viz UserViewModel.saveFcmToken()
        android.util.Log.d("FCM", "Nový token: $token")
    }

    // ─────────────────────────────────────────────────────
    // Zobrazení notifikace
    // ─────────────────────────────────────────────────────
    private fun showNotification(title: String, body: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Vytvoř kanál (nutné pro Android 8+)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifikace o bodech a odměnách"
        }
        notificationManager.createNotificationChannel(channel)

        // Kliknutí na notifikaci otevře appku
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}