package cz.cannaclub.cannaapp.model

import com.google.firebase.Timestamp

/**
 * Pravidla věrnostního programu.
 *  - bonus za rank je pevný a počítá se automaticky podle ranku zákazníka
 *    (musí sedět s RANK_BONUS ve functions/loyalty.js)
 *  - akci "více bodů" spouští obsluha z adminu (Firestore config/loyalty, zapisuje jen server)
 */
data class LoyaltyConfig(
    /** Bonus bodů za nákup v % podle ranku, klíč = MemberRank.name (pevný). */
    val rankBonus: Map<String, Int> = DEFAULT_RANK_BONUS,
    val promo: Promo? = null
) {
    fun bonusFor(rank: MemberRank): Int = rankBonus[rank.name] ?: DEFAULT_RANK_BONUS[rank.name] ?: 0

    /** Akce, která právě běží, nebo null. */
    fun activePromo(now: Long = System.currentTimeMillis()): Promo? =
        promo?.takeIf { it.multiplier > 1 && now in it.startsAtMillis..it.endsAtMillis }

    companion object {
        val DEFAULT_RANK_BONUS = mapOf(
            "ZAKAZNIK" to 0, "BRONZOVY" to 5, "STRIBRNY" to 10, "ZLATY" to 15, "RODINA" to 20
        )

        /** Ruční převod z Firestore (mapa s čísly jako Long). */
        @Suppress("UNCHECKED_CAST")
        fun from(data: Map<String, Any?>?): LoyaltyConfig {
            if (data == null) return LoyaltyConfig()
            val p = data["promo"] as? Map<String, Any?>
            val promo = p?.let {
                val ends = it["endsAt"] as? Timestamp ?: return@let null
                Promo(
                    multiplier     = (it["multiplier"] as? Number)?.toInt() ?: 1,
                    label          = it["label"] as? String ?: "Více bodů",
                    startsAtMillis = (it["startsAt"] as? Timestamp)?.toDate()?.time ?: 0L,
                    endsAtMillis   = ends.toDate().time
                )
            }
            return LoyaltyConfig(promo = promo)
        }
    }
}

data class Promo(
    val multiplier: Int,
    val label: String,
    val startsAtMillis: Long,
    val endsAtMillis: Long
)
