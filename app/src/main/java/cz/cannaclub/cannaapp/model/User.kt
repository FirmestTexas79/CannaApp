package cz.cannaclub.cannaapp.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val points: Int = 0,
    val totalPoints: Int = 0,   // celkové nasbírané body — základ pro rank
    val dotykackaId: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    constructor() : this("", "", "", "", 0, 0, "", Timestamp.now())

    val initials: String
        get() = name
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }

    // ── Rank podle celkových bodů ─────────────────────────
    val rank: MemberRank
        get() = when {
            totalPoints >= 2500 -> MemberRank.RODINA
            totalPoints >= 1000 -> MemberRank.ZLATY
            totalPoints >= 500  -> MemberRank.STRIBRNY
            totalPoints >= 250  -> MemberRank.BRONZOVY
            else                -> MemberRank.ZAKAZNIK
        }
}

enum class MemberRank(
    val label: String,
    val icon: String,
    val requiredPoints: Int
) {
    ZAKAZNIK("Zákazník",  "🌱", 0),
    BRONZOVY("Bronzový",  "🥉", 250),
    STRIBRNY("Stříbrný",  "🥈", 500),
    ZLATY   ("Zlatý",     "🥇", 1000),
    RODINA  ("Rodina",    "💚", 2500)
}