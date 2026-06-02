package cz.cannaclub.cannaapp.viewmodel

import android.content.Context
import android.net.Uri
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
                // Vždy přepíše 4 základní produkty (fixní ID) — změna v kódu = změna ve Firebase
                repository.seedProducts()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Real-time stream
            repository.getProductsFlow().collect { list ->
                _products.value = list
            }
        }
    }

    // ── Převod Uri → ByteArray bezpečně na IO vlákně ─────────────
    private suspend fun readBytesFromUri(context: Context, uri: Uri): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (e: Exception) {
                e.printStackTrace()
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
                    if (bytes != null) {
                        // Nahraj obrázek pod stejným ID jako produkt
                        val url = repository.uploadProductImage(bytes, product.id)
                        product.copy(imageUrl = url)
                    } else {
                        product
                    }
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
                    if (bytes != null) {
                        val url = repository.uploadProductImage(bytes, product.id)
                        product.copy(imageUrl = url)             // ← imageUrl (opraveno)
                    } else {
                        product
                    }
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