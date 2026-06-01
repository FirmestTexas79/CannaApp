package cz.cannaclub.cannaapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import cz.cannaclub.cannaapp.ui.theme.LeafDecor

/**
 * Dekorativní botanické prvky — listy sized přesně do okrajových pásů pillu.
 *
 * Klíčové: délka listu = ~9 % výšky obrazovky ≈ 72 dp na typickém telefonu.
 * Tím se listy vejdou do viditelného okraje za pillem a vytváří peek efekt.
 *
 * Pás vrchu/spodu = PillVerticalPadding = 72 dp
 * Pás stran       = PillHorizontalPadding = 28 dp
 */
@Composable
fun DecorativePlants(
    modifier: Modifier = Modifier,
    color: Color = LeafDecor
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Listy calibrované na okrajové pásy pillu
        // vL = délka listu pro horní/spodní pás (~9 % výšky = ~72 dp)
        // hL = délka pro boční pás (~8 % šířky = ~29 dp)
        val vL = h * 0.09f
        val vW = vL * 0.26f
        val hL = w * 0.095f
        val hW = hL * 0.26f

        clipRect {

            // ── Spodní-levý cluster ────────────────────────────────
            // Fan 3 listů vyrůstajících ze spodního levého rohu nahoru
            drawLeaf(0f,          h, vL * 1.15f, vW * 1.15f, -62f, color)
            drawLeaf(0f,          h, vL,          vW,          -42f, color)
            drawLeaf(w * 0.05f,   h, vL * 0.82f,  vW * 0.82f, -24f, color)
            // Malý boční list z levého okraje (ve spodní třetině)
            drawLeaf(0f, h * 0.72f, hL, hW, 32f,
                color.copy(alpha = color.alpha * 0.70f))

            // ── Horní-pravý cluster ────────────────────────────────
            drawLeaf(w,          0f, vL * 1.15f, vW * 1.15f, 118f, color)
            drawLeaf(w,          0f, vL,          vW,          138f, color)
            drawLeaf(w * 0.95f,  0f, vL * 0.82f,  vW * 0.82f, 156f, color)
            // Malý boční list z pravého okraje (v horní třetině)
            drawLeaf(w, h * 0.28f, hL, hW, -148f,
                color.copy(alpha = color.alpha * 0.70f))

            // ── Spodní-pravý roh (menší, subtilní) ────────────────
            drawLeaf(w, h, vL * 0.72f, vW * 0.72f, 158f,
                color.copy(alpha = color.alpha * 0.55f))
            drawLeaf(w, h, vL * 0.55f, vW * 0.55f, 138f,
                color.copy(alpha = color.alpha * 0.40f))

            // ── Horní-levý roh (menší, subtilní) ──────────────────
            drawLeaf(0f, 0f, vL * 0.68f, vW * 0.68f, -22f,
                color.copy(alpha = color.alpha * 0.52f))
            drawLeaf(0f, 0f, vL * 0.50f, vW * 0.50f, -5f,
                color.copy(alpha = color.alpha * 0.38f))
        }
    }
}

/**
 * Kreslí jeden organický list kotveným v (pivotX, pivotY).
 * List roste od pivotu NAHORU (záporné Y) a pak se otočí o [angle] stupňů.
 */
private fun DrawScope.drawLeaf(
    pivotX: Float,
    pivotY: Float,
    length: Float,
    width: Float,
    angle: Float,
    color: Color
) {
    rotate(degrees = angle, pivot = Offset(pivotX, pivotY)) {
        val hw = width * 0.5f

        val body = Path().apply {
            moveTo(pivotX, pivotY)
            cubicTo(
                pivotX - hw * 1.15f, pivotY - length * 0.25f,
                pivotX - hw * 1.20f, pivotY - length * 0.65f,
                pivotX,               pivotY - length
            )
            cubicTo(
                pivotX + hw * 1.20f, pivotY - length * 0.65f,
                pivotX + hw * 1.15f, pivotY - length * 0.25f,
                pivotX,               pivotY
            )
            close()
        }
        drawPath(path = body, color = color)

        // Středová žilka
        val vein = Path().apply {
            moveTo(pivotX, pivotY)
            cubicTo(
                pivotX + hw * 0.12f, pivotY - length * 0.30f,
                pivotX + hw * 0.08f, pivotY - length * 0.65f,
                pivotX,               pivotY - length
            )
        }
        drawPath(
            path  = vein,
            color = color.copy(alpha = color.alpha * 0.30f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
        )
    }
}