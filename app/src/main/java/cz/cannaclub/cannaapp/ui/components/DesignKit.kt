package cz.cannaclub.cannaapp.ui.components

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import cz.cannaclub.cannaapp.R
import cz.cannaclub.cannaapp.model.MemberRank
import cz.cannaclub.cannaapp.model.Transaction
import cz.cannaclub.cannaapp.model.TransactionType
import cz.cannaclub.cannaapp.ui.theme.BorderNormal
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.RankBronze
import cz.cannaclub.cannaapp.ui.theme.RankFamily
import cz.cannaclub.cannaapp.ui.theme.RankGold
import cz.cannaclub.cannaapp.ui.theme.RankSilver
import cz.cannaclub.cannaapp.ui.theme.RankSprout
import cz.cannaclub.cannaapp.ui.theme.TextFaint
import cz.cannaclub.cannaapp.ui.theme.TextMuted

/**
 * Sdílené stavební kameny vzhledu: ikony, odznaky ranků, popisky sekcí.
 * Ikony jsou vlastní vektory v res/drawable/ic_*.xml (tahové, 24 × 24),
 * takže vypadají na všech telefonech stejně — na rozdíl od emoji.
 */

/** Kolik Kč útraty = 1 bod. Musí sedět s KC_PER_POINT ve functions/.env. */
const val EARN_KC_PER_POINT = 10

@Composable
fun CannaIcon(
    @DrawableRes id: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    contentDescription: String? = null
) {
    Icon(
        painter            = painterResource(id),
        contentDescription = contentDescription,
        tint               = tint,
        modifier           = modifier.size(size)
    )
}

/** Ikona v jemně podbarveném kruhu — řádky seznamů, dlaždice. */
@Composable
fun IconBubble(
    @DrawableRes id: Int,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    Box(
        modifier         = modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        CannaIcon(id = id, tint = tint, size = iconSize)
    }
}

/** Malý velký-písmeny popisek nad sekcí. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = TextMuted) {
    Text(
        text     = text.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        color    = color,
        modifier = modifier
    )
}

// ── Ranky ────────────────────────────────────────────────────────────────────

val MemberRank.color: Color
    get() = when (this) {
        MemberRank.ZAKAZNIK -> RankSprout
        MemberRank.BRONZOVY -> RankBronze
        MemberRank.STRIBRNY -> RankSilver
        MemberRank.ZLATY    -> RankGold
        MemberRank.RODINA   -> RankFamily
    }

@get:DrawableRes
val MemberRank.iconRes: Int
    get() = when (this) {
        MemberRank.ZAKAZNIK -> R.drawable.ic_bud
        MemberRank.RODINA   -> R.drawable.ic_sparkle
        else                -> R.drawable.ic_leaf
    }

/**
 * Odznak ranku: kruh v barvě ranku s jemným leskem a ikonou.
 * [locked] = rank ještě není odemčený → jen obrys.
 */
@Composable
fun RankBadge(
    rank: MemberRank,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    locked: Boolean = false
) {
    val c = rank.color
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (locked) Modifier
                    .background(CardDefault)
                    .border(1.5.dp, BorderNormal, CircleShape)
                else Modifier.background(
                    Brush.linearGradient(listOf(lerp(c, Color.White, 0.28f), c, lerp(c, Color.Black, 0.18f)))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        CannaIcon(
            id   = rank.iconRes,
            tint = if (locked) TextFaint else Color.White,
            size = size * 0.46f
        )
    }
}

// ── Pohyby bodů ──────────────────────────────────────────────────────────────

@get:DrawableRes
val Transaction.iconRes: Int
    get() = when {
        type == TransactionType.SUBTRACT            -> R.drawable.ic_gift
        reason.startsWith("Nákup", ignoreCase = true) -> R.drawable.ic_bag
        else                                        -> R.drawable.ic_sparkle
    }

// ── Systémové lišty ──────────────────────────────────────────────────────────

/**
 * Barva ikon ve stavové a navigační liště podle pozadí obrazovky.
 * Světlé pozadí (zákazník) → tmavé ikony, tmavé (admin) → světlé.
 */
@Composable
fun SystemBarsAppearance(lightBackground: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(lightBackground) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = lightBackground
                isAppearanceLightNavigationBars = lightBackground
            }
        }
        onDispose { }
    }
}

// ── Google Peněženka ─────────────────────────────────────────────────────────

/**
 * Tlačítko "Přidat do Peněženky Google" podle pravidel Googlu:
 * tmavá pilulka + oficiální česká grafika (res/drawable/wallet_button_content_cs.xml, neupravovat).
 */
@Composable
fun AddToWalletButton(onClick: () -> Unit, modifier: Modifier = Modifier, busy: Boolean = false) {
    Box(
        modifier         = modifier
            .height(48.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .background(Color(0xFF1F1F1F))
            .border(1.dp, Color(0xFF747775), androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .clickable(enabled = !busy) { onClick() }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (busy) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier    = Modifier.size(22.dp),
                color       = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            androidx.compose.foundation.Image(
                painter            = painterResource(R.drawable.wallet_button_content_cs),
                contentDescription = "Přidat do Peněženky Google",
                modifier           = Modifier.height(24.dp)
            )
        }
    }
}

// ── Čas konce akce česky ─────────────────────────────────────────────────────

/** "dnes do 23:59", "zítra do 18:00", "do neděle 23:59", "do 3. 10. 23:59". */
fun czechUntil(millis: Long, now: Long = System.currentTimeMillis()): String {
    val end = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    val today = java.util.Calendar.getInstance().apply { timeInMillis = now }
    val time = String.format(java.util.Locale.ROOT, "%d:%02d", end.get(java.util.Calendar.HOUR_OF_DAY), end.get(java.util.Calendar.MINUTE))
    val days = run {
        fun dayNo(c: java.util.Calendar) = c.get(java.util.Calendar.YEAR) * 400 + c.get(java.util.Calendar.DAY_OF_YEAR)
        dayNo(end) - dayNo(today)
    }
    // 2. pád dní v týdnu (do pondělí, do středy…)
    val genitive = mapOf(
        java.util.Calendar.MONDAY to "pondělí", java.util.Calendar.TUESDAY to "úterý",
        java.util.Calendar.WEDNESDAY to "středy", java.util.Calendar.THURSDAY to "čtvrtka",
        java.util.Calendar.FRIDAY to "pátku", java.util.Calendar.SATURDAY to "soboty",
        java.util.Calendar.SUNDAY to "neděle"
    )
    return when {
        days <= 0 -> "dnes do $time"
        days == 1 -> "zítra do $time"
        days < 7  -> "do ${genitive[end.get(java.util.Calendar.DAY_OF_WEEK)]} $time"
        else      -> "do ${end.get(java.util.Calendar.DAY_OF_MONTH)}. ${end.get(java.util.Calendar.MONTH) + 1}. $time"
    }
}
