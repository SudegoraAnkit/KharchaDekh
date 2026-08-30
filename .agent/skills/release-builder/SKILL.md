---
name: release-builder
description: Automates compiling, signing, and archiving production Android builds (.apk and .aab) to the .build-outputs folder. Triggered by /release-builder.
---

# Goal
Automate compile, signing, and packaging for production binary distributions (.apk and .aab) in KharchaDekh, targeting the `.build-outputs/` folder.

# Versioning Principles
- **`versionCode` (Integer)**: Must **always increment by +1** on every new release build to ensure valid upgrades on Android devices and meet Google Play Store versioning criteria.
- **`versionName` (String)**: Reflects the product release version (e.g. `2.0.0.0`). Stays constant across build revisions within the same release cycle unless explicitly changed.

# Prerequisites
Ensure Gradle configurations and keystore files (`debug.keystore` or `my-upload-key.jks`) are present in the project root.

# Operational Sequence
1. Ensure `versionCode` in `app/build.gradle.kts` is incremented by 1 (e.g., from 20 to 21) while retaining the specified `versionName` (e.g., `2.0.0.0`).
2. Set the signing key passwords as environment variables (default is "password" if omitted):
   - `$env:STORE_PASSWORD="password"`
   - `$env:KEY_PASSWORD="password"`
3. Run the helper execution script using Python:
   ```powershell
   python .agent/skills/release-builder/scripts/build_release.py
   ```
4. The script will automatically:
   - Parse the current `versionName` and `versionCode` from `app/build.gradle.kts`.
   - Run the Gradle tasks `assembleRelease` and `bundleRelease` using standard build systems.
   - Copy the resulting `.apk` and `.aab` to `.build-outputs/kd_<versionName>.apk` and `.build-outputs/kd_<versionName>.aab`.

