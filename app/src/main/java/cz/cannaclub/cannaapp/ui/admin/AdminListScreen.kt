package cz.cannaclub.cannaapp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import cz.cannaclub.cannaapp.ui.theme.Background
import cz.cannaclub.cannaapp.ui.theme.BorderNormal
import cz.cannaclub.cannaapp.ui.theme.BorderSoft
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.CardHover
import cz.cannaclub.cannaapp.ui.theme.Cream
import cz.cannaclub.cannaapp.ui.theme.CreamDim
import cz.cannaclub.cannaapp.ui.theme.Gold
import cz.cannaclub.cannaapp.ui.theme.Sage
import cz.cannaclub.cannaapp.ui.theme.TextFaint
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import cz.cannaclub.cannaapp.viewmodel.AdminViewModel
import cz.cannaclub.cannaapp.viewmodel.OperationState

@Composable
fun AdminListScreen(
    viewModel: AdminViewModel,
    onLogout: () -> Unit
) {
    val users        by viewModel.filteredUsers.collectAsState()
    val searchQuery  by viewModel.searchQuery.collectAsState()
    val opState      by viewModel.operationState.collectAsState()

    var selectedUser  by remember { mutableStateOf<User?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarState = remember { SnackbarHostState() }

    // Snackbar při úspěchu/chybě operace
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

    Scaffold(
        containerColor = Background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = CardDefault,
                    contentColor   = Cream,
                    shape          = RoundedCornerShape(12.dp)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick          = { showAddDialog = true },
                shape            = RoundedCornerShape(17.dp),
                containerColor   = Sage,
                contentColor     = Background,
                modifier         = Modifier.size(54.dp)
            ) {
                Text(text = "+", fontSize = 28.sp)
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 22.dp)
        ) {

            // ── Hlavička ──────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(52.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Bottom
                ) {
                    Text(
                        text  = "Zákazníci",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Cream
                    )
                    Text(
                        text  = "${users.size} účtů",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // ── Searchbar ─────────────────────────────────
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
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Cream),
                    singleLine = true,
                    shape  = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = Gold,
                        unfocusedBorderColor    = BorderNormal,
                        focusedContainerColor   = CardDefault,
                        unfocusedContainerColor = CardDefault,
                        cursorColor             = Gold,
                        focusedTextColor        = Cream,
                        unfocusedTextColor      = Cream
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // ── Seznam uživatelů ──────────────────────────
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
                    UserPill(
                        user    = user,
                        onClick = { selectedUser = user }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(90.dp)) }
        }
    }

    // ── Edit points dialog ────────────────────────────────
    selectedUser?.let { user ->
        EditPointsDialog(
            user     = user,
            onDismiss = { selectedUser = null },
            onSave   = { newPoints ->
                viewModel.updatePoints(user, newPoints)
                selectedUser = null
            }
        )
    }

    // ── Add user dialog ───────────────────────────────────
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

// ─────────────────────────────────────────────────────────
// User Pill — karta zákazníka v seznamu
// ─────────────────────────────────────────────────────────
@Composable
fun UserPill(
    user: User,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(CardDefault)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar s iniciálami
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(CardHover),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = user.initials,
                style = MaterialTheme.typography.bodyLarge,
                color = Cream
            )
        }

        Spacer(modifier = Modifier.width(13.dp))

        // Jméno a email
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = user.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Cream
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text  = user.email,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        // Počet bodů
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text  = user.points.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = Sage
            )
            Text(
                text  = "bodů",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextMuted
            )
        }
    }
}