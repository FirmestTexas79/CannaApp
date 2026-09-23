package cz.cannaclub.cannaapp.ui.admin

import androidx.compose.foundation.background
import cz.cannaclub.cannaapp.viewmodel.ImportState
import cz.cannaclub.cannaapp.ui.theme.PointsRed
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.cannaclub.cannaapp.model.User
import cz.cannaclub.cannaapp.ui.components.DecorativePlants
import cz.cannaclub.cannaapp.ui.components.UserPillComponent
import cz.cannaclub.cannaapp.ui.theme.AdminBackground
import cz.cannaclub.cannaapp.ui.theme.Background
import cz.cannaclub.cannaapp.ui.theme.BorderNormal
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.Gold
import cz.cannaclub.cannaapp.ui.theme.GoldDim
import cz.cannaclub.cannaapp.ui.theme.LeafDecorAdmin
import cz.cannaclub.cannaapp.ui.theme.PillBackground
import cz.cannaclub.cannaapp.ui.theme.TextFaint
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import cz.cannaclub.cannaapp.ui.theme.TextPrimary
import cz.cannaclub.cannaapp.viewmodel.AdminViewModel
import cz.cannaclub.cannaapp.viewmodel.OperationState

private val PillHorizontalPadding = 28.dp
private val PillVerticalPadding   = 72.dp

