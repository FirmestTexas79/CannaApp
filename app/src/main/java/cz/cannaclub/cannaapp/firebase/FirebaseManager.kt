package cz.cannaclub.cannaapp.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.storage.FirebaseStorage

object FirebaseManager {

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().also { db ->
            db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(50L * 1024 * 1024)
                        .build()
                )
                .build()
        }
    }

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    object Collections {
        const val USERS        = "users"
        const val ADMINS       = "admins"
        const val TRANSACTIONS = "transactions"
        const val PRODUCTS     = "products"
    }

    val usersCollection
        get() = firestore.collection(Collections.USERS)

    val adminsCollection
        get() = firestore.collection(Collections.ADMINS)

    val productsCol
        get() = firestore.collection(Collections.PRODUCTS)

    fun transactionsCollection(userId: String) =
        usersCollection.document(userId).collection(Collections.TRANSACTIONS)

    val currentAdminId: String?
        get() = auth.currentUser?.uid

    val isAdminLoggedIn: Boolean
        get() = auth.currentUser != null
}