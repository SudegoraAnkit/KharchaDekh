# KharchaDekh Developer Documentation

Welcome to the KharchaDekh developer guide. This document provides an overview of the codebase architecture, development environment setup, key services, build processes, project challenges, roadmap, and contribution guidelines.

---

## 1. Codebase Architecture

KharchaDekh is built using modern Android development practices, utilizing **Jetpack Compose** for UI and following the **MVVM (Model-View-ViewModel)** architectural pattern.

```
com.ankitsudegora/
│
├── data/              # Room Database, DAOs, Entities, and Repository
│   ├── AppDatabase.kt
│   ├── Category.kt
│   ├── Transaction.kt
│   ├── RecurringSchedule.kt
│   └── ExpenseRepository.kt
│
├── receiver/          # Broadcast Receivers (e.g. BootReceiver for Worker re-registrations)
│
├── service/           # Notification Listener Service for local SMS parsing
│   └── TransactionNotificationListener.kt
│
├── ui/                # UI Layer (Compose screens, components, and themes)
│   ├── screens/       # DashboardScreen, SettingsScreen, OnboardingScreen, EnrichmentScreen
│   ├── components/    # Icon mappings and UI widgets
│   └── theme/         # Color palettes, Typographies, and Theme definitions
│
├── util/              # Utility Classes (Database backup, Reports exporter)
│   ├── BackupManager.kt
│   └── Exporter.kt
│
├── viewmodel/         # ViewModels managing UI state & scheduling logic
│   └── ExpenseViewModel.kt
│
├── worker/            # WorkManager Background Tasks
│   └── ReminderWorker.kt
│
└── MainActivity.kt    # App Entry Point & SAF Intent Pickers
```

### Key Components

*   **Database (`AppDatabase` & Room)**: Persists transaction entries, customized budget categories, and active recurring payment schedules locally. It operates with Write-Ahead Logging (WAL) enabled.
*   **Notification Listener (`TransactionNotificationListener`)**: Runs on-device. It captures incoming notifications from banking and UPI apps (e.g., HDFC, SBI, Google Pay, Paytm) and parses amounts, merchants, and transaction types using regex patterns locally.
*   **WorkManager Reminders (`ReminderWorker`)**: Schedules 4 daily periodic checkpoints (Morning, Midday, Afternoon, and Night). The night reminder is unconditional, reminding the user to update their ledger even if they have already logged spends today.
*   **SAF Cloud Backups (`BackupManager`)**: Integrates with Android's Storage Access Framework (SAF) to let users backup and restore database files directly to/from Google Drive, OneDrive, or local folders. Checkpointing (`PRAGMA wal_checkpoint(FULL)`) is forced prior to backup to guarantee integrity.
*   **Exports Engine (`Exporter`)**: Generates Excel-compatible CSV statements and native canvas-based A4 PDF reports for sharing.

---

## 2. DPDP Act 2023 Compliance

KharchaDekh is built as a **privacy-first application** in compliance with India's Digital Personal Data Protection (DPDP) Act:
1.  **No Server Processing**: All notification parsing, transaction analysis, and database storage are handled offline on the user's phone. No financial data leaves the device.
2.  **Explicit Consent**: The onboarding screen forces an explicit terms-of-use checkbox before notification listeners can be enabled. The app behaves as a fully functional manual tracker if notification permission is denied.
3.  **Data Deletion Rights**: The Settings screen contains a "Flush All Ledger States" button which clears all Room databases and consent preferences.

---

## 3. Environment Setup

*   **Minimum SDK**: 24 (Android 7.0)
*   **Target/Compile SDK**: 36 (Android 15 / 16 preview)
*   **Gradle Build System**: Gradle 9.3.1 (with Kotlin Script `build.gradle.kts` configuration)
*   **Java Toolchain**: JDK 17 / 21 / 25 compatible

### Local Properties Configuration
Create a `.env` file in the root directory and define local keys:
```env
GEMINI_API_KEY=your_gemini_api_key_here
```

---

## 4. Keystore & Release Signing

Release builds require a valid keystore. The project includes a self-signed upload keystore (`my-upload-key.jks`) in the root directory.

### Signing Environment Variables
To compile signed builds, export the following environment variables prior to running the Gradle task:
*   `STORE_PASSWORD`: Keystore passcode (Default: `password`)
*   `KEY_PASSWORD`: Key alias passcode (Default: `password`)
*   `KEYSTORE_PATH`: Absolute path to `my-upload-key.jks` (Defaults to root directory location if omitted)

---

## 5. Compilation & Package Commands

Use the following commands from the root directory to generate production builds:

### Compile Signed Release APK
In PowerShell (Windows):
```powershell
$env:STORE_PASSWORD="password"; $env:KEY_PASSWORD="password"; .\gradlew.bat assembleRelease
```
*   **Output Path**: `app/build/outputs/apk/release/app-release.apk`

### Compile Signed Release Android App Bundle (AAB)
In PowerShell (Windows):
```powershell
$env:STORE_PASSWORD="password"; $env:KEY_PASSWORD="password"; .\gradlew.bat bundleRelease
```
*   **Output Path**: `app/build/outputs/bundle/release/app-release.aab`

### Clean Build Directory
```bash
.\gradlew.bat clean
```

---

## 6. Project Challenges & Resolutions

