package cz.cannaclub.cannaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.cannaclub.cannaapp.model.LoyaltyConfig
import cz.cannaclub.cannaapp.repository.LoyaltyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/** Admin: akce "více bodů" a hromadné zprávy. Bonus za rank se počítá automaticky. */
class LoyaltyAdminViewModel(
    private val repo: LoyaltyRepository = LoyaltyRepository()
) : ViewModel() {

    private val _config = MutableStateFlow(LoyaltyConfig())
    val config: StateFlow<LoyaltyConfig> = _config.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Jednorázová zpráva pro snackbar. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun messageShown() { _message.value = null }

    /** Kolik zákazníků dostane zprávu při zvoleném publiku (null = počítá se). */
    private val _recipients = MutableStateFlow<Int?>(null)
    val recipients: StateFlow<Int?> = _recipients.asStateFlow()
    private var countJob: Job? = null

    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            repo.configFlow().collect { cfg ->
                _config.value = cfg
            }
        }
        countRecipients(0)
    }

    // ── Akce ──────────────────────────────────────────────
    enum class Duration(val label: String) { TODAY("Do konce dne"), H24("24 hodin"), SUNDAY("Do neděle"), WEEK("7 dní") }

    fun startPromo(multiplier: Int, duration: Duration, label: String, notify: Boolean) {
        val end = endOf(duration)
        val name = label.trim().ifBlank { if (multiplier == 2) "Dvojité body" else "${multiplier}× body" }
        _busy.value = true
        viewModelScope.launch {
            repo.startPromo(multiplier, end, name, notify).fold(
                onSuccess = { n ->
                    _message.value = if (notify) "Akce běží, upozornění dostalo $n zákazníků" else "Akce běží"
                },
                onFailure = { _message.value = it.message }
            )
            _busy.value = false
        }
    }

    fun stopPromo() = run("Akce ukončena") { repo.stopPromo() }

    private fun endOf(d: Duration): Long {
        val c = Calendar.getInstance()
        when (d) {
            Duration.TODAY  -> c.endOfDay()
            Duration.H24    -> c.add(Calendar.HOUR_OF_DAY, 24)
            Duration.SUNDAY -> {
                while (c.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) c.add(Calendar.DAY_OF_MONTH, 1)
                c.endOfDay()
            }
            Duration.WEEK   -> { c.add(Calendar.DAY_OF_MONTH, 6); c.endOfDay() }
        }
        return c.timeInMillis
    }

    private fun Calendar.endOfDay() {
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 0)
    }

    // ── Hromadná zpráva ───────────────────────────────────
    fun countRecipients(minRank: Int) {
        countJob?.cancel()
        _recipients.value = null
        countJob = viewModelScope.launch {
            delay(250)
            // -1 = nepodařilo se zjistit (server nedostupný)
            _recipients.value = repo.broadcast("", "", minRank, dryRun = true).getOrNull()?.recipients ?: -1
        }
    }

    fun sendBroadcast(title: String, body: String, minRank: Int, onSent: () -> Unit) {
        _busy.value = true
        viewModelScope.launch {
            repo.broadcast(title.trim(), body.trim(), minRank, dryRun = false).fold(
                onSuccess = {
                    _message.value = "Zpráva odeslána ${it.sent} zákazníkům"
                    onSent()
                },
                onFailure = { _message.value = it.message }
            )
            _busy.value = false
        }
    }

    private fun run(ok: String, block: suspend () -> Result<Unit>) {
        _busy.value = true
        viewModelScope.launch {
            block().fold(onSuccess = { _message.value = ok }, onFailure = { _message.value = it.message })
            _busy.value = false
        }
    }
}
