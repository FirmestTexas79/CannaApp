package cz.cannaclub.cannaapp.ui.components

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
