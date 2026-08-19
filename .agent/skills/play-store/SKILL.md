---
name: play-store
description: Production release auditor for Google Play Console. Enforces developer policies, Android Vitals threshold checks, and App Store Optimization (ASO) rules. Triggered by /play-store.
---

# Goal
Audit KharchaDekh updates, metadata changes, and feature plans to ensure total compliance with modern Google Play developer distribution policies, preventing production blocks or unexpected store removal.

# Core Compliance Pillars
1. **Target API Mandates:** Ensure build profiles meet current platform floors. For 2026 releases, submissions must target Android 15 (API 35) minimum, transitioning strictly to Android 16 (API 36) by August 31, 2026.
2. **Android Vitals Thresholds:** Code changes must not push telemetry past Play Store "Bad Behavior" limits. Keep the user-perceived crash rate strictly under 1.09% and monitor ANR thresholds to protect App Store Optimization (ASO) rankings.
3. **Data Safety & Transparency:** Since KharchaDekh tracks finance data and utilizes a cloud backup mechanism, audit code for exact Data Safety disclosures. Ensure the app adheres to clear user disclosures and matches declaration forms.
4. **Store Metadata Restrictions:** Store listings must avoid promotional spam. Titles must be strictly 30 characters or fewer, short descriptions under 80 characters, and completely free of restricted buzzwords like "Best", "Free", or "#1".
5. **Release Notes Limits:** "What's new" release notes for each update submission must be strictly under 300 characters to satisfy Google Play Console constraints and prevent truncation on user devices.

# Output Format
- **Compliance Status:** Pass/Fail assessment against current Play Store terms.
- **Risk Analysis:** Specific evaluation of dangerous permissions, metadata traps, or Vitals risks.
- **Remediation Script:** Clear configuration, manifest, or string modifications required to clear review safely.