package cz.cannaclub.cannaapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import cz.cannaclub.cannaapp.R
import cz.cannaclub.cannaapp.model.Product
import cz.cannaclub.cannaapp.ui.theme.BorderSoft
import cz.cannaclub.cannaapp.ui.theme.CannaAppTheme
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.CardHover
import cz.cannaclub.cannaapp.ui.theme.Forest
import cz.cannaclub.cannaapp.ui.theme.Gold
import cz.cannaclub.cannaapp.ui.theme.OnBrand
import cz.cannaclub.cannaapp.ui.theme.Paper
import cz.cannaclub.cannaapp.ui.theme.PillBackground
import cz.cannaclub.cannaapp.ui.theme.Sage
import cz.cannaclub.cannaapp.ui.theme.SageGlow
import cz.cannaclub.cannaapp.ui.theme.Surface
import cz.cannaclub.cannaapp.ui.theme.TextFaint
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import cz.cannaclub.cannaapp.ui.theme.TextPrimary
import cz.cannaclub.cannaapp.viewmodel.ProductOperationState
import cz.cannaclub.cannaapp.viewmodel.ProductViewModel

/**
 * Nabídka — zákaznický i admin pohled v jedné obrazovce.
 * Hlavička + kategorie, první produkt kategorie velký, zbytek v mřížce 2×N.
 * [isAdmin] = true → přidat / upravit.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val gridState    = rememberLazyGridState()

    var detailProduct by remember { mutableStateOf<Product?>(null) }
    var editProduct   by remember { mutableStateOf<Product?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var category      by remember { mutableStateOf(Product.CATEGORIES.first()) }
    val categories = remember(products) { Product.CATEGORIES.filter { c -> products.any { it.displayCategory == c } } }
    val current    = category.takeIf { it in categories } ?: categories.firstOrNull() ?: category
    val shown      = remember(products, current) { products.filter { it.displayCategory == current } }

    // Fotky stáhnout dopředu do cache → při přepínání kategorií už jsou hned vidět
    LaunchedEffect(products) {
        products.filter { it.imageUrl.isNotEmpty() }.forEach {
            context.imageLoader.enqueue(ImageRequest.Builder(context).data(it.imageUrl).size(600).build())
        }
    }
    LaunchedEffect(current) { gridState.scrollToItem(0) }

    LaunchedEffect(opState) {
        when (val s = opState) {
            is ProductOperationState.Success -> { snackbarHost.showSnackbar(s.message); viewModel.resetOpState() }
            is ProductOperationState.Error   -> { snackbarHost.showSnackbar(s.message); viewModel.resetOpState() }
            else -> {}
        }
    }

    SystemBarsAppearance(lightBackground = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PillBackground)
    ) {
        LazyVerticalGrid(
            columns               = GridCells.Fixed(2),
            state                 = gridState,
            modifier              = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding        = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement   = Arrangement.spacedBy(14.dp)
        ) {
            // ── Hlavička ──────────────────────────────────────
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RoundButton(R.drawable.ic_chevron_left, "Zpět", onBack)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text     = "${products.size} produktů",
                            style    = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color    = Sage,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(SageGlow)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                    Text("CannaClub · Hradec Králové", style = MaterialTheme.typography.labelSmall, color = TextFaint)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = "Nabídka",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp, letterSpacing = (-0.5).sp),
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = "Na prodejně ti s výběrem rádi poradíme.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }

            // ── Kategorie ─────────────────────────────────────
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    items(categories) { c ->
                        CategoryChip(
                            label    = c,
                            count    = products.count { it.displayCategory == c },
                            selected = c == current,
                            onClick  = { category = c }
                        )
                    }
                }
            }

            if (products.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.fillMaxWidth().padding(vertical = 56.dp)
                    ) {
                        IconBubble(R.drawable.ic_leaf, tint = Sage, background = SageGlow, size = 64.dp, iconSize = 28.dp)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text      = if (isAdmin) "Zatím žádné produkty.\nPřidej první tlačítkem +" else "Nabídku právě doplňujeme.",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                shown.firstOrNull()?.let { first ->
                    item(key = first.id, span = { GridItemSpan(maxLineSpan) }) {
                        FeaturedCard(first, isAdmin, onClick = { detailProduct = first }, onEdit = { editProduct = first })
                    }
                }
                items(shown.drop(1), key = { it.id }) { p ->
                    GridCard(p, isAdmin, onClick = { detailProduct = p }, onEdit = { editProduct = p })
                }
            }
        }

        // ── Admin: přidat produkt ─────────────────────────────
        if (isAdmin) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 20.dp)
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Gold)
                    .clickable { showAddDialog = true },
                contentAlignment = Alignment.Center
            ) {
                CannaIcon(R.drawable.ic_plus, tint = Color.White, size = 24.dp, contentDescription = "Přidat produkt")
            }
        }

        SnackbarHost(
            hostState = snackbarHost,
            modifier  = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
        ) { data ->
            Snackbar(
                snackbarData   = data,
                containerColor = TextPrimary,
                contentColor   = PillBackground,
                shape          = RoundedCornerShape(14.dp),
                modifier       = Modifier.padding(horizontal = 18.dp)
            )
        }
    }

    // ── Detail produktu ───────────────────────────────────────
    detailProduct?.let { product ->
        ModalBottomSheet(
            onDismissRequest = { detailProduct = null },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = Surface,
            shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            ProductDetail(
                product = product,
                isAdmin = isAdmin,
                onEdit  = { editProduct = product; detailProduct = null }
            )
        }
    }

    if (showAddDialog) {
        CannaAppTheme {
            AddEditProductDialog(
                nextOrderIndex  = (products.maxOfOrNull { it.orderIndex } ?: -1) + 1,
                defaultCategory = current,
                onDismiss       = { showAddDialog = false },
                onSave          = { productToSave, uriToSave ->
                    viewModel.addProduct(context, productToSave, uriToSave)
                    showAddDialog = false
                }
            )
        }
    }

    editProduct?.let { product ->
        CannaAppTheme {
            AddEditProductDialog(
                product   = product,
                onDismiss = { editProduct = null },
                onSave    = { productToSave, uriToSave ->
                    viewModel.updateProduct(context, productToSave, uriToSave)
                    editProduct = null
                },
                onDelete  = { id -> viewModel.deleteProduct(id) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Stavební kameny
// ─────────────────────────────────────────────────────────────────

@Composable
private fun RoundButton(icon: Int, description: String, onClick: () -> Unit, tint: Color = TextPrimary, bg: Color = Paper) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, BorderSoft, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CannaIcon(icon, tint = tint, size = 20.dp, contentDescription = description)
    }
}

@Composable
private fun CategoryChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Sage else Paper, label = "chipBg")
    val fg by animateColorAsState(if (selected) OnBrand else TextPrimary, label = "chipFg")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, if (selected) Sage else BorderSoft, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 10.dp, top = 9.dp, bottom = 9.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
        Spacer(Modifier.size(6.dp))
        Text(
            text     = "$count",
            style    = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
            color    = if (selected) OnBrand else TextMuted,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) Color.White.copy(alpha = 0.16f) else CardDefault)
                .padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}

/** Fotka produktu: vlastní fotka (ořez), nebo průhledná sklenice z drawable (celá), nebo lísteček. */
@Composable
private fun ProductPhoto(product: Product, modifier: Modifier, jarPadding: PaddingValues = PaddingValues(14.dp)) {
    val context = LocalContext.current
    val resId = remember(product.imageName) {
        if (product.imageName.isBlank()) 0
        else context.resources.getIdentifier(product.imageName, "drawable", context.packageName)
    }
    Box(
        modifier = modifier.background(
            Brush.radialGradient(listOf(Paper, CardHover))
        ),
        contentAlignment = Alignment.Center
    ) {
        var failed by remember(product.imageUrl) { mutableStateOf(false) }
        when {
            resId != 0 -> androidx.compose.foundation.Image(
                painter            = painterResource(resId),
                contentDescription = product.name,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.fillMaxSize().padding(jarPadding)
            )
            product.imageUrl.isNotEmpty() && !failed -> AsyncImage(
                model              = ImageRequest.Builder(context).data(product.imageUrl).size(600).crossfade(180).build(),
                contentDescription = product.name,
                contentScale       = ContentScale.Crop,
                placeholder        = ColorPainter(CardHover),
                onError            = { failed = true },
                modifier           = Modifier.fillMaxSize()
            )
            else -> CannaIcon(R.drawable.ic_leaf, tint = Sage.copy(alpha = 0.45f), size = 36.dp)
        }
    }
}

