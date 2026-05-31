package cz.cannaclub.cannaapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.cannaclub.cannaapp.ui.theme.BorderNormal
import cz.cannaclub.cannaapp.ui.theme.Sage
import cz.cannaclub.cannaapp.ui.theme.SageDim
import cz.cannaclub.cannaapp.ui.theme.SageGlow
import cz.cannaclub.cannaapp.ui.theme.SageLight
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import cz.cannaclub.cannaapp.ui.theme.TextPrimary
import kotlin.math.roundToInt

@Composable
fun PointsCard(
    points: Int,
    onRewardsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedValue = remember { Animatable(0f) }

    LaunchedEffect(points) {
        animatedValue.animateTo(
            targetValue   = points.toFloat(),
            animationSpec = tween(
                durationMillis = 900,
                easing         = FastOutSlowInEasing
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
    ) {
        // Zelený top border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(SageLight, Sage, SageDim)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp)
                .padding(horizontal = 26.dp, vertical = 24.dp)
        ) {

            // Label + odkaz na odměny
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = "VĚRNOSTNÍ BODY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text     = "Odměny →",
                    style    = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color    = Sage,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SageGlow)
                        .clickable { onRewardsClick() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Animovaný counter
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text  = animatedValue.value.roundToInt().toString(),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize      = 68.sp,
                        letterSpacing = (-2).sp
                    ),
                    color = Sage
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text     = "b",
                    style    = MaterialTheme.typography.bodyLarge.copy(
                        fontSize  = 22.sp,
                        fontStyle = FontStyle.Italic
                    ),
                    color    = SageDim,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            // Oddělovač
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderNormal)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Hodnota v Kč
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = "Hodnota slevy",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Text(
                    text  = "$points Kč",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
        }
    }
}