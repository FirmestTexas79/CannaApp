package cz.cannaclub.cannaapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cz.cannaclub.cannaapp.R

// ── 1. Fredoka ─────────────────────────────────────────────────────────────────────
private val fredokaFamily = FontFamily(
    Font(resId = R.font.fredoka_semibold, weight = FontWeight.SemiBold)
)

// ── 2. Playfair Display ────────────────────────────────────────────────────────────
private val playfairFamily = FontFamily(
    Font(resId = R.font.playfair_display, weight = FontWeight.Normal),
    Font(resId = R.font.playfair_display, weight = FontWeight.SemiBold),
    Font(resId = R.font.playfair_display, weight = FontWeight.Bold)
)

// ── 3. DM Sans ─────────────────────────────────────────────────────────────────────
private val dmSansFamily = FontFamily(
    Font(resId = R.font.dm_sans, weight = FontWeight.Light),
    Font(resId = R.font.dm_sans, weight = FontWeight.Normal),
    Font(resId = R.font.dm_sans, weight = FontWeight.Medium),
    Font(resId = R.font.dm_sans, weight = FontWeight.SemiBold)
)

// ── Typography ─────────────────────────────────────────────────────────────────────
val CannaTypography = Typography(

    // Velký titulek — název appky, jméno zákazníka
    displayLarge = TextStyle(
        fontFamily = playfairFamily,
        fontWeight = FontWeight.Bold,
        fontSize   = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp
    ),

    // Counter bodů
    displayMedium = TextStyle(
        fontFamily = playfairFamily,
        fontWeight = FontWeight.Bold,
        fontSize   = 72.sp,
        lineHeight = 72.sp,
        letterSpacing = (-3).sp
    ),

    // Sekce titulky — "Zákazníci", "Odměny"
    headlineLarge = TextStyle(
        fontFamily = playfairFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 28.sp,
        lineHeight = 34.sp
    ),

    // Název produktu v detailu popupu (Fredoka)
    headlineMedium = TextStyle(
        fontFamily = fredokaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 24.sp,
        lineHeight = 30.sp
    ),

    // Název produktu v ProductCard kartě (Fredoka)
    titleLarge = TextStyle(
        fontFamily = fredokaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 22.sp,
        lineHeight = 28.sp
    ),

    // Jméno v pillce, název transakce
    bodyLarge = TextStyle(
        fontFamily = dmSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 15.sp,
        lineHeight = 22.sp
    ),

    // Běžný text, email, popis
    bodyMedium = TextStyle(
        fontFamily = dmSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 20.sp
    ),

    // Datum transakce, drobný popis
    bodySmall = TextStyle(
        fontFamily = dmSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        lineHeight = 16.sp
    ),

    // Tlačítka
    labelLarge = TextStyle(
        fontFamily = dmSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 14.sp,
        letterSpacing = 1.2.sp
    ),

    // Uppercase labely nad inputy
    labelSmall = TextStyle(
        fontFamily = dmSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize   = 10.sp,
        letterSpacing = 1.8.sp
    )
)