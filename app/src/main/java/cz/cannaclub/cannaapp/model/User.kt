package cz.cannaclub.cannaapp.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val points: Int = 0,
    val createdAt: Timestamp = Timestamp.now()
) {
    // Bezparametrický konstruktor — Firebase ho vyžaduje pro deserializaci
    constructor() : this("", "", "", "", 0, Timestamp.now())

    // Iniciály pro avatar v admin seznamu — "Jan Novák" → "JN"
    val initials: String
        get() = name
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
}