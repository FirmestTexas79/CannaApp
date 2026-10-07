package cz.cannaclub.cannaapp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.cannaclub.cannaapp.R
import cz.cannaclub.cannaapp.ui.components.CannaIcon
import cz.cannaclub.cannaapp.ui.components.DecorativePlants
import cz.cannaclub.cannaapp.ui.components.SectionLabel
import cz.cannaclub.cannaapp.ui.components.SystemBarsAppearance
import cz.cannaclub.cannaapp.ui.theme.AdminBackground
import cz.cannaclub.cannaapp.ui.theme.BorderNormal
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.Gold
import cz.cannaclub.cannaapp.ui.theme.GoldDim
import cz.cannaclub.cannaapp.ui.theme.Honey
import cz.cannaclub.cannaapp.ui.theme.LeafDecorAdmin
import cz.cannaclub.cannaapp.ui.theme.Paper
import cz.cannaclub.cannaapp.ui.theme.PillBackground
import cz.cannaclub.cannaapp.ui.theme.PointsRed
import cz.cannaclub.cannaapp.ui.theme.TextFaint
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import cz.cannaclub.cannaapp.ui.theme.TextPrimary
import cz.cannaclub.cannaapp.viewmodel.LoyaltyAdminViewModel
import cz.cannaclub.cannaapp.viewmodel.LoyaltyAdminViewModel.Duration

