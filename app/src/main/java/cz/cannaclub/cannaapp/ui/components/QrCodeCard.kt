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
import androidx.compose.foundation.layout.size
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
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import cz.cannaclub.cannaapp.ui.theme.TextMuted

/**
 * Členská karta: QR kód + čárový kód (Code 128) se stejným číslem.
 * Prodavač ji naskenuje čtečkou přímo na pokladně Dotykačka — ta podle pole
 * "Čárový kód" sama načte zákazníka na účet. Stejný kód umí i admin appka.
 *
 * [code]  = členský kód (nebo ID, dokud ho server nepřidělí)
 * [ready] = false → kód ještě není propojený s Dotykačkou
 */
@Composable
fun QrCodeCard(code: String, ready: Boolean = true) {
    var fullscreen by remember { mutableStateOf(false) }
    val qr      = remember(code) { renderCode(code, BarcodeFormat.QR_CODE, 512, 512) }
    val barcode = remember(code) { if (ready) renderCode(code, BarcodeFormat.CODE_128, 800, 200) else null }

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

        CodeImages(qr = qr, barcode = barcode, qrSize = 180, code = code, showDigits = ready)

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
        FullscreenCode(qr = qr, barcode = barcode, code = code, showDigits = ready) {
            fullscreen = false
        }
    }
}

@Composable
private fun CodeImages(qr: Bitmap?, barcode: Bitmap?, qrSize: Int, code: String, showDigits: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (qr != null) {
            Image(
                bitmap             = qr.asImageBitmap(),
                contentDescription = "QR kód člena",
                modifier           = Modifier.size(qrSize.dp),
                filterQuality      = FilterQuality.None   // ostré hrany → lepší čitelnost
            )
        } else {
            Box(
                modifier = Modifier
                    .size(qrSize.dp)
                    .background(androidx.compose.ui.graphics.Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) { Text(text = "⚠️", fontSize = 32.sp) }
        }
        if (barcode != null) {
            Image(
                bitmap             = barcode.asImageBitmap(),
                contentDescription = "Čárový kód člena",
                modifier           = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                contentScale       = ContentScale.FillBounds,
                filterQuality      = FilterQuality.None
            )
        }
        if (showDigits) {
            Text(
                text       = code.chunked(4).joinToString(" "),
                fontFamily = FontFamily.Monospace,
                fontSize   = 16.sp,
                color      = androidx.compose.ui.graphics.Color.Black
            )
        }
    }
}

/** Celá obrazovka bílá + jas na maximum — čtečky tak displej přečtou mnohem líp. */
@Composable
private fun FullscreenCode(qr: Bitmap?, barcode: Bitmap?, code: String, showDigits: Boolean, onDismiss: () -> Unit) {
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
                CodeImages(qr = qr, barcode = barcode, qrSize = 260, code = code, showDigits = showDigits)
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
    val hints = mutableMapOf<EncodeHintType, Any>(EncodeHintType.MARGIN to if (format == BarcodeFormat.QR_CODE) 1 else 10)
    if (format == BarcodeFormat.QR_CODE) hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
    val matrix = MultiFormatWriter().encode(text, format, width, height, hints)
    val pixels = IntArray(matrix.width * matrix.height) { i ->
        if (matrix.get(i % matrix.width, i / matrix.width)) Color.BLACK else Color.WHITE
    }
    Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
} catch (e: Exception) {
    android.util.Log.e("MemberCode", "Kód se nepodařilo vykreslit", e)
    null
}
