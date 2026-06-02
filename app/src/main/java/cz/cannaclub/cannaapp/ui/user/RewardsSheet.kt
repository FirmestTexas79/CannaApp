package cz.cannaclub.cannaapp.ui.user

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.cannaclub.cannaapp.model.MemberRank
import cz.cannaclub.cannaapp.ui.theme.BorderNormal
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.Sage
import cz.cannaclub.cannaapp.ui.theme.SageGlow
import cz.cannaclub.cannaapp.ui.theme.SageLight
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import cz.cannaclub.cannaapp.ui.theme.TextPrimary

data class ShopItem(
    val icon: String,
    val name: String,
    val pointsCost: Int
)

// ── Aktualizovaný ceník ───────────────────────────────────
val shopItems = listOf(
    ShopItem("🚬", "Prémiový preroll",      20),
    ShopItem("🚬", "3× preroll",            40),
    ShopItem("🌿", "3g kytky",              50),
    ShopItem("🍫", "4g hashe",             100),
)

private val CircleSize = 52.dp

@Composable
fun RewardsSheet(
    currentPoints: Int,
    totalPoints: Int
) {
    val ranks = MemberRank.entries
    val currentRank = when {
        totalPoints >= 2500 -> MemberRank.RODINA
        totalPoints >= 1000 -> MemberRank.ZLATY
        totalPoints >= 500  -> MemberRank.STRIBRNY
        totalPoints >= 250  -> MemberRank.BRONZOVY
        else                -> MemberRank.ZAKAZNIK
    }

    val nextRank = ranks.getOrNull(ranks.indexOf(currentRank) + 1)

    // ── verticalScroll zajistí scrollování celého obsahu ──
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 48.dp)
    ) {

        // ── Aktuální rank karta ───────────────────────────
        Text(
            text  = "Členský rank",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )
        Text(
            text  = "Celkem nasbíráno: $totalPoints b",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SageGlow)
                .border(1.dp, Sage.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = currentRank.icon, fontSize = 36.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = currentRank.label,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Sage
                )
                if (nextRank != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val progress = ((totalPoints - currentRank.requiredPoints).toFloat() /
                            (nextRank.requiredPoints - currentRank.requiredPoints))
                        .coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress   = { progress },
                        modifier   = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color      = Sage,
                        trackColor = BorderNormal,
                        strokeCap  = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text  = "Do ${nextRank.label} ${nextRank.icon} chybí ${nextRank.requiredPoints - totalPoints} b",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text  = "Nejvyšší rank — jsi součástí rodiny 💚",
                        style = MaterialTheme.typography.labelSmall,
                        color = SageLight
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Cesta ranků ───────────────────────────────────
        Text(
            text  = "CESTA RANKŮ",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(12.dp))

        ranks.forEachIndexed { index, rank ->
            val unlocked = totalPoints >= rank.requiredPoints

            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    Text(text = rank.icon, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

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
                        text  = rank.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text  = if (rank.requiredPoints == 0) "Výchozí rank"
                        else "${rank.requiredPoints} celkových bodů",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (unlocked) Sage else TextMuted
                    )
                    if (unlocked && rank == currentRank) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text     = "✓ Aktuální rank",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = SageLight,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Sage.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (index < ranks.lastIndex) {
                WindingPath(
                    unlocked    = unlocked,
                    wiggleLeft  = if (index % 2 == 0) 18f else 10f,
                    wiggleRight = if (index % 2 == 0) 10f else 20f
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Ceník ─────────────────────────────────────────
        Text(
            text  = "ZA CO UTRATIT BODY",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(12.dp))

        shopItems.forEach { item ->
            val canAfford = currentPoints >= item.pointsCost
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDefault)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = item.icon, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text  = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        Text(
                            text  = "${item.pointsCost} bodů",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (canAfford) Sage else TextMuted
                        )
                    }
                }
                if (canAfford) {
                    Text(
                        text     = "Mám ✓",
                        style    = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color    = Sage,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SageGlow)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                } else {
                    Text(
                        text     = "−${item.pointsCost - currentPoints} b",
                        style    = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color    = TextMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardDefault)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

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
            cubicTo(cx - wiggleLeft, q1, cx + wiggleRight, q2, cx, bot)
        }

        drawPath(
            path  = path,
            color = pathColor,
            style = Stroke(
                width      = 3.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            )
        )

        drawCircle(
            color  = pathColor,
            radius = 5f,
            center = Offset(cx, bot)
        )
    }
}