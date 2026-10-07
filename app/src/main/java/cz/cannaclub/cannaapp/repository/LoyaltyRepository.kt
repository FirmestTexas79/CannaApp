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

    fun configFlow(): Flow<LoyaltyConfig> = callbackFlow {
        val reg = db.collection("config").document("loyalty")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    android.util.Log.w("Loyalty", "config/loyalty nejde číst", error)
                    trySend(LoyaltyConfig())
                    return@addSnapshotListener
                }
                trySend(LoyaltyConfig.from(snap?.data))
            }
        awaitClose { reg.remove() }
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
