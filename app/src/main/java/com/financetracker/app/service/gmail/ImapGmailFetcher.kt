package com.financetracker.app.service.gmail

import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton
import javax.mail.*
import javax.mail.internet.MimeMultipart
import javax.mail.search.AndTerm
import javax.mail.search.ComparisonTerm
import javax.mail.search.OrTerm
import javax.mail.search.ReceivedDateTerm
import javax.mail.search.SearchTerm
import javax.mail.search.SubjectTerm
import java.util.Date

data class FetchedEmail(
    val subject: String,
    val body: String,
    val receivedMs: Long
)

sealed class ImapResult {
    data class Success(val emails: List<FetchedEmail>) : ImapResult()
    data class AuthError(val message: String) : ImapResult()
    data class NetworkError(val message: String) : ImapResult()
}

@Singleton
class ImapGmailFetcher @Inject constructor(
    private val credentialsManager: ImapCredentialsManager
) {
    companion object {
        private const val IMAP_HOST = "imap.gmail.com"
        private const val IMAP_PORT = "993"
        private val SUBJECT_KEYWORDS = listOf(
            "debited", "credited", "transaction", "statement",
            "payment", "spent", "withdrawn", "received"
        )
    }

    /** Fetches relevant bank/finance emails since [lastSyncMs]. */
    suspend fun fetchEmails(lastSyncMs: Long, maxRetries: Int = 3): ImapResult =
        withContext(Dispatchers.IO) {
            val email = credentialsManager.getEmail() ?: return@withContext ImapResult.AuthError("No credentials")
            val password = credentialsManager.getAppPassword() ?: return@withContext ImapResult.AuthError("No credentials")

            // On first sync (lastSyncMs == 0), use the user-selected sync-from year
            val effectiveSince = if (lastSyncMs == 0L) {
                credentialsManager.getSyncFromMs()
            } else {
                lastSyncMs
            }

            var attempt = 0
            var lastError: Exception? = null

            while (attempt < maxRetries) {
                try {
                    // runInterruptible makes blocking Java I/O actually cancellable by the timeout
                    val result = withTimeoutOrNull(30_000L) {
                        runInterruptible { doFetch(email, password, effectiveSince) }
                    }
                    if (result != null) return@withContext result
                    return@withContext ImapResult.NetworkError("Connection timed out — check your network and try again")
                } catch (e: AuthenticationFailedException) {
                    return@withContext ImapResult.AuthError(e.message ?: "Authentication failed")
                } catch (e: MessagingException) {
                    lastError = e
                    attempt++
                    if (attempt < maxRetries) {
                        Thread.sleep((1000L * (1 shl attempt)))
                    }
                }
            }
            ImapResult.NetworkError(lastError?.message ?: "Network error after $maxRetries attempts")
        }

    private fun doFetch(email: String, password: String, lastSyncMs: Long): ImapResult {
        val props = Properties().apply {
            put("mail.imap.host", IMAP_HOST)
            put("mail.imap.port", IMAP_PORT)
            put("mail.imap.ssl.enable", "true")
            put("mail.imap.ssl.protocols", "TLSv1.2")
            put("mail.imap.connectiontimeout", "15000")
            put("mail.imap.timeout", "20000")
            put("mail.imap.writetimeout", "20000")
        }

        val session = Session.getInstance(props)
        val store = session.getStore("imap")
        store.connect(IMAP_HOST, email, password)

        val inbox = store.getFolder("INBOX") as IMAPFolder
        inbox.open(Folder.READ_ONLY)

        val subjectTerms = SUBJECT_KEYWORDS.map { keyword ->
            SubjectTerm(keyword) as SearchTerm
        }.toTypedArray()

        val dateTerm = ReceivedDateTerm(
            ComparisonTerm.GT,
            Date(lastSyncMs)
        )

        val searchTerm = AndTerm(OrTerm(subjectTerms), dateTerm)
        val messages = inbox.search(searchTerm)

        val result = messages.mapNotNull { message ->
            runCatching {
                FetchedEmail(
                    subject    = message.subject ?: "",
                    body       = extractBody(message),
                    receivedMs = message.receivedDate?.time ?: System.currentTimeMillis()
                )
            }.getOrNull()
        }

        inbox.close(false)
        store.close()

        return ImapResult.Success(result)
    }

    private fun extractBody(message: Message): String {
        return when {
            message.isMimeType("text/plain") ->
                message.content as? String ?: ""
            message.isMimeType("text/html") ->
                (message.content as? String)?.let { stripHtml(it) } ?: ""
            message.isMimeType("multipart/*") -> {
                val multipart = message.content as? MimeMultipart ?: return ""
                buildString {
                    for (i in 0 until multipart.count) {
                        val part = multipart.getBodyPart(i)
                        when {
                            part.isMimeType("text/plain") ->
                                append(part.content as? String ?: "")
                            part.isMimeType("text/html") ->
                                append(stripHtml(part.content as? String ?: ""))
                        }
                    }
                }
            }
            else -> ""
        }
    }

    private fun stripHtml(html: String): String =
        html.replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
