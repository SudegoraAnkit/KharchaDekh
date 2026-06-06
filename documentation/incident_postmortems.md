# Incident Postmortems: KharchaDekh

This document details postmortem analyses for two critical bugs I resolved during the development of KharchaDekh.

---

## Incident Postmortem 1: Restored Database Corruption & Crashes
*   **Date**: June 2, 2026
*   **Severity**: High (Data Loss / App Crash)

### Summary
After restoring a database backup, the application crashed immediately on launch, displaying a SQLite exception: *"disk image is malformed"* or *"schema mismatch"*.

### Root Cause
Android Room runs with Write-Ahead Logging (WAL) enabled by default. In WAL mode:
1.  SQLite caches transactions in log files (`-wal` and `-shm`) instead of writing them directly to the main database file (`kharcha_dekh_db`).
2.  My original backup routine simply copied the main database file, resulting in an incomplete backup since active data was still stored in the WAL files.
3.  On restoration, the app copied the incomplete database file over the existing database, leaving behind the older device's `-wal` and `-shm` files, corrupting the database state.

### Resolution & Prevention
*   **WAL Checkpoint**: I updated the backup routine to query `PRAGMA wal_checkpoint(FULL)` on the writable database helper. This commits all cached log data from WAL journals to the main database file before copying.
*   **File Clean-up on Restore**: The restore routine now deletes any existing `-wal` and `-shm` files on the device before streaming the backup database.
*   **Programmatic Process Kill**: Added `Runtime.getRuntime().exit(0)` after restoration to force-close and restart the app, ensuring Room builds new database memory caches.

---

## Incident Postmortem 2: Silent Failures in Periodic Reminders Queue
*   **Date**: June 4, 2026
*   **Severity**: Medium (Silent Failure / Low Engagement)

### Summary
Users reported that the daily evening reminder notifications (scheduled for 8:30 PM) were not triggering.

### Root Cause
1.  The `ExpenseViewModel` enqueued the reminder work using WorkManager.
2.  On every ViewModel initialization (which occurred whenever the application was opened), the app enqueued the reminder work.
3.  The task was enqueued using standard configuration policies that reschedulded the task. This reset the initial delay countdown to 24 hours every time the user launched the app. Users who opened the app frequently never received reminders.

### Resolution & Prevention
*   **Policy Distinction**: I updated the startup initialization tasks in the ViewModel to use `ExistingPeriodicWorkPolicy.KEEP`. This maintains existing queue timers without resetting them.
*   **Explicit Update Trigger**: We use `ExistingPeriodicWorkPolicy.UPDATE` only when the user explicitly modifies the reminder hour/minute in the settings screen.
*   **Robust Logging**: Added debug logging to trace delay durations and verify reminder tasks in the WorkManager queue.
