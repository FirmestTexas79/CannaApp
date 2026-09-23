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
import cz.cannaclub.cannaapp.navigation.CannaNavGraph
import cz.cannaclub.cannaapp.service.CannaFirebaseMessagingService
import cz.cannaclub.cannaapp.ui.theme.Background
import cz.cannaclub.cannaapp.ui.theme.CannaAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()

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

    // Kanál musí existovat dřív, než dorazí první push (Android 8+).
    // Samotné notifikace posílá server (Cloud Function notifyOnTransaction).
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CannaFirebaseMessagingService.CHANNEL_ID,
            CannaFirebaseMessagingService.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifikace o bodech a odměnách"
            enableLights(true)
            enableVibration(true)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
