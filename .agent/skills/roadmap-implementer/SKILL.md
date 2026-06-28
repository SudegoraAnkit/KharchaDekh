---
name: roadmap-implementer
description: Guidelines and instructions for implementing features step-by-step from the KharchaDekh version roadmap (v1.1.0 to v2.0.0).
---

# KharchaDekh Roadmap Implementation Guide

Use this skill when the user requests to implement a specific version or milestone from the product roadmap.

---

## 1. Roadmap Version Specifications

### v1.1.0 — Local Grocery Lists & Drafts (Offline Utility)
*   **Goal**: Create list-making tools for grocery budgeting offline.
*   **Features**:
    *   `DRAFT_SCHEDULED` grocery list state that does not impact active monthly budgets.
    *   In-aisle item price inputs and sticky running total banner.
    *   "Mark as Paid" checkout button to finalize list to main ledger (prompting for payment method).
    *   Price estimate look-ups based on historical purchase data.
    *   Visual budget cap indicators (Green/Yellow/Red running totals).
    *   Draft list duplication, templates, and caching on screen changes to prevent data loss.

### v1.2.0 — Offline-First Multi-Currency
*   **Goal**: Expand tracking capabilities with automatic offline exchange rate look-ups.
*   **Features**:
    *   `ExchangeRates` Room table caching rates.
    *   Daily WorkManager task updating rates when on Wi-Fi.
    *   Settings layout switches to enable/disable multi-currency tracking.

### v1.3.0 — Cloud Backups & Guardrails
*   **Goal**: Safe, size-constrained cloud backups.
*   **Features**:
    *   Voluntary Google/Firebase Auth backed cloud backups of SQLite ledger records.
    *   GZIP backup compression and a strict 5 MB file size limit check.

### v1.4.0 — On-Device OCR & Reconciling
*   **Goal**: Reduce data entry friction locally.
*   **Features**:
    *   Google ML Kit OCR receipt scanning offline.
    *   Smart reconciling to merge manual logs with parsed notification logs.

### v1.5.0 — Predictive Budgeting & Advanced Visualizations
*   **Goal**: Category sorting automation and detailed analytics.
*   **Features**:
    *   On-device lightweight category classification.
    *   Interactive savings dials and cash outflow comparison bars.

### v2.0.0 — Group Sharing & E2EE Cloud Sync
*   **Goal**: Multi-user shared tracking and encrypted sync.
*   **Features**:
    *   Shared family/flatmate group budgets with permissions.
    *   End-to-End Encrypted (E2EE) database sync using personal cloud drives (GDrive/OneDrive).

---

## 2. Implementation & Integration Rules

### Rule A: Follow the Mandatory Execution Pipeline
For any version implementation, you must execute work in isolated phases:
1.  **Phase 1 (Context & Target)**: Identify the target files (e.g., Room entities, UI screens, viewmodels).
2.  **Phase 2 (Implementation Plan)**: Output a structured design plan detailing changes, and wait for the user to respond with "PROCEED" before modifying files.
3.  **Phase 3 (Modular Modifications)**: Apply clean, production-ready changes. No speculative code, commented-out sections, or `TODO` annotations.
4.  **Phase 4 (Compile & Verify)**: Run `./gradlew compileDebugSources --parallel` and test checks. Roll back changes immediately if compilation fails.

### Rule B: Privacy First
All features must maintain 100% user privacy in compliance with the DPDP Act 2023. No network telemetry, remote logging, or unencrypted external data sharing.

### Rule C: Version & Release Management
When finalizing a release milestone:
1.  Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2.  Update the footer version display in `SettingsScreen.kt`.
3.  Build both signed release APK and AAB binaries using production env variables:
    `$env:STORE_PASSWORD="password"; $env:KEY_PASSWORD="password"; .\gradlew.bat assembleRelease bundleRelease`
4.  Copy output binaries into the `.build-outputs/` folder, name them `kd_<version>.apk`/`kd_<version>.aab`, and append release summaries to `.build-outputs/release_notes.txt`.
