package cz.cannaclub.cannaapp.ui.user

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.cannaclub.cannaapp.R
import cz.cannaclub.cannaapp.model.Transaction
import cz.cannaclub.cannaapp.model.TransactionType
import cz.cannaclub.cannaapp.ui.components.DecorativePlants
import cz.cannaclub.cannaapp.ui.components.PointsCard
import cz.cannaclub.cannaapp.ui.theme.Background
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.Cream
import cz.cannaclub.cannaapp.ui.theme.LeafDecor
import cz.cannaclub.cannaapp.ui.theme.PillBackground
import cz.cannaclub.cannaapp.ui.theme.PointsGreen
import cz.cannaclub.cannaapp.ui.theme.PointsRed
import cz.cannaclub.cannaapp.ui.theme.Sage
import cz.cannaclub.cannaapp.ui.theme.SageGlow
import cz.cannaclub.cannaapp.ui.theme.Surface
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import cz.cannaclub.cannaapp.ui.theme.TextPrimary
import cz.cannaclub.cannaapp.viewmodel.UserViewModel

private val PillHorizontalPadding = 28.dp
private val PillVerticalPadding   = 72.dp

private val sharpenMatrix = ColorMatrix(
    floatArrayOf(
        1.5f,  0f,   0f,   0f, -30f,
        0f,    1.5f, 0f,   0f, -30f,
        0f,    0f,   1.5f, 0f, -30f,
        0f,    0f,   0f,   1f,   0f
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: UserViewModel,
    onLogout: () -> Unit,
    onProductsClick: () -> Unit = {}
) {
    val user         by viewModel.currentUser.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    var showRewards  by remember { mutableStateOf(false) }
    val sheetState   = rememberModalBottomSheetState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        DecorativePlants(modifier = Modifier.fillMaxSize())

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
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp)
            ) {

                // ── Logo ──────────────────────────────────────
                item {
                    Image(
                        painter            = painterResource(id = R.drawable.cannalogo),
                        contentDescription = "Cannaclub logo",
                        modifier           = Modifier
                            .fillMaxWidth()
                            .scale(1.5f, 1.5f)
                            .height(150.dp),
                        contentScale       = ContentScale.Fit,
                        colorFilter        = ColorFilter.colorMatrix(sharpenMatrix)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // ── Hlavička ──────────────────────────────────
                item {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text  = "VÍTEJ ZPĚT",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text  = user?.name ?: "",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Cream
                            )
                        }
                        IconButton(onClick = onLogout) {
                            Text(text = "←", fontSize = 22.sp, color = TextMuted)
                        }
                    }
                    Spacer(modifier = Modifier.height(22.dp))
                }

                // ── Karta bodů ────────────────────────────────
                item {
                    PointsCard(
                        points         = user?.points ?: 0,
                        onRewardsClick = { showRewards = true }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ── Tlačítko Naše zeleň — výrazné, centrované ─
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(SageGlow)
                            .border(1.5.dp, Sage.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .clickable { onProductsClick() }
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text       = "🌿  Naše zeleň",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color      = Sage,
                                textAlign  = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text      = "Zobrazit nabídku produktů",
                                style     = MaterialTheme.typography.bodySmall,
                                color     = Sage.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // ── Transakce ─────────────────────────────────
                item {
                    Text(
                        text  = "POSLEDNÍ POHYBY",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (transactions.isEmpty()) {
                    item {
                        Text(
                            text      = "Zatím žádné pohyby",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = TextMuted,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        )
                    }
                } else {
                    items(transactions) { transaction ->
                        TransactionRow(transaction = transaction)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }

        if (showRewards) {
            ModalBottomSheet(
                onDismissRequest = { showRewards = false },
                sheetState       = sheetState,
                containerColor   = Surface,
                shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                RewardsSheet(currentPoints = user?.points ?: 0)
            }
        }
    }
}

@Composable
fun TransactionRow(transaction: Transaction) {
    val isPositive  = transaction.type == TransactionType.ADD
    val dotColor    = if (isPositive) PointsGreen else PointsRed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardDefault)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = transaction.reason,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = formatTimestamp(transaction.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        Text(
            text  = transaction.formattedAmount,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
            color = dotColor
        )
    }
}

private val dateFmt = java.text.SimpleDateFormat("d. M.", java.util.Locale("cs"))
private val timeFmt = java.text.SimpleDateFormat("HH:mm",  java.util.Locale("cs"))

private fun formatTimestamp(timestamp: com.google.firebase.Timestamp): String {
    val now  = java.util.Calendar.getInstance()
    val date = java.util.Calendar.getInstance().apply { time = timestamp.toDate() }
    return if (
        now.get(java.util.Calendar.DAY_OF_YEAR) == date.get(java.util.Calendar.DAY_OF_YEAR) &&
        now.get(java.util.Calendar.YEAR) == date.get(java.util.Calendar.YEAR)
    ) {
        "dnes · ${timeFmt.format(timestamp.toDate())}"
    } else {
        dateFmt.format(timestamp.toDate())
    }
}