### Challenge A: Google Play Protect Warnings during Sideloading
*   **Why it occurred**: The application utilizes a `NotificationListenerService` to parse transactions locally and maintain user privacy. Because this permission grants access to notification contents, Google Play Protect flags any sideloaded (non-Play Store compiled) APK requesting this service as suspicious or high-risk.
*   **How we resolved it**: 
    1.  We implemented clear prominent disclosures on the onboarding screen detailing exactly why Notification access is required.
    2.  The application will work fully offline as a manual log if permission is denied.
    3.  Uploading the signed release bundle (`.aab`) to the Google Play Console (even under the Internal Test Track) registers the signing signature with Google Play services, whitelisting the app package and resolving the warning.

### Challenge B: Room Database Backups & WAL Corruption
*   **Why it occurred**: By default, Room operates in Write-Ahead Logging (WAL) mode. When changes are written to the database, SQLite saves them to journal files (`-wal` and `-shm`) instead of directly modifying the main database file (`kharcha_dekh_db`). Simply copying the main database file during a backup results in incomplete or corrupted data since active transactions are still stored in the WAL files.
*   **How we resolved it**: 
    1.  Prior to backing up, we run `PRAGMA wal_checkpoint(FULL)` query on the writable database helper. This forces SQLite to write all cached log data from WAL journals back to the main database file, ensuring it is self-contained.
    2.  During the restoration process, we close the active database connections, explicitly delete any pre-existing WAL and SHM files on the device, overwrite the database file, and programmatically restart the application to reset Room's in-memory memory reference pools.

### Challenge C: WorkManager Reminder Reset Loop
*   **Why it occurred**: The application initialization enqueues periodic tasks. Enqueuing unique periodic work using default policies on every ViewModel initialization resets the delay timer, meaning that opening the app repeatedly restarts the worker queue and prevents reminders from ever triggering.
*   **How we resolved it**: 
    1.  We updated startup initialization tasks to use `ExistingPeriodicWorkPolicy.KEEP`. This maintains pre-existing queue timers without resetting them.
    2.  We use `ExistingPeriodicWorkPolicy.UPDATE` only when the user explicitly modifies the reminder hour/minute in the settings screen.
    3.  We split background checks into 4 distinct enqueued jobs (Morning, Midday, Afternoon, and Night), forcing the Night reminder to run unconditionally.

### Challenge D: WCAG Contrast and Theme Consistency Issues
*   **Why it occurred**: The transaction ledger items used hardcoded color values (mint-green container `#E6F9F6` and dark-green text `#0F766E` for credits). While these had adequate contrast in light mode, switching to dark mode rendered them with extremely poor contrast (white/green text against bright containers on slate backgrounds), violating WCAG AA compliance and breaking dark mode aesthetics.
*   **How we resolved it**:
    1.  We refactored color configurations to adapt dynamically by querying `isSystemInDarkTheme()`.
    2.  For credit transactions, in dark mode, we render a deep dark green background (`#0F2D24`) and light teal text (`#4ADE80`), ensuring high contrast and visual consistency.
    3.  We updated the SMS parsed alert card badge to use a dynamic surface color (`MaterialTheme.colorScheme.surfaceVariant`) instead of a static white background.

### Challenge E: Gradle Compilation and Code Warnings
*   **Why it occurred**: Moving files and upgrading targets to compilation SDK 36 caused compilation issues due to a missing `getIconVector` import statement in the settings screen, deprecated properties like `isBoldText` on custom `Paint` rendering in the native PDF generator, and missing signing configuration passwords in the build shell.
*   **How we resolved it**:
    1.  Imported `getIconVector` inside the Settings UI class.
    2.  Replaced `isBoldText = true` with `isFakeBoldText = true` inside `Exporter.kt`.
    3.  Configured the PowerShell build process to pass keystore properties (`STORE_PASSWORD`, `KEY_PASSWORD`) in the environment variables to sign the production-ready outputs.

---

## 7. Future Roadmap

We aim to continuously improve the app's local capabilities and analytics. Future development priorities include:

1.  **On-Device OCR & Receipt Scanning**: Integrate Google ML Kit locally to scan paper receipts, extract transaction metrics (amount, date, merchant), and auto-fill manual spend logs offline.
2.  **Predictive Smart Budgeting**: Implement local light-weight machine learning classifiers (e.g. Random Forest or Decision Trees running locally) to automatically predict categories for manual spends based on historical merchant mapping.
3.  **Advanced Visualizations**: Introduce detailed interactive charts (savings rate meters, scrollable weekly cash outflow comparisons, and category comparison bars over time).
4.  **End-to-End Encrypted (E2EE) Syncing**: Create private folder synchronization tools enabling users to sync database backups across multiple devices using their personal cloud accounts (Drive/OneDrive) securely, without using external centralized servers.
5.  **Smart Transaction Reconciling**: Automatically detect and merge overlapping transactions (e.g., matching a manually logged transaction with an incoming UPI notification) to prevent duplicate entries.

---

## 8. Contributing Guidelines

We welcome contributions from the developer community! If you wish to contribute, please follow these guidelines:

### Core Contribution Philosophy
*   **Privacy First**: All features must operate strictly locally on the user's device. No remote logging, analytics, tracking, or network transmission hook implementations will be approved.

### Getting Started
1.  Fork the repository and create a feature branch (`feature/your-feature-name` or `bugfix/issue-name`).
2.  Follow the official Android coding conventions. Ensure code formatting matches the rules defined in `gradle.properties` (`kotlin.code.style=official`).
3.  Verify your changes build cleanly:
    ```bash
    .\gradlew.bat assembleDebug
    ```
4.  Write unit and integration tests where applicable. Run test checks via:
    ```bash
    .\gradlew.bat test
    ```
5.  Submit a Pull Request (PR) with a clear description of changes, visual changes (screenshots/GIFs), and verification steps.
