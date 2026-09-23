package cz.cannaclub.cannaapp.model

import com.google.firebase.Timestamp

data class Transaction(
    val id: String = "",
    val type: TransactionType = TransactionType.ADD,
    val amount: Int = 0,
    val reason: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    // Rozpis bodů za nákup (zapisuje server): základ + akce + bonus za rank
    val basePoints: Int = 0,
    val promoBonus: Int = 0,
    val promoLabel: String? = null,
    val rankBonus: Int = 0,
    val rankLabel: String? = null
) {
    // Bezparametrický konstruktor pro Firebase
    constructor() : this(id = "")

    /** "64 b + 64 b Dvojité body + 19 b rank Zlatý", nebo null, když nebyl žádný bonus. */
    val breakdown: String?
        get() {
            if (promoBonus <= 0 && rankBonus <= 0) return null
            val parts = mutableListOf("$basePoints b")
            if (promoBonus > 0) parts += "$promoBonus b ${promoLabel ?: "akce"}"
            if (rankBonus > 0) parts += "$rankBonus b rank ${rankLabel ?: ""}".trimEnd()
            return parts.joinToString(" + ")
        }

    // True pokud jde o přičtení bodů
    val isPositive: Boolean
        get() = type == TransactionType.ADD

    // Formátovaný string pro UI — "+30 b" nebo "−100 b"
    val formattedAmount: String
        get() = if (isPositive) "+$amount b" else "−$amount b"
}

enum class TransactionType {
    ADD,      // přičtení — vrácení obalů, bonus
    SUBTRACT  // odečtení — uplatnění při nákupu
}