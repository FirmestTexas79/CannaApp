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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.cannaclub.cannaapp.R
import cz.cannaclub.cannaapp.model.LoyaltyConfig
import cz.cannaclub.cannaapp.model.MemberRank
import cz.cannaclub.cannaapp.ui.components.CannaIcon
import cz.cannaclub.cannaapp.ui.components.IconBubble
import cz.cannaclub.cannaapp.ui.components.RankBadge
import cz.cannaclub.cannaapp.ui.components.SectionLabel
import cz.cannaclub.cannaapp.ui.components.color
import cz.cannaclub.cannaapp.ui.theme.BorderNormal
import cz.cannaclub.cannaapp.ui.theme.BorderSoft
import cz.cannaclub.cannaapp.ui.theme.Brand
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.Forest
import cz.cannaclub.cannaapp.ui.theme.Honey
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
    val pointsCost: Int,
    /** Od jakého ranku se odměna odemkne. */
    val rank: MemberRank = MemberRank.ZAKAZNIK
)

/** Ikona odměny podle klíče z config/rewards (stejné klíče jako ve webovém adminu). */
fun rewardIcon(key: String): Int = when (key) {
    "preroll" -> R.drawable.ic_preroll
    "bud"     -> R.drawable.ic_bud
    "cube"    -> R.drawable.ic_cube
    "leaf"    -> R.drawable.ic_leaf
    "bag"     -> R.drawable.ic_bag
    "sparkle" -> R.drawable.ic_sparkle
    else      -> R.drawable.ic_gift
}

// ── Výchozí ceník odměn (než ho obsluha upraví ve webovém adminu) ──
val shopItems = listOf(
    ShopItem(R.drawable.ic_gift, "Papírky dle výběru", 100),
    ShopItem(R.drawable.ic_gift, "Zapalovač", 150),
    ShopItem(R.drawable.ic_cube, "Drtička Bulldog", 300, MemberRank.BRONZOVY),
    ShopItem(R.drawable.ic_leaf, "CBD čaj", 400, MemberRank.BRONZOVY),
    ShopItem(R.drawable.ic_leaf, "CBD med", 550, MemberRank.STRIBRNY),
    ShopItem(R.drawable.ic_bud, "1 g kytek dle výběru", 650, MemberRank.STRIBRNY),
    ShopItem(R.drawable.ic_leaf, "CBD olej 10 %", 1400, MemberRank.ZLATY),
    ShopItem(R.drawable.ic_bud, "3 g kytek dle výběru", 1800, MemberRank.ZLATY),
    ShopItem(R.drawable.ic_bag, "Klobouk Bulldog", 2200, MemberRank.RODINA),
    ShopItem(R.drawable.ic_leaf, "CBD olej 20 %", 3500, MemberRank.RODINA),
)

/** Celý ceník odměn: z config/rewards, jinak výchozí; seřazený od nejlevnější. */
fun rewardList(loyalty: LoyaltyConfig): List<ShopItem> = loyalty.rewards
    .map { r ->
        ShopItem(rewardIcon(r.icon), r.name, r.cost,
            MemberRank.entries.firstOrNull { it.name == r.rank } ?: MemberRank.ZAKAZNIK)
    }
    .ifEmpty { shopItems }
    .sortedBy { it.pointsCost }

/** Odměny, které už zákazník svým rankem odemkl. */
fun unlockedRewards(loyalty: LoyaltyConfig, totalPoints: Int): List<ShopItem> {
    val rank = MemberRank.forPoints(totalPoints)
    return rewardList(loyalty).filter { it.rank.ordinal <= rank.ordinal }
}

/** Text pod body: kolik chybí do nejbližší odemčené odměny. */
fun nextRewardText(points: Int, totalPoints: Int, loyalty: LoyaltyConfig): String {
    val list = unlockedRewards(loyalty, totalPoints)
    val next = list.firstOrNull { it.pointsCost > points }
    val canTake = list.count { it.pointsCost <= points }
    return when {
        next == null && canTake > 0 -> "Můžeš si vzít jakoukoli odměnu"
        next == null -> "Sbírej body za každý nákup"
        canTake > 0 -> "Na $canTake ${if (canTake == 1) "odměnu" else if (canTake < 5) "odměny" else "odměn"} už máš · do další chybí ${next.pointsCost - points} b"
        else -> "Do odměny „${next.name}“ chybí ${next.pointsCost - points} b"
    }
}

