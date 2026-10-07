package cz.cannaclub.cannaapp.repository

import com.google.firebase.firestore.Query
import cz.cannaclub.cannaapp.firebase.FirebaseManager
import cz.cannaclub.cannaapp.model.Transaction
import cz.cannaclub.cannaapp.model.TransactionType
import cz.cannaclub.cannaapp.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val auth     = FirebaseManager.auth
    private val usersCol = FirebaseManager.usersCollection
    private fun txCol(uid: String) = FirebaseManager.transactionsCollection(uid)

    private val functions = com.google.firebase.functions.FirebaseFunctions.getInstance("us-central1")

    /**
     * Zákazník je v Firebase Auth přihlášený anonymně (neviditelné, bez hesla).
     * Server ho podle e-mailu a telefonu přiváže k jeho účtu (users/{id}.authUids)
     * a pravidla databáze ho pak pustí jen k vlastním datům.
     */
    private suspend fun ensureSignedIn() {
        if (auth.currentUser == null) auth.signInAnonymously().await()
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun callForUserId(name: String, data: Map<String, Any?>): String? {
        ensureSignedIn()
        val res = functions.getHttpsCallable(name).call(data).await()
        return (res.getData() as? Map<String, Any?>)?.get("userId") as? String
    }

    /**
     * Přihlášení zákazníka: e-mail + telefon, nebo e-mail + jméno u zákazníků
     * převzatých z Dotykačky bez telefonu. Ověřuje server (functions/customers.js).
     */
    suspend fun loginUser(name: String, email: String, phone: String): User? = try {
        callForUserId(
            "customerLogin",
            mapOf("email" to email.trim().lowercase(), "phone" to phone.trim(), "name" to name.trim())
        )?.let { getUserById(it) }
    } catch (e: Exception) {
        android.util.Log.w("Login", "Přihlášení zákazníka selhalo", e)
        null
    }

    /** Smaže účet přihlášeného zákazníka i s historií bodů (požadavek Google Play). */
    suspend fun deleteMyAccount(): Boolean = try {
        functions.getHttpsCallable("deleteMyAccount").call().await()
        auth.signOut()
        true
    } catch (e: Exception) {
        android.util.Log.e("Account", "Smazání účtu selhalo", e)
        false
    }

    /** Odhlášení zákazníka — zařízení se odpojí od účtu. Obsluhu (heslo) neodhlašuje. */
    fun logoutCustomer() {
        if (auth.currentUser?.isAnonymous == true) auth.signOut()
    }

    suspend fun loginAdmin(email: String, password: String): Boolean {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            val uid = auth.currentUser?.uid ?: return false
            val adminDoc = FirebaseManager.adminsCollection.document(uid).get().await()
            adminDoc.exists()
        } catch (e: Exception) {
            false
        }
    }

    fun logoutAdmin() { auth.signOut() }

    fun getAllUsersFlow(): Flow<List<User>> = callbackFlow {
        val listener = usersCol
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val users = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                }
                trySend(users)
            }
        awaitClose { listener.remove() }
    }


    // Živý odběr jednoho zákazníka — body na dashboardu se aktualizují hned,
    // jak obsluha nebo Dotykačka připíše body (dřív se načetly jen jednou při přihlášení).
    fun getUserFlow(userId: String): Flow<User?> = callbackFlow {
        val listener = usersCol.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                trySend(
                    if (snapshot.exists()) snapshot.toObject(User::class.java)?.copy(id = snapshot.id)
                    else null
                )
            }
        awaitClose { listener.remove() }
    }

    /** Najde zákazníka podle naskenovaného kódu — členského kódu nebo (starší QR) ID dokumentu. */
    suspend fun getUserByScanCode(code: String): User? {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val byCode = usersCol.whereEqualTo("memberCode", trimmed).limit(1).get().await()
            byCode.documents.firstOrNull()?.let { doc ->
                doc.toObject(User::class.java)?.copy(id = doc.id)
            } ?: if (trimmed.contains('/')) null else getUserById(trimmed)
        } catch (e: Exception) { null }
    }

    suspend fun getUserById(userId: String): User? {
        return try {
            val doc = usersCol.document(userId).get().await()
            if (doc.exists()) doc.toObject(User::class.java)?.copy(id = doc.id) else null
        } catch (e: Exception) { null }
    }

    fun getTransactionsFlow(userId: String): Flow<List<Transaction>> = callbackFlow {
        val listener = txCol(userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val transactions = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                }
                trySend(transactions)
            }
        awaitClose { listener.remove() }
    }

    // ─────────────────────────────────────────────────────
    // BODY — obsluha nastaví nový zůstatek, zapíše se ROZDÍL.
    // Transakce čte aktuální stav z Firestore, takže když mezitím
    // Dotykačka připíše body za nákup, obsluha je nepřepíše starou hodnotou.
    // totalPoints se nikdy nesnižuje — základ pro rank.
    // ─────────────────────────────────────────────────────
    suspend fun updatePoints(
        userId: String,
        oldPoints: Int,
        newPoints: Int,
        reason: String = "Úprava obsluhou"
    ): Boolean {
        val diff = newPoints - oldPoints
        if (diff == 0) return true
        return try {
            val db     = FirebaseManager.firestore
            val userRef = usersCol.document(userId)
            db.runTransaction { tx ->
                val snap    = tx.get(userRef)
                val current = snap.getLong("points")?.toInt() ?: 0
                val total   = snap.getLong("totalPoints")?.toInt() ?: 0
                val updated = (current + diff).coerceAtLeast(0)
                val applied = updated - current

                tx.update(
                    userRef,
                    mapOf(
                        "points"      to updated,
                        "totalPoints" to if (applied > 0) total + applied else total
                    )
                )
                val txRef = txCol(userId).document()
                tx.set(
                    txRef,
                    Transaction(
                        id     = txRef.id,
                        type   = if (applied >= 0) TransactionType.ADD else TransactionType.SUBTRACT,
                        amount = kotlin.math.abs(applied),
                        reason = reason
                    )
                )
                null
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun addUser(
        name: String,
        email: String,
        phone: String,
        initialPoints: Int = 0
    ): AddUserResult {
        return try {
            val normalizedEmail = email.trim().lowercase()
            val existing = usersCol
                .whereEqualTo("email", normalizedEmail)
                .limit(1)
                .get()
                .await()
            if (!existing.isEmpty) return AddUserResult.DuplicateEmail

            val user = User(
                name        = name,
                email       = normalizedEmail,
                phone       = phone.trim(),
                points      = initialPoints,
                totalPoints = initialPoints
            )
            usersCol.add(user).await()
            AddUserResult.Success
        } catch (e: Exception) {
            AddUserResult.Error
        }
    }

    /**
     * Samoregistrace zákazníka z appky. Členský kód a zákazníka v Dotykačce
     * doplní server (Cloud Function onUserCreated) hned po založení.
     */
    suspend fun registerUser(name: String, email: String, phone: String): RegisterResult = try {
        val id = callForUserId(
            "customerRegister",
            mapOf(
                "name"    to name.trim(),
                "email"   to email.trim().lowercase(),
                "phone"   to phone.trim(),
                "consent" to true      // zaškrtnutí kontroluje RegisterScreen / UserViewModel
            )
        )
        val user = id?.let { getUserById(it) }
        if (user != null) RegisterResult.Success(user) else RegisterResult.Error
    } catch (e: com.google.firebase.functions.FirebaseFunctionsException) {
        if (e.code == com.google.firebase.functions.FirebaseFunctionsException.Code.ALREADY_EXISTS)
            RegisterResult.DuplicateEmail
        else RegisterResult.Error
    } catch (e: Exception) {
        RegisterResult.Error
    }

    // Uloží FCM token do Firestore
    suspend fun saveFcmToken(userId: String, token: String): Boolean {
        return try {
            usersCol.document(userId)
                .update("fcmToken", token)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

}

sealed class AddUserResult {
    object Success        : AddUserResult()
    object DuplicateEmail : AddUserResult()
    object Error          : AddUserResult()
}

sealed class RegisterResult {
    data class Success(val user: User) : RegisterResult()
    object DuplicateEmail : RegisterResult()
    object Error          : RegisterResult()
}
