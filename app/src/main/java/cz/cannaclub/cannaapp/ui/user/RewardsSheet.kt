package cz.cannaclub.cannaapp.ui.user

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.cannaclub.cannaapp.ui.theme.BorderSoft
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.Cream
import cz.cannaclub.cannaapp.ui.theme.Sage
import cz.cannaclub.cannaapp.ui.theme.SageGlow
import cz.cannaclub.cannaapp.ui.theme.TextMuted

// Data class pro položku odměny
data class Reward(
    val icon: String,
    val name: String,
    val pointsCost: Int
)

// Seznam všech odměn
private val rewards = listOf(
    Reward("🏷️", "100 Kč sleva na produkt",   100),
    Reward("🌿", "Prémiový preroll",         500),
    Reward("🎁", "Překvapení od obsluhy",    750),
    Reward("👕", "Cannaclub merch",         1000),
)

@Composable
fun RewardsSheet(currentPoints: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(bottom = 36.dp)
    ) {
        // Titulek
        Text(
            text  = "Odměny",
            style = MaterialTheme.typography.headlineLarge,
            color = Cream
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Seznam odměn
        rewards.forEach { reward ->
            RewardItem(
                reward        = reward,
                currentPoints = currentPoints
            )
            Spacer(modifier = Modifier.height(9.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────
// Jedna položka odměny
// ─────────────────────────────────────────────────────────
@Composable
private fun RewardItem(
    reward: Reward,
    currentPoints: Int
) {
    val canAfford = currentPoints >= reward.pointsCost

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardDefault)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ikona
        Text(
            text     = reward.icon,
            fontSize = 26.sp
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Název a cena
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = reward.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Cream
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = "${reward.pointsCost} bodů",
                style = MaterialTheme.typography.bodySmall,
                color = Sage
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Tlačítko Uplatnit — zobrazí se jen pokud má dost bodů
        if (canAfford) {
            Text(
                text     = "Uplatnit",
                style    = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color    = Sage,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(SageGlow)
                    .padding(horizontal = 13.dp, vertical = 8.dp)
            )
        } else {
            Text(
                text     = "${reward.pointsCost - currentPoints} b chybí",
                style    = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color    = TextMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(CardDefault)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}