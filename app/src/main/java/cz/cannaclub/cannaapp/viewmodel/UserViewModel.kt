package cz.cannaclub.cannaapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import cz.cannaclub.cannaapp.model.Transaction
import cz.cannaclub.cannaapp.model.User
import cz.cannaclub.cannaapp.preferences.UserPreferences
import cz.cannaclub.cannaapp.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserRepository()
    private val userPrefs  = UserPreferences(application)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    // Živé Firestore listenery — rušíme je při odhlášení / novém přihlášení
    private var userJob: Job? = null
    private var transactionJob: Job? = null

    // Předvyplněné hodnoty z minulého přihlášení
    val savedName:  String get() = userPrefs.getSavedName()
    val savedEmail: String get() = userPrefs.getSavedEmail()
    val savedPhone: String get() = userPrefs.getSavedPhone()

    /** True, pokud se má po splashi zkusit automatické přihlášení. */
    val canAutoLogin: Boolean get() = userPrefs.hasSavedUser()

    fun loginUser(name: String, email: String, phone: String) {
        if (name.isBlank() || email.isBlank() || phone.isBlank()) {
            _loginState.value = LoginState.Error("Vyplň všechna pole")
            return
        }
        if (!email.contains("@")) {
            _loginState.value = LoginState.Error("Neplatný email")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            val user = repository.loginUser(name, email, phone)

            if (user != null) {
                userPrefs.saveUser(name, email, phone, user.id)
                _currentUser.value = user
                observeUser(user.id)
                loadTransactions(user.id)
                saveFcmToken(user.id)
                _loginState.value = LoginState.Success
            } else {
                _loginState.value = LoginState.Error("Zákazník nenalezen")
            }
        }
    }

    /** Přihlášení uloženými údaji hned po spuštění (volá splash). */
    fun autoLogin() {
        if (!canAutoLogin) return
        loginUser(savedName, savedEmail, savedPhone)
    }

    private fun observeUser(userId: String) {
        userJob?.cancel()
        userJob = viewModelScope.launch {
            repository.getUserFlow(userId).collect { user ->
                if (user != null) _currentUser.value = user
            }
        }
    }

    private fun loadTransactions(userId: String) {
        transactionJob?.cancel()
        transactionJob = viewModelScope.launch {
            repository.getTransactionsFlow(userId).collect { txList ->
                _transactions.value = txList
            }
        }
    }

    fun logout() {
        userJob?.cancel()
        userJob = null
        transactionJob?.cancel()
        transactionJob = null

        _currentUser.value  = null
        _transactions.value = emptyList()
        _loginState.value   = LoginState.Idle
        // Údaje necháme předvyplněné, jen vypneme automatické přihlášení
        userPrefs.markLoggedOut()
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    private fun saveFcmToken(userId: String) {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                repository.saveFcmToken(userId, token)
            } catch (e: Exception) {
                android.util.Log.e("FCM", "Token se nepodařilo uložit", e)
            }
        }
    }
}

sealed class LoginState {
    object Idle    : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
