---
name: release-builder
description: Automates compiling, signing, and archiving production Android builds (.apk and .aab) to the .build-outputs folder. Triggered by /release-builder.
---

# Goal
Automate compile, signing, and packaging for production binary distributions (.apk and .aab) in KharchaDekh, targeting the `.build-outputs/` folder.

# Prerequisites
Ensure Gradle configurations and keystore files (`debug.keystore` or `my-upload-key.jks`) are present in the project root.

# Operational Sequence
1. Set the signing key passwords as environment variables (default is "password" if omitted):
   - `$env:STORE_PASSWORD="password"`
   - `$env:KEY_PASSWORD="password"`
2. Run the helper execution script using Python:
   ```powershell
   python .agent/skills/release-builder/scripts/build_release.py
   ```
3. The script will automatically:
   - Parse the current `versionName` from `app/build.gradle.kts`.
   - Run the Gradle tasks `assembleRelease` and `bundleRelease` using standard build systems.
   - Copy the resulting `.apk` and `.aab` to `.build-outputs/kd_<versionName>.apk` and `.build-outputs/kd_<versionName>.aab`.
