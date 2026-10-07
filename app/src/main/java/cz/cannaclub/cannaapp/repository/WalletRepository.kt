package cz.cannaclub.cannaapp.repository

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.google.android.gms.pay.Pay
import com.google.android.gms.pay.PayApiAvailabilityStatus
import com.google.android.gms.pay.PayClient
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await

/**
 * Členská kartička v Google Peněžence.
 * Kartičku (objekt v Google Wallet) zakládá a podepisuje server (functions/wallet.js),
 * appka jen dostane podepsaný JWT a předá ho Peněžence.
 */
class WalletRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1")
) {

    data class Pass(val jwt: String, val saveUrl: String)

    suspend fun createPass(userId: String, memberCode: String): Result<Pass> = try {
        val response = functions
            .getHttpsCallable("walletPassJwt")
            .call(mapOf("userId" to userId, "memberCode" to memberCode))
            .await()
        @Suppress("UNCHECKED_CAST")
        val d = response.getData() as? Map<String, Any?> ?: emptyMap()
        val jwt = d["jwt"] as? String
        val url = d["saveUrl"] as? String
        if (jwt.isNullOrBlank() || url.isNullOrBlank()) Result.failure(Exception("Server nevrátil kartičku"))
        else Result.success(Pass(jwt, url))
    } catch (e: FirebaseFunctionsException) {
        // Server posílá česky formulované chyby
        Result.failure(Exception(e.message ?: "Kartičku se nepodařilo připravit"))
    } catch (e: Exception) {
        android.util.Log.e("Wallet", "walletPassJwt selhalo", e)
        Result.failure(Exception("Nepodařilo se spojit se serverem"))
    }

    companion object {
        /** requestCode pro výsledek uložení (MainActivity.onActivityResult). */
        const val SAVE_REQUEST_CODE = 4711

        /**
         * Otevře uložení do Peněženky: nativní okno přes Google Play služby,
         * a když nejsou k dispozici, webovou stránku Googlu (funguje i bez appky Peněženka).
         */
        suspend fun launchSave(activity: Activity, pass: Pass) {
            val client: PayClient = Pay.getClient(activity)
            val available = try {
                client.getPayApiAvailabilityStatus(PayClient.RequestType.SAVE_PASSES).await() ==
                    PayApiAvailabilityStatus.AVAILABLE
            } catch (e: Exception) {
                false
            }
            if (available) {
                client.savePassesJwt(pass.jwt, activity, SAVE_REQUEST_CODE)
            } else {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pass.saveUrl)))
            }
        }
    }
}
