# Project Overview: KharchaDekh

## 1. Introduction & Core Philosophy
I built **KharchaDekh** to solve a personal pain point: managing my daily expenses without compromising my financial privacy. In India's fast-growing digital economy, almost every transaction triggers a notification or SMS. Existing personal finance apps scrape these messages and upload the contents to remote servers for processing. I found this invasive. 

My vision for KharchaDekh was simple: create a smart, automated expense tracker that runs **100% offline and on-device**. No cloud accounts, no remote database syncing, and no external tracking servers. Every transaction parsed, category calculated, and budget tracked happens strictly within the local application sandbox.

---

## 2. The Problem Statement
Many expense managers in India require extensive permissions to read, receive, and scrape broad SMS logs. They upload financial transactional data to build consumer credit risk profiles or sell targeted ads. 

Furthermore:
*   Users are uncomfortable giving apps open-ended read access to their entire SMS inbox, which contains sensitive personal messages and OTPs.
*   Integrating bank accounts through APIs or third-party screen-scraping services is fragile and frequently breaks.
*   Apps often function poorly or not at all when the device is offline or has a weak internet connection.

---

## 3. The Vision & Solution
I designed KharchaDekh to be an offline-first, compliant alternative:
1.  **On-Device Automation**: Use Android's `NotificationListenerService` to locally capture transaction alerts from banking, credit card, and UPI apps (Google Pay, Paytm, PhonePe, HDFC, SBI, etc.) in real time.
2.  **Privacy by Design**: Process notifications entirely locally using regex engines. The app conforms strictly to India's **Digital Personal Data Protection (DPDP) Act 2023**, ensuring all financial histories are kept private.
3.  **Hybrid Logging**: Offer a balance between automation (via notification alerts) and control (manual cash logs, transaction edit states, and recurring schedules).
4.  **Local Ledger Control**: Allow users to backup and restore their SQL database directly to their own personal Google Drive or OneDrive using Android's Storage Access Framework (SAF), bypassing the need for a proprietary developer-hosted sync server.

---

## 4. Technology Stack Selection
I selected the following stack to ensure high performance, robust data integrity, and a premium Material 3 user experience:

*   **Language**: Kotlin (for clean, type-safe, and asynchronous coroutine-driven code).
*   **UI Framework**: Jetpack Compose (enabling a fully declarative, reactive UI layer with premium animations and smooth transitions).
*   **Database**: Room Database (SQLite wrapper, configured with Write-Ahead Logging to prevent corruption and enable high-speed transactions).
*   **Background Jobs**: WorkManager (essential for scheduling battery-efficient, reliable periodic reminder notifications).
*   **File Interfacing**: Storage Access Framework (SAF) + FileProvider (enabling secure PDF/CSV exports and cloud sync without external SDKs).
