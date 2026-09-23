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
 * Členská karta: jeden čárový kód (Code 128) + číslo pod ním.
 * Code 128 přečte laserová i 2D čtečka, QR jen 2D — proto jen čárový kód.
 * Když čtečka nezabere, prodavač číslo opíše.
 * Prodavač ji naskenuje čtečkou přímo na pokladně Dotykačka — ta podle pole
 * "Čárový kód" sama načte zákazníka na účet. Stejný kód umí i admin appka.
 *
 * [code]  = členský kód (nebo ID, dokud ho server nepřidělí)
 * [ready] = false → kód ještě není propojený s Dotykačkou
 */
@Composable
fun QrCodeCard(code: String, ready: Boolean = true) {
    var fullscreen by remember { mutableStateOf(false) }
    val barcode = remember(code) { renderCode(code, BarcodeFormat.CODE_128, 800, 200) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(androidx.compose.ui.graphics.Color.White)
            .clickable { fullscreen = true }
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text  = "VÁŠ ČLENSKÝ KÓD",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(16.dp))

        CodeImages(barcode = barcode, height = 110, code = code, showDigits = ready)

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text      = if (ready) "Ukažte u pokladny · klepnutím zvětšíte"
                        else "Kód pro pokladnu se připravuje…",
            style     = MaterialTheme.typography.bodySmall,
            color     = TextMuted,
            textAlign = TextAlign.Center
        )
    }

    if (fullscreen) {
        FullscreenCode(barcode = barcode, code = code, showDigits = ready) {
            fullscreen = false
        }
    }
}

@Composable
private fun CodeImages(barcode: Bitmap?, height: Int, code: String, showDigits: Boolean) {
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

/** Celá obrazovka bílá + jas na maximum — čtečky tak displej přečtou mnohem líp. */
@Composable
private fun FullscreenCode(barcode: Bitmap?, code: String, showDigits: Boolean, onDismiss: () -> Unit) {
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
                CodeImages(barcode = barcode, height = 180, code = code, showDigits = showDigits)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text  = "Klepnutím zavřete",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}

private fun renderCode(text: String, format: BarcodeFormat, width: Int, height: Int): Bitmap? = try {
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
