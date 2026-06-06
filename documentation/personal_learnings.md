# Personal Learnings: KharchaDekh

Building KharchaDekh was an invaluable learning experience that helped me grow as an Android developer and software architect. This document outlines my key takeaways from the project.

---

## 1. Deep Diving into SQLite & Room Internals
I had used Room database wrappers before, but always as a black box for basic operations. This project forced me to understand the underlying SQLite database mechanics:
*   **Write-Ahead Logging (WAL)**: Resolving the database backup corruption bug taught me how SQLite processes transactions using log files (`-wal` and `-shm`). I learned that copying a database file directly while in WAL mode can result in corrupted backups.
*   **Database Checkpoints**: I learned how to use `PRAGMA wal_checkpoint(FULL)` to flush cached journal logs back to the main database file before copying.
*   **Resource Management**: I learned that restoring a database backup requires closing active database connections first, deleting old journal logs, and restarting the app process to reset the Room database reference cache.

---

## 2. Master WorkManager Scheduling Constraints
Scheduling background tasks in Android can be difficult due to battery optimization and system standby limitations.
*   I learned how to configure WorkManager constraints (`setRequiresBatteryNotLow`) to allow reminders to run in the background.
*   I resolved the rescheduling loop bug by distinguishing between ViewModel startup tasks (using `ExistingPeriodicWorkPolicy.KEEP` to preserve timers) and explicit user settings updates (using `ExistingPeriodicWorkPolicy.UPDATE` to reschedule immediately).

---

## 3. Designing for Accessibility & System Themes
I gained a deeper understanding of accessibility and Material 3 design principles:
*   I learned to avoid hardcoding colors. Hardcoding colors for text and surface elements created contrast issues when the system switched to dark theme.
*   By querying `isSystemInDarkTheme()`, I learned how to style elements dynamically in Jetpack Compose, ensuring readability in both light and dark themes.
*   I overhauled the color palette to meet WCAG AA contrast standards.

---

## 4. Privacy-First App Architecture
Designing for privacy requires a different development approach:
*   Instead of relying on remote servers, I learned to perform data processing (regex parsing of transaction notifications) entirely on-device, in compliance with India's DPDP Act 2023.
*   I integrated the Storage Access Framework (SAF) to let users manage their backups directly to Google Drive or OneDrive without routing files through third-party servers.
*   I kept the application size small by using Android's native `PdfDocument` and canvas drawing tools instead of importing large external libraries.

---

## 5. Navigating Android Security & Play Store Compliance
*   Using sensitive permissions like `NotificationListenerService` can cause sideloaded APKs to be flagged by Google Play Protect as suspicious.
*   I learned that uploading a signed release bundle (`.aab`) to the Google Play Console (even on the Internal Test Track) registers the signing signature with Google Play services, whitelisting the app package and resolving the warning.
