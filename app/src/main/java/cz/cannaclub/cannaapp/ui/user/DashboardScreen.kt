package cz.cannaclub.cannaapp.ui.user

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cz.cannaclub.cannaapp.R
import cz.cannaclub.cannaapp.ShortcutRequests
import cz.cannaclub.cannaapp.model.Transaction
import cz.cannaclub.cannaapp.model.User
import cz.cannaclub.cannaapp.ui.components.CannaIcon
import cz.cannaclub.cannaapp.ui.components.DecorativePlants
import cz.cannaclub.cannaapp.ui.components.EARN_KC_PER_POINT
import cz.cannaclub.cannaapp.ui.components.IconBubble
import cz.cannaclub.cannaapp.ui.components.MemberCardOverlay
import cz.cannaclub.cannaapp.ui.components.PointsCard
import cz.cannaclub.cannaapp.ui.components.RankBadge
import cz.cannaclub.cannaapp.ui.components.SectionLabel
import cz.cannaclub.cannaapp.ui.components.SystemBarsAppearance
import cz.cannaclub.cannaapp.ui.components.WOLT_URL
import cz.cannaclub.cannaapp.ui.components.iconRes
import cz.cannaclub.cannaapp.ui.theme.Background
import cz.cannaclub.cannaapp.ui.theme.BorderSoft
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.Cream
import cz.cannaclub.cannaapp.ui.theme.Paper
import cz.cannaclub.cannaapp.ui.theme.PillBackground
import cz.cannaclub.cannaapp.ui.theme.PointsGreen
import cz.cannaclub.cannaapp.ui.theme.PointsRed
import cz.cannaclub.cannaapp.ui.theme.Sage
import cz.cannaclub.cannaapp.ui.theme.SageGlow
import cz.cannaclub.cannaapp.ui.theme.Surface
import cz.cannaclub.cannaapp.ui.theme.TextFaint
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import cz.cannaclub.cannaapp.ui.theme.TextPrimary
import cz.cannaclub.cannaapp.viewmodel.UserViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: UserViewModel,
    onLogout: () -> Unit,
    onProductsClick: () -> Unit = {}
) {
    val user           by viewModel.currentUser.collectAsState()
    val transactions   by viewModel.transactions.collectAsState()
    val celebration    by viewModel.celebration.collectAsState()
    val showOnboarding by viewModel.showOnboarding.collectAsState()

    DashboardContent(
        user                 = user,
        transactions         = transactions,
        celebration          = celebration,
        showOnboarding       = showOnboarding,
        onCelebrationShown   = { viewModel.celebrationShown() },
        onOnboardingFinished = { viewModel.onboardingFinished() },
        onOpenOnboarding     = { viewModel.openOnboarding() },
        onLogout             = onLogout,
        onProductsClick      = onProductsClick
    )
}

/** Co je po otevření hned vidět — jen pro náhledy a testy vzhledu. */
enum class DashboardOverlay { NONE, REWARDS, PROFILE, CARD }

