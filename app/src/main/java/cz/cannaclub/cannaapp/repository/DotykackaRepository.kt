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
}