/** Admin: akce "více bodů" a hromadné zprávy zákazníkům. Bonus za rank se počítá sám. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminLoyaltyScreen(
    viewModel: LoyaltyAdminViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { viewModel.start() }

    val config     by viewModel.config.collectAsState()
    val busy       by viewModel.busy.collectAsState()
    val message    by viewModel.message.collectAsState()
    val recipients by viewModel.recipients.collectAsState()
    val snackbar   = remember { SnackbarHostState() }

    // Akce
    var multiplier by remember { mutableIntStateOf(2) }
    var duration   by remember { mutableStateOf(Duration.TODAY) }
    var promoName  by remember { mutableStateOf("") }
    var notify     by remember { mutableStateOf(true) }
    var confirmPromo by remember { mutableStateOf(false) }
    var confirmStop  by remember { mutableStateOf(false) }

    // Zpráva
    var title      by remember { mutableStateOf("") }
    var body       by remember { mutableStateOf("") }
    var audience   by remember { mutableIntStateOf(0) }
    var confirmSend by remember { mutableStateOf(false) }

    SystemBarsAppearance(lightBackground = false)

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val active = config.activePromo()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminBackground)
    ) {
        DecorativePlants(modifier = Modifier.fillMaxSize(), color = LeafDecorAdmin)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(PillBackground)
        ) {
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 40.dp)
            ) {
                // ── Hlavička ──────────────────────────────
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionLabel("Obsluha", color = Gold)
                            Text("Akce a zprávy", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                        }
                        Box(
                            modifier         = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(CardDefault)
                                .clickable { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            CannaIcon(id = R.drawable.ic_close, tint = TextMuted, size = 20.dp)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── 1. Akce "více bodů" ───────────────────
                item {
                    SectionLabel("Akce: více bodů za nákup")
                    Spacer(modifier = Modifier.height(10.dp))
                    AdminCard {
                        if (active != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier         = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Honey.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CannaIcon(id = R.drawable.ic_sparkle, tint = Honey, size = 22.dp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text  = "Běží: ${active.label} (${active.multiplier}×)",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text  = cz.cannaclub.cannaapp.ui.components.czechUntil(active.endsAtMillis),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            SecondaryButton("Ukončit akci", PointsRed, enabled = !busy) { confirmStop = true }
                        } else {
                            Text(
                                text  = "Zákazníci dostanou za nákup násobek bodů. V appce uvidí banner s akcí.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            FieldLabel("Kolikrát víc bodů")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(2, 3).forEach { m ->
                                    Chip("$m×", multiplier == m) { multiplier = m }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            FieldLabel("Jak dlouho")
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement   = Arrangement.spacedBy(8.dp)
                            ) {
                                Duration.entries.forEach { d -> Chip(d.label, duration == d) { duration = d } }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            FieldLabel("Název akce (nepovinné)")
                            AdminTextField(
                                value         = promoName,
                                onValueChange = { promoName = it.take(40) },
                                placeholder   = if (multiplier == 2) "Dvojité body" else "Trojité body"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CheckRow("Poslat zákazníkům upozornění", notify) { notify = it }
                            Spacer(modifier = Modifier.height(10.dp))
                            PrimaryAdminButton("Spustit akci", enabled = !busy) { confirmPromo = true }
                        }
                    }
                    Spacer(modifier = Modifier.height(26.dp))
                }

                // ── 2. Zpráva zákazníkům ──────────────────
                item {
                    SectionLabel("Zpráva zákazníkům")
                    Spacer(modifier = Modifier.height(10.dp))
                    AdminCard {
                        FieldLabel("Nadpis")
                        AdminTextField(value = title, onValueChange = { title = it.take(65) }, placeholder = "Dorazila nová odrůda")
                        Spacer(modifier = Modifier.height(12.dp))
                        FieldLabel("Text")
                        AdminTextField(
                            value         = body,
                            onValueChange = { body = it.take(240) },
                            placeholder   = "Lemon Haze je zpátky skladem. Stav se!",
                            singleLine    = false
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FieldLabel("Komu")
                        val audiences = listOf("Všem", "Bronzový+", "Stříbrný+", "Zlatý+", "Rodina")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement   = Arrangement.spacedBy(8.dp)
                        ) {
                            audiences.forEachIndexed { i, label ->
                                Chip(label, audience == i) {
                                    audience = i
                                    viewModel.countRecipients(i)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text  = when {
                                recipients == null -> "Počítám příjemce…"
                                recipients!! < 0   -> "Počet příjemců se nepodařilo zjistit"
                                else               -> "Zprávu dostane $recipients zákazníků s appkou"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PrimaryAdminButton(
                            text    = "Odeslat zprávu",
                            enabled = title.isNotBlank() && body.isNotBlank() && !busy && (recipients ?: 0) > 0
                        ) { confirmSend = true }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) { data ->
            Snackbar(
                snackbarData   = data,
                containerColor = TextPrimary,
                contentColor   = PillBackground,
                shape          = RoundedCornerShape(12.dp),
                modifier       = Modifier.padding(horizontal = 20.dp)
            )
        }

        if (busy) {
            CircularProgressIndicator(
                color    = Gold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 28.dp)
                    .size(24.dp),
                strokeWidth = 2.dp
            )
        }
    }

    // ── Potvrzení ─────────────────────────────────────────
    if (confirmPromo) {
        val name = promoName.ifBlank { if (multiplier == 2) "Dvojité body" else "Trojité body" }
        ConfirmDialog(
            title   = "Spustit akci „$name“?",
            text    = "${multiplier}× body za každý nákup, ${duration.label.lowercase()}." +
                      if (notify) " Zákazníci dostanou upozornění." else "",
            confirm = "Spustit",
            onConfirm = {
                confirmPromo = false
                viewModel.startPromo(multiplier, duration, promoName, notify)
            },
            onDismiss = { confirmPromo = false }
        )
    }
    if (confirmStop) {
        ConfirmDialog(
            title     = "Ukončit akci?",
            text      = "Nákupy od teď dostanou zase normální počet bodů.",
            confirm   = "Ukončit",
            onConfirm = { confirmStop = false; viewModel.stopPromo() },
            onDismiss = { confirmStop = false }
        )
    }
    if (confirmSend) {
        ConfirmDialog(
            title     = "Odeslat zprávu?",
            text      = "„$title“ dostane ${recipients ?: 0} zákazníků. Odeslanou zprávu už nejde vzít zpět.",
            confirm   = "Odeslat",
            onConfirm = {
                confirmSend = false
                viewModel.sendBroadcast(title, body, audience) { title = ""; body = "" }
            },
            onDismiss = { confirmSend = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AdminCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Paper)
            .border(1.dp, BorderNormal.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color    = TextMuted,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text     = label,
        style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color    = if (selected) PillBackground else TextMuted,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) TextPrimary else CardDefault)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked         = checked,
            onCheckedChange = onChange,
            colors          = CheckboxDefaults.colors(checkedColor = Gold, uncheckedColor = BorderNormal)
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

@Composable
private fun AdminTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = Modifier.fillMaxWidth(),
        placeholder   = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = TextFaint) },
        textStyle     = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
        singleLine    = singleLine,
        minLines      = if (singleLine) 1 else 3,
        shape         = RoundedCornerShape(14.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = Gold,
            unfocusedBorderColor    = BorderNormal,
            focusedContainerColor   = CardDefault,
            unfocusedContainerColor = CardDefault,
            cursorColor             = Gold
        )
    )
}

@Composable
private fun PrimaryAdminButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = Gold,
            contentColor           = Color.White,
            disabledContainerColor = GoldDim,
            disabledContentColor   = TextFaint
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SecondaryButton(text: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = PillBackground,
        title            = { Text(title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary) },
        text             = { Text(text, color = TextMuted) },
        confirmButton    = { TextButton(onClick = onConfirm) { Text(confirm, color = Gold) } },
        dismissButton    = { TextButton(onClick = onDismiss) { Text("Zrušit", color = TextMuted) } }
    )
}
