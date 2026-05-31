package cz.cannaclub.cannaapp.model

import com.google.firebase.Timestamp

data class Transaction(
    val id: String = "",
    val type: TransactionType = TransactionType.ADD,
    val amount: Int = 0,
    val reason: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    // Bezparametrický konstruktor pro Firebase
    constructor() : this("", TransactionType.ADD, 0, "", Timestamp.now())

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