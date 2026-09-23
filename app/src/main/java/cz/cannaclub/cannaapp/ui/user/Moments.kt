package cz.cannaclub.cannaapp.ui.user

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.cannaclub.cannaapp.R
import cz.cannaclub.cannaapp.ui.components.CannaIcon
import cz.cannaclub.cannaapp.ui.components.EARN_KC_PER_POINT
import cz.cannaclub.cannaapp.ui.theme.Brand
import cz.cannaclub.cannaapp.ui.theme.BorderNormal
import cz.cannaclub.cannaapp.ui.theme.Honey
import cz.cannaclub.cannaapp.ui.theme.OnBrand
import cz.cannaclub.cannaapp.ui.theme.PillBackground
import cz.cannaclub.cannaapp.ui.theme.RankSprout
import cz.cannaclub.cannaapp.ui.theme.Sage
import cz.cannaclub.cannaapp.ui.theme.SageGlow
import cz.cannaclub.cannaapp.ui.theme.TextFaint
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import cz.cannaclub.cannaapp.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// Oslava: přibyly body → padající lístky, vibrace, velké "+64 b"
// ─────────────────────────────────────────────────────────────────────────────

private class Leaf(
    val x: Float, val delay: Float, val speed: Float, val size: Float,
    val spin: Float, val sway: Float, val color: Color
)

@Composable
fun PointsCelebration(gained: Int, onFinished: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val t      = remember { Animatable(0f) }
    val pop    = remember { Animatable(0.4f) }
    val leaves = remember {
        val palette = listOf(Brand, Sage, RankSprout, Honey, Honey.copy(alpha = 0.8f))
        List(46) {
            Leaf(
                x     = Random.nextFloat(),
                delay = Random.nextFloat() * 0.35f,
                speed = 0.7f + Random.nextFloat() * 0.6f,
                size  = 9f + Random.nextFloat() * 12f,
                spin  = Random.nextFloat() * 720f - 360f,
                sway  = Random.nextFloat() * 60f - 30f,
                color = palette.random()
            )
        }
    }

    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        launch { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
        launch { t.animateTo(1f, tween(durationMillis = 3200, easing = LinearEasing)) }
        delay(4200)
        onFinished()
    }

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onFinished() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            leaves.forEach { l ->
                val p = ((t.value - l.delay) / (1f - l.delay) * l.speed).coerceIn(0f, 1.2f)
                if (p <= 0f) return@forEach
                val y  = -40f + p * (size.height + 80f)
                val x  = l.x * size.width + kotlin.math.sin(p * 9f) * l.sway
                val w  = l.size * density / 2.2f
                rotate(degrees = l.spin * p, pivot = Offset(x, y)) {
                    drawOval(
                        color   = l.color.copy(alpha = l.color.alpha * (1f - (p - 0.85f).coerceAtLeast(0f) * 4f).coerceIn(0f, 1f)),
                        topLeft = Offset(x - w, y - w * 0.45f),
                        size    = Size(w * 2f, w * 0.9f)
                    )
                }
            }
        }

        Column(
            modifier            = Modifier
                .scale(pop.value)
                .padding(horizontal = 40.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(PillBackground)
                .padding(horizontal = 36.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier         = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Honey.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                CannaIcon(id = R.drawable.ic_sparkle, tint = Honey, size = 28.dp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text  = "+$gained b",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 56.sp, lineHeight = 60.sp),
                color = Sage
            )
            Text(
                text      = "Body ti přibyly. Díky za nákup!",
                style     = MaterialTheme.typography.bodyMedium,
                color     = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Úvod "Jak to funguje" — 3 obrazovky, ukáže se jednou po prvním přihlášení
// ─────────────────────────────────────────────────────────────────────────────

private data class IntroPage(val icon: Int, val title: String, val text: String)

private val introPages = listOf(
    IntroPage(
        R.drawable.ic_barcode,
        "Tvoje kartička je v telefonu",
        "U pokladny klepni na Ukázat kartičku a prodavač ji naskenuje. Nic dalšího nosit nemusíš."
    ),
    IntroPage(
        R.drawable.ic_bag,
        "Nakupuj a sbírej",
        "Za každých $EARN_KC_PER_POINT Kč dostaneš 1 bod. Připíšou se samy do pár minut a přijde ti upozornění."
    ),
    IntroPage(
        R.drawable.ic_gift,
        "Body jsou peníze",
        "1 bod = 1 Kč. Vyměň je za slevu na cokoli, nebo za odměnu. Čím víc nasbíráš, tím vyšší rank."
    )
)

@Composable
fun OnboardingOverlay(onFinished: () -> Unit) {
    val pager = rememberPagerState { introPages.size }
    val scope = rememberCoroutineScope()
    val last  = pager.currentPage == introPages.lastIndex

    BackHandler { onFinished() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PillBackground)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Text(
            text     = "Přeskočit",
            style    = MaterialTheme.typography.bodyMedium,
            color    = TextFaint,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onFinished() }
                .padding(horizontal = 20.dp, vertical = 16.dp)
        )

        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                val p = introPages[page]
                Column(
                    modifier            = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier         = Modifier
                            .size(132.dp)
                            .clip(CircleShape)
                            .background(SageGlow),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(92.dp)
                                .clip(CircleShape)
                                .background(Sage),
                            contentAlignment = Alignment.Center
                        ) {
                            CannaIcon(id = p.icon, tint = OnBrand, size = 44.dp)
                        }
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        text      = p.title,
                        style     = MaterialTheme.typography.headlineLarge,
                        color     = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text      = p.text,
                        style     = MaterialTheme.typography.bodyLarge,
                        color     = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Tečky
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(introPages.size) { i ->
                    val w by animateDpAsState(if (i == pager.currentPage) 24.dp else 8.dp, label = "dot")
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(w)
                            .clip(CircleShape)
                            .background(if (i == pager.currentPage) Sage else BorderNormal)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick  = {
                    if (last) onFinished()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .height(56.dp),
                shape    = RoundedCornerShape(18.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Sage, contentColor = OnBrand)
            ) {
                Text(
                    text  = if (last) "Začít sbírat" else "Další",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
