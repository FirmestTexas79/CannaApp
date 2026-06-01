package cz.cannaclub.cannaapp.model

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Produkt v nabídce Cannaclub.
 *
 * [imageUrl]  = URL v Firebase Storage (primární zdroj obrázku).
 * [imageName] = fallback na lokální drawable (použit při seedování bez uploadu).
 * [forms]     = dostupné formy: ["1g", "3g", "volně"].
 * [orderIndex]= pořadí v cikcak výstavce.
 */
@IgnoreExtraProperties
data class Product(
    val id: String = "",
    val name: String = "",
    val imageUrl: String = "",      // Firebase Storage URL (primární)
    val imageName: String = "",     // lokální drawable fallback
    val forms: List<String> = emptyList(),
    val description: String = "",
    val orderIndex: Int = 0
)