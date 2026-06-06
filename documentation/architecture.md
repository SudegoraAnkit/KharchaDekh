# System & Tech Architecture: KharchaDekh

This document outlines the architectural patterns, data flow, and components I chose to construct KharchaDekh.

---

## 1. Architectural Patterns
I implemented a standard **MVVM (Model-View-ViewModel)** architectural pattern. Since the app is offline-first, I designed the local SQLite database as the single source of truth.

```
┌────────────────────────────────────────────────────────────────────────┐
│                              VIEW LAYER                                │
│                     (MainActivity & Jetpack Compose Screens)           │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │  Observes StateFlows
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                           VIEWMODEL LAYER                              │
│                    (ExpenseViewModel - State Holders)                   │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │  Invokes Suspend Methods
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                           REPOSITORY LAYER                             │
│                     (ExpenseRepository - Data Logic)                    │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │  Reads / Writes Data
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                             MODEL LAYER                                │
│                 (Room DB / SQLite Sandbox Local Files)                  │
└────────────────────────────────────────────────────────────────────────┘
```

*   **View Layer (Declarative Compose)**: Displays states reactively. `MainActivity` serves as the single-activity entry point. Compose screens (Dashboard, Settings, Onboarding, Enrichment) observe ViewModel states as Compose state variables.
*   **ViewModel Layer (`ExpenseViewModel`)**: Prepares UI-ready data flows. It combines raw transaction lists, budget categories, and filter selections (Daily, Weekly, Monthly, Yearly) into unified state wrappers. It executes database modifications on background threads using Kotlin Coroutines (`viewModelScope`).
*   **Repository Layer (`ExpenseRepository`)**: Decouples database operations from UI flows, managing references to DAOs.
*   **Model Layer (Room DB)**: Runs on SQLite. Room handles schema management, transactions, and migration logic.

---

## 2. Background Component Architecture

The app uses background processing services to support automated parsing and reminders.

```
┌─────────────────────────┐     Captures Alert      ┌─────────────────────────────────┐
│ System Notifications    ├────────────────────────►│ TransactionNotificationListener │
└─────────────────────────┘                         └────────────────┬────────────────┘
                                                                     │
                                                                     │ Runs Regex Engine &
                                                                     │ Saves Pending Transaction
                                                                     ▼
┌─────────────────────────┐   Enqueues Alarms       ┌─────────────────────────────────┐
│       WorkManager       ├────────────────────────►│          AppDatabase            │
│  (4x ReminderWorkers)   │                         │            (Room)               │
└─────────────────────────┘                         └─────────────────────────────────┘
```

### A. Notification Parsing Service
*   **Component**: `TransactionNotificationListener` extends Android's native `NotificationListenerService`.
*   **Lifecycle**: The Android system starts and binds this service automatically. It runs continuously in the background.
*   **Data Capture**: When a notification is posted (`onNotificationPosted`), the service extracts the text content and validates the sender package (e.g. Google Pay, Paytm, or bank applications).
*   **Local Processing**: If a sender matches, the text is evaluated by the regex parser. Once matched, it immediately inserts a pending record into the `AppDatabase` via a coroutine scope, bypassing the UI.

### B. Periodic Reminders Scheduler
*   **Component**: `ReminderWorker` extends `CoroutineWorker`.
*   **Lifecycle**: Managed by WorkManager. Jobs are registered to run every 24 hours with custom delays corresponding to the 4 scheduled daily times (9:00 AM, 1:30 PM, 5:30 PM, and the user-configured evening hour).
*   **System Constraints**: Scheduled with battery-saving constraints (`setRequiresBatteryNotLow(true)`).

---

## 3. Data Flow Diagrams

### Transaction Capture Lifecycle
1.  **Notification posted** -> System triggers service -> Service extracts notification string -> Regex match extracts amount/merchant -> Room database writes entry as `isPending = true`.
2.  **App opened** -> ViewModel exposes pending list -> Dashboard Screen displays "Action Required" list.
3.  **Transaction Categorized** -> User selects category in Enrichment Screen -> ViewModel updates entry (`isPending = false`, links `categoryId`) -> Room database commits update -> UI reactively updates totals.

### SAF Database Backup Lifecycle
1.  User clicks **Backup** -> MainActivity launches `ACTION_CREATE_DOCUMENT` file picker -> User chooses Google Drive path.
2.  MainActivity receives the output `Uri` -> Calls `BackupManager.backupDatabase(context, uri)`.
3.  `BackupManager` executes `PRAGMA wal_checkpoint(FULL)` -> Room commits WAL logs to main DB file -> Binary stream reads database file -> Output stream writes directly to selected Drive path.
