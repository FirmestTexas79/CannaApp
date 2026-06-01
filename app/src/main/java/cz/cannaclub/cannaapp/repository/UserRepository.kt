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

    // ─────────────────────────────────────────────────────
    // USER AUTH
    //
    // FIX: Původní trojitý compound query (name + email + phone) vyžadoval
    // composite index na Firestore, který musí být explicitně definován
    // v firestore.indexes.json — bez něj query selže na větší kolekci.
    //
    // Nové řešení: dotazujeme pouze podle emailu (unikátní pole, jeden index)
    // a jméno + telefon ověřujeme client-side. Telefon porovnáváme bez mezer
    // pro robustnost (+420 666 420 911 == +420666420911).
    // ─────────────────────────────────────────────────────

    suspend fun loginUser(name: String, email: String, phone: String): User? {
        return try {
            val snapshot = usersCol
                .whereEqualTo("email", email.trim().lowercase())
                .limit(5) // defensive limit pro případ duplicit
                .get()
                .await()

            if (snapshot.isEmpty) return null

            // Client-side ověření jména a telefonu
            val normalizedPhone = phone.replace(Regex("\\s+"), "")

            snapshot.documents.firstNotNullOfOrNull { doc ->
                val user = doc.toObject(User::class.java)?.copy(id = doc.id)
                    ?: return@firstNotNullOfOrNull null

                val nameMatch  = user.name.trim().equals(name.trim(), ignoreCase = true)
                val phoneMatch = user.phone.replace(Regex("\\s+"), "") == normalizedPhone

                if (nameMatch && phoneMatch) user else null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ─────────────────────────────────────────────────────
    // ADMIN AUTH
    // ─────────────────────────────────────────────────────

    suspend fun loginAdmin(email: String, password: String): Boolean {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            val uid = auth.currentUser?.uid ?: return false
            val adminDoc = FirebaseManager.adminsCollection
                .document(uid)
                .get()
                .await()
            adminDoc.exists()
        } catch (e: Exception) {
            false
        }
    }

    fun logoutAdmin() {
        auth.signOut()
    }

    // ─────────────────────────────────────────────────────
    // USERS — real-time stream pro admin seznam
    // ─────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────
    // TRANSACTIONS — posledních 10 pro dashboard
    // ─────────────────────────────────────────────────────

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
    // BODY — úprava adminem (atomic batch)
    // ─────────────────────────────────────────────────────

    suspend fun updatePoints(
        userId: String,
        oldPoints: Int,
        newPoints: Int,
        reason: String = "Úprava obsluhou"
    ): Boolean {
        return try {
            val diff = newPoints - oldPoints
            val type = if (diff >= 0) TransactionType.ADD else TransactionType.SUBTRACT

            FirebaseManager.firestore.runBatch { batch ->
                batch.update(usersCol.document(userId), "points", newPoints)
                val txRef = txCol(userId).document()
                val transaction = Transaction(
                    id     = txRef.id,
                    type   = type,
                    amount = kotlin.math.abs(diff),
                    reason = reason
                )
                batch.set(txRef, transaction)
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // ─────────────────────────────────────────────────────
    // PŘIDÁNÍ UŽIVATELE — admin
    // ─────────────────────────────────────────────────────

    suspend fun addUser(
        name: String,
        email: String,
        phone: String,
        initialPoints: Int = 0
    ): Boolean {
        return try {
            val user = User(
                name   = name,
                email  = email.trim().lowercase(),
                phone  = phone,
                points = initialPoints
            )
            usersCol.add(user).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}