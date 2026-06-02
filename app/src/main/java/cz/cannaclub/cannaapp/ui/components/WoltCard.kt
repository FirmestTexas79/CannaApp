package cz.cannaclub.cannaapp.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.cannaclub.cannaapp.ui.theme.Sage
import cz.cannaclub.cannaapp.ui.theme.SageGlow
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import cz.cannaclub.cannaapp.ui.theme.TextPrimary

// Zatím odkaz vede na hlavní stránku Woltu v HK
private const val WOLT_URL = "https://wolt.com/cs/cze/hradec-kralove"

@Composable
fun WoltCard() {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.5.dp, Sage.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WOLT_URL))
                context.startActivity(intent)
            }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ikona
        Text(text = "🛵", fontSize = 32.sp)

        Spacer(modifier = Modifier.width(16.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = "Rozvoz domů",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = "Objednejte si přes Wolt",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        // Odkaz
        Text(
            text     = "Otevřít →",
            style    = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color    = Sage,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SageGlow)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}