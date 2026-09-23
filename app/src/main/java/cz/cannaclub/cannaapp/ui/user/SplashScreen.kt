package cz.cannaclub.cannaapp.ui.user

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cz.cannaclub.cannaapp.R
import cz.cannaclub.cannaapp.ui.theme.Background
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Stejný sharpen matrix jako na dashboardu
private val splashSharpenMatrix = ColorMatrix(
    floatArrayOf(
        1.5f,  0f,   0f,   0f, -30f,
        0f,    1.5f, 0f,   0f, -30f,
        0f,    0f,   1.5f, 0f, -30f,
        0f,    0f,   0f,   1f,   0f
    )
)

@Composable
fun SplashScreen(onFinished: () -> Unit) {

    // ── Animační hodnoty ──────────────────────────────────
    val alpha  = remember { Animatable(0f) }
    val scale  = remember { Animatable(0.82f) }
    val fadeOut = remember { Animatable(1f) }

    val naturalEasing = CubicBezierEasing(0.25f, 0.46f, 0.45f, 0.94f)

    LaunchedEffect(Unit) {
        // 1. Fade in + scale up — logo se objeví organicky
        launch {
            alpha.animateTo(
                targetValue   = 1f,
                animationSpec = tween(
                    durationMillis = 700,
                    easing         = naturalEasing
                )
            )
        }
        scale.animateTo(
            targetValue   = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing         = naturalEasing
            )
        )

        // 2. Chvilku zůstane viditelné
        delay(900)

        // 3. Jemný fade out celé obrazovky
        fadeOut.animateTo(
            targetValue   = 0f,
            animationSpec = tween(
                durationMillis = 500,
                easing         = naturalEasing
            )
        )

        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(fadeOut.value)
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter            = painterResource(id = R.drawable.cannalogo),
            contentDescription = "Cannaclub",
            modifier           = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .alpha(alpha.value)
                .scale(scale.value),
            contentScale       = ContentScale.Fit,
            colorFilter        = ColorFilter.colorMatrix(splashSharpenMatrix)
        )
    }
}