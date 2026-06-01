package cz.cannaclub.cannaapp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Scaffold
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import cz.cannaclub.cannaapp.ui.theme.AdminBackground
import cz.cannaclub.cannaapp.ui.theme.Background
import cz.cannaclub.cannaapp.ui.theme.BorderNormal
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.Gold
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

    var selectedUser  by remember { mutableStateOf<User?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarState = remember { SnackbarHostState() }

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

    // ── Vrstvené pozadí (stejný pattern jako ostatní obrazovky) ──
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminBackground)
    ) {
        // Vrstva 2: Světlé listy na tmavém admin pozadí
        DecorativePlants(modifier = Modifier.fillMaxSize(), color = LeafDecorAdmin)

        // Vrstva 3: Pill — obsah admin seznamu
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

                // ── Hlavička ──────────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(36.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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

                // ── Searchbar ─────────────────────────────────────
                item {
                    OutlinedTextField(
                        value         = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier      = Modifier.fillMaxWidth(),
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
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // ── Seznam zákazníků ──────────────────────────────
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

                // ── Naše zeleň ───────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(CardDefault)
                            .clickable { onProductsClick() }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
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
                    Spacer(
                        modifier = Modifier
                            .height(100.dp)
                            .navigationBarsPadding()
                    )
                }
            }
        }

        // ── FAB mimo pill — zlatý, plovoucí nad pillem ────────────
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

        // ── Snackbar ──────────────────────────────────────────────
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

    // ── Edit points dialog ────────────────────────────────────────
    selectedUser?.let { user ->
        EditPointsDialog(
            user      = user,
            onDismiss = { selectedUser = null },
            onSave    = { newPoints ->
                viewModel.updatePoints(user, newPoints)
                selectedUser = null
            }
        )
    }

    // ── Add user dialog ───────────────────────────────────────────
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