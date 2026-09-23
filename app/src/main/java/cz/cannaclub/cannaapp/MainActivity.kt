package cz.cannaclub.cannaapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import cz.cannaclub.cannaapp.navigation.CannaNavGraph
import cz.cannaclub.cannaapp.service.CannaFirebaseMessagingService
import cz.cannaclub.cannaapp.ui.theme.Background
import cz.cannaclub.cannaapp.ui.theme.CannaAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Požadavky ze zkratek na ikoně appky (zatím jen "Členská kartička"). */
object ShortcutRequests {
    const val ACTION_SHOW_CARD = "cz.cannaclub.cannaapp.SHOW_CARD"

    private val _showCard = MutableStateFlow(false)
    val showCard: StateFlow<Boolean> = _showCard.asStateFlow()

    fun handle(intent: Intent?) {
        if (intent?.action == ACTION_SHOW_CARD) _showCard.value = true
    }

    fun consume() { _showCard.value = false }
}

/** Výsledek uložení kartičky do Google Peněženky (přichází přes onActivityResult). */
object WalletEvents {
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun onSaved() { _saved.value = true }
    fun consume() { _saved.value = false }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Světlé lišty s tmavými ikonami — appka má krémové pozadí i v tmavém režimu systému
        enableEdgeToEdge(
            statusBarStyle     = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        createNotificationChannel()
        ShortcutRequests.handle(intent)

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

    @Deprecated("Google Pay API vrací výsledek uložení jen přes onActivityResult")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cz.cannaclub.cannaapp.repository.WalletRepository.SAVE_REQUEST_CODE &&
            resultCode == RESULT_OK
        ) {
            WalletEvents.onSaved()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ShortcutRequests.handle(intent)
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