@Composable
fun AdminListScreen(
    viewModel: AdminViewModel,
    onLogout: () -> Unit,
    onProductsClick: () -> Unit = {}
) {
    val users        by viewModel.filteredUsers.collectAsState()
    val searchQuery  by viewModel.searchQuery.collectAsState()
    val opState      by viewModel.operationState.collectAsState()
    val scannedUser  by viewModel.scannedUser.collectAsState()
    val dotykacka    by viewModel.dotykackaState.collectAsState()
    val importState  by viewModel.importState.collectAsState()

    var selectedUser  by remember { mutableStateOf<User?>(null) }
    var openedByScan  by remember { mutableStateOf(false) }   // karta otevřená skenem → ukázat stav pokladny
    var showAddDialog by remember { mutableStateOf(false) }
    var showScanner   by remember { mutableStateOf(false) }
    val snackbarState = remember { SnackbarHostState() }

    // ── Reakce na operace ─────────────────────────────────
    LaunchedEffect(opState) {
        when (val state = opState) {
            is OperationState.Success -> {
                snackbarState.showSnackbar(state.message)
                viewModel.resetOperationState()
            }
            is OperationState.Error -> {
                snackbarState.showSnackbar(state.message)
                viewModel.resetOperationState()
            }
            else -> {}
        }
    }

    // ── Reakce na QR skenování — otevře dialog zákazníka ──
    LaunchedEffect(scannedUser) {
        scannedUser?.let {
            selectedUser = it
            openedByScan = true
            viewModel.clearScannedUser()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminBackground)
    ) {
        DecorativePlants(modifier = Modifier.fillMaxSize(), color = LeafDecorAdmin)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PillHorizontalPadding, vertical = PillVerticalPadding)
                .clip(RoundedCornerShape(32.dp))
                .background(PillBackground)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp)
            ) {

                // ── Hlavička ──────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(36.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.Bottom
                    ) {
                        Text(
                            text  = "Zákazníci",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary
                        )
                        Text(
                            text     = "${users.size} účtů",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = TextMuted,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // ── Searchbar + Skener ────────────────────
                item {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value         = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            modifier      = Modifier.weight(1f),
                            placeholder   = {
                                Text(
                                    text  = "⌕  Hledat zákazníka…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextFaint
                                )
                            },
                            textStyle  = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                            singleLine = true,
                            shape      = RoundedCornerShape(14.dp),
                            colors     = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = Gold,
                                unfocusedBorderColor    = BorderNormal,
                                focusedContainerColor   = CardDefault,
                                unfocusedContainerColor = CardDefault,
                                cursorColor             = Gold,
                                focusedTextColor        = TextPrimary,
                                unfocusedTextColor      = TextPrimary
                            )
                        )

                        // Tlačítko skeneru
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(GoldDim)
                                .clickable { showScanner = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "📷", fontSize = 24.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // ── Seznam zákazníků ──────────────────────
                if (users.isEmpty()) {
                    item {
                        Text(
                            text      = if (searchQuery.isBlank()) "Žádní zákazníci"
                            else "Žádný výsledek pro „$searchQuery",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = TextMuted,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp)
                        )
                    }
                } else {
                    items(users, key = { it.id }) { user ->
                        UserPillComponent(
                            user    = user,
                            onClick = { selectedUser = user }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // ── Správa produktů ───────────────────────
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(CardDefault)
                            .clickable { onProductsClick() }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text  = "SPRÁVA PRODUKTŮ",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text  = "Naše zeleň 🌿",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary
                            )
                        }
                        Text("→", fontSize = 20.sp, color = TextMuted)
                    }

                    // ── Import zákazníků z Dotykačky ──────────
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(CardDefault)
                            .clickable { viewModel.previewImport() }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text  = "DOTYKAČKA",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text  = "Převzít zákazníky z pokladny",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary
                            )
                        }
                        Text("→", fontSize = 20.sp, color = TextMuted)
                    }
                    Spacer(
                        modifier = Modifier
                            .height(100.dp)
                            .navigationBarsPadding()
                    )
                }
            }
        }

        // ── FAB ───────────────────────────────────────────
        FloatingActionButton(
            onClick        = { showAddDialog = true },
            shape          = RoundedCornerShape(17.dp),
            containerColor = Gold,
            contentColor   = AdminBackground,
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 56.dp)
                .size(54.dp)
        ) {
            Text(text = "+", fontSize = 28.sp, color = Background)
        }

        // ── Snackbar ──────────────────────────────────────
        SnackbarHost(
            hostState = snackbarState,
            modifier  = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(
                snackbarData   = data,
                containerColor = CardDefault,
                contentColor   = TextPrimary,
                shape          = RoundedCornerShape(12.dp),
                modifier       = Modifier.padding(horizontal = PillHorizontalPadding + 4.dp)
            )
        }
    }

    // ── QR Skener ─────────────────────────────────────────
    if (showScanner) {
        QrScannerDialog(
            onDismiss = { showScanner = false },
            onScanned = { scannedUserId ->
                showScanner = false
                viewModel.findUserByQrCode(scannedUserId)
            }
        )
    }

    // ── Edit dialog ───────────────────────────────────────
    selectedUser?.let { user ->
        val close = {
            selectedUser = null
            openedByScan = false
            viewModel.resetDotykackaState()
        }
        // Ukládá se ROZDÍL oproti stavu při otevření karty (viz UserRepository.updatePoints),
        // takže body připsané mezitím z Dotykačky se nepřepíšou.
        EditPointsDialog(
            user           = user,
            dotykackaState = if (openedByScan) dotykacka else null,
            onRetryDotykacka = { viewModel.assignToDotykacka(user.id) },
            onDismiss      = close,
            onSave         = { newPoints, reason ->
                viewModel.updatePoints(user, newPoints, reason)
                close()
            }
        )
    }

    // ── Import z Dotykačky ────────────────────────────────
    ImportCustomersDialog(
        state     = importState,
        onConfirm = { viewModel.confirmImport() },
        onDismiss = { viewModel.dismissImport() }
    )

    // ── Add dialog ────────────────────────────────────────
    if (showAddDialog) {
        AddUserDialog(
            onDismiss = { showAddDialog = false },
            onAdd     = { name, email, phone, pts ->
                viewModel.addUser(name, email, phone, pts)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ImportCustomersDialog(
    state: ImportState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (state is ImportState.Idle) return

    val loading = state is ImportState.Loading
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        containerColor   = PillBackground,
        title = {
            Text(
                text  = "Zákazníci z Dotykačky",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        },
        text = {
            when (state) {
                is ImportState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Gold, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text  = if (state.dryRun) "Načítám zákazníky z pokladny…" else "Zakládám účty…",
                        color = TextMuted
                    )
                }
                is ImportState.Done -> {
                    val r = state.result
                    Text(
                        color = TextMuted,
                        text  = if (r.dryRun) {
                            "V Dotykačce je ${r.totalInDotykacka} zákazníků.\n\n" +
                            "• převezme se: ${r.imported}\n" +
                            "• už v appce jsou: ${r.alreadyInApp}\n" +
                            "• bez e-mailu (nejde převzít): ${r.noEmail}\n\n" +
                            "Převzatí zákazníci se přihlásí svým e-mailem a telefonem (nebo jménem, když telefon v pokladně nemají). " +
                            "Body začínají na nule."
                        } else {
                            "Hotovo, převzato ${r.imported} zákazníků. Členské kódy se do Dotykačky zapíší během pár minut."
                        }
                    )
                }
                is ImportState.Error -> Text(text = state.message, color = PointsRed)
                ImportState.Idle -> {}
            }
        },
        confirmButton = {
            if (state is ImportState.Done && state.result.dryRun && state.result.imported > 0) {
                TextButton(onClick = onConfirm) { Text("Převzít ${state.result.imported}", color = Gold) }
            }
        },
        dismissButton = {
            if (!loading) {
                TextButton(onClick = onDismiss) {
                    Text(if (state is ImportState.Done && !state.result.dryRun) "Zavřít" else "Zrušit", color = TextMuted)
                }
            }
        }
    )
}
