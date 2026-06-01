package cz.cannaclub.cannaapp.ui.user

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.cannaclub.cannaapp.ui.theme.BorderNormal
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.Sage
import cz.cannaclub.cannaapp.ui.theme.SageGlow
import cz.cannaclub.cannaapp.ui.theme.SageLight
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import cz.cannaclub.cannaapp.ui.theme.TextPrimary

data class Reward(
    val icon: String,
    val name: String,
    val pointsCost: Int
)

private val rewards = listOf(
    Reward("🏷️", "100 Kč sleva na produkt",  100),
    Reward("🌿", "Prémiový preroll",          500),
    Reward("🎁", "Překvapení od obsluhy",     750),
    Reward("👕", "Cannaclub merch",          1000),
)

// Průměr kroužku na cestě
private val CircleSize = 52.dp

@Composable
fun RewardsSheet(currentPoints: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 32.dp)
    ) {
        Text(
            text  = "Cesta odměn",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )
        Text(
            text  = "Sbírej body a postupuj dál",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        rewards.forEachIndexed { index, reward ->
            val unlocked = currentPoints >= reward.pointsCost

            // ── Checkpoint: kroužek + karta ───────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kroužek s ikonou
                Box(
                    modifier = Modifier
                        .size(CircleSize)
                        .clip(CircleShape)
                        .background(if (unlocked) Sage else CardDefault)
                        .border(
                            width = if (unlocked) 0.dp else 1.5.dp,
                            color = if (unlocked) Sage else BorderNormal,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text     = reward.icon,
                        fontSize = 22.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Karta s popisem
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (unlocked) SageGlow else CardDefault)
                        .border(
                            width = 1.dp,
                            color = if (unlocked) Sage.copy(alpha = 0.3f) else BorderNormal,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text  = reward.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text  = "${reward.pointsCost} bodů",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (unlocked) Sage else TextMuted
                    )
                    if (unlocked) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text     = "✓ Uplatnit u obsluhy",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = SageLight,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Sage.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text  = "chybí ${reward.pointsCost - currentPoints} b",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // ── Klikatá cesta k dalšímu checkpointu ──────────
            if (index < rewards.lastIndex) {
                WindingPath(
                    unlocked    = unlocked,
                    // lehce různý tvar pro každý úsek — přirozené bloudění
                    wiggleLeft  = if (index % 2 == 0) 18f else 10f,
                    wiggleRight = if (index % 2 == 0) 10f else 20f
                )
            }
        }
    }
}

/**
 * Canvas kreslí čárkovanou organicky klikatou cestu.
 * Šířka odpovídá CircleSize (zarovnaná pod kroužkem), výška 56 dp.
 * Cesta vychází ze středu shora a končí uprostřed dole.
 */
@Composable
private fun WindingPath(
    unlocked: Boolean,
    wiggleLeft: Float,
    wiggleRight: Float
) {
    val pathColor = if (unlocked) Sage.copy(alpha = 0.55f) else TextMuted.copy(alpha = 0.35f)

    Canvas(
        modifier = Modifier
            .width(CircleSize)
            .height(56.dp)
    ) {
        val cx  = size.width / 2f
        val bot = size.height
        val q1  = bot * 0.33f
        val q2  = bot * 0.67f

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, 0f)
            cubicTo(
                cx - wiggleLeft,  q1,
                cx + wiggleRight, q2,
                cx,               bot
            )
        }

        drawPath(
            path  = path,
            color = pathColor,
            style = Stroke(
                width      = 3.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            )
        )

        // Tečka ukazatele směru na konci segmentu
        drawCircle(
            color  = pathColor,
            radius = 5f,
            center = Offset(cx, bot)
        )
    }
}