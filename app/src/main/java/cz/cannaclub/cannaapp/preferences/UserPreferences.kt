package cz.cannaclub.cannaapp.preferences

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("canna_user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NAME      = "user_name"
        private const val KEY_EMAIL     = "user_email"
        private const val KEY_PHONE     = "user_phone"
        private const val KEY_USER_ID   = "user_id"      // kdo je právě přihlášený (pro FCM onNewToken)
        private const val KEY_LOGGED_IN = "logged_in"    // automatické přihlášení po spuštění
    }

    fun saveUser(name: String, email: String, phone: String, userId: String) {
        prefs.edit()
            .putString(KEY_NAME,  name)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PHONE, phone)
            .putString(KEY_USER_ID, userId)
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
    }

    fun getSavedName():   String = prefs.getString(KEY_NAME,  "") ?: ""
    fun getSavedEmail():  String = prefs.getString(KEY_EMAIL, "") ?: ""
    fun getSavedPhone():  String = prefs.getString(KEY_PHONE, "") ?: ""
    fun getSavedUserId(): String =
        if (prefs.getBoolean(KEY_LOGGED_IN, false)) prefs.getString(KEY_USER_ID, "") ?: "" else ""

    fun hasSavedUser(): Boolean =
        prefs.getBoolean(KEY_LOGGED_IN, false) &&
            getSavedEmail().isNotBlank() && getSavedPhone().isNotBlank()

    /** Odhlášení — údaje zůstanou předvyplněné, jen se přestane automaticky přihlašovat. */
    fun markLoggedOut() {
        prefs.edit().putBoolean(KEY_LOGGED_IN, false).apply()
    }

    fun clearUser() {
        prefs.edit().clear().apply()
    }
}
