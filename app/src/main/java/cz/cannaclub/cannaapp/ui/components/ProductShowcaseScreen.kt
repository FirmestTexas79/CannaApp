package cz.cannaclub.cannaapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import cz.cannaclub.cannaapp.R
import cz.cannaclub.cannaapp.model.Product
import cz.cannaclub.cannaapp.ui.theme.AdminBackground
import cz.cannaclub.cannaapp.ui.theme.BorderNormal
import cz.cannaclub.cannaapp.ui.theme.CannaAppTheme
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.CardHover
import cz.cannaclub.cannaapp.ui.theme.Gold
import cz.cannaclub.cannaapp.ui.theme.LeafDecorAdmin
import cz.cannaclub.cannaapp.ui.theme.PillBackground
import cz.cannaclub.cannaapp.ui.theme.Sage
import cz.cannaclub.cannaapp.ui.theme.SageGlow
import cz.cannaclub.cannaapp.ui.theme.SageLight
import cz.cannaclub.cannaapp.ui.theme.TextFaint
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import cz.cannaclub.cannaapp.ui.theme.TextPrimary
import cz.cannaclub.cannaapp.viewmodel.ProductOperationState
import cz.cannaclub.cannaapp.viewmodel.ProductViewModel

private val PillH = 28.dp
private val PillV = 72.dp

