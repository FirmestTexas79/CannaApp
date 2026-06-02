package cz.cannaclub.cannaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.cannaclub.cannaapp.model.User
import cz.cannaclub.cannaapp.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repository: UserRepository = UserRepository()
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

    // ── Zákazník nalezený přes QR skenování ──────────────
    private val _scannedUser = MutableStateFlow<User?>(null)
    val scannedUser: StateFlow<User?> = _scannedUser.asStateFlow()

    init {
        viewModelScope.launch {
            combine(_allUsers, _searchQuery) { users, query ->
                if (query.isBlank()) users
                else users.filter { user ->
                    user.name.contains(query, ignoreCase = true) ||
                            user.email.contains(query, ignoreCase = true) ||
                            user.phone.contains(query, ignoreCase = true)
                }
            }.collect { filtered ->
                _filteredUsers.value = filtered
            }
        }
    }

    // ─────────────────────────────────────────────────────
    // Admin přihlášení
    // ─────────────────────────────────────────────────────
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

    private fun loadUsers() {
        viewModelScope.launch {
            repository.getAllUsersFlow().collect { users ->
                _allUsers.value = users
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    // ─────────────────────────────────────────────────────
    // QR skenování — hledá zákazníka přímo ve Firestore
    // Funguje i když seznam ještě není načtený
    // ─────────────────────────────────────────────────────
    fun findUserByQrCode(userId: String) {
        viewModelScope.launch {
            val user = repository.getUserById(userId)
            if (user != null) {
                _scannedUser.value = user
            } else {
                _operationState.value = OperationState.Error("Zákazník nenalezen")
            }
        }
    }

    fun clearScannedUser() {
        _scannedUser.value = null
    }

    // ─────────────────────────────────────────────────────
    // Úprava bodů
    // ─────────────────────────────────────────────────────
    fun updatePoints(user: User, newPoints: Int) {
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
                oldTotalPoints = user.totalPoints
            )

            _operationState.value = if (success) {
                OperationState.Success("Body uloženy")
            } else {
                OperationState.Error("Nepodařilo se uložit body")
            }
        }
    }

    // ─────────────────────────────────────────────────────
    // Přidání zákazníka
    // ─────────────────────────────────────────────────────
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

            val success = repository.addUser(name, email, phone, initialPoints)

            _operationState.value = if (success) {
                OperationState.Success("Zákazník přidán")
            } else {
                OperationState.Error("Nepodařilo se přidat zákazníka")
            }
        }
    }

    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }

    fun resetLoginState() {
        _loginState.value = AdminLoginState.Idle
    }

    fun setError(message: String) {
        _operationState.value = OperationState.Error(message)
    }

    fun logout() {
        repository.logoutAdmin()
        _allUsers.value = emptyList()
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