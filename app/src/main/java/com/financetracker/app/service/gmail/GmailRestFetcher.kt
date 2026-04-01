package com.financetracker.app.service.gmail

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches Gmail messages via the Gmail REST API using an OAuth2 Bearer token.
 * Returns the same [ImapResult] / [FetchedEmail] types as [ImapGmailFetcher] so
 * [com.financetracker.app.workers.GmailFetchWorker] can use either interchangeably.
 */
@Singleton
class GmailRestFetcher @Inject constructor(
    private val oauthManager: GmailOAuthManager,
    private val credentialsManager: ImapCredentialsManager
) {
    companion object {
        private const val BASE_URL   = "https://gmail.googleapis.com/gmail/v1/users/me"
        private const val MAX_RESULTS = 200
        private val KEYWORDS = listOf(
            "debited", "credited", "transaction", "statement",
            "payment", "spent", "withdrawn", "received"
        )
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Full email fetch used by the background worker. */
    suspend fun fetchEmails(lastSyncMs: Long): ImapResult = withContext(Dispatchers.IO) {
        val token = oauthManager.getAccessToken()
            ?: return@withContext ImapResult.AuthError(
                "OAuth token unavailable. Please sign in with Google again."
            )

        val effectiveSince = if (lastSyncMs == 0L) credentialsManager.getSyncFromMs() else lastSyncMs
        val afterDate = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(effectiveSince))
        val keywordQ  = KEYWORDS.joinToString(" OR ") { "subject:$it" }
        val query     = "after:$afterDate ($keywordQ)"

        try {
            val ids = fetchMessageIds(token, query)
            if (ids.isEmpty()) return@withContext ImapResult.Success(emptyList())

            val emails = mutableListOf<FetchedEmail>()
            for (id in ids) {
                val email = fetchMessage(token, id) ?: continue
                emails.add(email)
            }
            ImapResult.Success(emails)
        } catch (e: UnauthorizedException) {
            oauthManager.invalidateToken(token)
            ImapResult.AuthError("OAuth session expired. Please sign in with Google again.")
        } catch (e: Exception) {
            ImapResult.NetworkError(e.message ?: "Gmail API error")
        }
    }

    /** Quick connectivity test — lists a single inbox message. */
    suspend fun testConnection(): ImapResult = withContext(Dispatchers.IO) {
        val token = oauthManager.getAccessToken()
            ?: return@withContext ImapResult.AuthError(
                "No OAuth token. Please sign in with Google."
            )
        try {
            val body = get("$BASE_URL/messages?q=in:inbox&maxResults=1", token)
            if (body != null) ImapResult.Success(emptyList())
            else ImapResult.NetworkError("No response from Gmail API")
        } catch (e: UnauthorizedException) {
            oauthManager.invalidateToken(token)
            ImapResult.AuthError("Invalid OAuth token. Please sign in again.")
        } catch (e: Exception) {
            ImapResult.NetworkError(e.message ?: "Network error")
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private fun fetchMessageIds(token: String, query: String): List<String> {
        val q    = URLEncoder.encode(query, "UTF-8")
        val body = get("$BASE_URL/messages?q=$q&maxResults=$MAX_RESULTS", token) ?: return emptyList()
        val arr  = JSONObject(body).optJSONArray("messages") ?: return emptyList()
        return List(arr.length()) { arr.getJSONObject(it).getString("id") }
    }

    private fun fetchMessage(token: String, id: String): FetchedEmail? {
        val body    = get("$BASE_URL/messages/$id?format=full", token) ?: return null
        val json    = JSONObject(body)
        val date    = json.optLong("internalDate", System.currentTimeMillis())
        val payload = json.optJSONObject("payload") ?: return null
        val subject = extractHeader(payload, "Subject") ?: ""
        val text    = extractBody(payload)
        return FetchedEmail(subject = subject, body = text, receivedMs = date)
    }

    private fun extractHeader(payload: JSONObject, name: String): String? {
        val headers = payload.optJSONArray("headers") ?: return null
        for (i in 0 until headers.length()) {
            val h = headers.getJSONObject(i)
            if (h.optString("name").equals(name, ignoreCase = true)) return h.optString("value")
        }
        return null
    }

    private fun extractBody(payload: JSONObject): String {
        // Direct body
        val direct = payload.optJSONObject("body")?.optString("data")
        if (!direct.isNullOrBlank()) return decodeBase64(direct)

        // Multipart: prefer text/plain
        val parts = payload.optJSONArray("parts") ?: return ""
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.optString("mimeType") == "text/plain") {
                val data = part.optJSONObject("body")?.optString("data")
                if (!data.isNullOrBlank()) return decodeBase64(data)
            }
        }
        // Fallback: any part with data
        for (i in 0 until parts.length()) {
            val data = parts.getJSONObject(i).optJSONObject("body")?.optString("data")
            if (!data.isNullOrBlank()) return decodeBase64(data)
        }
        return ""
    }

    private fun decodeBase64(encoded: String): String = try {
        String(Base64.decode(encoded, Base64.URL_SAFE), Charsets.UTF_8)
    } catch (e: Exception) { "" }

    private fun get(url: String, token: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()
        val response = client.newCall(request).execute()
        if (response.code == 401) throw UnauthorizedException()
        return if (response.isSuccessful) response.body?.string() else null
    }

    private class UnauthorizedException : Exception()
}
