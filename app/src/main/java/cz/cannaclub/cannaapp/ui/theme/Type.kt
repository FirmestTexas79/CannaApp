package cz.cannaclub.cannaapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import cz.cannaclub.cannaapp.R

// ── Google Fonts provider ─────────────────────────────────
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)

// ── Playfair Display — titulky, counter bodů ──────────────
private val playfairFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Playfair Display"),
        fontProvider = provider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("Playfair Display"),
        fontProvider = provider,
        weight = FontWeight.SemiBold
    ),
    Font(
        googleFont = GoogleFont("Playfair Display"),
        fontProvider = provider,
        weight = FontWeight.Bold
    ),
    Font(
        googleFont = GoogleFont("Playfair Display"),
        fontProvider = provider,
        weight = FontWeight.Normal,
        style = FontStyle.Italic
    )
)

// ── DM Sans — tělo, labely, tlačítka ─────────────────────
private val dmSansFamily = FontFamily(
    Font(
        googleFont = GoogleFont("DM Sans"),
        fontProvider = provider,
        weight = FontWeight.Light
    ),
    Font(
        googleFont = GoogleFont("DM Sans"),
        fontProvider = provider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("DM Sans"),
        fontProvider = provider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = GoogleFont("DM Sans"),
        fontProvider = provider,
        weight = FontWeight.SemiBold
    )
)

// ── Typography ────────────────────────────────────────────
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

    // Popup titulky
    headlineMedium = TextStyle(
        fontFamily = playfairFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 24.sp,
        lineHeight = 30.sp
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
        fontWeight = FontWeight.Light,
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