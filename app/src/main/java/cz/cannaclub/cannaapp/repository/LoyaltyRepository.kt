package cz.cannaclub.cannaapp.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import cz.cannaclub.cannaapp.model.LoyaltyConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Akce "více bodů" a hromadné zprávy (bonus za rank je pevný, viz LoyaltyConfig).
 * Číst může každý (config/loyalty), měnit jen obsluha přes server (functions/loyalty.js).
 */
class LoyaltyRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1")
) {

    /** Akce (config/loyalty) + ceník odměn (config/rewards) v jednom objektu. */
    fun configFlow(): Flow<LoyaltyConfig> = callbackFlow {
        var cfg = LoyaltyConfig()
        var rewards = emptyList<cz.cannaclub.cannaapp.model.Reward>()
        var finale: cz.cannaclub.cannaapp.model.Reward? = LoyaltyConfig.DEFAULT_FINALE
        fun emit() { trySend(cfg.copy(rewards = rewards, finale = finale)) }
        val reg = db.collection("config").document("loyalty")
            .addSnapshotListener { snap, error ->
                if (error != null) android.util.Log.w("Loyalty", "config/loyalty nejde číst", error)
                cfg = if (error != null) LoyaltyConfig() else LoyaltyConfig.from(snap?.data)
                emit()
            }
        val regRewards = db.collection("config").document("rewards")
            .addSnapshotListener { snap, _ ->
                fun reward(map: Map<*, *>?): cz.cannaclub.cannaapp.model.Reward? {
                    val m = map ?: return null
                    val name = m["name"] as? String ?: return null
                    val cost = (m["cost"] as? Number)?.toInt()?.takeIf { it > 0 } ?: return null
                    return cz.cannaclub.cannaapp.model.Reward(
                        name, cost, m["icon"] as? String ?: "gift", m["rank"] as? String ?: "ZAKAZNIK"
                    )
                }
                rewards = (snap?.get("items") as? List<*>).orEmpty().mapNotNull { reward(it as? Map<*, *>) }
                finale = if (snap?.contains("finale") == true) reward(snap?.get("finale") as? Map<*, *>)
                         else LoyaltyConfig.DEFAULT_FINALE
                val rankFrom = (snap?.get("rankFrom") as? Map<*, *>).orEmpty()
                    .mapNotNull { (k, v) -> (k as? String)?.let { key -> (v as? Number)?.toInt()?.let { key to it } } }.toMap()
                cz.cannaclub.cannaapp.model.RankScale.apply(rankFrom)
                emit()
            }
        awaitClose { reg.remove(); regRewards.remove() }
    }

    /** Spustí akce "N× body" do [endsAtMillis]. Vrací počet odeslaných notifikací. */
    suspend fun startPromo(multiplier: Int, endsAtMillis: Long, label: String, notify: Boolean): Result<Int> =
        call(
            "adminUpdateLoyalty",
            mapOf(
                "promo" to mapOf(
                    "multiplier"   to multiplier,
                    "endsAtMillis" to endsAtMillis,
                    "label"        to label,
                    "notify"       to notify
                )
            )
        ).map { (it["notified"] as? Number)?.toInt() ?: 0 }

    suspend fun stopPromo(): Result<Unit> =
        call("adminUpdateLoyalty", mapOf("promo" to null)).map { }

    data class BroadcastResult(val recipients: Int, val sent: Int)

    suspend fun broadcast(title: String, body: String, minRank: Int, dryRun: Boolean): Result<BroadcastResult> =
        call(
            "adminBroadcast",
            mapOf("title" to title, "body" to body, "minRank" to minRank, "dryRun" to dryRun)
        ).map {
            BroadcastResult(
                recipients = (it["recipients"] as? Number)?.toInt() ?: 0,
                sent       = (it["sent"] as? Number)?.toInt() ?: 0
            )
        }

    private suspend fun call(name: String, data: Map<String, Any?>): Result<Map<String, Any?>> = try {
        val res = functions.getHttpsCallable(name).call(data).await()
        @Suppress("UNCHECKED_CAST")
        Result.success(res.getData() as? Map<String, Any?> ?: emptyMap())
    } catch (e: FirebaseFunctionsException) {
        Result.failure(Exception(e.message ?: "Server akci odmítl"))
    } catch (e: Exception) {
        android.util.Log.e("Loyalty", "$name selhalo", e)
        Result.failure(Exception("Nepodařilo se spojit se serverem"))
    }
}
