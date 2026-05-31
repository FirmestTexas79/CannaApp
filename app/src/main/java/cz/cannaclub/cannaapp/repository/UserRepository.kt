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
    // USER AUTH — hledá podle jména + emailu + telefonu
    // ─────────────────────────────────────────────────────

    suspend fun loginUser(name: String, email: String, phone: String): User? {
        return try {
            val snapshot = usersCol
                .whereEqualTo("name", name)
                .whereEqualTo("email", email)
                .whereEqualTo("phone", phone)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) null
            else {
                val doc = snapshot.documents.first()
                doc.toObject(User::class.java)?.copy(id = doc.id)
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
    // BODY — úprava adminem
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
                email  = email,
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