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

    /**
     * Přihlášení zákazníka: e-mail + telefon, nebo e-mail + jméno u zákazníků
     * převzatých z Dotykačky, kteří tam telefon nemají (ten se pak doplní).
     * Telefon se porovnává podle posledních 9 číslic (+420 / mezery nevadí),
     * jméno bez diakritiky a v libovolném pořadí.
     */
    suspend fun loginUser(name: String, email: String, phone: String): User? {
        return try {
            val snapshot = usersCol
                .whereEqualTo("email", email.trim().lowercase())
                .limit(5)
                .get()
                .await()

            if (snapshot.isEmpty) return null

            val enteredPhone = phoneKey(phone)
            val enteredName  = nameKey(name)

            val users = snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(id = doc.id)
            }

            users.firstOrNull { u ->
                enteredPhone.isNotEmpty() && phoneKey(u.phone) == enteredPhone
            } ?: users.firstOrNull { u ->
                phoneKey(u.phone).isEmpty() && namesMatch(u.name, enteredName)
            }?.also { u ->
                if (enteredPhone.isNotEmpty()) {
                    usersCol.document(u.id).update("phone", phone.trim()).await()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun phoneKey(p: String): String =
        p.filter { it.isDigit() }.takeLast(9).let { if (it.length == 9) it else "" }

    private fun nameKey(n: String): List<String> =
        java.text.Normalizer.normalize(n, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .sorted()

    private fun namesMatch(stored: String, entered: List<String>): Boolean =
        entered.isNotEmpty() && nameKey(stored) == entered

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
    suspend fun registerUser(name: String, email: String, phone: String): RegisterResult {
        return try {
            val normalizedEmail = email.trim().lowercase()
            val existing = usersCol.whereEqualTo("email", normalizedEmail).limit(1).get().await()
            if (!existing.isEmpty) return RegisterResult.DuplicateEmail

            val ref = usersCol.document()
            val data = mapOf(
                "id"                 to "",
                "name"               to name.trim().replace(Regex("\\s+"), " "),
                "email"              to normalizedEmail,
                "phone"              to phone.trim(),
                "points"             to 0,
                "totalPoints"        to 0,
                "dotykackaId"        to "",
                "fcmToken"           to "",
                "memberCode"         to "",
                "selfRegistered"     to true,
                "consentAt"          to com.google.firebase.Timestamp.now(),
                "createdAt"          to com.google.firebase.Timestamp.now()
            )
            ref.set(data).await()
            val user = User(
                id    = ref.id,
                name  = data["name"] as String,
                email = normalizedEmail,
                phone = phone.trim()
            )
            RegisterResult.Success(user)
        } catch (e: Exception) {
            RegisterResult.Error
        }
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