/**
 * Výstavka produktů — zákaznický i admin pohled v jedné obrazovce.
 *
 * [isAdmin] = true → zobrazit tlačítka přidat / upravit / smazat.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ProductShowcaseScreen(
    viewModel: ProductViewModel,
    isAdmin: Boolean = false,
    onBack: () -> Unit
) {
    val products     by viewModel.products.collectAsState()
    val opState      by viewModel.opState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val context      = LocalContext.current

    var detailProduct    by remember { mutableStateOf<Product?>(null) }
    var editProduct      by remember { mutableStateOf<Product?>(null) }
    var showAddDialog    by remember { mutableStateOf(false) }

    // ── Snackbar pro operace ───────────────────────────────────────
    LaunchedEffect(opState) {
        when (val s = opState) {
            is ProductOperationState.Success -> { snackbarHost.showSnackbar(s.message); viewModel.resetOpState() }
            is ProductOperationState.Error   -> { snackbarHost.showSnackbar(s.message); viewModel.resetOpState() }
            else -> {}
        }
    }

    // ── Pozadí shodné s adminem nebo uživatelem ───────────────────
    val bg = if (isAdmin) AdminBackground else cz.cannaclub.cannaapp.ui.theme.Background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        DecorativePlants(
            modifier = Modifier.fillMaxSize(),
            color    = if (isAdmin) LeafDecorAdmin else cz.cannaclub.cannaapp.ui.theme.LeafDecor
        )

        // ── Pill ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PillH, vertical = PillV)
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text  = "CANNACLUB",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Text(
                                text  = "Naše zeleň",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary
                            )
                        }
                        IconButton(onClick = onBack) {
                            Text("←", fontSize = 22.sp, color = TextMuted)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text  = "${products.size} odrůd v nabídce",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextFaint
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // ── Cikcak výstavka ───────────────────────────────
                if (products.isEmpty()) {
                    item {
                        Text(
                            text      = if (isAdmin) "Žádné produkty. Přidej první! →" else "Brzy přidáme novinky 🌿",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = TextMuted,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.fillMaxWidth().padding(vertical = 48.dp)
                        )
                    }
                } else {
                    itemsIndexed(products, key = { _, p -> p.id }) { index, product ->
                        ProductCard(
                            product      = product,
                            context      = context,
                            isAdmin      = isAdmin,
                            imageOnRight = index % 2 != 0,
                            modifier     = Modifier.fillMaxWidth(),
                            onClick      = { detailProduct = product },
                            onEditClick  = { editProduct = product }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                item {
                    Spacer(
                        modifier = Modifier
                            .height(90.dp)
                            .navigationBarsPadding()
                    )
                }
            }

            // ── Admin FAB přidat produkt ───────────────────────────
            if (isAdmin) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 24.dp)
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Gold)
                        .clickable { showAddDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontSize = 26.sp, color = Color.White)
                }
            }
        }

        // ── Snackbar ──────────────────────────────────────────────
        SnackbarHost(
            hostState = snackbarHost,
            modifier  = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(
                snackbarData   = data,
                containerColor = CardDefault,
                contentColor   = TextPrimary,
                shape          = RoundedCornerShape(12.dp),
                modifier       = Modifier.padding(horizontal = PillH + 4.dp)
            )
        }
    }

    // ── Detail popup (zákazník i admin) ───────────────────────────
    detailProduct?.let { product ->
        CannaAppTheme {
            ProductDetailDialog(
                product   = product,
                context   = context,
                isAdmin   = isAdmin,
                onDismiss = { detailProduct = null },
                onEdit    = { editProduct = product; detailProduct = null }
            )
        }
    }

    // ── DIALOG: PŘIDAT PRODUKT ──────────────────────────────────────────
    if (showAddDialog) {
        CannaAppTheme {
            AddEditProductDialog(
                nextOrderIndex = products.size,
                onDismiss = { showAddDialog = false },
                onSave = { productToSave, uriToSave ->
                    // Předán context pro bezpečné zpracování Uri na pozadí
                    viewModel.addProduct(context, productToSave, uriToSave)
                    showAddDialog = false
                }
            )
        }
    }

    // ── DIALOG: UPRAVIT PRODUKT ─────────────────────────────────────────
    editProduct?.let { product ->
        CannaAppTheme {
            AddEditProductDialog(
                product = product,
                onDismiss = { editProduct = null },
                onSave = { productToSave, uriToSave ->
                    // Předán context pro bezpečné zpracování Uri na pozadí
                    viewModel.updateProduct(context, productToSave, uriToSave)
                    editProduct = null
                },
                onDelete = { id ->
                    viewModel.deleteProduct(id)
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Karta v cikcak výstavce — thumbnail + název (+ edit ikona)
// ─────────────────────────────────────────────────────────────────
@Composable
private fun ProductCard(
    product: Product,
    context: android.content.Context,
    isAdmin: Boolean,
    imageOnRight: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val resId = remember(product.imageName) {
        context.resources.getIdentifier(product.imageName, "drawable", context.packageName)
    }

    @Composable
    fun ProductImage() {
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(CardHover)
            )
            if (product.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model              = ImageRequest.Builder(context)
                        .data(product.imageUrl).crossfade(true).build(),
                    contentDescription = product.name,
                    modifier           = Modifier.size(72.dp),
                    contentScale       = ContentScale.Fit,
                    placeholder        = if (resId != 0) painterResource(resId) else null,
                    error              = if (resId != 0) painterResource(resId) else null
                )
            } else if (resId != 0) {
                androidx.compose.foundation.Image(
                    painter            = painterResource(id = resId),
                    contentDescription = product.name,
                    modifier           = Modifier.size(72.dp),
                    contentScale       = ContentScale.Fit
                )
            } else {
                Text("🌿", fontSize = 32.sp)
            }
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(CardDefault)
            .border(1.dp, BorderNormal, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!imageOnRight) ProductImage()

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = product.name,
                style      = MaterialTheme.typography.titleLarge,
                color      = TextPrimary
            )
            if (product.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text     = product.description,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isAdmin) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text     = "✏  upravit",
                    style    = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PillBackground)
                        .clickable { onEditClick() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        if (imageOnRight) ProductImage()
    }
}

// ─────────────────────────────────────────────────────────────────
// Detail popup — temně zatmavené pozadí, obdélníkový popup na výšku
// ─────────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductDetailDialog(
    product: Product,
    context: android.content.Context,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val resId = remember(product.imageName) {
        context.resources.getIdentifier(product.imageName, "drawable", context.packageName)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 88.dp)
                .clickable(onClick = onDismiss)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(PillBackground)
                    .verticalScroll(rememberScrollState())
                    .clickable(enabled = false) {}
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(CardHover),
                    contentAlignment = Alignment.Center
                ) {
                    if (product.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model              = ImageRequest.Builder(context)
                                .data(product.imageUrl).crossfade(true).build(),
                            contentDescription = product.name,
                            modifier           = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            contentScale       = ContentScale.Fit,
                            placeholder        = if (resId != 0) painterResource(resId) else null,
                            error              = if (resId != 0) painterResource(resId) else null
                        )
                    } else if (resId != 0) {
                        androidx.compose.foundation.Image(
                            painter            = painterResource(id = resId),
                            contentDescription = product.name,
                            modifier           = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            contentScale       = ContentScale.Fit
                        )
                    } else {
                        Text("🌿", fontSize = 72.sp)
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text       = product.name,
                    style      = MaterialTheme.typography.headlineMedium,
                    color      = TextPrimary,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (product.forms.isNotEmpty()) {
                    FlowRow(
                        modifier            = Modifier.padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement   = Arrangement.spacedBy(8.dp)
                    ) {
                        product.forms.forEach { form ->
                            Text(
                                text     = form,
                                style    = MaterialTheme.typography.labelSmall,
                                color    = SageLight,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(SageGlow)
                                    .border(1.dp, Sage.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (product.description.isNotBlank()) {
                    Text(
                        text      = product.description,
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (isAdmin) {
                    Text(
                        text     = "✏  Upravit",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = TextMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardDefault)
                            .clickable { onEdit() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}