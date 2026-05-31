package cz.cannaclub.cannaapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Světlé téma — béžové pozadí jako Cannaclub
private val CannaColorScheme = lightColorScheme(
    primary          = Sage,
    onPrimary        = Color(0xFFF0EBE3),
    secondary        = Gold,
    onSecondary      = Color(0xFFF0EBE3),
    background       = Background,
    onBackground     = TextPrimary,
    surface          = Surface,
    onSurface        = TextPrimary,
    surfaceVariant   = CardDefault,
    onSurfaceVariant = TextMuted,
    outline          = BorderNormal,
    error            = PointsRed,
)

@Composable
fun CannaAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CannaColorScheme,
        typography  = CannaTypography,
        content     = content
    )
}