/** Bezstavová obrazovka — data dostává zvenku (appka z ViewModelu, náhled z ukázkových dat). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    user: User?,
    transactions: List<Transaction>,
    celebration: Int?,
    showOnboarding: Boolean,
    onCelebrationShown: () -> Unit,
    onOnboardingFinished: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onLogout: () -> Unit,
    onProductsClick: () -> Unit,
    initialOverlay: DashboardOverlay = DashboardOverlay.NONE
) {
    val showCardReq    by ShortcutRequests.showCard.collectAsState()

    var showRewards  by remember { mutableStateOf(initialOverlay == DashboardOverlay.REWARDS) }
    var showProfile  by remember { mutableStateOf(initialOverlay == DashboardOverlay.PROFILE) }
    var showCard     by remember { mutableStateOf(initialOverlay == DashboardOverlay.CARD) }
    var confirmOut   by remember { mutableStateOf(false) }
    val context      = LocalContext.current

    SystemBarsAppearance(lightBackground = true)

    // Zkratka "Členská kartička" z ikony appky → rovnou otevřít kód
    LaunchedEffect(showCardReq, user) {
        if (showCardReq && user != null) {
            showCard = true
            ShortcutRequests.consume()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        DecorativePlants(modifier = Modifier.fillMaxSize())

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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp
                )
            ) {

                // ── Horní lišta: logo + profil ────────────────
                item {
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter            = painterResource(R.drawable.canna_wordmark),
                            contentDescription = "CannaClub",
                            modifier           = Modifier.height(24.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        user?.let { u ->
                            Box(
                                modifier         = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(SageGlow)
                                    .border(1.dp, Sage.copy(alpha = 0.25f), CircleShape)
                                    .clickable { showProfile = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text  = u.initials.ifBlank { "?" },
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = Sage
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(22.dp))
                }

                // ── Pozdrav ───────────────────────────────────
                item {
                    Text(
                        text  = "Ahoj${user?.firstName?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Cream
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── Karta bodů ────────────────────────────────
                item {
                    PointsCard(
                        points         = user?.points ?: 0,
                        totalPoints    = user?.totalPoints ?: 0,
                        onRewardsClick = { showRewards = true }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Kartička pro pokladnu ─────────────────────
                item {
                    ShowCardButton(
                        ready   = user?.memberCode?.isNotBlank() == true,
                        onClick = { showCard = true }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Rychlé akce ───────────────────────────────
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickTile(
                            icon     = R.drawable.ic_leaf,
                            title    = "Nabídka",
                            subtitle = "Co máme skladem",
                            onClick  = onProductsClick,
                            modifier = Modifier.weight(1f)
                        )
                        QuickTile(
                            icon     = R.drawable.ic_bike,
                            title    = "Rozvoz",
                            subtitle = "Objednej přes Wolt",
                            onClick  = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WOLT_URL)))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // ── Pohyby bodů ───────────────────────────────
                if (transactions.isEmpty()) {
                    item {
                        SectionLabel("Jak sbírat body")
                        Spacer(modifier = Modifier.height(10.dp))
                        HowItWorksCard()
                    }
                } else {
                    item {
                        SectionLabel("Pohyby bodů")
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    groupByDay(transactions).forEach { (label, dayTx) ->
                        item(key = "h_$label") {
                            Text(
                                text     = label,
                                style    = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color    = TextMuted,
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 8.dp)
                            )
                        }
                        item(key = "d_$label") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Paper)
                                    .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
                            ) {
                                dayTx.forEachIndexed { i, tx ->
                                    if (i > 0) HorizontalDivider(
                                        color     = BorderSoft,
                                        thickness = 1.dp,
                                        modifier  = Modifier.padding(start = 68.dp)
                                    )
                                    TransactionRow(tx)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // ── Oslava nových bodů ────────────────────────────────
        celebration?.let { gained ->
            PointsCelebration(gained = gained, onFinished = onCelebrationShown)
        }

        // ── Kartička přes celou obrazovku ─────────────────────
        if (showCard) {
            user?.let { u ->
                MemberCardOverlay(
                    code       = u.scanCode,
                    ready      = u.memberCode.isNotBlank(),
                    holderName = u.name,
                    onDismiss  = { showCard = false }
                )
            }
        }

        // ── Úvod "Jak to funguje" ─────────────────────────────
        if (showOnboarding) {
            OnboardingOverlay(onFinished = onOnboardingFinished)
        }
    }

    // ── Odměny ────────────────────────────────────────────────
    if (showRewards) {
        ModalBottomSheet(
            onDismissRequest = { showRewards = false },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = Surface,
            shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            RewardsSheet(
                currentPoints = user?.points ?: 0,
                totalPoints   = user?.totalPoints ?: 0
            )
        }
    }

    // ── Profil ────────────────────────────────────────────────
    if (showProfile) {
        user?.let { u ->
            ModalBottomSheet(
                onDismissRequest = { showProfile = false },
                sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor   = Surface,
                shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                ProfileSheet(
                    user         = u,
                    onHowItWorks = {
                        showProfile = false
                        onOpenOnboarding()
                    },
                    onLogout     = { confirmOut = true }
                )
            }
        }
    }

    if (confirmOut) {
        AlertDialog(
            onDismissRequest = { confirmOut = false },
            containerColor   = PillBackground,
            title            = { Text("Odhlásit se?", style = MaterialTheme.typography.headlineSmall, color = TextPrimary) },
            text             = {
                Text(
                    "Body ti zůstanou. Příště se přihlásíš e-mailem a telefonem.",
                    color = TextMuted
                )
            },
            confirmButton    = {
                TextButton(onClick = {
                    confirmOut  = false
                    showProfile = false
                    onLogout()
                }) { Text("Odhlásit", color = PointsRed) }
            },
            dismissButton    = {
                TextButton(onClick = { confirmOut = false }) { Text("Zůstat", color = Sage) }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ShowCardButton(ready: Boolean, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Paper)
            .border(1.dp, BorderSoft, RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(TextPrimary),
            contentAlignment = Alignment.Center
        ) {
            CannaIcon(id = R.drawable.ic_barcode, tint = Paper, size = 26.dp)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = "Ukázat kartičku",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
            Text(
                text  = if (ready) "Prodavač ji naskenuje u pokladny" else "Kartička se připravuje…",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        CannaIcon(id = R.drawable.ic_chevron_right, tint = TextFaint, size = 20.dp)
    }
}

@Composable
private fun QuickTile(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(CardDefault)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        IconBubble(id = icon, tint = Sage, background = Paper, size = 38.dp, iconSize = 20.dp)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text  = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )
        Text(
            text  = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

@Composable
private fun HowItWorksCard() {
    val steps = listOf(
        Triple(R.drawable.ic_barcode, "Ukaž kartičku", "U pokladny klepni na Ukázat kartičku, prodavač ji naskenuje."),
        Triple(R.drawable.ic_bag, "Nakup jako obvykle", "Za každých $EARN_KC_PER_POINT Kč dostaneš 1 bod, připíšou se do pár minut."),
        Triple(R.drawable.ic_gift, "Vyměň body", "Za odměnu, nebo za slevu na cokoli. 1 bod = 1 Kč.")
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Paper)
            .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
            .padding(vertical = 6.dp)
    ) {
        steps.forEachIndexed { i, (icon, title, text) ->
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                IconBubble(id = icon, tint = Sage, background = SageGlow, size = 38.dp, iconSize = 19.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = "${i + 1}. $title",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    Text(text = text, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
        }
    }
}

@Composable
fun TransactionRow(transaction: Transaction) {
    val positive = transaction.isPositive
    val color    = if (positive) PointsGreen else PointsRed
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(
            id         = transaction.iconRes,
            tint       = color,
            background = color.copy(alpha = 0.1f),
            size       = 40.dp,
            iconSize   = 19.dp
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = transaction.reason.ifBlank { if (positive) "Připsáno" else "Uplatněno" },
                style    = MaterialTheme.typography.bodyLarge,
                color    = TextPrimary,
                maxLines = 1
            )
            Text(
                text  = timeFmt.format(transaction.createdAt.toDate()),
                style = MaterialTheme.typography.bodySmall,
                color = TextFaint
            )
        }
        Text(
            text  = transaction.formattedAmount,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}

// ── Profil ───────────────────────────────────────────────────────────────────

@Composable
private fun ProfileSheet(user: User, onHowItWorks: () -> Unit, onLogout: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier         = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SageGlow),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = user.initials.ifBlank { "?" },
                style = MaterialTheme.typography.headlineLarge,
                color = Sage
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = user.name, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RankBadge(rank = user.rank, size = 20.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Rank ${user.rank.label}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }

        Spacer(modifier = Modifier.height(22.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Paper)
                .border(1.dp, BorderSoft, RoundedCornerShape(18.dp))
        ) {
            InfoRow("E-mail", user.email)
            HorizontalDivider(color = BorderSoft)
            InfoRow("Telefon", user.phone.ifBlank { "—" })
            HorizontalDivider(color = BorderSoft)
            InfoRow("Členské číslo", user.memberCode.ifBlank { "připravuje se" }.chunked(4).joinToString(" "))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Paper)
                .border(1.dp, BorderSoft, RoundedCornerShape(18.dp))
        ) {
            ActionRow(R.drawable.ic_info, "Jak to funguje", TextPrimary, onHowItWorks)
            HorizontalDivider(color = BorderSoft)
            ActionRow(R.drawable.ic_logout, "Odhlásit se", PointsRed, onLogout)
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text      = "Změnu údajů ti rádi uděláme na prodejně.",
            style     = MaterialTheme.typography.bodySmall,
            color     = TextFaint,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextMuted, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

@Composable
private fun ActionRow(icon: Int, label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CannaIcon(id = icon, tint = color, size = 20.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = color, modifier = Modifier.weight(1f))
        CannaIcon(id = R.drawable.ic_chevron_right, tint = TextFaint, size = 18.dp)
    }
}

// ── Datum ────────────────────────────────────────────────────────────────────

private val czech   = Locale("cs", "CZ")
private val timeFmt = SimpleDateFormat("HH:mm", czech)
private val dayFmt  = SimpleDateFormat("EEEE d. MMMM", czech)
private val yearFmt = SimpleDateFormat("d. MMMM yyyy", czech)

/** Seskupí pohyby podle dne: "Dnes", "Včera", "pondělí 15. září"… (zachová pořadí). */
private fun groupByDay(list: List<Transaction>): List<Pair<String, List<Transaction>>> {
    val now = Calendar.getInstance()
    fun label(tx: Transaction): String {
        val d = Calendar.getInstance().apply { time = tx.createdAt.toDate() }
        val sameYear = d.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        val diff = now.get(Calendar.DAY_OF_YEAR) - d.get(Calendar.DAY_OF_YEAR)
        return when {
            sameYear && diff == 0 -> "Dnes"
            sameYear && diff == 1 -> "Včera"
            sameYear              -> dayFmt.format(d.time).replaceFirstChar { it.uppercase() }
            else                  -> yearFmt.format(d.time)
        }
    }
    return list.groupBy(::label).toList()
}
