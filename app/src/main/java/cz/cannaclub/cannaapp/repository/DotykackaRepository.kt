package cz.cannaclub.cannaapp.repository.dotykacka

import cz.cannaclub.cannaapp.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * DotykackaRepository — Kompletní produkční integrace s Dotykačka API v2.
 * Stačí vyplnit údaje od majitele podniku v companion objectu.
 */
class DotykackaRepository {

    companion object {
        // ── ⚠️ TADY NA MÍSTĚ DOPLŇ ÚDAJE OD ŠÉFA ────────────────
        private const val REFRESH_TOKEN = "REFRESH_TOKEN_OD_SEFA"
        private const val CLOUD_ID      = "304899966"
        private const val BRANCH_ID     = "191033401" // ID konkrétní prodejny/pokladny

        // Dotykačka API v2 URL adresy
        private const val AUTH_URL      = "https://api.dotykacka.cz/v2/auth/token"
        private const val BASE_URL      = "https://api.dotykacka.cz/v2/clouds/$CLOUD_ID"
    }

    // Dočasná paměť pro vygenerovaný Access Token, ať se nemusí žádat při každém kliknutí znova
    private var cachedAccessToken: String? = null

    /**
     * Automaticky získá validní Access Token pomocí Refresh Tokenu.
     * Dotykačka API v2 vyžaduje OAuth2 autorizaci.
     */
    private fun getAccessToken(): String? {
        if (cachedAccessToken != null) return cachedAccessToken

        return try {
            val url = URL(AUTH_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val body = JSONObject().apply {
                put("refresh_token", REFRESH_TOKEN)
            }

            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

            if (connection.responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val token = json.optString("access_token", null)
                cachedAccessToken = token
                token
            } else {
                android.util.Log.e("DotykackaAPI", "Chyba OAuth autorizace: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Synchronizace zákazníka.
     * Zkontroluje, zda už má přiřazené ID, pokud ne, zkusí ho najít podle e-mailu.
     * Když v Dotykačce ještě není, automaticky ho vytvoří.
     */
    suspend fun syncCustomer(user: User): String? = withContext(Dispatchers.IO) {
        try {
            if (user.dotykackaId.isNotBlank()) {
                return@withContext user.dotykackaId
            }

            val existingId = findCustomerByEmail(user.email)
            if (existingId != null) return@withContext existingId

            val createdId = createCustomer(user)
            createdId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Vyhledá zákazníka v Dotykačce podle jeho e-mailu.
     */
    private fun findCustomerByEmail(email: String): String? {
        val token = getAccessToken() ?: return null
        return try {
            // FIltrování v API v2 vyžaduje URL kódování (např. email==test@seznam.cz)
            val filter = URLEncoder.encode("email==$email", "UTF-8")
            val url = URL("$BASE_URL/customers?filter=$filter")

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")

            if (connection.responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                if (jsonArray.length() > 0) {
                    jsonArray.getJSONObject(0).optLong("id").toString()
                } else null
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Vytvoří nového zákazníka v databázi Dotykačky.
     */
    private fun createCustomer(user: User): String? {
        val token = getAccessToken() ?: return null
        return try {
            val url = URL("$BASE_URL/customers")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Rozdělení jména na křestní a příjmení, jak vyžaduje API
            val nameParts = user.name.trim().split("\\s+".toRegex())
            val firstName = nameParts.firstOrNull() ?: "Zákazník"
            val lastName = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else "CannaClub"

            val body = JSONObject().apply {
                put("firstName", firstName)
                put("lastName", lastName)
                put("email", user.email)
                put("phone", user.phone)
                put("externalId", user.id) // Propojení s tvým Firebase UserId
            }

            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

            if (connection.responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                json.optLong("id").toString()
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Přiřadí naskenovaného zákazníka k aktuálně otevřenému účtu na pokladně.
     * Volá se ihned po naskenování QR kódu u pultu.
     */
    suspend fun assignCustomerToCurrentOrder(dotykackaId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken() ?: return@withContext false

                // 1. Zjistíme ID otevřené objednávky na dané pokladně (pobočce)
                val openOrderId = getFirstOpenOrderId(token) ?: return@withContext false

                // 2. Provedeme PUT požadavek pro navázání zákazníka na tento účet
                val url = URL("$BASE_URL/branches/$BRANCH_ID/orders/$openOrderId/customer")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "PUT"
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val body = JSONObject().apply {
                    put("customerId", dotykackaId.toLong())
                }

                OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

                connection.responseCode in 200..299
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    /**
     * Pomocná interní funkce, která najde aktuální aktivní (otevřený) účet na pokladně.
     */
    private fun getFirstOpenOrderId(token: String): String? {
        return try {
            val url = URL("$BASE_URL/branches/$BRANCH_ID/orders?filter=status==OPEN")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")

            if (connection.responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                if (jsonArray.length() > 0) {
                    jsonArray.getJSONObject(0).optLong("id").toString()
                } else null
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}