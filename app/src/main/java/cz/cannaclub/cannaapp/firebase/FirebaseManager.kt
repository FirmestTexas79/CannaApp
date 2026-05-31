package cz.cannaclub.cannaapp.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

object FirebaseManager {

    // ── Firestore instance ────────────────────────────────
    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().also { db ->
            // Offline persistence — appka funguje i bez internetu
            // data se synchronizují jakmile se spojení obnoví
            val cacheSettings = PersistentCacheSettings.newBuilder()
                .setSizeBytes(50L * 1024 * 1024) // 50 MB cache
                .build()

            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(cacheSettings)
                .build()

            db.firestoreSettings = settings
        }
    }

    // ── Auth instance ─────────────────────────────────────
    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // ── Kolekce — centrální definice názvů ────────────────
    // Kdybychom chtěli přejmenovat kolekci, změníme jen zde
    object Collections {
        const val USERS        = "users"
        const val ADMINS       = "admins"
        const val TRANSACTIONS = "transactions"
    }

    // ── Zkratky na kolekce ────────────────────────────────
    val usersCollection
        get() = firestore.collection(Collections.USERS)

    val adminsCollection
        get() = firestore.collection(Collections.ADMINS)

    fun transactionsCollection(userId: String) =
        usersCollection.document(userId).collection(Collections.TRANSACTIONS)

    // ── Aktuální přihlášený admin ─────────────────────────
    val currentAdminId: String?
        get() = auth.currentUser?.uid

    val isAdminLoggedIn: Boolean
        get() = auth.currentUser != null
}