/** Kartička se stiskem: lehce se zmenší pod prstem. */
@Composable
private fun PressCard(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .scale(if (pressed) 0.98f else 1f)
            .clip(RoundedCornerShape(24.dp))
            .background(Paper)
            .border(1.dp, BorderSoft, RoundedCornerShape(24.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) { content() }
}

@Composable
private fun FormsLine(forms: List<String>, color: Color = Sage) {
    if (forms.isEmpty()) return
    Text(
        text     = forms.joinToString("  ·  "),
        style    = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color    = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun EditBadge(onEdit: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Gold)
            .clickable(onClick = onEdit),
        contentAlignment = Alignment.Center
    ) { CannaIcon(R.drawable.ic_edit, tint = Color.White, size = 16.dp, contentDescription = "Upravit") }
}

/** První produkt kategorie — velká fotka, název přes spodní přechod. */
@Composable
private fun FeaturedCard(product: Product, isAdmin: Boolean, onClick: () -> Unit, onEdit: () -> Unit) {
    PressCard(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.25f)) {
            ProductPhoto(product, Modifier.fillMaxSize(), jarPadding = PaddingValues(start = 70.dp, end = 70.dp, top = 14.dp, bottom = 84.dp))
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Forest.copy(alpha = 0.92f)))
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
            ) {
                Text(
                    text  = product.displayCategory.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnBrand.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = product.name,
                    style    = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 34.sp),
                    color    = OnBrand,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (product.forms.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    FormsLine(product.forms, color = OnBrand)
                }
            }
            if (isAdmin) EditBadge(onEdit, Modifier.align(Alignment.TopEnd).padding(12.dp))
        }
    }
}

