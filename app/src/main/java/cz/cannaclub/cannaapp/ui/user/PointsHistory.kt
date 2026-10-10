package cz.cannaclub.cannaapp.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.cannaclub.cannaapp.model.Transaction
import cz.cannaclub.cannaapp.ui.components.IconBubble
import cz.cannaclub.cannaapp.ui.components.SectionLabel
import cz.cannaclub.cannaapp.ui.components.iconRes
import cz.cannaclub.cannaapp.ui.theme.BorderSoft
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.Honey
import cz.cannaclub.cannaapp.ui.theme.Paper
import cz.cannaclub.cannaapp.ui.theme.PointsGreen
import cz.cannaclub.cannaapp.ui.theme.PointsRed
import cz.cannaclub.cannaapp.ui.theme.TextFaint
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import cz.cannaclub.cannaapp.ui.theme.TextPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val czech   = Locale("cs", "CZ")
private val timeFmt = SimpleDateFormat("HH:mm", czech)
private val dayFmt  = SimpleDateFormat("EEEE d. MMMM", czech)
private val yearFmt = SimpleDateFormat("d. MMMM yyyy", czech)
private const val PREVIEW_COUNT = 8
private const val DAY_MS = 24L * 60 * 60 * 1000

/**
 * Pohyby bodů na hlavní obrazovce: souhrn za 30 dní + časová osa po dnech.
 * Ukáže prvních [PREVIEW_COUNT] pohybů, zbytek po klepnutí na "Zobrazit starší".
 */
fun LazyListScope.pointsHistory(
    transactions: List<Transaction>,
    expanded: Boolean,
    onExpand: () -> Unit
) {
    item(key = "hist_head") {
        SectionLabel("Pohyby bodů")
        Spacer(Modifier.height(10.dp))
        HistorySummary(transactions)
        Spacer(Modifier.height(18.dp))
    }
    val visible = if (expanded) transactions else transactions.take(PREVIEW_COUNT)
    groupByDay(visible).forEach { (label, dayTx) ->
        item(key = "h_$label") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.padding(start = 4.dp, bottom = 8.dp)
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(TextFaint))
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = label,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextMuted
                )
                Spacer(Modifier.weight(1f))
                val sum = dayTx.sumOf { if (it.isPositive) it.amount else -it.amount }
                Text(
                    text  = if (sum >= 0) "+$sum b" else "−${-sum} b",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextFaint
                )
            }
        }
        item(key = "d_$label") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Paper)
                    .border(1.dp, BorderSoft, RoundedCornerShape(22.dp))
            ) {
                dayTx.forEachIndexed { i, tx ->
                    if (i > 0) HorizontalDivider(color = BorderSoft, thickness = 1.dp, modifier = Modifier.padding(start = 70.dp))
                    TransactionRow(tx)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (!expanded && transactions.size > PREVIEW_COUNT) {
        item(key = "hist_more") {
            Text(
                text     = "Zobrazit starší pohyby (${transactions.size - PREVIEW_COUNT})",
                style    = MaterialTheme.typography.labelLarge,
                color    = TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
                    .clickable(onClick = onExpand)
                    .padding(vertical = 14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/** Dvě dlaždice: kolik bodů přibylo a kolik se uplatnilo za posledních 30 dní. */
@Composable
private fun HistorySummary(transactions: List<Transaction>) {
    val since  = System.currentTimeMillis() - 30 * DAY_MS
    val recent = transactions.filter { it.createdAt.toDate().time >= since }
    val gained = recent.filter { it.isPositive }.sumOf { it.amount }
    val spent  = recent.filter { !it.isPositive }.sumOf { it.amount }
    val visits = recent.count { it.isPositive && it.reason.startsWith("Nákup", ignoreCase = true) }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("Získáno", "+$gained", PointsGreen, Modifier.weight(1f))
        StatTile("Uplatněno", if (spent > 0) "−$spent" else "0", PointsRed, Modifier.weight(1f))
        StatTile("Nákupů", "$visits", TextPrimary, Modifier.weight(1f))
    }
    Spacer(Modifier.height(6.dp))
    Text("za posledních 30 dní", style = MaterialTheme.typography.bodySmall, color = TextFaint, modifier = Modifier.padding(start = 4.dp))
}

@Composable
private fun StatTile(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(CardDefault)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text  = value,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 24.sp, lineHeight = 28.sp),
            color = color,
            maxLines = 1
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionRow(transaction: Transaction) {
    val positive = transaction.isPositive
    val color    = if (positive) PointsGreen else PointsRed
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(
            id         = transaction.iconRes,
            tint       = color,
            background = color.copy(alpha = 0.1f),
            size       = 42.dp,
            iconSize   = 19.dp
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = transaction.reason.ifBlank { if (positive) "Připsáno" else "Uplatněno" },
                style    = MaterialTheme.typography.bodyLarge,
                color    = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(4.dp)
            ) {
                Text(timeFmt.format(transaction.createdAt.toDate()), style = MaterialTheme.typography.bodySmall, color = TextFaint)
                if (transaction.promoBonus > 0) {
                    BonusChip("+${transaction.promoBonus} ${transaction.promoLabel ?: "akce"}", Honey)
                }
                if (transaction.rankBonus > 0) {
                    BonusChip("+${transaction.rankBonus} rank ${transaction.rankLabel ?: ""}".trimEnd(), PointsGreen)
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text     = transaction.formattedAmount,
            style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color    = color,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.09f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun BonusChip(text: String, color: Color) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
        color    = color,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    )
}

/** Seskupí pohyby podle dne: "Dnes", "Včera", "Pondělí 15. září"… (zachová pořadí). */
private fun groupByDay(list: List<Transaction>): List<Pair<String, List<Transaction>>> {
    val now = Calendar.getInstance()
    fun label(tx: Transaction): String {
        val d = Calendar.getInstance().apply { time = tx.createdAt.toDate() }
        val sameYear = d.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        val diff = now.get(Calendar.DAY_OF_YEAR) - d.get(Calendar.DAY_OF_YEAR)
        return when {
            sameYear && diff == 0 -> "Dnes"
            sameYear && diff == 1 -> "Včera"
            sameYear              -> dayFmt.format(d.time).replaceFirstChar { it.uppercase() }
            else                  -> yearFmt.format(d.time)
        }
    }
    return list.groupBy(::label).toList()
}
