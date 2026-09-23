package cz.cannaclub.cannaapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import cz.cannaclub.cannaapp.model.Transaction
import cz.cannaclub.cannaapp.model.User
import cz.cannaclub.cannaapp.preferences.UserPreferences
import cz.cannaclub.cannaapp.repository.RegisterResult
import cz.cannaclub.cannaapp.repository.UserRepository
import cz.cannaclub.cannaapp.repository.WalletRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserRepository()
    private val userPrefs  = UserPreferences(application)
    private val walletRepo = WalletRepository()
    private val loyaltyRepo = cz.cannaclub.cannaapp.repository.LoyaltyRepository()

    /** Bonusy za rank a běžící akce (config/loyalty). */
    private val _loyalty = MutableStateFlow(cz.cannaclub.cannaapp.model.LoyaltyConfig())
    val loyalty: StateFlow<cz.cannaclub.cannaapp.model.LoyaltyConfig> = _loyalty.asStateFlow()

    init {
        viewModelScope.launch { loyaltyRepo.configFlow().collect { _loyalty.value = it } }
    }

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    /** Kolik bodů právě přibylo → dashboard ukáže oslavu. null = nic. */
    private val _celebration = MutableStateFlow<Int?>(null)
    val celebration: StateFlow<Int?> = _celebration.asStateFlow()

    /** Úvodní "Jak to funguje" (poprvé po přihlášení, nebo z profilu). */
    private val _showOnboarding = MutableStateFlow(false)
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()

    // ── Google Peněženka ─────────────────────────────────
    private val _walletSaved = MutableStateFlow(false)
    val walletSaved: StateFlow<Boolean> = _walletSaved.asStateFlow()

    private val _walletBusy = MutableStateFlow(false)
    val walletBusy: StateFlow<Boolean> = _walletBusy.asStateFlow()

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
                maybeShowOnboarding()
                _loginState.value = LoginState.Success
            } else {
                _loginState.value = LoginState.Error("Zákazník nenalezen")
            }
        }
    }

    /** Registrace nového zákazníka → rovnou přihlášení. */
    fun register(name: String, email: String, phone: String, consent: Boolean) {
        val digits = phone.filter { it.isDigit() }
        val error = when {
            name.trim().split(Regex("\\s+")).size < 2 -> "Vyplň jméno i příjmení"
            !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim()) -> "Neplatný e-mail"
            digits.length < 9                            -> "Neplatné telefonní číslo"
            !consent                                     -> "Potvrď, že je ti 18 let a souhlasíš se zpracováním údajů"
            else -> null
        }
        if (error != null) {
            _loginState.value = LoginState.Error(error)
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            _loginState.value = when (val r = repository.registerUser(name, email, phone)) {
                is RegisterResult.Success -> {
                    val user = r.user
                    userPrefs.saveUser(user.name, user.email, user.phone, user.id)
                    _currentUser.value = user
                    observeUser(user.id)
                    loadTransactions(user.id)
                    saveFcmToken(user.id)
                    maybeShowOnboarding()
                    LoginState.Success
                }
                RegisterResult.DuplicateEmail -> LoginState.Error("Tento e-mail už je registrovaný. Přihlas se.")
                RegisterResult.Error          -> LoginState.Error("Registrace se nepovedla, zkus to znovu")
            }
        }
    }

    /** Přihlášení uloženými údaji hned po spuštění (volá splash). */
    fun autoLogin() {
        if (!canAutoLogin) return
        loginUser(savedName, savedEmail, savedPhone)
    }

    private fun observeUser(userId: String) {
        _walletSaved.value = userPrefs.isWalletSaved(userId)
        userJob?.cancel()
        userJob = viewModelScope.launch {
            repository.getUserFlow(userId).collect { user ->
                if (user != null) {
                    _currentUser.value = user
                    checkPointsGain(user)
                }
            }
        }
    }

    /**
     * Porovná body s tím, co zákazník viděl naposledy. Přibyly → oslava,
     * i když body dorazily, zatímco byla appka zavřená.
     */
    private fun checkPointsGain(user: User) {
        val seen = userPrefs.getSeenPoints(user.id)
        when {
            seen == null        -> userPrefs.setSeenPoints(user.id, user.points)   // první spuštění, nic neslavit
            user.points > seen  -> _celebration.value = user.points - seen
            user.points < seen  -> userPrefs.setSeenPoints(user.id, user.points)   // uplatnil odměnu
        }
    }

    /** Oslava se ukázala → uložit aktuální stav jako viděný. */
    fun celebrationShown() {
        _celebration.value = null
        _currentUser.value?.let { userPrefs.setSeenPoints(it.id, it.points) }
    }

    /** Přidat členskou kartičku do Google Peněženky. */
    fun addToWallet(activity: android.app.Activity) {
        val user = _currentUser.value ?: return
        if (_walletBusy.value) return
        if (user.memberCode.isBlank()) {
            toast("Kartička se ještě připravuje, zkus to za pár minut")
            return
        }
        viewModelScope.launch {
            _walletBusy.value = true
            walletRepo.createPass(user.id, user.memberCode).fold(
                onSuccess = { pass ->
                    try {
                        WalletRepository.launchSave(activity, pass)
                    } catch (e: Exception) {
                        android.util.Log.e("Wallet", "Uložení do Peněženky selhalo", e)
                        toast("Peněženku se nepodařilo otevřít")
                    }
                },
                onFailure = { toast(it.message ?: "Kartičku se nepodařilo připravit") }
            )
            _walletBusy.value = false
        }
    }

    /** Peněženka potvrdila uložení. */
    fun onWalletSaved() {
        _currentUser.value?.let { userPrefs.setWalletSaved(it.id) }
        _walletSaved.value = true
        toast("Kartička je v Peněžence Google")
    }

    private fun toast(msg: String) {
        android.widget.Toast.makeText(getApplication(), msg, android.widget.Toast.LENGTH_LONG).show()
    }

    private fun maybeShowOnboarding() {
        if (!userPrefs.isOnboardingDone()) _showOnboarding.value = true
    }

    fun openOnboarding() { _showOnboarding.value = true }

    fun onboardingFinished() {
        _showOnboarding.value = false
        userPrefs.setOnboardingDone()
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
        _celebration.value  = null
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
