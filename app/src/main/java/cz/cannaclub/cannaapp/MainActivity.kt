package cz.cannaclub.cannaapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import cz.cannaclub.cannaapp.navigation.CannaNavGraph
import cz.cannaclub.cannaapp.service.CannaFirebaseMessagingService
import cz.cannaclub.cannaapp.ui.theme.Background
import cz.cannaclub.cannaapp.ui.theme.CannaAppTheme
import android.os.Handler
import android.os.Looper



class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()

        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("FCM_TEST", "Token: ${task.result}")
                } else {
                    android.util.Log.e("FCM_TEST", "Token selhalo: ${task.exception}")
                }
            }

        setContent {
            CannaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = Background
                ) {
                    CannaNavGraph()
                }
            }
        }
    }

    private fun createNotificationChannel() {

        Handler(Looper.getMainLooper()).postDelayed({
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationCompat.Builder(this, "canna_points")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Test CannaApp")
                .setContentText("Notifikace fungují!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            manager.notify(1, notification)
        }, 3000)

        val channel = NotificationChannel(
            CannaFirebaseMessagingService.CHANNEL_ID,
            CannaFirebaseMessagingService.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description     = "Notifikace o bodech a odměnách"
            enableLights(true)
            enableVibration(true)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        android.util.Log.d("NOTIF_CHANNEL", "Kanál vytvořen: ${channel.id}")
    }

}