@Composable
private fun GridCard(product: Product, isAdmin: Boolean, onClick: () -> Unit, onEdit: () -> Unit) {
    PressCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(6.dp)) {
            Box {
                ProductPhoto(
                    product,
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp))
                )
                if (isAdmin) EditBadge(onEdit, Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
            Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 8.dp)) {
                Text(
                    text     = product.name,
                    style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, lineHeight = 19.sp),
                    color    = TextPrimary,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                if (product.forms.isNotEmpty()) FormsLine(product.forms)
                else Text("Detail", style = MaterialTheme.typography.bodySmall, color = TextFaint)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductDetail(product: Product, isAdmin: Boolean, onEdit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        ProductPhoto(
            product,
            Modifier.fillMaxWidth().aspectRatio(1.15f).clip(RoundedCornerShape(24.dp)),
            jarPadding = PaddingValues(30.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(product.displayCategory.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextFaint)
        Spacer(Modifier.height(4.dp))
        Text(product.name, style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        if (product.forms.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                product.forms.forEach { form ->
                    Text(
                        text     = form,
                        style    = MaterialTheme.typography.labelLarge,
                        color    = Sage,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SageGlow)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
        if (product.description.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text  = product.description,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = TextMuted
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardDefault)
                .padding(14.dp)
        ) {
            CannaIcon(R.drawable.ic_info, tint = TextMuted, size = 18.dp)
            Spacer(Modifier.size(10.dp))
            Text(
                "K nákupu ukaž kartičku — body se ti připíšou samy.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        if (isAdmin) {
            Spacer(Modifier.height(14.dp))
            Text(
                text      = "Upravit produkt",
                style     = MaterialTheme.typography.labelLarge,
                color     = Color.White,
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Gold)
                    .clickable(onClick = onEdit)
                    .padding(vertical = 14.dp)
            )
        }
    }
}
