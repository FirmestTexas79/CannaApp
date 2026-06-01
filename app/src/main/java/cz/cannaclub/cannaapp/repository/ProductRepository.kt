package cz.cannaclub.cannaapp.repository

import cz.cannaclub.cannaapp.firebase.FirebaseManager
import cz.cannaclub.cannaapp.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProductRepository {

    // ── Seed produkty — fixní ID, vždy se přepíší při spuštění ──────
    // Změna textu/pořadí v kódu se hned projeví i ve Firebase.
    private val seedProducts = listOf(
        Product(
            id          = "seed_lemon_haze",
            name        = "Lemon Haze",
            imageName   = "produkt1",
            forms       = listOf("1g", "3g", "volně"),
            description = "Svěží sativa s intenzivní citrusovou vůní. Povzbuzující efekt vhodný pro kreativní chvíle a společenské setkání.",
            orderIndex  = 0
        ),
        Product(
            id          = "seed_ak47",
            name        = "AK-47",
            imageName   = "produkt2",
            forms       = listOf("1g", "3g", "volně"),
            description = "Legendární hybridní odrůda se zemitou a kořeněnou chutí. Vyvážený efekt mezi relaxací a mentální čilostí.",
            orderIndex  = 1
        ),
        Product(
            id          = "seed_orange_bud",
            name        = "Orange Bud",
            imageName   = "produkt3",
            forms       = listOf("1g", "3g", "volně"),
            description = "Hybridní odrůda s lahodnou pomerančovou vůní a jemnou sladkostí. Příjemně euforická, ideální pro večerní odpočinek.",
            orderIndex  = 2
        ),
        Product(
            id          = "seed_skunk_candy",
            name        = "Skunk Candy",
            imageName   = "produkt4",
            forms       = listOf("1g", "3g", "volně"),
            description = "Sladká a ovocná odrůda s výrazným candy aroma. Jemná zemitá note a příjemná relaxace — oblíbená volba pro odpoledne.",
            orderIndex  = 3
        )
    )

    // ── Real-time stream produktů z Firestore ─────────────────────
    fun getProductsFlow(): Flow<List<Product>> = callbackFlow {
        val listener = FirebaseManager.productsCol
            .orderBy("orderIndex")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── Seed — vždy přepíše seed produkty (fixní ID) ─────────────
    // Admin-přidané produkty (jiná ID) zůstanou nedotčeny.
    suspend fun seedProducts() {
        seedProducts.forEach { product ->
            FirebaseManager.productsCol.document(product.id).set(product).await()
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────
    suspend fun addProduct(product: Product): String {
        val docRef = FirebaseManager.productsCol.document()
        val finalProduct = product.copy(id = docRef.id)
        docRef.set(finalProduct).await()
        return docRef.id
    }

    suspend fun updateProduct(product: Product) {
        FirebaseManager.productsCol.document(product.id).set(product).await()
    }

    suspend fun deleteProduct(id: String) {
        FirebaseManager.productsCol.document(id).delete().await()
        try {
            FirebaseManager.storage.reference.child("products/$id.jpg").delete().await()
        } catch (_: Exception) {}
    }

    // ── Upload obrázku do Firebase Storage (ByteArray) ───────────
    suspend fun uploadProductImage(imageBytes: ByteArray, productId: String): String {
        val storageRef = FirebaseManager.storage.reference
            .child("products/$productId.jpg")
        val snapshot = storageRef.putBytes(imageBytes).await()
        return snapshot.storage.downloadUrl.await().toString()
    }
}