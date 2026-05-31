package cz.cannaclub.cannaapp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cz.cannaclub.cannaapp.ui.theme.Background
import cz.cannaclub.cannaapp.ui.theme.Cream
import cz.cannaclub.cannaapp.ui.theme.Gold
import cz.cannaclub.cannaapp.ui.theme.Sage
import cz.cannaclub.cannaapp.ui.theme.Surface
import cz.cannaclub.cannaapp.ui.theme.TextMuted

@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, email: String, phone: String, points: Int) -> Unit
) {
    var name  by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pts   by remember { mutableStateOf("0") }
    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Surface)
                .padding(26.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text  = "Nový zákazník",
                style = MaterialTheme.typography.headlineMedium,
                color = Cream
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = "Vyplň registrační údaje",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(22.dp))

            AdminTextField(
                value         = name,
                onValueChange = { name = it },
                label         = "JMÉNO",
                placeholder   = "Jan Novák",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            AdminTextField(
                value         = email,
                onValueChange = { email = it },
                label         = "EMAIL",
                placeholder   = "jan@email.cz",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            AdminTextField(
                value         = phone,
                onValueChange = { phone = it },
                label         = "TELEFON",
                placeholder   = "+420 777 000 000",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction    = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            AdminTextField(
                value         = pts,
                onValueChange = { if (it.all { c -> c.isDigit() }) pts = it },
                label         = "POČÁTEČNÍ BODY",
                placeholder   = "0",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction    = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Zrušit", color = TextMuted)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = {
                        onAdd(
                            name,
                            email,
                            phone,
                            pts.toIntOrNull() ?: 0
                        )
                    },
                    modifier = Modifier.weight(2f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor   = Background
                    )
                ) {
                    Text(
                        text  = "Přidat zákazníka",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}