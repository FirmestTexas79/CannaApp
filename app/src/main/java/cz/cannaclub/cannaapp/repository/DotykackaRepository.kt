package cz.cannaclub.cannaapp.repository

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await

/**
 * Tenká vrstva nad Cloud Functions. Aplikace s Dotykačkou NEMLUVÍ přímo —
 * refresh token je jen na serveru (Firebase Secret Manager), nikdy v APK.
 * Serverový kód: functions/index.js, nastavení: DOTYKACKA_NAVOD.md.
 */
class DotykackaRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1")
) {

    data class AssignResult(
        val orderTotal: Double?,
        val openWithoutCustomer: Int,
        val alreadyAssigned: Boolean
    )

    /**
     * Najde/založí zákazníka v Dotykačce a připojí ho k nejnovějšímu otevřenému
     * účtu na pokladně. Volá se hned po naskenování QR kódu.
     */
    suspend fun assignCustomerToOrder(userId: String): Result<AssignResult> = try {
        val response = functions
            .getHttpsCallable("assignCustomerToOrder")
            .call(mapOf("userId" to userId))
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = response.getData() as? Map<String, Any?> ?: emptyMap()
        Result.success(
            AssignResult(
                orderTotal          = (data["orderTotal"] as? Number)?.toDouble(),
                openWithoutCustomer = (data["openWithoutCustomer"] as? Number)?.toInt() ?: 0,
                alreadyAssigned     = data["alreadyAssigned"] as? Boolean ?: false
            )
        )
    } catch (e: FirebaseFunctionsException) {
        // Server posílá česky formulované chyby — ukážeme je obsluze tak, jak jsou
        Result.failure(Exception(e.message ?: "Dotykačka nedostupná"))
    } catch (e: Exception) {
        android.util.Log.e("Dotykacka", "assignCustomerToOrder selhalo", e)
        Result.failure(Exception("Nepodařilo se spojit se serverem"))
    }

    data class ImportResult(
        val dryRun: Boolean,
        val totalInDotykacka: Int,
        val imported: Int,
        val alreadyInApp: Int,
        val noEmail: Int
    )

    /**
     * Převezme stávající zákazníky z Dotykačky do appky.
     * [dryRun] = true jen spočítá, kolik by se jich převzalo, nic nezakládá.
     */
    suspend fun importCustomers(dryRun: Boolean): Result<ImportResult> = try {
        val response = functions
            .getHttpsCallable("importDotykackaCustomers")
            .withTimeout(540, java.util.concurrent.TimeUnit.SECONDS)
            .call(mapOf("dryRun" to dryRun))
            .await()

        @Suppress("UNCHECKED_CAST")
        val d = response.getData() as? Map<String, Any?> ?: emptyMap()
        fun n(k: String) = (d[k] as? Number)?.toInt() ?: 0
        Result.success(
            ImportResult(
                dryRun           = d["dryRun"] as? Boolean ?: dryRun,
                totalInDotykacka = n("totalInDotykacka"),
                imported         = n("imported"),
                alreadyInApp     = n("alreadyInApp"),
                noEmail          = n("noEmail")
            )
        )
    } catch (e: FirebaseFunctionsException) {
        Result.failure(Exception(e.message ?: "Import selhal"))
    } catch (e: Exception) {
        android.util.Log.e("Dotykacka", "importCustomers selhalo", e)
        Result.failure(Exception("Nepodařilo se spojit se serverem"))
    }
}
