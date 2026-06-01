package cz.cannaclub.cannaapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tmavé téma — warm dark s Cannaclub branding
private val CannaColorScheme = darkColorScheme(
    primary             = Sage,
    onPrimary           = Cream,
    secondary           = Gold,
    onSecondary         = Background,
    background          = Background,
    onBackground        = TextPrimary,
    surface             = Surface,
    onSurface           = TextPrimary,
    surfaceVariant      = CardDefault,
    onSurfaceVariant    = TextMuted,
    outline             = BorderNormal,
    error               = PointsRed,
    onError             = Cream,
    surfaceContainer    = PillBackground,
    surfaceContainerHigh = CardDefault,
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