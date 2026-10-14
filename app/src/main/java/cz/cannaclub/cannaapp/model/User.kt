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
    private val defaultPoints: Int
) {
    ZAKAZNIK("Zákazník",  "🌱", 0),
    BRONZOVY("Bronzový",  "🥉", 500),
    STRIBRNY("Stříbrný",  "🥈", 1500),
    ZLATY   ("Zlatý",     "🥇", 4000),
    RODINA  ("Rodina",    "💚", 10000);

    /** Body (za celou dobu) potřebné na rank — z config/rewards.rankFrom, jinak výchozí. */
    val requiredPoints: Int
        get() = RankScale.from[name] ?: defaultPoints

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

/**
 * Hranice ranků nastavené obsluhou v adminu (config/rewards.rankFrom).
 * Stavová mapa → Compose se při změně sám překreslí.
 */
object RankScale {
    val from = androidx.compose.runtime.mutableStateMapOf<String, Int>()

    fun apply(rankFrom: Map<String, Int>) {
        val steps = MemberRank.entries.drop(1).map { rankFrom[it.name] ?: 0 }
        from.clear()
        if (steps.all { it > 0 } && steps.zipWithNext().all { (a, b) -> b > a }) {
            MemberRank.entries.drop(1).forEachIndexed { i, r -> from[r.name] = steps[i] }
        }
    }
}
