package cz.cannaclub.cannaapp.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import cz.cannaclub.cannaapp.ui.theme.TextMuted

/**
 * Členský kód: jeden čárový kód (Code 128) + číslo pod ním.
 * Code 128 přečte laserová i 2D čtečka. Když čtečka nezabere, prodavač číslo opíše.
 * Pokladna Dotykačka podle pole "Čárový kód" sama načte zákazníka na účet.
 *
 * Zákazník: MemberCardOverlay (celá obrazovka). Obsluha: MemberBarcodeCompact v kartě zákazníka.
 */

@Composable
internal fun CodeImages(barcode: Bitmap?, height: Int, code: String, showDigits: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (barcode != null) {
            Image(
                bitmap             = barcode.asImageBitmap(),
                contentDescription = "Čárový kód člena",
                modifier           = Modifier
                    .fillMaxWidth()
                    .height(height.dp),
                contentScale       = ContentScale.FillBounds,
                filterQuality      = FilterQuality.None   // ostré hrany → lepší čitelnost
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height.dp)
                    .background(androidx.compose.ui.graphics.Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) { Text(text = "⚠️", fontSize = 32.sp) }
        }
        if (showDigits) {
            Text(
                text       = code.chunked(4).joinToString(" "),
                fontFamily = FontFamily.Monospace,
                fontSize   = 18.sp,
                color      = androidx.compose.ui.graphics.Color.Black
            )
        }
    }
}

/**
 * Celá obrazovka bílá + jas na maximum + displej nezhasne.
 * Čtečky tak displej přečtou mnohem líp.
 */
@Composable
internal fun FullscreenCode(
    barcode: Bitmap?,
    code: String,
    showDigits: Boolean,
    holderName: String? = null,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(window) {
            window?.let { w ->
                val attrs = w.attributes
                attrs.screenBrightness = 1f
                w.attributes = attrs
                w.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            }
            onDispose { }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.White)
                .clickable { onDismiss() }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.foundation.Image(
                    painter            = androidx.compose.ui.res.painterResource(cz.cannaclub.cannaapp.R.drawable.canna_wordmark),
                    contentDescription = "CannaClub",
                    modifier           = Modifier.height(26.dp)
                )
                if (!holderName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text  = holderName,
                        style = MaterialTheme.typography.titleLarge,
                        color = androidx.compose.ui.graphics.Color.Black
                    )
                }
                Spacer(modifier = Modifier.height(36.dp))
                CodeImages(barcode = barcode, height = 180, code = code, showDigits = showDigits)
                Spacer(modifier = Modifier.height(36.dp))
                Text(
                    text      = if (showDigits) "Ukaž prodavači k naskenování"
                                else "Kód pro pokladnu se ještě připravuje, zkus to za chvíli",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = TextMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text  = "Klepnutím zavřeš",
                    style = MaterialTheme.typography.bodySmall,
                    color = cz.cannaclub.cannaapp.ui.theme.TextFaint
                )
            }
        }
    }
}

/**
 * Členská kartička přes celou obrazovku — z hlavní obrazovky i ze zkratky na ikoně.
 * Kreslí se přímo v okně aktivity (ne jako dialog), takže bílá sahá i pod stavovou lištu.
 * Po dobu zobrazení: jas na maximum, displej nezhasne. Zpět nebo klepnutí zavře.
 */
@Composable
fun MemberCardOverlay(code: String, ready: Boolean, holderName: String, onDismiss: () -> Unit) {
    val barcode = remember(code) { renderCode(code, BarcodeFormat.CODE_128, 800, 200) }
    val view    = LocalView.current

    androidx.activity.compose.BackHandler { onDismiss() }
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        val before = window?.attributes?.screenBrightness
        window?.let { w ->
            w.attributes = w.attributes.apply { screenBrightness = 1f }
            w.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.let { w ->
                w.attributes = w.attributes.apply {
                    screenBrightness = before ?: android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
                w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.White)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication        = null
            ) { onDismiss() }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.foundation.Image(
                painter            = androidx.compose.ui.res.painterResource(cz.cannaclub.cannaapp.R.drawable.canna_wordmark),
                contentDescription = "CannaClub",
                modifier           = Modifier.height(26.dp)
            )
            if (holderName.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text  = holderName,
                    style = MaterialTheme.typography.titleLarge,
                    color = androidx.compose.ui.graphics.Color.Black
                )
            }
            Spacer(modifier = Modifier.height(36.dp))
            CodeImages(barcode = barcode, height = 180, code = code, showDigits = ready)
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text      = if (ready) "Ukaž prodavači k naskenování"
                            else "Kód pro pokladnu se ještě připravuje, zkus to za chvíli",
                style     = MaterialTheme.typography.bodyMedium,
                color     = TextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text  = "Klepnutím zavřeš",
                style = MaterialTheme.typography.bodySmall,
                color = cz.cannaclub.cannaapp.ui.theme.TextFaint
            )
        }
    }
}

internal fun renderCode(text: String, format: BarcodeFormat, width: Int, height: Int): Bitmap? = try {
    val hints = mapOf<EncodeHintType, Any>(EncodeHintType.MARGIN to 10)   // tichá zóna kolem kódu
    val matrix = MultiFormatWriter().encode(text, format, width, height, hints)
    val pixels = IntArray(matrix.width * matrix.height) { i ->
        if (matrix.get(i % matrix.width, i / matrix.width)) Color.BLACK else Color.WHITE
    }
    Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
} catch (e: Exception) {
    android.util.Log.e("MemberCode", "Kód se nepodařilo vykreslit", e)
    null
}

/**
 * Čárový kód zákazníka pro obsluhu (karta zákazníka v adminu).
 * Klepnutím se zvětší na celou obrazovku — dá se naskenovat čtečkou
 * pokladny i z tabletu/telefonu obsluhy, když zákazník nemá telefon u sebe.
 */
@Composable
fun MemberBarcodeCompact(code: String) {
    var fullscreen by remember { mutableStateOf(false) }
    val barcode = remember(code) { renderCode(code, BarcodeFormat.CODE_128, 800, 200) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(androidx.compose.ui.graphics.Color.White)
            .clickable { fullscreen = true }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CodeImages(barcode = barcode, height = 56, code = code, showDigits = true)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text  = "Klepnutím zvětšíte",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }

    if (fullscreen) {
        FullscreenCode(barcode = barcode, code = code, showDigits = true) { fullscreen = false }
    }
}
