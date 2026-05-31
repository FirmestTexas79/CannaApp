package cz.cannaclub.cannaapp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cz.cannaclub.cannaapp.ui.theme.Background
import cz.cannaclub.cannaapp.ui.theme.Gold
import cz.cannaclub.cannaapp.ui.theme.Sage

enum class ButtonVariant { SAGE, GOLD }

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.SAGE,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    val bgColor = when (variant) {
        ButtonVariant.SAGE -> Sage
        ButtonVariant.GOLD -> Gold
    }

    Button(
        onClick  = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape   = RoundedCornerShape(16.dp),
        colors  = ButtonDefaults.buttonColors(
            containerColor         = bgColor,
            contentColor           = Background,
            disabledContainerColor = bgColor.copy(alpha = 0.5f),
            disabledContentColor   = Background.copy(alpha = 0.5f)
        ),
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(22.dp),
                color       = Background,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text  = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}