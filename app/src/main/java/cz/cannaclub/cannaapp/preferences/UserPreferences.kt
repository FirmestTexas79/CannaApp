package cz.cannaclub.cannaapp.preferences

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("canna_user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NAME  = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_PHONE = "user_phone"
    }

    fun saveUser(name: String, email: String, phone: String) {
        prefs.edit()
            .putString(KEY_NAME,  name)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PHONE, phone)
            .apply()
    }

    fun getSavedName():  String = prefs.getString(KEY_NAME,  "") ?: ""
    fun getSavedEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""
    fun getSavedPhone(): String = prefs.getString(KEY_PHONE, "") ?: ""

    fun hasSavedUser(): Boolean =
        getSavedEmail().isNotBlank() && getSavedPhone().isNotBlank()

    fun clearUser() {
        prefs.edit().clear().apply()
    }
}