/**
 * Odměny: body jdou vyměnit za odměny z ceníku (upravuje obsluha ve webovém adminu).
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
    val all         = rewardList(loyalty)
    val unlocked    = all.filter { it.rank.ordinal <= currentRank.ordinal }
    val locked      = all.filter { it.rank.ordinal > currentRank.ordinal }.groupBy { it.rank }.toSortedMap()

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

        // ── 1. Hlavní odměna ───────────────────────────────
        loyalty.finale?.let { f ->
            FinaleCard(name = f.name, cost = f.cost, points = currentPoints)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── 2. Odemčené odměny z ceníku ───────────────────
        SectionLabel("Za body si můžeš vzít")
        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Paper)
                .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
        ) {
            unlocked.forEachIndexed { i, item ->
                if (i > 0) HorizontalDivider(color = BorderSoft, modifier = Modifier.padding(start = 70.dp))
                RewardRow(item = item, points = currentPoints)
            }
            if (unlocked.isEmpty()) Text(
                text     = "Zatím tu nic není.",
                style    = MaterialTheme.typography.bodySmall,
                color    = TextMuted,
                modifier = Modifier.padding(16.dp)
            )
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
                text  = "Jak uplatnit: řekni si u pokladny o odměnu a ukaž kartičku. Obsluha ti body odečte.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        // ── 3. Zamčené odměny vyšších ranků (bez ceny) ────
        if (locked.isNotEmpty()) {
            Spacer(modifier = Modifier.height(26.dp))
            SectionLabel("Odemkneš s vyšším rankem")
            Spacer(modifier = Modifier.height(10.dp))
            LockedRewards(locked)
        }

        Spacer(modifier = Modifier.height(30.dp))

        // ── 4. Rank ───────────────────────────────────────
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
                            append(if (rank.requiredPoints == 0) "Hned od registrace" else "od ${formatPoints(rank.requiredPoints)} nasbíraných bodů")
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

/** Hlavní odměna — tmavá karta se zlatým akcentem a postupem k ní. */
@Composable
private fun FinaleCard(name: String, cost: Int, points: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2E4526), Forest, Color(0xFF1B2A16))))
            .drawBehind {
                val glow = Offset(size.width, 0f)
                drawCircle(
                    brush  = Brush.radialGradient(listOf(Honey.copy(alpha = 0.35f), Color.Transparent), center = glow, radius = size.width * 0.75f),
                    radius = size.width * 0.75f,
                    center = glow
                )
            }
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CannaIcon(id = R.drawable.ic_sparkle, tint = Honey, size = 15.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text("HLAVNÍ ODMĚNA", style = MaterialTheme.typography.labelSmall, color = Honey)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(name, style = MaterialTheme.typography.headlineLarge, color = OnBrand)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text  = formatPoints(cost),
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 40.sp, lineHeight = 44.sp),
                color = Honey
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("bodů", style = MaterialTheme.typography.bodyMedium, color = OnBrand.copy(alpha = 0.75f),
                modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(OnBrand.copy(alpha = 0.16f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((points.toFloat() / cost).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Honey)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text  = if (points >= cost) "Máš dost bodů, řekni si o ni na prodejně."
                    else "Máš ${formatPoints(points)} b · chybí ${formatPoints(cost - points)} b",
            style = MaterialTheme.typography.bodySmall,
            color = OnBrand.copy(alpha = 0.85f)
        )
    }
}

/** Odměny vyšších ranků: vidí se název, cena až po získání ranku. */
@Composable
private fun LockedRewards(locked: Map<MemberRank, List<ShopItem>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Paper)
            .border(1.dp, BorderNormal, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            text  = "Každý rank ti odemkne nové odměny. Kolik bodů stojí, uvidíš, jakmile rank získáš.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        locked.forEach { (rank, items) ->
            HorizontalDivider(color = BorderSoft, modifier = Modifier.padding(vertical = 12.dp))
            Row(verticalAlignment = Alignment.Top) {
                RankBadge(rank = rank, size = 34.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(rank.label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                    Text("od ${formatPoints(rank.requiredPoints)} nasbíraných bodů", style = MaterialTheme.typography.bodySmall, color = TextFaint)
                    Spacer(modifier = Modifier.height(6.dp))
                    items.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            CannaIcon(id = R.drawable.ic_lock, tint = TextFaint, size = 14.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(item.name, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

/** 20000 → "20 000" */
fun formatPoints(n: Int): String = "%,d".format(java.util.Locale.ROOT, n).replace(',', '\u00A0')
