package cz.cannaclub.cannaapp.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.cannaclub.cannaapp.model.Product
import cz.cannaclub.cannaapp.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private const val MAX_IMAGE_SIDE = 1024

sealed class ProductOperationState {
    object Idle    : ProductOperationState()
    object Loading : ProductOperationState()
    data class Success(val message: String) : ProductOperationState()
    data class Error(val message: String)   : ProductOperationState()
}

class ProductViewModel : ViewModel() {

    private val repository = ProductRepository()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _opState = MutableStateFlow<ProductOperationState>(ProductOperationState.Idle)
    val opState: StateFlow<ProductOperationState> = _opState.asStateFlow()

    init {
        seedAndLoad()
    }

    private fun seedAndLoad() {
        viewModelScope.launch {
            try {
                repository.seedProductsIfNeeded()
            } catch (e: Exception) {
                android.util.Log.w("Products", "Seed přeskočen", e)
            }
            // Real-time stream
            repository.getProductsFlow().collect { list ->
                _products.value = list
            }
        }
    }

    // ── Převod Uri → ByteArray bezpečně na IO vlákně ─────────────
    /**
     * Načte obrázek z galerie, zmenší ho na max 1024 px, otočí podle EXIF
     * a převede na JPEG. Fotky z mobilu mají klidně 5–10 MB nebo formát HEIC,
     * což se pomalu nahrává a v seznamu se nemusí vůbec vykreslit.
     */
    private suspend fun readBytesFromUri(context: Context, uri: Uri): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // ImageDecoder umí HEIC i otočení podle EXIF
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        val w = info.size.width
                        val h = info.size.height
                        val scale = minOf(1f, MAX_IMAGE_SIDE.toFloat() / maxOf(w, h))
                        if (scale < 1f) decoder.setTargetSize((w * scale).toInt(), (h * scale).toInt())
                    }
                } else {
                    // Android 8.x
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_IMAGE_SIDE) sample *= 2
                    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                        ?: return@withContext null
                }
                ByteArrayOutputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    out.toByteArray()
                }
            } catch (e: Exception) {
                android.util.Log.e("Products", "Obrázek se nepodařilo načíst: $uri", e)
                null
            }
        }

    // ── Přidat produkt (volitelně s obrázkem z galerie) ──────────
    fun addProduct(context: Context, product: Product, imageUri: Uri? = null) {
        viewModelScope.launch {
            _opState.value = ProductOperationState.Loading
            try {
                // Nejdřív nahraj obrázek pokud existuje — ID je už známé z dialogu
                val finalProduct = if (imageUri != null) {
                    val bytes = readBytesFromUri(context, imageUri)
                        ?: throw IllegalStateException("Obrázek se nepodařilo načíst z galerie")
                    val url = repository.uploadProductImage(bytes, product.id)
                    product.copy(imageUrl = url)
                } else {
                    product
                }

                // Ulož produkt s imageUrl do Firestore
                repository.addProduct(finalProduct)
                _opState.value = ProductOperationState.Success("Produkt přidán")
            } catch (e: Exception) {
                e.printStackTrace()
                _opState.value = ProductOperationState.Error("Chyba: ${e.localizedMessage}")
            }
        }
    }

    // ── Upravit produkt (volitelně s novým obrázkem) ─────────────
    fun updateProduct(context: Context, product: Product, imageUri: Uri? = null) {
        viewModelScope.launch {
            _opState.value = ProductOperationState.Loading
            try {
                val updated = if (imageUri != null) {
                    val bytes = readBytesFromUri(context, imageUri)
                        ?: throw IllegalStateException("Obrázek se nepodařilo načíst z galerie")
                    val url = repository.uploadProductImage(bytes, product.id)
                    product.copy(imageUrl = url)
                } else {
                    product
                }

                repository.updateProduct(updated)
                _opState.value = ProductOperationState.Success("Produkt upraven")
            } catch (e: Exception) {
                e.printStackTrace()
                _opState.value = ProductOperationState.Error("Úprava selhala: ${e.localizedMessage}")
            }
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch {
            _opState.value = ProductOperationState.Loading
            try {
                repository.deleteProduct(id)
                _opState.value = ProductOperationState.Success("Produkt smazán")
            } catch (e: Exception) {
                _opState.value = ProductOperationState.Error("Chyba: ${e.localizedMessage}")
            }
        }
    }

    fun resetOpState() { _opState.value = ProductOperationState.Idle }
}