package com.financetracker.app.service.gmail

import android.accounts.Account
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Gmail OAuth tokens via Google Sign-In + GoogleAuthUtil.
 * The signed-in account email is persisted; tokens are fetched fresh on demand
 * (GoogleAuthUtil caches them internally until expiry).
 */
@Singleton
class GmailOAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_FILE      = "gmail_oauth"
        private const val KEY_OAUTH_EMAIL = "oauth_account_email"
        // Full-access IMAP scope — covers both Gmail REST API and IMAP XOAUTH2
        const val GMAIL_SCOPE = "oauth2:https://mail.google.com/"
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

    fun saveEmail(email: String) {
        prefs.edit().putString(KEY_OAUTH_EMAIL, email.trim()).apply()
    }

    fun getEmail(): String? = prefs.getString(KEY_OAUTH_EMAIL, null)

    fun hasCredentials(): Boolean = !getEmail().isNullOrBlank()

    fun clearCredentials() {
        prefs.edit().remove(KEY_OAUTH_EMAIL).apply()
    }

    /**
     * Returns a valid access token for [GMAIL_SCOPE], or null if unavailable.
     * Must be called from a background thread (GoogleAuthUtil makes network calls).
     */
    fun getAccessToken(): String? {
        val email = getEmail() ?: return null
        val account = Account(email, "com.google")
        return try {
            GoogleAuthUtil.getToken(context, account, GMAIL_SCOPE)
        } catch (e: UserRecoverableAuthException) {
            // User needs to re-grant the scope — caller should show a re-sign-in prompt
            null
        } catch (e: Exception) {
            null
        }
    }

    /** Clears the cached token so the next [getAccessToken] call forces a fresh fetch. */
    fun invalidateToken(token: String) {
        try { GoogleAuthUtil.invalidateToken(context, token) } catch (e: Exception) { /* ignore */ }
    }
}
