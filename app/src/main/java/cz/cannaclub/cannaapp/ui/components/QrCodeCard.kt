package cz.cannaclub.cannaapp.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.cannaclub.cannaapp.ui.theme.BorderNormal
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import qrcode.QRCode
import qrcode.color.Colors

@Composable
fun QrCodeCard(userId: String) {

    val qrBitmap: Bitmap? = remember(userId) {
        try {
            val qrCode = QRCode.ofSquares()
                .withColor(Colors.css("#4A6741"))     // sage zelená
                .withBackgroundColor(Colors.WHITE)
                .withSize(10)
                .build(userId)

            val imageData = qrCode.render()
            val nativeImage = imageData.nativeImage()

            if (nativeImage is Bitmap) {
                nativeImage
            } else {
                // Fallback — překreslíme přes Canvas
                val size = 300
                val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(Color.WHITE)
                bmp
            }
        } catch (e: Exception) {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(androidx.compose.ui.graphics.Color.White)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text  = "VÁŠ ČLENSKÝ KÓD",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (qrBitmap != null) {
            Image(
                bitmap             = qrBitmap.asImageBitmap(),
                contentDescription = "QR kód člena",
                modifier           = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⚠️", fontSize = 32.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text  = "Ukažte obsluze pro rychlé přihlášení",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}