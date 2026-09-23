package cz.cannaclub.cannaapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.cannaclub.cannaapp.R
import cz.cannaclub.cannaapp.model.MemberRank
import cz.cannaclub.cannaapp.ui.theme.Brand
import cz.cannaclub.cannaapp.ui.theme.Forest
import cz.cannaclub.cannaapp.ui.theme.OnBrand
import cz.cannaclub.cannaapp.ui.theme.Sage
import kotlin.math.roundToInt

/** Kolik Kč slevy má 1 bod. Za kolik Kč útraty se 1 bod získá, je v functions/.env (KC_PER_POINT). */
const val POINT_VALUE_KC = 1

/**
 * Hlavní karta zákazníka — vypadá jako fyzická členská karta.
 * Body (animovaně), jejich hodnota v Kč, rank a postup k dalšímu ranku.
 * Klepnutí otevře odměny.
 */
@Composable
fun PointsCard(
    points: Int,
    totalPoints: Int,
    onRewardsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animated = remember { Animatable(points.toFloat()) }
    LaunchedEffect(points) {
        animated.animateTo(points.toFloat(), tween(durationMillis = 1100, easing = FastOutSlowInEasing))
    }

    val rank     = MemberRank.forPoints(totalPoints)
    val next     = rank.next
    val progress by animateFloatAsState(
        targetValue   = rank.progress(totalPoints),
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label         = "rankProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Brand, Sage, Forest)))
            .clickable { onRewardsClick() }
    ) {
        // Vodoznak — velký list v rohu
        CannaIcon(
            id       = R.drawable.ic_leaf,
            tint     = OnBrand.copy(alpha = 0.07f),
            size     = 190.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 46.dp, y = (-30).dp)
                .rotate(-18f)
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                SectionLabel("Věrnostní body", color = OnBrand.copy(alpha = 0.72f))
                RankChip(rank)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text  = animated.value.roundToInt().toString(),
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 64.sp, lineHeight = 70.sp),
                    color = OnBrand
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text     = "b",
                    style    = MaterialTheme.typography.headlineMedium,
                    color    = OnBrand.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Text(
                text  = "= ${points * POINT_VALUE_KC} Kč sleva na cokoli",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = OnBrand.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Postup k dalšímu ranku
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(OnBrand.copy(alpha = 0.18f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(OnBrand)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = if (next != null) "Do ranku ${next.label} chybí ${next.requiredPoints - totalPoints} b"
                            else "Nejvyšší rank, patříš do rodiny",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBrand.copy(alpha = 0.75f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = "Odměny",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = OnBrand
                    )
                    CannaIcon(id = R.drawable.ic_chevron_right, tint = OnBrand, size = 16.dp)
                }
            }
        }
    }
}

@Composable
private fun RankChip(rank: MemberRank) {
    Row(
        modifier          = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(OnBrand.copy(alpha = 0.14f))
            .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RankBadge(rank = rank, size = 20.dp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text  = rank.label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = OnBrand
        )
    }
}
