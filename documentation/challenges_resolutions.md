# Challenges & Resolutions: KharchaDekh

This document outlines the major technical hurdles I encountered while building KharchaDekh and the engineering solutions I implemented.

---

## Challenge 1: Google Play Protect Warnings (Sideloading Restriction)
*   **The Issue**: The application relies on `NotificationListenerService` to parse transactions locally and maintain privacy. Because this permission grants access to notification content, Google Play Protect flags any sideloaded (non-Play Store compiled) APK requesting this service as suspicious or malicious.
*   **Why it occurred**: Android security models restrict sensitive permission scopes on sideloaded packages. Play Protect defaults to a high-alert blocking mode unless the application signature is verified by the Google Play Console ecosystem.
*   **My Resolution**:
    1.  I added prominent disclosure panels on the onboarding consent screen to explain why notification permissions are requested.
    2.  I updated the application logic to allow fully manual offline expense logging if permission is denied.
    3.  I registered and compiled a signed release Android App Bundle (`.aab`) and uploaded it to the Google Play Console under the Internal Test Track. This uploads and registers the app's signing signature, whitelisting the package and resolving the warning.

---

## Challenge 2: Room Database Backups & WAL File Corruption
*   **The Issue**: When users triggered a database backup, the output SQLite database was missing recent entries or became corrupted on restore.
*   **Why it occurred**: Room uses Write-Ahead Logging (WAL) by default. In WAL mode, SQLite writes new transactions to auxiliary journal files (`kharcha_dekh_db-wal` and `kharcha_dekh_db-shm`) instead of directly modifying the main database file. Backing up the main database file by streaming it directly resulted in incomplete backups since active changes remained cached in the WAL journals.
*   **My Resolution**:
    1.  I integrated a database checkpoint mechanism before backup in [BackupManager.kt](file:///d:/2026/Project/KharchaDekh/app/src/main/java/com/ankitsudegora/util/BackupManager.kt). I force a full WAL checkpoint to write cached logs to the main database file before copying:
        ```kotlin
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        ```
    2.  For database recovery, I close the active database connections, delete any pre-existing WAL and SHM files on the device, overwrite the database, and programmatically restart the application process to reset Room's in-memory memory reference pools.

---

## Challenge 3: WorkManager Reminder Rescheduling Loop
*   **The Issue**: Scheduled reminder notifications never fired.
*   **Why it occurred**: The application initialization enqueues periodic tasks. Calling `enqueueUniquePeriodicWork` with default settings on every ViewModel initialization restarted the delay timer, resetting the worker queue and preventing reminders from firing.
*   **My Resolution**:
    1.  I updated the startup initialization tasks in the ViewModel to use `ExistingPeriodicWorkPolicy.KEEP`. This maintains existing queue timers without resetting them.
    2.  I use `ExistingPeriodicWorkPolicy.UPDATE` only when the user explicitly modifies the reminder hour/minute in the settings screen.
    3.  I split the background checks into 4 distinct enqueued jobs (Morning, Midday, Afternoon, and Night), forcing the Night reminder to run unconditionally.

---

## Challenge 4: WCAG Contrast and Dark Theme Inconsistencies
*   **The Issue**: Hardcoded light-theme colors (mint-green container `#E6F9F6` and dark-green text `#0F766E` for credit entries) led to severe contrast/readability issues and broken visuals in dark mode.
*   **Why it occurred**: The UI elements did not dynamically adjust to theme state, resulting in bright containers rendering on slate backgrounds.
*   **My Resolution**:
    1.  I refactored color configurations to adapt dynamically by querying `isSystemInDarkTheme()`.
    2.  For credit transactions, in dark mode, we render a deep dark green background (`#0F2D24`) and light teal text (`#4ADE80`), ensuring high contrast and visual consistency.
    3.  I updated the SMS parsed alert card badge to use a dynamic surface color (`MaterialTheme.colorScheme.surfaceVariant`) instead of a static white background.
