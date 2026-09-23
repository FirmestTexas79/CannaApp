package cz.cannaclub.cannaapp.ui.user

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.cannaclub.cannaapp.R
import cz.cannaclub.cannaapp.model.LoyaltyConfig
import cz.cannaclub.cannaapp.model.MemberRank
import cz.cannaclub.cannaapp.ui.components.CannaIcon
import cz.cannaclub.cannaapp.ui.components.IconBubble
import cz.cannaclub.cannaapp.ui.components.POINT_VALUE_KC
import cz.cannaclub.cannaapp.ui.components.RankBadge
import cz.cannaclub.cannaapp.ui.components.SectionLabel
import cz.cannaclub.cannaapp.ui.components.color
import cz.cannaclub.cannaapp.ui.theme.BorderNormal
import cz.cannaclub.cannaapp.ui.theme.BorderSoft
import cz.cannaclub.cannaapp.ui.theme.Brand
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.Forest
import cz.cannaclub.cannaapp.ui.theme.OnBrand
import cz.cannaclub.cannaapp.ui.theme.Paper
import cz.cannaclub.cannaapp.ui.theme.Sage
import cz.cannaclub.cannaapp.ui.theme.SageGlow
import cz.cannaclub.cannaapp.ui.theme.TextFaint
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import cz.cannaclub.cannaapp.ui.theme.TextPrimary

data class ShopItem(
    @DrawableRes val icon: Int,
    val name: String,
    val pointsCost: Int
)

// ── Ceník odměn ──────────────────────────────────────────
val shopItems = listOf(
    ShopItem(R.drawable.ic_preroll, "Prémiový preroll", 20),
    ShopItem(R.drawable.ic_preroll, "3× preroll",       40),
    ShopItem(R.drawable.ic_bud,     "3g kytky",         50),
    ShopItem(R.drawable.ic_cube,    "4g hashe",        100),
)

/**
 * Odměny: body jdou vyměnit za slevu na cokoli (1 b = 1 Kč), nebo za odměnu z ceníku.
 * Pod tím rank a cesta ranků.
 */
@Composable
fun RewardsSheet(
    currentPoints: Int,
    totalPoints: Int,
    loyalty: LoyaltyConfig = LoyaltyConfig()
) {
    val currentRank = MemberRank.forPoints(totalPoints)
    val nextRank    = currentRank.next

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
            .padding(top = 4.dp, bottom = 40.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text     = "Odměny",
                style    = MaterialTheme.typography.headlineLarge,
                color    = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text     = "Máš $currentPoints b",
                style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color    = Sage,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SageGlow)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ── 1. Sleva na cokoli ────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Brand, Forest)))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CannaIcon(id = R.drawable.ic_tag, tint = OnBrand, size = 20.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    SectionLabel("Sleva na cokoli", color = OnBrand.copy(alpha = 0.8f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text  = "${currentPoints * POINT_VALUE_KC} Kč",
                    style = MaterialTheme.typography.displayLarge,
                    color = OnBrand
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = "Body můžeš u pokladny kdykoli vyměnit za slevu na jakýkoli produkt. 1 bod = $POINT_VALUE_KC Kč.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBrand.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // ── 2. Nebo odměna z ceníku ───────────────────────
        SectionLabel("Nebo si vyber odměnu")
        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Paper)
                .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
        ) {
            shopItems.forEachIndexed { i, item ->
                if (i > 0) HorizontalDivider(color = BorderSoft, modifier = Modifier.padding(start = 70.dp))
                RewardRow(item = item, points = currentPoints)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Jak uplatnit
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardDefault)
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            CannaIcon(id = R.drawable.ic_info, tint = TextMuted, size = 18.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text  = "Jak uplatnit: řekni si u pokladny o slevu nebo odměnu a ukaž kartičku. Obsluha ti body odečte.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // ── 3. Rank ───────────────────────────────────────
        SectionLabel("Tvůj rank")
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Paper)
                .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RankBadge(rank = currentRank, size = 52.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = currentRank.label,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text  = loyalty.bonusFor(currentRank).let { b ->
                        if (b > 0) "+$b % bodů za každý nákup · celkem $totalPoints b" else "Celkem nasbíráno $totalPoints b"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                ProgressBar(progress = currentRank.progress(totalPoints), color = currentRank.color)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = nextRank?.let {
                        val nb = loyalty.bonusFor(it)
                        "Do ranku ${it.label}${if (nb > 0) " (+$nb % bodů)" else ""} chybí ${it.requiredPoints - totalPoints} b"
                    }
                            ?: "Nejvyšší rank, patříš do rodiny",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextFaint
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        SectionLabel("Cesta ranků")
        Spacer(modifier = Modifier.height(12.dp))

        val ranks = MemberRank.entries
        ranks.forEachIndexed { index, rank ->
            val unlocked = totalPoints >= rank.requiredPoints
            val current  = rank == currentRank
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RankBadge(rank = rank, size = 46.dp, locked = !unlocked)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = rank.label,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (unlocked) TextPrimary else TextMuted
                    )
                    Text(
                        text  = buildString {
                            append(if (rank.requiredPoints == 0) "Hned od registrace" else "od ${rank.requiredPoints} nasbíraných bodů")
                            val b = loyalty.bonusFor(rank)
                            if (b > 0) append(" · +$b % bodů")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextFaint
                    )
                }
                if (current) {
                    Text(
                        text     = "Teď",
                        style    = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color    = OnBrand,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(rank.color)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                } else if (unlocked) {
                    CannaIcon(id = R.drawable.ic_check, tint = Sage, size = 18.dp)
                }
            }
            if (index < ranks.lastIndex) {
                RankConnector(unlocked = totalPoints >= ranks[index + 1].requiredPoints)
            }
        }
    }
}

@Composable
private fun RewardRow(item: ShopItem, points: Int) {
    val canAfford = points >= item.pointsCost
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(
            id         = item.icon,
            tint       = if (canAfford) Sage else TextMuted,
            background = if (canAfford) SageGlow else CardDefault,
            size       = 42.dp,
            iconSize   = 21.dp
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(
                text  = "${item.pointsCost} b",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        if (canAfford) {
            Row(
                modifier          = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Sage)
                    .padding(start = 8.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CannaIcon(id = R.drawable.ic_check, tint = OnBrand, size = 14.dp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text  = "Můžeš si vzít",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = OnBrand
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(84.dp)) {
                Text(
                    text  = "chybí ${item.pointsCost - points} b",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(5.dp))
                ProgressBar(progress = points.toFloat() / item.pointsCost, color = Sage, height = 4)
            }
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, color: androidx.compose.ui.graphics.Color, height: Int = 6) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape((height / 2).dp))
            .background(BorderSoft)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape((height / 2).dp))
                .background(color)
        )
    }
}

/** Svislá čárkovaná spojnice mezi odznaky ranků. */
@Composable
private fun RankConnector(unlocked: Boolean) {
    val c = if (unlocked) Sage.copy(alpha = 0.6f) else BorderNormal
    Canvas(
        modifier = Modifier
            .width(46.dp)
            .height(26.dp)
    ) {
        drawLine(
            color       = c,
            start       = Offset(size.width / 2, 4f),
            end         = Offset(size.width / 2, size.height - 4f),
            strokeWidth = 2.5f * density / 1.5f,
            pathEffect  = if (unlocked) null else PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        )
    }
}
