package cz.cannaclub.cannaapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cz.cannaclub.cannaapp.model.Transaction
import cz.cannaclub.cannaapp.model.User
import cz.cannaclub.cannaapp.preferences.UserPreferences
import cz.cannaclub.cannaapp.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserRepository()
    private val userPrefs  = UserPreferences(application)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    // FIX: trackujeme Job transakčního listeneru, abychom ho mohli zrušit při
    // opětovném přihlášení nebo odhlášení — jinak by v paměti žily duplicitní
    // Firestore SnapshotListenery a zbytečně čerpaly data.
    private var transactionJob: Job? = null

    // Předvyplněné hodnoty z minulého přihlášení
    val savedName:  String get() = userPrefs.getSavedName()
    val savedEmail: String get() = userPrefs.getSavedEmail()
    val savedPhone: String get() = userPrefs.getSavedPhone()

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
                userPrefs.saveUser(name, email, phone)
                _currentUser.value = user
                loadTransactions(user.id)
                _loginState.value = LoginState.Success
            } else {
                _loginState.value = LoginState.Error("Zákazník nenalezen")
            }
        }
    }

    private fun loadTransactions(userId: String) {
        // Zruší případný předchozí listener — klíčová oprava memory leaku
        transactionJob?.cancel()
        transactionJob = viewModelScope.launch {
            repository.getTransactionsFlow(userId).collect { txList ->
                _transactions.value = txList
            }
        }
    }

    fun logout() {
        // Zruší Firestore listener okamžitě při odhlášení
        transactionJob?.cancel()
        transactionJob = null

        _currentUser.value   = null
        _transactions.value  = emptyList()
        _loginState.value    = LoginState.Idle
        // Záměrně NESMAŽEME userPrefs — chceme předvyplnit příště
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }
}

sealed class LoginState {
    object Idle    : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}