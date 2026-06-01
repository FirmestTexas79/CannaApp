package cz.cannaclub.cannaapp.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import cz.cannaclub.cannaapp.model.Product
import cz.cannaclub.cannaapp.ui.theme.BorderNormal
import cz.cannaclub.cannaapp.ui.theme.CardDefault
import cz.cannaclub.cannaapp.ui.theme.CardHover
import cz.cannaclub.cannaapp.ui.theme.Gold
import cz.cannaclub.cannaapp.ui.theme.PillBackground
import cz.cannaclub.cannaapp.ui.theme.PointsRed
import cz.cannaclub.cannaapp.ui.theme.Sage
import cz.cannaclub.cannaapp.ui.theme.SageGlow
import cz.cannaclub.cannaapp.ui.theme.Surface
import cz.cannaclub.cannaapp.ui.theme.TextMuted
import cz.cannaclub.cannaapp.ui.theme.TextPrimary

private val ALL_FORMS = listOf("1g", "3g", "volně", "5g", "drť", "preroll")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditProductDialog(
    product: Product? = null,
    nextOrderIndex: Int = 0,
    onDismiss: () -> Unit,
    onSave: (Product, Uri?) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val isEdit = product != null
    val context = LocalContext.current

    var name        by remember { mutableStateOf(product?.name        ?: "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    val forms = remember {
        mutableStateListOf<String>().apply { addAll(product?.forms ?: listOf("1g", "3g", "volně")) }
    }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) selectedImageUri = uri
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(PillBackground)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text  = if (isEdit) "Upravit produkt" else "Přidat produkt",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text  = "OBRÁZEK",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardHover)
                        .border(1.dp, BorderNormal, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        selectedImageUri != null -> {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(selectedImageUri).crossfade(true).build(),
                                contentDescription = name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        product?.imageUrl?.isNotEmpty() == true -> {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(product.imageUrl).crossfade(true).build(),
                                contentDescription = name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> {
                            val resId = remember(product?.imageName) {
                                if (product?.imageName?.isNotEmpty() == true)
                                    context.resources.getIdentifier(
                                        product.imageName, "drawable", context.packageName
                                    ) else 0
                            }
                            if (resId != 0) {
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text("🌿", fontSize = 30.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = if (selectedImageUri != null) "Nový obrázek vybrán ✓"
                        else if (product?.imageUrl?.isNotEmpty() == true) "Stávající obrázek"
                        else "Žádný obrázek",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectedImageUri != null) Sage else TextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "📷  Vybrat z galerie",
                        style = MaterialTheme.typography.labelSmall,
                        color = Gold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardDefault)
                            .clickable { imagePicker.launch("image/*") }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ProductField(
                value = name,
                onValueChange = { name = it },
                label = "Název odrůdy"
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProductField(
                value = description,
                onValueChange = { description = it },
                label = "Popis odrůdy",
                singleLine = false,
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text  = "DOSTUPNÉ FORMY",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ALL_FORMS.forEach { form ->
                    val selected = form in forms
                    Text(
                        text = form,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Sage else TextMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) SageGlow else CardDefault)
                            .border(
                                width = 1.dp,
                                color = if (selected) Sage.copy(alpha = 0.4f) else BorderNormal,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                if (selected) forms.remove(form) else forms.add(form)
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                if (isEdit && onDelete != null) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = PointsRed)
                    ) {
                        Text("Smazat")
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = onDismiss) {
                    Text("Zrušit", color = TextMuted)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        // 1. Pokud je to editace, použijeme stávající ID.
                        // Pokud je to nový produkt, vygenerujeme ID (např. přes UUID),
                        // aby měl repozitář a Storage hned od začátku jasnou lokaci.
                        val finalId = if (isEdit) (product?.id ?: "") else java.util.UUID.randomUUID().toString()

                        val saving = Product(
                            id          = finalId,
                            name        = name.trim(),
                            imageName   = product?.imageName ?: "",
                            imageUrl    = product?.imageUrl ?: "", // Pokud je prázdná, ViewModel s ní musí umět pracovat
                            forms       = forms.toList(),
                            description = description.trim(),
                            orderIndex  = product?.orderIndex ?: nextOrderIndex
                        )
                        onSave(saving, selectedImageUri)
                    },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Sage,
                        contentColor = Color.White
                    )
                ) {
                    Text("Uložit")
                }
            }
        }
    }

    if (showDeleteConfirm && product != null && onDelete != null) {
        Dialog(onDismissRequest = { showDeleteConfirm = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface)
                    .padding(24.dp)
            ) {
                Text(
                    text  = "Smazat „${product.name}“",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text  = "Tato akce je nevratná.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Zrušit", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onDelete(product.id); onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = PointsRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Smazat", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column {
        Text(
            text  = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(5.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder, color = TextMuted.copy(alpha = 0.5f)) },
            singleLine = singleLine,
            minLines = minLines,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = Sage,
                unfocusedBorderColor    = BorderNormal,
                focusedContainerColor   = CardDefault,
                unfocusedContainerColor = CardDefault,
                focusedTextColor        = TextPrimary,
                unfocusedTextColor      = TextPrimary,
                cursorColor             = Sage
            )
        )
    }
}