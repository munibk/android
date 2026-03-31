package com.financetracker.app.service.gmail

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores and retrieves Gmail IMAP credentials using EncryptedSharedPreferences
 * backed by Android Keystore. Credentials are never stored in plain text.
 */
@Singleton
class ImapCredentialsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_FILE = "gmail_credentials"
        private const val KEY_EMAIL  = "gmail_email"
        private const val KEY_PASSWORD = "gmail_app_password"
    }

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveCredentials(email: String, appPassword: String) {
        prefs.edit()
            .putString(KEY_EMAIL, email.trim())
            .putString(KEY_PASSWORD, appPassword.trim())
            .apply()
    }

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun getAppPassword(): String? = prefs.getString(KEY_PASSWORD, null)

    fun hasCredentials(): Boolean = !getEmail().isNullOrBlank() && !getAppPassword().isNullOrBlank()

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_EMAIL)
            .remove(KEY_PASSWORD)
            .apply()
    }
}
