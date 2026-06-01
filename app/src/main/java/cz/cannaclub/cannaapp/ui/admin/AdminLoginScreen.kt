package cz.cannaclub.cannaapp.ui.admin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.cannaclub.cannaapp.ui.components.DecorativePlants
import cz.cannaclub.cannaapp.ui.theme.AdminBackground
import cz.cannaclub.cannaapp.ui.theme.Background
import cz.cannaclub.cannaapp.ui.theme.LeafDecorAdmin
import cz.cannaclub.cannaapp.ui.theme.BorderNormal
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.Cream
import cz.cannaclub.cannaapp.ui.theme.Gold
import cz.cannaclub.cannaapp.ui.theme.GoldDim
import cz.cannaclub.cannaapp.ui.theme.PillBackground
import cz.cannaclub.cannaapp.ui.theme.TextFaint
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import cz.cannaclub.cannaapp.ui.theme.TextPrimary
import cz.cannaclub.cannaapp.viewmodel.AdminLoginState
import cz.cannaclub.cannaapp.viewmodel.AdminViewModel

private val PillHorizontalPadding = 28.dp
private val PillVerticalPadding   = 72.dp

@Composable
fun AdminLoginScreen(
    viewModel: AdminViewModel,
    onLoginSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val loginState    by viewModel.loginState.collectAsState()
    val focusManager  = LocalFocusManager.current
    val snackbarState = remember { SnackbarHostState() }

    val badgeOffset = remember { Animatable(-30f) }
    LaunchedEffect(Unit) {
        badgeOffset.animateTo(targetValue = 0f, animationSpec = tween(400))
    }

    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is AdminLoginState.Success -> {
                onLoginSuccess()
                viewModel.resetLoginState()
            }
            is AdminLoginState.Error -> {
                snackbarState.showSnackbar(state.message)
                viewModel.resetLoginState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminBackground)
    ) {
        // Vrstva 2: Světlé listy na tmavém pozadí
        DecorativePlants(modifier = Modifier.fillMaxSize(), color = LeafDecorAdmin)

        // Vrstva 3: Pill — mezery ze všech stran
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PillHorizontalPadding, vertical = PillVerticalPadding)
                .clip(RoundedCornerShape(32.dp))
                .background(PillBackground)
        ) {
            // Zlatý gradient nahoře uvnitř pillu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(GoldDim, Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(64.dp))

                // Emblem
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(CardDefault),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🔐", fontSize = 36.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Admin badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(GoldDim)
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        text  = "ADMIN PŘÍSTUP",
                        style = MaterialTheme.typography.labelSmall,
                        color = Gold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text  = "Cannaclub",
                    style = MaterialTheme.typography.displayLarge,
                    color = Cream
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text  = "SPRÁVA ZÁKAZNÍKŮ",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(48.dp))

                AdminTextField(
                    value         = email,
                    onValueChange = { email = it },
                    label         = "PŘIHLAŠOVACÍ EMAIL",
                    placeholder   = "obsluha@cannaclub.cz",
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
                    value         = password,
                    onValueChange = { password = it },
                    label         = "HESLO",
                    placeholder   = "••••••••",
                    isPassword    = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction    = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.loginAdmin(email, password)
                        }
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.loginAdmin(email, password)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape  = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor   = Background   // tmavý text na zlaté — luxury look
                    ),
                    enabled = loginState !is AdminLoginState.Loading
                ) {
                    if (loginState is AdminLoginState.Loading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(22.dp),
                            color       = Background,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text  = "VSTOUPIT DO SPRÁVY",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "← Zpět na zákaznický login",
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.Underline
                    ),
                    color     = TextFaint,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier
                        .clickable { onBackClick() }
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarState,
            modifier  = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(
                snackbarData   = data,
                containerColor = CardDefault,
                contentColor   = TextPrimary,
                shape          = RoundedCornerShape(12.dp),
                modifier       = Modifier.padding(horizontal = PillHorizontalPadding + 8.dp)
            )
        }
    }
}

// ── Admin TextField — zlatý focus ─────────────────────────────
@Composable
fun AdminTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value                = value,
            onValueChange        = onValueChange,
            modifier             = Modifier.fillMaxWidth(),
            placeholder          = {
                Text(
                    text  = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextFaint
                )
            },
            textStyle            = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            singleLine           = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation()
            else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions      = keyboardOptions,
            keyboardActions      = keyboardActions,
            shape                = RoundedCornerShape(14.dp),
            colors               = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = Gold,
                unfocusedBorderColor    = BorderNormal,
                focusedContainerColor   = CardDefault,
                unfocusedContainerColor = CardDefault,
                cursorColor             = Gold,
                focusedTextColor        = TextPrimary,
                unfocusedTextColor      = TextPrimary
            )
        )
    }
}