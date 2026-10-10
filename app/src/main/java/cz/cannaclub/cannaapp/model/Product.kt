package cz.cannaclub.cannaapp.model

import com.google.firebase.firestore.Exclude
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
    val orderIndex: Int = 0,
    val category: String = ""
) {
    /** Kategorie pro zobrazení; starší produkty bez kategorie patří ke konopí. */
    @get:Exclude
    val displayCategory: String
        get() = category.takeIf { it in CATEGORIES } ?: CATEGORIES.first()

    companion object {
        /** Pořadí kategorií v nabídce (stejné jako CATEGORIES ve webové appce). */
        val CATEGORIES = listOf("Konopí", "CBD", "Kosmetika", "Kuřácké potřeby", "Pro mazlíčky", "Pití", "Doplňky")
    }
}