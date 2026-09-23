package cz.cannaclub.cannaapp.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.cannaclub.cannaapp.ui.components.DecorativePlants
import cz.cannaclub.cannaapp.ui.theme.Background
import cz.cannaclub.cannaapp.ui.theme.BorderNormal
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.PillBackground
import cz.cannaclub.cannaapp.ui.theme.Sage
import cz.cannaclub.cannaapp.ui.theme.TextFaint
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import cz.cannaclub.cannaapp.ui.theme.TextPrimary
import cz.cannaclub.cannaapp.viewmodel.LoginState
import cz.cannaclub.cannaapp.viewmodel.UserViewModel

/**
 * Samoregistrace zákazníka. Po založení účtu server (onUserCreated)
 * zákazníka sám najde / založí v Dotykačce a zapíše mu členský kód,
 * takže ho pokladna pozná skenem během pár desítek sekund.
 */
@Composable
fun RegisterScreen(
    viewModel: UserViewModel,
    onRegistered: () -> Unit,
    onBack: () -> Unit
) {
    var name    by remember { mutableStateOf("") }
    var email   by remember { mutableStateOf("") }
    var phone   by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    cz.cannaclub.cannaapp.ui.components.SystemBarsAppearance(lightBackground = true)

    val state        by viewModel.loginState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbar     = remember { SnackbarHostState() }
    val loading      = state is LoginState.Loading

    LaunchedEffect(state) {
        when (val s = state) {
            is LoginState.Success -> { onRegistered(); viewModel.resetLoginState() }
            is LoginState.Error   -> { snackbar.showSnackbar(s.message); viewModel.resetLoginState() }
            else -> {}
        }
    }

    fun submit() {
        focusManager.clearFocus()
        viewModel.register(name, email, phone, consent)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        DecorativePlants(modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(PillBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 32.dp)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.foundation.Image(
                    painter            = androidx.compose.ui.res.painterResource(cz.cannaclub.cannaapp.R.drawable.canna_wordmark),
                    contentDescription = "CannaClub",
                    modifier           = Modifier.height(28.dp)
                )
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text  = "Registrace",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text      = "Po registraci dostaneš členskou kartičku. Ukaž ji u pokladny a za každý nákup ti naskočí body.",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                CannaTextField(
                    value           = name,
                    onValueChange   = { name = it },
                    label           = "JMÉNO A PŘÍJMENÍ",
                    placeholder     = "Marry Jane",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                Spacer(modifier = Modifier.height(12.dp))
                CannaTextField(
                    value           = email,
                    onValueChange   = { email = it },
                    label           = "EMAIL",
                    placeholder     = "vas@email.cz",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                Spacer(modifier = Modifier.height(12.dp))
                CannaTextField(
                    value           = phone,
                    onValueChange   = { phone = it },
                    label           = "TELEFON",
                    placeholder     = "+420 777 000 000",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { consent = !consent }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked         = consent,
                        onCheckedChange = { consent = it },
                        colors          = CheckboxDefaults.colors(
                            checkedColor   = Sage,
                            uncheckedColor = BorderNormal
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text  = "Je mi 18 let a souhlasím se zpracováním jména, e-mailu a telefonu pro věrnostní program Cannaclub.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick  = { submit() },
                    enabled  = !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape  = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Sage, contentColor = Color.White)
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(text = "ZAREGISTROVAT SE", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text     = "Už mám účet, přihlásit se",
                    style    = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                    color    = TextFaint,
                    modifier = Modifier
                        .clickable(enabled = !loading) { onBack() }
                        .padding(8.dp)
                )
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier  = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(
                snackbarData   = data,
                containerColor = CardDefault,
                contentColor   = TextPrimary,
                shape          = RoundedCornerShape(12.dp),
                modifier       = Modifier.padding(horizontal = 36.dp)
            )
        }
    }
}
