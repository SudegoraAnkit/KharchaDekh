# Requirements & Use Cases: KharchaDekh

This document details the functional scope, compliance requirements, and specific use cases I defined to build the application.

---

## 1. Functional Requirements

### A. Automation & Notification Parsing
*   **Req 1.1**: The app must run a background service that captures incoming notifications from registered financial packages (e.g. UPI, NetBanking, card alerts).
*   **Req 1.2**: Notification text must be parsed locally using regular expressions to extract transaction amount, type (DEBIT or CREDIT), and merchant details.
*   **Req 1.3**: Parsed notification data must be saved to the database as "Pending" transactions to prevent automatic categorization without user verification.
*   **Req 1.4**: Users must have the option to simulate notification parsing inside settings for testing without requiring real SMS alerts.

### B. Expense Dashboard & Insights (Feed)
*   **Req 2.1**: The dashboard must display real-time metric indicators for the current monthly cycle: Total Outflow, Budget Left, and Daily Spends Average.
*   **Req 2.2**: The dashboard must display a color-coded percentage breakdown of categorized expenses.
*   **Req 2.3**: The dashboard must display a prominent "Action Required" section listing all unfinalized SMS alerts.
*   **Req 2.4**: Ledger lists must distinguish incoming credits from debits using clear visual markers that are highly readable in both light and dark themes.

### C. Ledger Customization & Budgets
*   **Req 3.1**: Users must be able to log manual transactions (e.g., cash spent) with custom dates, payment methods, and notes.
*   **Req 3.2**: Users must be able to create custom categories with tailored icons and assign spending budget limits.
*   **Req 3.3**: The dashboard must trace budget limits and show alert bars when a category spends exceed 80% (close to limit) or 100% (exceeded).

### D. Reporting & Backup Interoperability
*   **Req 4.1**: Users must be able to export their transaction lists to standard Excel-compatible CSV files.
*   **Req 4.2**: Users must be able to generate and share structured A4 PDF statements containing summary statistics and transaction tables.
*   **Req 4.3**: The database backup and restore functions must interface with native Android Storage Access Framework (SAF) pickers to allow syncing to personal Google Drive, OneDrive, or local drives without cloud account configuration.

### E. Background Reminders
*   **Req 5.1**: The app must schedule 4 daily periodic background alarms (Morning 9:00 AM, Midday 1:30 PM, Afternoon 5:30 PM, and Night review).
*   **Req 5.2**: The first three reminders must be conditional (only trigger if there are unfinalized transactions). The night reminder must trigger unconditionally to prompt a final daily ledger check.

---

## 2. Compliance & Privacy Constraints
*   **Comp 1**: **Digital Personal Data Protection (DPDP) Act Compliance**: No personal financial identifiers, text alerts, or logs may be uploaded to external servers. All operations must run offline.
*   **Comp 2**: **Right to Be Forgotten**: The settings menu must expose an option to revoke consent and completely wipe the local database sandbox.

---

## 3. Core Use Cases

### Use Case 1: Automated SMS Notification Reconciling
1.  The user receives a notification from Google Pay: *"Rs. 150.00 spent at Swiggy via HDFC Bank"*
2.  The background `TransactionNotificationListener` captures the notification, matches the HDFC debit pattern, and extracts `Amount = 150`, `Merchant = Swiggy`, `Type = DEBIT`, and `Method = UPI`.
3.  The transaction is saved to the local database with `isPending = true`.
4.  The user opens the app and sees a red "NEW ALERT" banner. Under "Action Required", they tap "Categorize" on the Swiggy transaction card.
5.  The app displays the Enrichment Screen. The user selects the "Food" category, adds notes (optional), and hits "Save".
6.  The transaction updates to `isPending = false`, the red banner vanishes, and the Swiggy entry joins the finalized ledger list.

### Use Case 2: Manual Cash Log with Recurrence
1.  The user pays ₹500 in cash for local taxi services.
2.  The user taps the "Log Cash" tab.
3.  The user inputs: `Amount = 500`, `Category = Transport`, `Merchant = Auto Rickshaw`, and `Payment Method = Cash`.
4.  The user toggles "Designate as Recurring" and chooses "Monthly" (e.g. for a monthly transport pass).
5.  The user taps "Save".
6.  The transaction is recorded instantly, and a recurring task is created in the database. When the next month arrives, a new pending transaction is auto-generated in the user's dashboard feed for review.

### Use Case 3: Ledger Database Migration (Google Drive Backup)
1.  The user wants to transfer their expense history to a new phone.
2.  On the old device, the user opens "Settings" and taps "Backup".
3.  Android's Storage Access Framework file picker opens. The user selects their personal "Google Drive" sync folder and taps "Save".
4.  The app flushes Room database cache logs using a SQLite WAL checkpoint, zips the database binary, and writes it directly to the user's Drive folder.
5.  On the new device, the user logs in, installs the app, goes to "Settings", and taps "Restore".
6.  The user picks the backup file from Google Drive via the SAF file explorer.
7.  The app shuts down the database connection, overwrites the sandbox database, and restarts the app process. The entire history loads successfully on the new device.
