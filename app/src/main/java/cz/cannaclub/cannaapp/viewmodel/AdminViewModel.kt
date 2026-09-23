package cz.cannaclub.cannaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import cz.cannaclub.cannaapp.model.User
import cz.cannaclub.cannaapp.repository.UserRepository
import cz.cannaclub.cannaapp.repository.AddUserResult
import cz.cannaclub.cannaapp.repository.DotykackaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repository: UserRepository = UserRepository(),
    private val dotykackaRepository: DotykackaRepository = DotykackaRepository()
) : ViewModel() {

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredUsers = MutableStateFlow<List<User>>(emptyList())
    val filteredUsers: StateFlow<List<User>> = _filteredUsers.asStateFlow()

    private val _loginState = MutableStateFlow<AdminLoginState>(AdminLoginState.Idle)
    val loginState: StateFlow<AdminLoginState> = _loginState.asStateFlow()

    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    private val _scannedUser = MutableStateFlow<User?>(null)
    val scannedUser: StateFlow<User?> = _scannedUser.asStateFlow()

    // ── Stav Dotykačka synchronizace ─────────────────────
    private val _dotykackaState = MutableStateFlow<DotykackaState>(DotykackaState.Idle)
    val dotykackaState: StateFlow<DotykackaState> = _dotykackaState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(_allUsers, _searchQuery) { users, query ->
                if (query.isBlank()) users
                else users.filter { user ->
                    user.name.contains(query, ignoreCase = true) ||
                            user.email.contains(query, ignoreCase = true) ||
                            user.phone.contains(query, ignoreCase = true) ||
                            user.memberCode.contains(query)
                }
            }.collect { filtered ->
                _filteredUsers.value = filtered
            }
        }
    }

    fun loginAdmin(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = AdminLoginState.Error("Vyplň všechna pole")
            return
        }
        viewModelScope.launch {
            _loginState.value = AdminLoginState.Loading
            val success = repository.loginAdmin(email, password)
            if (success) {
                loadUsers()
                _loginState.value = AdminLoginState.Success
            } else {
                _loginState.value = AdminLoginState.Error("Nesprávné přihlašovací údaje")
            }
        }
    }

    private var usersJob: Job? = null

    private fun loadUsers() {
        usersJob?.cancel()   // při opakovaném přihlášení nezakládat druhý listener
        usersJob = viewModelScope.launch {
            repository.getAllUsersFlow().collect { users ->
                _allUsers.value = users
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    // ─────────────────────────────────────────────────────
    // QR skenování:
    // 1. Najde zákazníka ve Firebase → otevře se jeho karta
    // 2. Na pozadí ho přes server připojí k účtu v Dotykačce;
    //    průběh se ukazuje přímo v kartě zákazníka (EditPointsDialog)
    // ─────────────────────────────────────────────────────
    fun findUserByQrCode(userId: String) {
        viewModelScope.launch {
            val user = repository.getUserByScanCode(userId)
            if (user == null) {
                _operationState.value = OperationState.Error("Zákazník nenalezen")
                return@launch
            }
            _scannedUser.value = user
            assignToDotykacka(user.id)
        }
    }

    /** Připojení k účtu na pokladně (i opakovaný pokus z karty zákazníka). */
    fun assignToDotykacka(userId: String) {
        viewModelScope.launch {
            _dotykackaState.value = DotykackaState.Syncing
            _dotykackaState.value = dotykackaRepository.assignCustomerToOrder(userId).fold(
                onSuccess = { r ->
                    val total = r.orderTotal?.let { " (${it.toInt()} Kč)" } ?: ""
                    val msg = when {
                        r.alreadyAssigned         -> "Zákazník už je na účtu$total"
                        r.openWithoutCustomer > 1 -> "Připojeno k nejnovějšímu účtu$total — otevřených je ${r.openWithoutCustomer}, zkontroluj pokladnu"
                        else                      -> "Připojeno k účtu na pokladně$total"
                    }
                    DotykackaState.Assigned(msg)
                },
                onFailure = { e -> DotykackaState.Error(e.message ?: "Dotykačka nedostupná") }
            )
        }
    }

    // ─────────────────────────────────────────────────────
    // Import stávajících zákazníků z Dotykačky
    // 1. preview (jen spočítá)  2. potvrzení  3. skutečný import
    // ─────────────────────────────────────────────────────
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    fun previewImport() = runImport(dryRun = true)
    fun confirmImport() = runImport(dryRun = false)
    fun dismissImport() { _importState.value = ImportState.Idle }

    private fun runImport(dryRun: Boolean) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading(dryRun)
            _importState.value = dotykackaRepository.importCustomers(dryRun).fold(
                onSuccess = { ImportState.Done(it) },
                onFailure = { ImportState.Error(it.message ?: "Import selhal") }
            )
        }
    }

    fun clearScannedUser() {
        _scannedUser.value = null
    }

    fun resetDotykackaState() {
        _dotykackaState.value = DotykackaState.Idle
    }

    fun updatePoints(user: User, newPoints: Int, reason: String = "Úprava obsluhou") {
        if (newPoints < 0) {
            _operationState.value = OperationState.Error("Body nemůžou být záporné")
            return
        }
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val success = repository.updatePoints(
                userId         = user.id,
                oldPoints      = user.points,
                newPoints      = newPoints,
                reason         = reason
            )
            _operationState.value = if (success) {
                OperationState.Success("Body uloženy")
            } else {
                OperationState.Error("Nepodařilo se uložit body")
            }
        }
    }

    fun addUser(name: String, email: String, phone: String, initialPoints: Int) {
        if (name.isBlank() || email.isBlank() || phone.isBlank()) {
            _operationState.value = OperationState.Error("Vyplň jméno, email a telefon")
            return
        }
        if (!email.contains("@")) {
            _operationState.value = OperationState.Error("Neplatný email")
            return
        }
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            _operationState.value = when (repository.addUser(name, email, phone, initialPoints)) {
                AddUserResult.Success        -> OperationState.Success("Zákazník přidán")
                AddUserResult.DuplicateEmail -> OperationState.Error("Zákazník s tímto e-mailem už existuje")
                AddUserResult.Error          -> OperationState.Error("Nepodařilo se přidat zákazníka")
            }
        }
    }

    fun resetOperationState() { _operationState.value = OperationState.Idle }
    fun resetLoginState() { _loginState.value = AdminLoginState.Idle }
    fun setError(message: String) { _operationState.value = OperationState.Error(message) }

    fun logout() {
        repository.logoutAdmin()
        usersJob?.cancel()
        usersJob = null
        _allUsers.value = emptyList()
        _dotykackaState.value = DotykackaState.Idle
        _loginState.value = AdminLoginState.Idle
    }
}

sealed class AdminLoginState {
    object Idle    : AdminLoginState()
    object Loading : AdminLoginState()
    object Success : AdminLoginState()
    data class Error(val message: String) : AdminLoginState()
}

sealed class OperationState {
    object Idle    : OperationState()
    object Loading : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String)   : OperationState()
}

// ── Stav Dotykačka synchronizace ─────────────────────────
sealed class DotykackaState {
    object Idle     : DotykackaState()
    object Syncing  : DotykackaState()
    data class Assigned(val message: String) : DotykackaState()  // zákazník připojen k účtu
    data class Error(val message: String) : DotykackaState()
}

sealed class ImportState {
    object Idle : ImportState()
    data class Loading(val dryRun: Boolean) : ImportState()
    data class Done(val result: DotykackaRepository.ImportResult) : ImportState()
    data class Error(val message: String) : ImportState()
}
