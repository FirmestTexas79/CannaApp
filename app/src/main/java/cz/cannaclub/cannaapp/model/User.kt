package cz.cannaclub.cannaapp.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val points: Int = 0,
    val totalPoints: Int = 0,
    val dotykackaId: String = "",
    val fcmToken: String = "",
    val memberCode: String = "",    // číselný členský kód = "Čárový kód" zákazníka v Dotykačce
    val createdAt: Timestamp = Timestamp.now()
) {
    constructor() : this("", "", "", "", 0, 0, "", "", "", Timestamp.now())

    /** Kód, který appka ukazuje k naskenování. Než server přidělí členský kód, použije se ID. */
    @get:Exclude
    val scanCode: String
        get() = memberCode.ifBlank { id }
    val initials: String
        get() = name
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }

    // ── Rank podle celkových bodů ─────────────────────────
    @get:Exclude
    val rank: MemberRank
        get() = MemberRank.forPoints(totalPoints)

    /** Křestní jméno pro oslovení. */
    @get:Exclude
    val firstName: String
        get() = name.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
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
    RODINA  ("Rodina",    "💚", 2500);

    /** Následující rank, nebo null u nejvyššího. */
    val next: MemberRank?
        get() = entries.getOrNull(ordinal + 1)

    /** Postup k dalšímu ranku 0..1 (u nejvyššího 1). */
    fun progress(totalPoints: Int): Float {
        val n = next ?: return 1f
        return ((totalPoints - requiredPoints).toFloat() / (n.requiredPoints - requiredPoints)).coerceIn(0f, 1f)
    }

    companion object {
        fun forPoints(totalPoints: Int): MemberRank =
            entries.last { totalPoints >= it.requiredPoints }
    }
}
