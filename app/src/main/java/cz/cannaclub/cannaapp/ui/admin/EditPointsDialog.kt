package cz.cannaclub.cannaapp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cz.cannaclub.cannaapp.model.User
import cz.cannaclub.cannaapp.ui.components.MemberBarcodeCompact
import cz.cannaclub.cannaapp.ui.theme.BorderNormal
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.Gold
import cz.cannaclub.cannaapp.ui.theme.PillBackground
import cz.cannaclub.cannaapp.ui.theme.PointsRed
import cz.cannaclub.cannaapp.ui.theme.Sage
import cz.cannaclub.cannaapp.ui.theme.SageGlow
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import cz.cannaclub.cannaapp.ui.theme.TextPrimary
import cz.cannaclub.cannaapp.viewmodel.DotykackaState

// ── Předvolené důvody ─────────────────────────────────────
private val reasons = listOf(
    "Vrácení obalů",
    "Nákup v obchodě",
    "Věrnostní bonus",
    "Oprava chyby",
    "Uplatnění odměny",
    "Jiný důvod"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditPointsDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (Int, String) -> Unit,
    dotykackaState: DotykackaState? = null,   // null = karta otevřená ze seznamu, ne skenem
    onRetryDotykacka: () -> Unit = {}
) {
    var points         by remember { mutableStateOf(user.points) }
    var selectedReason by remember { mutableStateOf(reasons.first()) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(PillBackground)
                .verticalScroll(rememberScrollState())   // na menších telefonech se karta nevejde
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Hlavička ──────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SageGlow)
                        .border(1.dp, Sage.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = user.initials,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Sage
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = user.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text  = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Text(
                        text  = user.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                }
            }

            // ── Členská kartička (čárový kód) ─────────────
            if (user.memberCode.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                MemberBarcodeCompact(code = user.memberCode)
            }

            // ── Stav připojení k účtu na pokladně ─────────
            if (dotykackaState != null && dotykackaState !is DotykackaState.Idle) {
                Spacer(modifier = Modifier.height(12.dp))
                DotykackaBanner(state = dotykackaState, onRetry = onRetryDotykacka)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Rank + body ───────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDefault)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    cz.cannaclub.cannaapp.ui.components.RankBadge(rank = user.rank, size = 22.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text  = user.rank.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Sage
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text  = "Celkem: ${user.totalPoints} b",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text  = "Zůstatek: ${user.points} b",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = BorderNormal)
            Spacer(modifier = Modifier.height(20.dp))

            // ── Důvod úpravy ──────────────────────────────
            Text(
                text  = "DŮVOD ÚPRAVY",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                reasons.forEach { reason ->
                    val selected = reason == selectedReason
                    Text(
                        text     = reason,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = if (selected) Sage else TextMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) SageGlow else CardDefault)
                            .border(
                                width = 1.dp,
                                color = if (selected) Sage.copy(alpha = 0.4f) else BorderNormal,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedReason = reason }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = BorderNormal)
            Spacer(modifier = Modifier.height(20.dp))

            // ── Editor bodů ───────────────────────────────
            Text(
                text  = "NOVÝ ZŮSTATEK BODŮ",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x14B45050))
                        .border(1.dp, PointsRed.copy(alpha = 0.25f), CircleShape)
                        .clickable { if (points > 0) points -= 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text      = "−",
                        fontSize  = 24.sp,
                        color     = PointsRed,
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedTextField(
                    value         = points.toString(),
                    onValueChange = { str ->
                        str.toIntOrNull()?.let { v -> if (v >= 0) points = v }
                    },
                    modifier  = Modifier.width(140.dp),
                    textStyle = MaterialTheme.typography.displayMedium.copy(
                        textAlign     = TextAlign.Center,
                        fontSize      = 48.sp,
                        letterSpacing = (-1).sp
                    ),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = Color.Transparent,
                        unfocusedBorderColor    = Color.Transparent,
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor        = TextPrimary,
                        unfocusedTextColor      = TextPrimary,
                        cursorColor             = Sage
                    )
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SageGlow)
                        .border(1.dp, Sage.copy(alpha = 0.25f), CircleShape)
                        .clickable { points += 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text      = "+",
                        fontSize  = 24.sp,
                        color     = Sage,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Rychlé úpravy — ťukání po 1 bodu je u pokladny zbytečně pomalé
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                listOf(-10, -5, 5, 10, 20).forEach { step ->
                    Text(
                        text     = if (step > 0) "+$step" else "−${-step}",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = if (step > 0) Sage else PointsRed,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardDefault)
                            .clickable { points = (points + step).coerceAtLeast(0) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            val diff = points - user.points
            if (diff != 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text      = if (diff > 0) "+$diff bodů" else "$diff bodů",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = if (diff > 0) Sage else PointsRed,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Tlačítka ──────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Zrušit", color = TextMuted)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick  = { onSave(points, selectedReason) },
                    modifier = Modifier.weight(2f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Sage,
                        contentColor   = Color.White
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

@Composable
private fun DotykackaBanner(state: DotykackaState, onRetry: () -> Unit) {
    val (color, text) = when (state) {
        is DotykackaState.Syncing  -> Gold to "Připojuji k účtu na pokladně…"
        is DotykackaState.Assigned -> Sage to "✓ ${state.message}"
        is DotykackaState.Error    -> PointsRed to state.message
        DotykackaState.Idle        -> return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state is DotykackaState.Syncing) {
            CircularProgressIndicator(
                modifier    = Modifier.size(14.dp),
                color       = Gold,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text     = text,
            style    = MaterialTheme.typography.bodySmall,
            color    = color,
            modifier = Modifier.weight(1f)
        )
        if (state is DotykackaState.Error) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text     = "Znovu",
                style    = MaterialTheme.typography.labelSmall,
                color    = TextPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardDefault)
                    .clickable { onRetry() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}
