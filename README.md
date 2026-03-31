# Finance Tracker — Android App

A comprehensive Android finance tracker built with **Kotlin + Jetpack Compose**.  
Reads transactions from Gmail (IMAP) and SMS automatically, categorises them using ML-style keyword matching, and presents rich charts and dashboards.

---

## Features

| Phase | Feature |
|-------|---------|
| Data  | Room database — transactions, categories, credit cards |
| SMS   | Auto-reads bank SMS inbox; real-time listener for new messages |
| Gmail | IMAP fetch via App Password — no OAuth required |
| AI    | Keyword + rule-based category classifier with user overrides |
| UI    | Dashboard · Transactions · Charts · Categories · Credit Cards |
| Sync  | WorkManager periodic workers (1 / 6 / 12 / 24 h) |
| Alerts| Budget alerts at 80% · Credit card due-date reminders |
| CI    | GitHub Actions builds APK on every push — no local SDK needed |

---

## Tech Stack

| Purpose | Library |
|---------|---------|
| Language | Kotlin 1.9 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| Navigation | Navigation Compose |
| Database | Room 2.6 + Paging 3 |
| DI | Hilt 2.51 |
| Background | WorkManager 2.9 |
| Charts | MPAndroidChart 3.1 |
| Gmail | JavaMail android-mail 1.6.7 (IMAP + App Password) |
| Security | EncryptedSharedPreferences (Android Keystore) |
| Biometric | AndroidX Biometric 1.1 |

---

## Quick Start (GitHub Actions — no local Android SDK needed)

1. **Fork / clone** this repository to your GitHub account.
2. Push any change to the `main` branch.
3. Go to **Actions** tab → click the latest **"Build APK"** run.
4. Scroll to **Artifacts** and download `finance-tracker-debug.zip`.
5. Unzip → install `app-debug.apk` on your Android device  
   *(enable "Install from unknown sources" in device settings first)*.

> GitHub free tier gives **2,000 build minutes/month**.  
> First build ≈ 4–6 min (cold cache). Subsequent builds ≈ 1–2 min.

---

## VS Code Setup (recommended extensions)

| Extension | Purpose |
|-----------|---------|
| `mathiasfrohlich.Kotlin` | Kotlin syntax highlighting |
| `vscjava.vscode-java-pack` | Java / Kotlin code hints |
| `github.vscode-github-actions` | View Actions runs inline |
| `eamodio.gitlens` | Git history & branch management |

---

## Gmail App Password Setup

The app uses Gmail IMAP with an **App Password** — no Google Cloud Console or OAuth required.

1. Go to [myaccount.google.com → Security](https://myaccount.google.com/security).
2. Enable **2-Step Verification** if not already enabled.
3. Under 2-Step Verification, select **App Passwords**.
4. Create a new app password for "Mail" on "Android device".
5. Copy the 16-character password.
6. Open the app → **Settings → Gmail IMAP** → enter your Gmail address and the 16-char App Password → tap **Test** then **Save**.

---

## Project Structure

```
app/src/main/java/com/financetracker/app/
├── data/
│   ├── db/                  # Room entities, DAOs, FinanceDatabase
│   └── repository/          # TransactionRepository, CreditCardRepository, etc.
├── service/
│   ├── sms/                 # SmsParser, SmsReaderService
│   ├── gmail/               # ImapCredentialsManager, ImapGmailFetcher, GmailParser
│   ├── classifier/          # CategoryClassifier (keyword + override)
│   └── notification/        # (handled in SyncWorker)
├── presentation/
│   ├── navigation/          # AppNavigation, BottomNavItem
│   ├── viewmodel/           # DashboardViewModel, TransactionsViewModel, etc.
│   └── ui/                  # Compose screens + theme
├── di/                      # Hilt modules (Database, Repository)
├── workers/                 # GmailFetchWorker, SyncWorker
├── receiver/                # SmsBroadcastReceiver, BootReceiver
├── MainActivity.kt
└── FinanceTrackerApp.kt
```

---

## Permissions

| Permission | Purpose |
|-----------|---------|
| `READ_SMS` | Read existing SMS inbox |
| `RECEIVE_SMS` | Listen for new SMS messages |
| `INTERNET` | IMAP connection to Gmail |
| `RECEIVE_BOOT_COMPLETED` | Re-register workers after reboot |
| `USE_BIOMETRIC` | Optional biometric lock |
| `POST_NOTIFICATIONS` | Budget and due-date alerts |

---

## Development Plan

The full 13-phase development plan is in `FinanceApp_DevelopmentPlan_v3.docx`.

---

*Generated plan — Finance Tracker Android App | 13 phases | Kotlin + Jetpack Compose*
