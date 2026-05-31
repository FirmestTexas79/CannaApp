package cz.cannaclub.cannaapp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cz.cannaclub.cannaapp.model.User
import cz.cannaclub.cannaapp.ui.theme.Background
import cz.cannaclub.cannaapp.ui.theme.BorderNormal
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.Cream
import cz.cannaclub.cannaapp.ui.theme.PointsRed
import cz.cannaclub.cannaapp.ui.theme.Sage
import cz.cannaclub.cannaapp.ui.theme.SageGlow
import cz.cannaclub.cannaapp.ui.theme.Surface
import cz.cannaclub.cannaapp.ui.theme.TextMuted

@Composable
fun EditPointsDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var points by remember { mutableStateOf(user.points) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Surface)
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Titulek
            Text(
                text  = user.name,
                style = MaterialTheme.typography.headlineMedium,
                color = Cream
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = "Úprava bodového zůstatku",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Editor bodů — [ − ] [ číslo ] [ + ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardDefault),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Mínus
                IconButton(
                    onClick  = { if (points > 0) points -= 10 },
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                        .background(
                            androidx.compose.ui.graphics.Color(0x1AB45050)
                        )
                        .padding(4.dp)
                ) {
                    Text(
                        text     = "−",
                        fontSize = 26.sp,
                        color    = PointsRed
                    )
                }

                // Číselný input
                OutlinedTextField(
                    value         = points.toString(),
                    onValueChange = { str ->
                        str.toIntOrNull()?.let { v ->
                            if (v >= 0) points = v
                        }
                    },
                    modifier  = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        textAlign = TextAlign.Center,
                        fontSize  = 38.sp
                    ),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedBorderColor    = androidx.compose.ui.graphics.Color.Transparent,
                        focusedContainerColor   = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedTextColor        = Cream,
                        unfocusedTextColor      = Cream,
                        cursorColor             = Sage
                    )
                )

                // Plus
                IconButton(
                    onClick  = { points += 10 },
                    modifier = Modifier
                        .clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
                        .background(SageGlow)
                        .padding(4.dp)
                ) {
                    Text(
                        text     = "+",
                        fontSize = 26.sp,
                        color    = Sage
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tlačítka
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text  = "Zrušit",
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick  = { onSave(points) },
                    modifier = Modifier.weight(2f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Sage,
                        contentColor   = Background
                    )
                ) {
                    Text(
                        text  = "Uložit",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}